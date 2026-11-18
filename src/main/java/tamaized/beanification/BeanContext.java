package tamaized.beanification;

import org.jspecify.annotations.Nullable;
import tamaized.beanification.internal.BeanContextConfig;
import tamaized.beanification.processors.BeanAnnotationProcessorMetadata;
import tamaized.beanification.processors.IBeanProcessor;
import tamaized.beanification.processors.BeanProcessor;
import tamaized.beanification.scan.AnnotationScanData;
import tamaized.beanification.scan.ScanCondition;
import tamaized.beanification.util.LoggerWrapper;
import tamaized.beanification.util.SimpleMemoize;
import tamaized.beanification.util.SysoutLoggerWrapper;

import java.lang.annotation.ElementType;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class BeanContext extends AbstractBeanContext {

	static BeanContext INSTANCE = new BeanContext();

	@Nullable
	private static WeakReference<Object> LAST_INJECTED_INTO = null;

	@InternalAutowired
	private BeanContextConfig config;

	private final List<ContextConnector> connectors = new ArrayList<>();

	private final Map<BeanLifeCycle, List<IBeanProcessor>> beanProcessors = new HashMap<>();

	private final Map<BeanDefinition<?>, Object> connectorBeans = new HashMap<>();

	private LoggerWrapper logger = new SysoutLoggerWrapper();

	@Nullable
	private List<AnnotationScanData> cachedScanData;

	private BeanLifeCycle lifeCycle = BeanLifeCycle.Start;

	BeanAnnotationProcessorMetadata beanAnnotationProcessorMetadata = new BeanAnnotationProcessorMetadata();

	private BeanContext() {
		InternalBeanContext.injectInto(this);
	}

	/**
	 * Must be called before {@link #init(LoggerWrapper)}
	 */
	public static BeanContextConfig configure() {
		return INSTANCE.config;
	}

	public BeanLifeCycle getLifeCycle() {
		return lifeCycle;
	}

	/**
	 * Must be called before {@link #init(LoggerWrapper)}
	 */
	public static void registerConnectors(ContextConnector... connectors) {
		INSTANCE.connectors.addAll(List.of(connectors));
	}

	/**
	 * Should be called as early as possible to avoid null bean injections
	 */
	public static void init(LoggerWrapper logger) {
		INSTANCE.initInternal(logger, (_) -> {});
	}

	void initInternal(
		LoggerWrapper loggerWrapper,
		Consumer<InternalBeanRegistrar> extraRegister
	) {
		logger = loggerWrapper;
		final long ms = System.currentTimeMillis();
		logger.info("Starting Bean Context");
		if (isFrozen())
			throw new IllegalStateException("Bean Context already frozen");
		getBeans().clear();
		connectorBeans.clear();
		lifeCycle = BeanLifeCycle.Start;

		registerInternal(BeanContext.class, null, this);
		extraRegister.accept(this::registerInternal);
		connectors.forEach(connector -> connector.registerInternalBeans(this::registerConnectorBean));
		connectors.forEach(connector -> registerConnectorBean(connector.getClass(), null, connector));
		connectors.forEach(connector -> registerInternal(connector.getClass(), null, connector));
		connectorBeans.values().forEach(bean -> InternalBeanContext.injectInto(bean, connectorBeans));

		List<ScanCondition> conditions = connectors.stream().flatMap(connector -> connector.conditions().stream()).toList();
		List<AnnotationScanData> scanData = connectors.stream().map(connector -> connector.scan(conditions)).toList();

		AtomicReference<Object> currentInjection = new AtomicReference<>();

		try {
			logger.debug("Registering Bean annotation processors");
			beanProcessors.clear();
			scanData.stream()
				.flatMap(data -> data.filtered(ElementType.TYPE, BeanProcessor.class))
				.map(data -> {
					try {
						return Class.forName(data.member().metadata().clazz().className());
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				})
				.sorted(Comparator.comparingInt(c -> c.getAnnotation(BeanProcessor.class).priority()))
				.forEach(c -> {
					if (IBeanProcessor.class.isAssignableFrom(c)) {
						try {
							beanProcessors.computeIfAbsent(c.getAnnotation(BeanProcessor.class).value(), _ -> new ArrayList<>())
								.add(c.asSubclass(IBeanProcessor.class).getConstructor().newInstance());
						} catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
							throw new RuntimeException(ex);
						}
						logger.debug("Registered Bean processor: {}", c);
					} else {
						throw new RuntimeException("Bean processor must implement IBeanProcessor: " + c);
					}
				});

			beanProcessors.values().stream().flatMap(List::stream).forEach(processor -> InternalBeanContext.injectInto(processor, connectorBeans));

			lifeCycle = BeanLifeCycle.Gather;
			BeanLifeCycleContext lifeCycleContext = new BeanLifeCycleContext(
				Optional.of(new HashMap<>()),
				Optional.empty(),
				Optional.empty(),
				Optional.of(currentInjection),
				Optional.of(definition -> injectInternal(definition.type(), definition.name())),
				Optional.of(definition -> injectLazyUnchecked(definition.type(), definition.name())),
				Optional.of(this::injectFuzzyInternal),
				Optional.empty()
			);
			runAnnotationProcessor(beanProcessors, lifeCycle, lifeCycleContext, scanData);

			lifeCycle = BeanLifeCycle.Inspect;
			lifeCycleContext = new BeanLifeCycleContext(
				Optional.of(Collections.unmodifiableMap(lifeCycleContext.gather.orElseThrow())),
				Optional.of(new HashMap<>()),
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				Optional.empty()
			);
			runAnnotationProcessor(beanProcessors, lifeCycle, lifeCycleContext, scanData);

			lifeCycle = BeanLifeCycle.Validate;
			lifeCycleContext.dependencies().orElseThrow().replaceAll((k, v) -> Collections.unmodifiableList(v));
			lifeCycleContext = new BeanLifeCycleContext(
				lifeCycleContext.gather,
				Optional.of(Collections.unmodifiableMap(lifeCycleContext.dependencies.orElseThrow())),
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				Optional.empty()
			);
			runAnnotationProcessor(beanProcessors, lifeCycle, lifeCycleContext, scanData);

			lifeCycle = BeanLifeCycle.Construct;
			lifeCycleContext = new BeanLifeCycleContext(
				lifeCycleContext.gather,
				lifeCycleContext.dependencies,
				Optional.of((definition, bean) -> registerInternal(definition.type(), definition.name(), bean)),
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				Optional.empty()
			);
			runAnnotationProcessor(beanProcessors, lifeCycle, lifeCycleContext, scanData);

			freeze();

			lifeCycle = BeanLifeCycle.StaticInject;
			lifeCycleContext = new BeanLifeCycleContext(
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				Optional.of(currentInjection),
				Optional.of(definition -> injectChecked(definition.type(), definition.name()).orElse(null)),
				Optional.of(definition -> injectLazyUnchecked(definition.type(), definition.name())),
				Optional.of(this::injectFuzzyInternal),
				Optional.of(getBeans())
			);
			runAnnotationProcessor(beanProcessors, lifeCycle, lifeCycleContext, scanData);
			currentInjection.set(null);

			lifeCycle = BeanLifeCycle.Inject;
			lifeCycleContext = new BeanLifeCycleContext(
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				Optional.of(currentInjection),
				Optional.of(definition -> injectChecked(definition.type(), definition.name()).orElse(null)),
				Optional.of(definition -> injectLazyUnchecked(definition.type(), definition.name())),
				Optional.of(this::injectFuzzyInternal),
				Optional.of(getBeans())
			);
			runAnnotationProcessor(beanProcessors, lifeCycle, lifeCycleContext, scanData);
			currentInjection.set(null);

			lifeCycle = BeanLifeCycle.Finalize;
			lifeCycleContext = new BeanLifeCycleContext(
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				Optional.of(currentInjection),
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				Optional.of(getBeans())
			);
			runAnnotationProcessor(beanProcessors, lifeCycle, lifeCycleContext, scanData);

			lifeCycle = BeanLifeCycle.Complete;
			cachedScanData = scanData;

			logger.info("Bean Context loaded in {} ms", System.currentTimeMillis() - ms);
		} catch (Throwable e) {
			throwInjectionFailedException(currentInjection, e);
		}
	}

	private void runAnnotationProcessor(
		Map<BeanLifeCycle, List<IBeanProcessor>> beanProcessors,
		BeanLifeCycle lifeCycle,
		BeanLifeCycleContext lifeCycleContext,
		List<AnnotationScanData> scanData
	) throws Throwable {
		for (IBeanProcessor beanProcessor : beanProcessors.get(lifeCycle)) {
			if (INSTANCE.lifeCycle != BeanLifeCycle.Complete || config.loggingSettings().isInjectIntoEnabled())
				INSTANCE.logger.debug("Running processor {}", beanProcessor.getClass());
			beanProcessor.process(lifeCycleContext, scanData, beanAnnotationProcessorMetadata);
		}
	}

	private void throwInjectionFailedException(AtomicReference<Object> o, Throwable e) {
		throw new RuntimeException("Bean injection failed." + (o.get() == null ? "" : (" At: " + o)), e);
	}

	/**
	 * May be called in an object's Constructor to enable non-static {@link Autowired} and {@link Directory} annotations
	 */
	public static synchronized void injectInto(Object object) {
		if (LAST_INJECTED_INTO != null && LAST_INJECTED_INTO.get() != null && LAST_INJECTED_INTO.get() == object)
			return;
		LAST_INJECTED_INTO = new WeakReference<>(object);
		final long ms = System.currentTimeMillis();
		boolean loggingEnabled = INSTANCE.config.loggingSettings().isInjectIntoEnabled();
		if (loggingEnabled)
			INSTANCE.logger.debug("Processing {}", object);
		AtomicReference<Object> curInj = new AtomicReference<>();
		try {
			List<AnnotationScanData> scanData = INSTANCE.cachedScanData;
			if (scanData == null) {
				throw new IllegalStateException("BeanContext.init() must be ran first before calling BeanContext.injectInto(obj)");
			}
			INSTANCE.runAnnotationProcessor(
				INSTANCE.beanProcessors,
				BeanLifeCycle.Inject,
				new BeanLifeCycleContext(
					Optional.empty(),
					Optional.empty(),
					Optional.empty(),
					Optional.of(curInj),
					Optional.of(definition -> INSTANCE.injectChecked(definition.type(), definition.name()).orElse(null)),
					Optional.of(definition -> injectLazyUnchecked(definition.type(), definition.name())),
					Optional.of(INSTANCE::injectFuzzyInternal),
					Optional.of(Map.of(new BeanDefinition<>(object.getClass(), null), object))
				),
				scanData
			);
		} catch (Throwable e) {
			INSTANCE.throwInjectionFailedException(curInj, e);
		}
		if (loggingEnabled)
			INSTANCE.logger.debug("Finished processing {} in {} ms", object, System.currentTimeMillis() - ms);
	}

	@Override
	protected void registerInternal(Class<?> type, @Nullable String name, Object instance) {
		logger.debug("Registering Bean {} {}", type, name == null ? "" : ("with name: " + name));
		super.registerInternal(type, name, instance);
	}

	private void registerConnectorBean(Class<?> type, @Nullable String name, Object instance) {
		logger.debug("Registering connector Bean {} {}", type, name == null ? "" : ("with name: " + name));
		BeanDefinition<?> definition = new BeanDefinition<>(type, name);
		if (connectorBeans.putIfAbsent(definition, instance) != null)
			throw new RuntimeException("Attempted to register a duplicate connector Bean. " + definition);
	}

	@Override
	protected boolean canAccessUnfrozen() {
		return lifeCycle == BeanLifeCycle.Construct;
	}

	private <T> Optional<T> injectChecked(Class<T> type) {
		return injectChecked(type, null);
	}

	private <T> Optional<T> injectChecked(Class<T> type, @Nullable String name) {
		if (INSTANCE.getBeans().containsKey(new BeanDefinition<>(type, name)))
			return Optional.of(INSTANCE.injectInternal(type, name));
		return Optional.empty();
	}

	public static <T> T inject(Class<T> type) {
		return inject(type, null);
	}

	public static <T> T inject(Class<T> type, @Nullable String name) {
		return INSTANCE.injectInternal(type, name);
	}

	public static <T> Supplier<T> injectLazy(Class<T> type) {
		return injectLazy(type, null);
	}

	public static <T> Supplier<T> injectLazy(Class<T> type, @Nullable String name) {
		return SimpleMemoize.memoize(() -> INSTANCE.injectInternal(type, name));
	}

	private static <T> Supplier<Object> injectLazyUnchecked(Class<T> type, @Nullable String name) {
		return SimpleMemoize.memoize(() -> INSTANCE.injectInternal(type, name));
	}

	public record BeanLifeCycleContext(
		Optional<Map<BeanDefinition<?>, ThrowingSupplier<Object>>> gather,
		Optional<Map<BeanDefinition<?>, List<BeanDefinition<?>>>> dependencies,
		Optional<BiConsumer<BeanDefinition<?>, Object>> register,
		Optional<AtomicReference<Object>> currentInjection,
		Optional<Function<BeanDefinition<?>, Object>> strictInjector,
		Optional<Function<BeanDefinition<?>, Supplier<Object>>> lazyInjector,
		Optional<Function<Class<?>, List<?>>> fuzzyInjector,
		Optional<Map<BeanDefinition<?>, Object>> beansToProcess
	) {

	}

	@FunctionalInterface
	public interface ThrowingSupplier<R> {

		R get() throws Throwable;

	}

}
