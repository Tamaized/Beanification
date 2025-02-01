package tamaized.beanification;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tamaized.beanification.internal.BeanContextConfig;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.processors.AnnotationDataPostProcessor;
import tamaized.beanification.processors.AnnotationDataPreProcessor;
import tamaized.beanification.processors.AnnotationDataProcessor;
import tamaized.beanification.processors.BeanProcessor;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class BeanContext extends AbstractBeanContext {

	private static final Logger LOGGER = LogManager.getLogger(BeanContext.class);

	static BeanContext INSTANCE = new BeanContext();

	@Nullable
	private static WeakReference<Object> LAST_INJECTED_INTO = null;

	@InternalAutowired
	private BeanContextConfig config;

	@InternalAutowired
	private DistAnnotationRetriever distAnnotationRetriever;

	private final Map<BeanDefinition<?>, List<BeanDefinition<?>>> beanDependencies = new HashMap<>();

	private final BeanContextRegistrar beanContextRegistrar = new BeanContextRegistrar();
	private final BeanContextInternalDependencyTreeAccumulator beanContextInternalDependencyTreeAccumulator = new BeanContextInternalDependencyTreeAccumulator();
	private final BeanContextInternalRegistrar beanContextInternalRegistrar = new BeanContextInternalRegistrar();
	private final BeanContextInternalInjector beanContextInternalInjector = new BeanContextInternalInjector();

	@Nullable
	private ContainerContext currentContainerContext = null;

	private BeanContext() {
		InternalBeanContext.injectInto(this);
	}

	/**
	 * Must be called before {@link #init()} to have any effect.
	 */
	public static BeanContextConfig configure() {
		return INSTANCE.config;
	}

	/**
	 * @see #init(Consumer)
	 */
	public static void init() {
		init(null);
	}

	/**
	 * Should be called as early as possible to avoid null bean injections
	 */
	public static void init(@Nullable Consumer<BeanContextRegistrar> context) {
		INSTANCE.initInternal(context);
	}

	void initInternal(@Nullable Consumer<BeanContextRegistrar> context) {
		final long ms = System.currentTimeMillis();
		LOGGER.info("Starting Bean Context");
		if (isFrozen())
			throw new IllegalStateException("Bean Context already frozen");
		getBeans().clear();

		registerInternal(BeanContext.class, null, this);

		if (context != null)
			context.accept(beanContextRegistrar);

		ModContainer modContainer = ModLoadingContext.get().getActiveContainer();

		if (modContainer.getEventBus() == null)
			throw new RuntimeException("Mod EventBus is null");

		ModFileScanData scanData = modContainer.getModInfo().getOwningFile().getFile().getScanResult();
		AtomicReference<Object> currentInjection = new AtomicReference<>();

		try {
			LOGGER.debug("Registering Bean annotation processors");
			List<AnnotationDataPreProcessor> annotationDataPreProcessors = new ArrayList<>();
			List<AnnotationDataProcessor> annotationDataProcessors = new ArrayList<>();
			List<AnnotationDataPostProcessor> annotationDataPostProcessors = new ArrayList<>();

			for (Iterator<? extends Class<?>> it = distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, BeanProcessor.class)
				.map(a -> {
					try {
						return Class.forName(a.clazz().getClassName());
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				})
				.sorted(Comparator.comparingInt(c -> c.getAnnotation(BeanProcessor.class).priority()))
				.iterator(); it.hasNext(); ) {
				Class<?> c = it.next();
				if (AnnotationDataPreProcessor.class.isAssignableFrom(c)) {
					annotationDataPreProcessors.add((AnnotationDataPreProcessor) c.getConstructor().newInstance());
					LOGGER.debug("Registered Bean annotation pre processor: {}", c);
				} else if (AnnotationDataProcessor.class.isAssignableFrom(c)) {
					annotationDataProcessors.add((AnnotationDataProcessor) c.getConstructor().newInstance());
					LOGGER.debug("Registered Bean annotation processor: {}", c);
				} else if (AnnotationDataPostProcessor.class.isAssignableFrom(c)) {
					annotationDataPostProcessors.add((AnnotationDataPostProcessor) c.getConstructor().newInstance());
					LOGGER.debug("Registered Bean annotation post processor: {}", c);
				}
			}

			annotationDataPreProcessors.forEach(InternalBeanContext::injectInto);
			annotationDataProcessors.forEach(InternalBeanContext::injectInto);
			annotationDataPostProcessors.forEach(InternalBeanContext::injectInto);

			for (AnnotationDataPreProcessor annotationDataPreProcessor : annotationDataPreProcessors) {
				LOGGER.debug("Running pre processor {}", annotationDataPreProcessor.getClass());
				annotationDataPreProcessor.process(beanContextInternalDependencyTreeAccumulator, modContainer, scanData);
			}

			for (AnnotationDataProcessor annotationDataProcessor : annotationDataProcessors) {
				LOGGER.debug("Running processor {}", annotationDataProcessor.getClass());
				annotationDataProcessor.process(beanContextInternalRegistrar, modContainer, scanData);
			}

			beanDependencies.clear();
			freeze();

			for (AnnotationDataPostProcessor annotationDataPostProcessor : annotationDataPostProcessors) {
				LOGGER.debug("Running instanced post processor {}", annotationDataPostProcessor.getClass());
				for (Object bean : getBeans().values()) {
					annotationDataPostProcessor.process(beanContextInternalInjector, modContainer, scanData, bean, currentInjection);
				}
			}

			currentInjection.set(null);

			for (AnnotationDataPostProcessor annotationDataPostProcessor : annotationDataPostProcessors) {
				LOGGER.debug("Running static post processor {}", annotationDataPostProcessor.getClass());
				annotationDataPostProcessor.process(beanContextInternalInjector, modContainer, scanData, currentInjection);
			}

			currentInjection.set(null);

			currentContainerContext = new ContainerContext(modContainer, scanData, annotationDataPreProcessors, annotationDataProcessors, annotationDataPostProcessors);

			LOGGER.info("Bean Context loaded in {} ms", System.currentTimeMillis() - ms);
		} catch (Throwable e) {
			throwInjectionFailedException(currentInjection, e);
		}
	}

	private void throwInjectionFailedException(AtomicReference<Object> o, Throwable e) {
		throw new RuntimeException("Bean injection failed." + (o.get() == null ? "" : (" At: " + o)), e);
	}

	private void runAnnotationDataPostProcessors(Object o, AtomicReference<Object> curInj) throws Throwable {
		Objects.requireNonNull(currentContainerContext);
		for (AnnotationDataPostProcessor annotationDataPostProcessor : currentContainerContext.annotationDataPostProcessors) {
			annotationDataPostProcessor.process(beanContextInternalInjector, currentContainerContext.container, currentContainerContext.scanData, o, curInj);
		}
	}

	/**
	 * May be called in an object's Constructor to enable non-static {@link Autowired} annotations
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
			INSTANCE.runAnnotationDataPostProcessors(object, curInj);
		} catch (Throwable e) {
			INSTANCE.throwInjectionFailedException(curInj, e);
		}
		if (loggingEnabled)
			LOGGER.debug("Finished processing {} in {} ms", object, System.currentTimeMillis() - ms);
	}

	private boolean classOrSuperHasAnnotation(Class<?> c, Class<? extends Annotation> a) {
		return c.isAnnotationPresent(a) || (c.getSuperclass() instanceof Class<?> s && classOrSuperHasAnnotation(s, a));
	}

	@Override
	protected void registerInternal(Class<?> type, @org.jetbrains.annotations.Nullable String name, Object instance) {
		LOGGER.debug("Registering Bean {} {}", type, name == null ? "" : ("with name: " + name));
		super.registerInternal(type, name, instance);
	}

	public static <T> T inject(Class<T> type) {
		return inject(type, null);
	}

	public static <T> T inject(Class<T> type, @Nullable String name) {
		return INSTANCE.injectInternal(type, name);
	}

	public static <T> Lazy<T> injectLazy(Class<T> type) {
		return injectLazy(type, null);
	}

	public static <T> Lazy<T> injectLazy(Class<T> type, @Nullable String name) {
		return Lazy.of(() -> INSTANCE.injectInternal(type, name));
	}

	public final class BeanContextRegistrar {

		private BeanContextRegistrar() {

		}

		public <T> void register(Class<T> type, T instance) {
			register(type, null, instance);
		}

		public <T> void register(Class<T> type, @Nullable String name, T instance) {
			BeanContext.this.registerInternal(type, name, instance);
		}

	}

	public final class BeanContextInternalDependencyTreeAccumulator {

		private BeanContextInternalDependencyTreeAccumulator() {

		}

		public void addDependency(Class<?> type, @Nullable String name, Class<?> depType, @Nullable String depName) {
			LOGGER.info("Bean ({}{}) depends on ({}{})", type, name == null ? "" : ":".concat(name), depType, depName == null ? "" : ":".concat(depName));
			BeanDefinition<?> key = new BeanDefinition<>(type, name);
			List<BeanDefinition<?>> deps = BeanContext.this.beanDependencies.getOrDefault(key, new ArrayList<>());
			deps.add(new BeanDefinition<>(depType, depName));
			BeanContext.this.beanDependencies.put(key, deps);
		}

	}

	public final class BeanContextInternalRegistrar {

		private BeanContextInternalRegistrar() {

		}

		public List<BeanDefinition<?>> getDependencies(Class<?> type, @Nullable String name) {
			return BeanContext.this.beanDependencies.getOrDefault(new BeanDefinition<>(type, name), new ArrayList<>());
		}

		public Object getUnfrozenBean(BeanDefinition<?> definition) {
			return BeanContext.this.getBeans().get(definition);
		}

		public void register(Class<?> type, @Nullable String name, Object instance) {
			BeanContext.this.registerInternal(type, name, instance);
		}

	}

	public final class BeanContextInternalInjector {

		private BeanContextInternalInjector() {

		}

		public <T> T inject(Class<T> type, @Nullable String name) {
			return BeanContext.this.injectInternal(type, name);
		}

		public boolean contains(Class<?> type, @Nullable String name) {
			return BeanContext.this.getBeans().containsKey(new BeanDefinition<>(type, name));
		}

	}

	private record ContainerContext(
		ModContainer container,
		ModFileScanData scanData,
		List<AnnotationDataPreProcessor> annotationDataPreProcessors,
		List<AnnotationDataProcessor> annotationDataProcessors,
		List<AnnotationDataPostProcessor> annotationDataPostProcessors
	) {

	}

}
