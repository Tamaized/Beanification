package tamaized.beanification;

import com.google.common.base.Suppliers;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.apache.commons.lang3.function.TriConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tamaized.beanification.internal.AdditionalModuleNamesProvider;
import tamaized.beanification.internal.BeanContextConfig;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.processors.BeanAnnotationProcessorMetadata;
import tamaized.beanification.processors.IBeanProcessor;
import tamaized.beanification.processors.BeanProcessor;

import javax.annotation.Nullable;
import java.lang.annotation.ElementType;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class BeanContext extends AbstractBeanContext {

	private static final Logger LOGGER = LogManager.getLogger(BeanContext.class);

	static BeanContext INSTANCE = new BeanContext();

	@Nullable
	private static WeakReference<Object> LAST_INJECTED_INTO = null;

	@InternalAutowired
	private BeanContextConfig config;

	@InternalAutowired
	private AdditionalModuleNamesProvider additionalModuleNamesProvider;

	@InternalAutowired
	private DistAnnotationRetriever distAnnotationRetriever;

	private BeanLifeCycle lifeCycle = BeanLifeCycle.Start;
	private final Map<BeanLifeCycle, List<IBeanProcessor>> beanProcessors = new HashMap<>();

	@Nullable
	private ContainerContext currentContainerContext = null;

	BeanAnnotationProcessorMetadata beanAnnotationProcessorMetadata = new BeanAnnotationProcessorMetadata();

	private BeanContext() {
		InternalBeanContext.injectInto(this);
	}

	/**
	 * Must be called before {@link #init(String)} to have any effect.
	 */
	public static BeanContextConfig configure() {
		return INSTANCE.config;
	}

	public BeanLifeCycle getLifeCycle() {
		return lifeCycle;
	}

	/**
	 * Should be called as early as possible to avoid null bean injections
	 */
	public static void init(String modid) {
		INSTANCE.initInternal(modid, (_) -> {});
	}

	void initInternal(String modid, Consumer<TriConsumer<Class<?>, @org.jspecify.annotations.Nullable String, Object>> extraRegister) {
		final long ms = System.currentTimeMillis();
		LOGGER.info("Starting Bean Context");
		if (isFrozen())
			throw new IllegalStateException("Bean Context already frozen");
		getBeans().clear();
		lifeCycle = BeanLifeCycle.Start;

		registerInternal(BeanContext.class, null, this);
		extraRegister.accept(this::registerInternal);

		ModContainer modContainer = ModList.get().getModContainerById(modid).orElseThrow(() -> new RuntimeException("Where is " + modid + "???!"));

		if (modContainer.getEventBus() == null)
			throw new RuntimeException("Mod EventBus is null");

		ModFileScanData scanData = modContainer.getModInfo().getOwningFile().getFile().getScanResult();
		AtomicReference<Object> currentInjection = new AtomicReference<>();

		additionalModuleNamesProvider.setup(config.scanSettings().getAdditionalComponentScanModuleNames());

		try {
			LOGGER.debug("Registering Bean annotation processors");
			beanProcessors.clear();
			for (Iterator<? extends Class<?>> it = distAnnotationRetriever.retrieve(modid.equals("beanification"), scanData, ElementType.TYPE, BeanProcessor.class).map(a -> {
				try {
					return Class.forName(a.clazz().getClassName());
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}).sorted(Comparator.comparingInt(c -> c.getAnnotation(BeanProcessor.class).priority())).iterator(); it.hasNext(); ) {
				Class<?> c = it.next();
				if (IBeanProcessor.class.isAssignableFrom(c)) {
					beanProcessors.computeIfAbsent(c.getAnnotation(BeanProcessor.class).value(), k -> new ArrayList<>())
						.add((IBeanProcessor) c.getConstructor().newInstance());
					LOGGER.debug("Registered Bean processor: {}", c);
				} else {
					throw new RuntimeException("Bean processor must implement IBeanProcessor: " + c);
				}
			}

			beanProcessors.values().stream().flatMap(List::stream).forEach(InternalBeanContext::injectInto);

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
			runAnnotationProcessor(beanProcessors, lifeCycle, lifeCycleContext, modContainer, scanData);

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
			runAnnotationProcessor(beanProcessors, lifeCycle, lifeCycleContext, modContainer, scanData);

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
			runAnnotationProcessor(beanProcessors, lifeCycle, lifeCycleContext, modContainer, scanData);

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
			runAnnotationProcessor(beanProcessors, lifeCycle, lifeCycleContext, modContainer, scanData);

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
			runAnnotationProcessor(beanProcessors, lifeCycle, lifeCycleContext, modContainer, scanData);
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
			runAnnotationProcessor(beanProcessors, lifeCycle, lifeCycleContext, modContainer, scanData);
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
			runAnnotationProcessor(beanProcessors, lifeCycle, lifeCycleContext, modContainer, scanData);

			lifeCycle = BeanLifeCycle.Complete;
			currentContainerContext = new ContainerContext(modContainer, scanData);

			LOGGER.info("Bean Context loaded in {} ms", System.currentTimeMillis() - ms);
		} catch (Throwable e) {
			throwInjectionFailedException(currentInjection, e);
		}
	}

	private void runAnnotationProcessor(
		Map<BeanLifeCycle, List<IBeanProcessor>> beanProcessors,
		BeanLifeCycle lifeCycle,
		BeanLifeCycleContext lifeCycleContext,
		ModContainer modContainer,
		ModFileScanData scanData
	) throws Throwable {
		for (IBeanProcessor beanProcessor : beanProcessors.get(lifeCycle)) {
			if (INSTANCE.lifeCycle != BeanLifeCycle.Complete || config.loggingSettings().isInjectIntoEnabled())
				LOGGER.debug("Running processor {}", beanProcessor.getClass());
			beanProcessor.process(lifeCycleContext, modContainer, scanData, beanAnnotationProcessorMetadata);
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
			LOGGER.debug("Processing {}", object);
		AtomicReference<Object> curInj = new AtomicReference<>();
		try {
			ContainerContext context = INSTANCE.currentContainerContext;
			if (context == null) {
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
				context.container(),
				context.scanData()
			);
		} catch (Throwable e) {
			INSTANCE.throwInjectionFailedException(curInj, e);
		}
		if (loggingEnabled)
			LOGGER.debug("Finished processing {} in {} ms", object, System.currentTimeMillis() - ms);
	}

	@Override
	protected void registerInternal(Class<?> type, @org.jetbrains.annotations.Nullable String name, Object instance) {
		LOGGER.debug("Registering Bean {} {}", type, name == null ? "" : ("with name: " + name));
		super.registerInternal(type, name, instance);
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

	private <T> Optional<Supplier<T>> injectCheckedLazy(Class<T> type, @Nullable String name) {
		if (INSTANCE.getBeans().containsKey(new BeanDefinition<>(type, name)))
			return Optional.of(injectLazy(type, name));
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
		return Suppliers.memoize(() -> INSTANCE.injectInternal(type, name));
	}

	private static <T> Supplier<Object> injectLazyUnchecked(Class<T> type, @Nullable String name) {
		return Suppliers.memoize(() -> INSTANCE.injectInternal(type, name));
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

	private record ContainerContext(
		ModContainer container,
		ModFileScanData scanData
	) {

	}

}
