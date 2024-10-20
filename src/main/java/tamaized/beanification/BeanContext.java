package tamaized.beanification;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.processors.AnnotationDataPostProcessor;
import tamaized.beanification.processors.AnnotationDataProcessor;
import tamaized.beanification.processors.BeanProcessor;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class BeanContext extends AbstractBeanContext {

	private static final Logger LOGGER = LogManager.getLogger(BeanContext.class);

	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
	private static final Field EntityRenderDispatcher_renderers = ObfuscationReflectionHelper.findField(EntityRenderDispatcher.class, "renderers");
	private static final Field BlockEntityRenderDispatcher_renderers = ObfuscationReflectionHelper.findField(BlockEntityRenderDispatcher.class, "renderers");
	private static final MethodHandle handle_EntityRenderDispatcher_renderers;
	private static final MethodHandle handle_BlockEntityRenderDispatcher_renderers;

	static {
		MethodHandle tmp_handle_EntityRenderDispatcher_renderers = null;
		MethodHandle tmp_handle_BlockEntityRenderDispatcher_renderers = null;

		try {
			tmp_handle_EntityRenderDispatcher_renderers = LOOKUP.unreflectGetter(EntityRenderDispatcher_renderers);
			tmp_handle_BlockEntityRenderDispatcher_renderers = LOOKUP.unreflectGetter(BlockEntityRenderDispatcher_renderers);
		} catch (IllegalAccessException e) {
			LOGGER.error("Exception", e);
		}

		if (tmp_handle_EntityRenderDispatcher_renderers == null || tmp_handle_BlockEntityRenderDispatcher_renderers == null) {
			throw new RuntimeException("Could not construct BeanContext");
		}

		handle_EntityRenderDispatcher_renderers = tmp_handle_EntityRenderDispatcher_renderers;
		handle_BlockEntityRenderDispatcher_renderers = tmp_handle_BlockEntityRenderDispatcher_renderers;
	}

	static BeanContext INSTANCE = new BeanContext();

	@InternalAutowired
	private DistAnnotationRetriever distAnnotationRetriever;

	private final BeanContextRegistrar beanContextRegistrar = new BeanContextRegistrar();
	private final BeanContextInternalRegistrar beanContextInternalRegistrar = new BeanContextInternalRegistrar();
	private final BeanContextInternalInjector beanContextInternalInjector = new BeanContextInternalInjector();

	private BeanContext() {
		InternalBeanContext.injectInto(this);
	}

	/**
	 * @see #init(String, Consumer)
	 */
	public static void init(final String modid) {
		init(modid, null);
	}

	/**
	 * Should be called as early as possible to avoid null bean injections
	 */
	public static void init(final String modid, @Nullable Consumer<BeanContextRegistrar> context) {
		INSTANCE.initInternal(modid, context, false);
	}

	@SuppressWarnings("SameParameterValue")
	void initInternal(final String modid, @Nullable Consumer<BeanContextRegistrar> context, boolean forceInjectRegistries) {
		final long ms = System.currentTimeMillis();
		LOGGER.info("Starting Bean Context");
		if (isFrozen())
			throw new IllegalStateException("Bean Context already frozen");
		getBeans().clear();

		registerInternal(BeanContext.class, null, this);

		if (context != null)
			context.accept(beanContextRegistrar);

		ModContainer modContainer = ModList.get().getModContainerById(modid).orElseThrow();
		ModFileScanData scanData = modContainer.getModInfo().getOwningFile().getFile().getScanResult();
		AtomicReference<Object> currentInjection = new AtomicReference<>();
		try {
			LOGGER.debug("Registering Bean annotation processors");
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
				if (AnnotationDataProcessor.class.isAssignableFrom(c)) {
					annotationDataProcessors.add((AnnotationDataProcessor) c.getConstructor().newInstance());
					LOGGER.debug("Registered Bean annotation processor: {}", c);
				} else if (AnnotationDataPostProcessor.class.isAssignableFrom(c)) {
					annotationDataPostProcessors.add((AnnotationDataPostProcessor) c.getConstructor().newInstance());
					LOGGER.debug("Registered Bean annotation post processor: {}", c);
				}
			}

			annotationDataProcessors.forEach(InternalBeanContext::injectInto);
			annotationDataPostProcessors.forEach(InternalBeanContext::injectInto);

			for (AnnotationDataProcessor annotationDataProcessor : annotationDataProcessors) {
				LOGGER.debug("Running processor {}", annotationDataProcessor.getClass());
				annotationDataProcessor.process(beanContextInternalRegistrar, modContainer, scanData);
			}

			freeze();

			for (Object bean : getBeans().values()) {
				for (AnnotationDataPostProcessor annotationDataPostProcessor : annotationDataPostProcessors) {
					annotationDataPostProcessor.process(beanContextInternalInjector, modContainer, scanData, bean, currentInjection);
				}
			}

			currentInjection.set(null);

			for (AnnotationDataPostProcessor annotationDataPostProcessor : annotationDataPostProcessors) {
				LOGGER.debug("Running post processor {}", annotationDataPostProcessor.getClass());
				annotationDataPostProcessor.process(beanContextInternalInjector, modContainer, scanData, currentInjection);
			}

			currentInjection.set(null);

			// @Mod
			Objects.requireNonNull(modContainer.getEventBus()).addListener(ProcessBeanAnnotationsEvent.class, event -> handleProcessBeanAnnotationsEvent(event, modContainer, scanData, annotationDataPostProcessors));
			// Registries
			Objects.requireNonNull(modContainer.getEventBus()).addListener(FMLCommonSetupEvent.class, event -> injectRegistries(modContainer, scanData, annotationDataPostProcessors));
			// Registries (Data Gen)
			Objects.requireNonNull(modContainer.getEventBus()).addListener(EventPriority.HIGHEST, GatherDataEvent.class, event -> injectRegistries(modContainer, scanData, annotationDataPostProcessors));
			// Renderers (Entity, BlockEntity)
			Objects.requireNonNull(modContainer.getEventBus()).addListener(EventPriority.LOWEST, RegisterClientReloadListenersEvent.class,
				event -> event.registerReloadListener((ResourceManagerReloadListener) manager -> injectRenderers(modContainer, scanData, annotationDataPostProcessors))
			);

			if (forceInjectRegistries)
				injectRegistries(modContainer, scanData, annotationDataPostProcessors);

			LOGGER.info("Bean Context loaded in {} ms", System.currentTimeMillis() - ms);
		} catch (Throwable e) {
			throwInjectionFailedException(currentInjection, e);
		}
	}

	private void throwInjectionFailedException(AtomicReference<Object> o, Throwable e) {
		throw new RuntimeException("Bean injection failed." + (o.get() == null ? "" : (" At: " + o)), e);
	}

	private void runAnnotationDataPostProcessors(Object o, ModContainer modContainer, ModFileScanData scanData, List<AnnotationDataPostProcessor> annotationDataPostProcessors, AtomicReference<Object> curInj) throws Throwable {
		for (AnnotationDataPostProcessor annotationDataPostProcessor : annotationDataPostProcessors) {
			annotationDataPostProcessor.process(beanContextInternalInjector, modContainer, scanData, o, curInj);
		}
	}

	private void handleProcessBeanAnnotationsEvent(ProcessBeanAnnotationsEvent event, ModContainer modContainer, ModFileScanData scanData, List<AnnotationDataPostProcessor> annotationDataPostProcessors) {
		final long ms = System.currentTimeMillis();
		LOGGER.debug("Processing {}", event.getObjectToProcess());
		AtomicReference<Object> curInj = new AtomicReference<>();
		try {
			runAnnotationDataPostProcessors(event.getObjectToProcess(), modContainer, scanData, annotationDataPostProcessors, curInj);
		} catch (Throwable e) {
			throwInjectionFailedException(curInj, e);
		}
		LOGGER.debug("Finished processing {} in {} ms", event.getObjectToProcess(), System.currentTimeMillis() - ms);
	}

	private void injectRegistries(ModContainer modContainer, ModFileScanData scanData, List<AnnotationDataPostProcessor> annotationDataPostProcessors) {
		final long ms = System.currentTimeMillis();
		LOGGER.debug("Processing registry objects");
		AtomicReference<Object> curInj = new AtomicReference<>();
		BuiltInRegistries.REGISTRY.holders().flatMap(r -> r.value().holders()).forEach(holder -> {
			try {
				Object o = holder.value();
				if (classOrSuperHasAnnotation(o.getClass(), Configurable.class)) {
					runAnnotationDataPostProcessors(o, modContainer, scanData, annotationDataPostProcessors, curInj);
				}
			} catch (Throwable e) {
				throwInjectionFailedException(curInj, e);
			}
		});
		LOGGER.debug("Finished processing registry objects in {} ms", System.currentTimeMillis() - ms);
	}

	@SuppressWarnings("unchecked")
	void injectRenderers(ModContainer modContainer, ModFileScanData scanData, List<AnnotationDataPostProcessor> annotationDataPostProcessors) {
		final long ms = System.currentTimeMillis();
		LOGGER.debug("Processing renderer objects");
		AtomicReference<Object> curInj = new AtomicReference<>();
		try {
			Stream.concat(
				((Map<EntityType<?>, EntityRenderer<?>>) handle_EntityRenderDispatcher_renderers.invoke(Minecraft.getInstance().getEntityRenderDispatcher())).values().stream(),
				((Map<EntityType<?>, EntityRenderer<?>>) handle_BlockEntityRenderDispatcher_renderers.invoke(Minecraft.getInstance().getBlockEntityRenderDispatcher())).values().stream()
			).forEach(renderer -> {
				try {
					if (classOrSuperHasAnnotation(renderer.getClass(), Configurable.class)) {
						runAnnotationDataPostProcessors(renderer, modContainer, scanData, annotationDataPostProcessors, curInj);
					}
				} catch (Throwable e) {
					throwInjectionFailedException(curInj, e);
				}
			});
		} catch (Throwable e) {
			LOGGER.error("Exception during renderer injection", e);
			throw new RuntimeException(e);
		}
		LOGGER.debug("Finished processing renderer objects in {} ms", System.currentTimeMillis() - ms);
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

	public final class BeanContextInternalRegistrar {

		private BeanContextInternalRegistrar() {

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

}
