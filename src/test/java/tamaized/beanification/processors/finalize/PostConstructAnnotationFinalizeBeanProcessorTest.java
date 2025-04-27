package tamaized.beanification.processors.finalize;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.objectweb.asm.Type;
import tamaized.beanification.BeanContext;
import tamaized.beanification.BeanDefinition;
import tamaized.beanification.PostConstruct;
import tamaized.beanification.TestBean;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.junit.MockitoFixer;
import tamaized.beanification.junit.MockitoRunner;

import java.lang.annotation.ElementType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoFixer.class, MockitoRunner.class})
public class PostConstructAnnotationFinalizeBeanProcessorTest {

	@Mock
	private DistAnnotationRetriever distAnnotationRetriever;

	@Mock
	private InternalReflectionHelper internalReflectionHelper;

	@InjectMocks
	private PostConstructAnnotationFinalizeBeanProcessor instance;

	@Test
	public void process() throws NoSuchMethodException {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);

		when(context.beans()).thenReturn(Optional.of(Map.of(new BeanDefinition<>(TestBean.class, null), new TestBean())));

		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, PostConstruct.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));
		when(internalReflectionHelper.getType(TestBean.class)).thenReturn(Type.getType(TestBean.class));

		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method")).thenThrow(new NoSuchMethodException());
		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method", IEventBus.class)).thenThrow(new NoSuchMethodException());

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(context, never()).currentInjection();
		verify(internalReflectionHelper, never()).isStatic(any(Method.class));
	}

	@Test
	public void processNoArgs() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);

		TestBean bean = new TestBean();
		when(context.beans()).thenReturn(Optional.of(Map.of(new BeanDefinition<>(TestBean.class, null), bean)));

		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, PostConstruct.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));
		when(internalReflectionHelper.getType(TestBean.class)).thenReturn(Type.getType(TestBean.class));

		Method method = mock(Method.class);
		when(method.isAnnotationPresent(PostConstruct.class)).thenReturn(true);
		when(method.getParameterCount()).thenReturn(0);
		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method")).thenReturn(method);
		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method", IEventBus.class)).thenThrow(new NoSuchMethodException());

		when(context.currentInjection()).thenReturn(Optional.of(new AtomicReference<>()));

		when(internalReflectionHelper.isStatic(method)).thenReturn(false);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(method).trySetAccessible();
		verify(method).invoke(bean);
	}

	@Test
	public void processStatic() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);

		TestBean bean = new TestBean();
		when(context.beans()).thenReturn(Optional.of(Map.of(new BeanDefinition<>(TestBean.class, null), bean)));

		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, PostConstruct.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));
		when(internalReflectionHelper.getType(TestBean.class)).thenReturn(Type.getType(TestBean.class));

		Method method = mock(Method.class);
		when(method.isAnnotationPresent(PostConstruct.class)).thenReturn(true);
		when(method.getParameterCount()).thenReturn(0);
		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method")).thenReturn(method);
		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method", IEventBus.class)).thenThrow(new NoSuchMethodException());

		when(context.currentInjection()).thenReturn(Optional.of(new AtomicReference<>()));

		when(internalReflectionHelper.isStatic(method)).thenReturn(true);

		IllegalStateException result = assertThrows(IllegalStateException.class, () -> instance.process(context, modContainer, scanData));

		assertEquals("@PostConstruct methods must be non-static", result.getMessage());
		verify(method, never()).trySetAccessible();
		verify(method, never()).invoke(bean);
	}

	@Test
	public void processOneArg() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModContainer modContainer = mock(ModContainer.class);
		IEventBus modBus = mock(IEventBus.class);
		when(modContainer.getEventBus()).thenReturn(modBus);
		ModFileScanData scanData = mock(ModFileScanData.class);

		TestBean bean = new TestBean();
		when(context.beans()).thenReturn(Optional.of(Map.of(new BeanDefinition<>(TestBean.class, null), bean)));

		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, PostConstruct.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));
		when(internalReflectionHelper.getType(TestBean.class)).thenReturn(Type.getType(TestBean.class));

		Method method = mock(Method.class);
		when(method.isAnnotationPresent(PostConstruct.class)).thenReturn(true);
		when(method.getParameterCount()).thenReturn(1);
		when(method.getParameterTypes()).thenReturn(new Class[]{
			IEventBus.class
		});
		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method")).thenThrow(new NoSuchMethodException());
		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method", IEventBus.class)).thenReturn(method);

		when(context.currentInjection()).thenReturn(Optional.of(new AtomicReference<>()));

		when(internalReflectionHelper.isStatic(method)).thenReturn(false);

		PostConstruct annotation = mock(PostConstruct.class);
		when(annotation.value()).thenReturn(PostConstruct.Bus.MOD);
		when(method.getAnnotation(PostConstruct.class)).thenReturn(annotation);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(method).trySetAccessible();
		verify(method).invoke(bean, modBus);
	}

	@Test
	public void processOneArgGameBus() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModContainer modContainer = mock(ModContainer.class);
		IEventBus modBus = mock(IEventBus.class);
		when(modContainer.getEventBus()).thenReturn(modBus);
		ModFileScanData scanData = mock(ModFileScanData.class);

		TestBean bean = new TestBean();
		when(context.beans()).thenReturn(Optional.of(Map.of(new BeanDefinition<>(TestBean.class, null), bean)));

		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, PostConstruct.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));
		when(internalReflectionHelper.getType(TestBean.class)).thenReturn(Type.getType(TestBean.class));

		Method method = mock(Method.class);
		when(method.isAnnotationPresent(PostConstruct.class)).thenReturn(true);
		when(method.getParameterCount()).thenReturn(1);
		when(method.getParameterTypes()).thenReturn(new Class[]{
			IEventBus.class
		});
		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method")).thenThrow(new NoSuchMethodException());
		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method", IEventBus.class)).thenReturn(method);

		when(context.currentInjection()).thenReturn(Optional.of(new AtomicReference<>()));

		when(internalReflectionHelper.isStatic(method)).thenReturn(false);

		PostConstruct annotation = mock(PostConstruct.class);
		when(annotation.value()).thenReturn(PostConstruct.Bus.GAME);
		when(method.getAnnotation(PostConstruct.class)).thenReturn(annotation);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(method).trySetAccessible();
		verify(method).invoke(bean, NeoForge.EVENT_BUS);
	}

	@Test
	public void processOneArgWrongType() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModContainer modContainer = mock(ModContainer.class);
		IEventBus modBus = mock(IEventBus.class);
		when(modContainer.getEventBus()).thenReturn(modBus);
		ModFileScanData scanData = mock(ModFileScanData.class);

		TestBean bean = new TestBean();
		when(context.beans()).thenReturn(Optional.of(Map.of(new BeanDefinition<>(TestBean.class, null), bean)));

		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, PostConstruct.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));
		when(internalReflectionHelper.getType(TestBean.class)).thenReturn(Type.getType(TestBean.class));

		Method method = mock(Method.class);
		when(method.isAnnotationPresent(PostConstruct.class)).thenReturn(true);
		when(method.getParameterCount()).thenReturn(1);
		when(method.getParameterTypes()).thenReturn(new Class[]{
			TestBean.class
		});
		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method")).thenThrow(new NoSuchMethodException());
		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method", IEventBus.class)).thenReturn(method);

		when(context.currentInjection()).thenReturn(Optional.of(new AtomicReference<>()));

		when(internalReflectionHelper.isStatic(method)).thenReturn(false);

		PostConstruct annotation = mock(PostConstruct.class);
		when(annotation.value()).thenReturn(PostConstruct.Bus.MOD);
		when(method.getAnnotation(PostConstruct.class)).thenReturn(annotation);

		IllegalStateException result = assertThrows(IllegalStateException.class, () -> instance.process(context, modContainer, scanData));

		assertEquals("@PostConstruct methods must not have parameters or only have one or two IEventBus parameter(s)", result.getMessage());
		verify(method).trySetAccessible();
		verify(method, never()).invoke(bean, modBus);
	}

	@Test
	public void processTwoArgs() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModContainer modContainer = mock(ModContainer.class);
		IEventBus modBus = mock(IEventBus.class);
		when(modContainer.getEventBus()).thenReturn(modBus);
		ModFileScanData scanData = mock(ModFileScanData.class);

		TestBean bean = new TestBean();
		when(context.beans()).thenReturn(Optional.of(Map.of(new BeanDefinition<>(TestBean.class, null), bean)));

		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, PostConstruct.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));
		when(internalReflectionHelper.getType(TestBean.class)).thenReturn(Type.getType(TestBean.class));

		Method method = mock(Method.class);
		when(method.isAnnotationPresent(PostConstruct.class)).thenReturn(true);
		when(method.getParameterCount()).thenReturn(2);
		when(method.getParameterTypes()).thenReturn(new Class[]{
			IEventBus.class,
			IEventBus.class
		});
		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method")).thenThrow(new NoSuchMethodException());
		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method", IEventBus.class)).thenReturn(method);

		when(context.currentInjection()).thenReturn(Optional.of(new AtomicReference<>()));

		when(internalReflectionHelper.isStatic(method)).thenReturn(false);

		PostConstruct annotation = mock(PostConstruct.class);
		when(annotation.value()).thenReturn(PostConstruct.Bus.MOD);
		when(method.getAnnotation(PostConstruct.class)).thenReturn(annotation);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(method).trySetAccessible();
		verify(method).invoke(bean, modBus, NeoForge.EVENT_BUS);
	}

	@Test
	public void processTwoArgsGameBus() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModContainer modContainer = mock(ModContainer.class);
		IEventBus modBus = mock(IEventBus.class);
		when(modContainer.getEventBus()).thenReturn(modBus);
		ModFileScanData scanData = mock(ModFileScanData.class);

		TestBean bean = new TestBean();
		when(context.beans()).thenReturn(Optional.of(Map.of(new BeanDefinition<>(TestBean.class, null), bean)));

		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, PostConstruct.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));
		when(internalReflectionHelper.getType(TestBean.class)).thenReturn(Type.getType(TestBean.class));

		Method method = mock(Method.class);
		when(method.isAnnotationPresent(PostConstruct.class)).thenReturn(true);
		when(method.getParameterCount()).thenReturn(2);
		when(method.getParameterTypes()).thenReturn(new Class[]{
			IEventBus.class,
			IEventBus.class
		});
		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method")).thenThrow(new NoSuchMethodException());
		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method", IEventBus.class)).thenReturn(method);

		when(context.currentInjection()).thenReturn(Optional.of(new AtomicReference<>()));

		when(internalReflectionHelper.isStatic(method)).thenReturn(false);

		PostConstruct annotation = mock(PostConstruct.class);
		when(annotation.value()).thenReturn(PostConstruct.Bus.GAME);
		when(method.getAnnotation(PostConstruct.class)).thenReturn(annotation);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(method).trySetAccessible();
		verify(method).invoke(bean, NeoForge.EVENT_BUS, modBus);
	}

	@Test
	public void processTwoArgsWrongType() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModContainer modContainer = mock(ModContainer.class);
		IEventBus modBus = mock(IEventBus.class);
		when(modContainer.getEventBus()).thenReturn(modBus);
		ModFileScanData scanData = mock(ModFileScanData.class);

		TestBean bean = new TestBean();
		when(context.beans()).thenReturn(Optional.of(Map.of(new BeanDefinition<>(TestBean.class, null), bean)));

		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, PostConstruct.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));
		when(internalReflectionHelper.getType(TestBean.class)).thenReturn(Type.getType(TestBean.class));

		Method method = mock(Method.class);
		when(method.isAnnotationPresent(PostConstruct.class)).thenReturn(true);
		when(method.getParameterCount()).thenReturn(2);
		when(method.getParameterTypes()).thenReturn(new Class[]{
			IEventBus.class,
			TestBean.class
		});
		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method")).thenThrow(new NoSuchMethodException());
		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method", IEventBus.class)).thenReturn(method);

		when(context.currentInjection()).thenReturn(Optional.of(new AtomicReference<>()));

		when(internalReflectionHelper.isStatic(method)).thenReturn(false);

		PostConstruct annotation = mock(PostConstruct.class);
		when(annotation.value()).thenReturn(PostConstruct.Bus.MOD);
		when(method.getAnnotation(PostConstruct.class)).thenReturn(annotation);

		IllegalStateException result = assertThrows(IllegalStateException.class, () -> instance.process(context, modContainer, scanData));

		assertEquals("@PostConstruct methods must not have parameters or only have one or two IEventBus parameter(s)", result.getMessage());
		verify(method).trySetAccessible();
		verify(method, never()).invoke(bean, modBus);
	}

	@Test
	public void processTooManyArgs() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModContainer modContainer = mock(ModContainer.class);
		IEventBus modBus = mock(IEventBus.class);
		when(modContainer.getEventBus()).thenReturn(modBus);
		ModFileScanData scanData = mock(ModFileScanData.class);

		TestBean bean = new TestBean();
		when(context.beans()).thenReturn(Optional.of(Map.of(new BeanDefinition<>(TestBean.class, null), bean)));

		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, PostConstruct.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));
		when(internalReflectionHelper.getType(TestBean.class)).thenReturn(Type.getType(TestBean.class));

		Method method = mock(Method.class);
		when(method.isAnnotationPresent(PostConstruct.class)).thenReturn(true);
		when(method.getParameterCount()).thenReturn(3);
		when(method.getParameterTypes()).thenReturn(new Class[]{
			IEventBus.class,
			IEventBus.class,
			IEventBus.class
		});
		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method")).thenThrow(new NoSuchMethodException());
		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method", IEventBus.class)).thenReturn(method);

		when(context.currentInjection()).thenReturn(Optional.of(new AtomicReference<>()));

		when(internalReflectionHelper.isStatic(method)).thenReturn(false);

		PostConstruct annotation = mock(PostConstruct.class);
		when(annotation.value()).thenReturn(PostConstruct.Bus.MOD);
		when(method.getAnnotation(PostConstruct.class)).thenReturn(annotation);

		IllegalStateException result = assertThrows(IllegalStateException.class, () -> instance.process(context, modContainer, scanData));

		assertEquals("@PostConstruct methods must not have parameters or only have one or two IEventBus parameter(s)", result.getMessage());
		verify(method).trySetAccessible();
		verify(method, never()).invoke(bean, modBus);
	}

}
