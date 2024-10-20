package tamaized.beanification.processors;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.objectweb.asm.Type;
import tamaized.beanification.BeanContext;
import tamaized.beanification.Component;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoFixer.class, MockitoRunner.class})
public class PostConstructAnnotationDataPostProcessorTests {

	@Mock
	private DistAnnotationRetriever distAnnotationRetriever;

	@Mock
	private InternalReflectionHelper internalReflectionHelper;

	@InjectMocks
	private PostConstructAnnotationDataPostProcessor instance;

	@Test
	public void processNoArgs() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, PostConstruct.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method(V)V", new HashMap<>())
		));

		when(internalReflectionHelper.getType(TestBean.class)).thenCallRealMethod();

		Method target = mock(Method.class);
		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method")).thenReturn(target);
		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method", IEventBus.class)).thenThrow(new NoSuchMethodException());

		when(target.isAnnotationPresent(PostConstruct.class)).thenReturn(true);
		when(internalReflectionHelper.isStatic(target)).thenReturn(false);
		when(target.getParameterCount()).thenReturn(0);

		TestBean bean = new TestBean();
		BeanContext.BeanContextInternalInjector context = mock(BeanContext.BeanContextInternalInjector.class);
		ModContainer modContainer = mock(ModContainer.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData, bean, new AtomicReference<>()));

		verify(target, times(1)).trySetAccessible();
		verify(target, times(1)).invoke(bean);
	}

	@Test
	public void processEventBusArg() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, PostConstruct.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method(LIEventBus;)V", new HashMap<>())
		));

		when(internalReflectionHelper.getType(TestBean.class)).thenCallRealMethod();

		Method target = mock(Method.class);
		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method")).thenThrow(new NoSuchMethodException());
		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method", IEventBus.class)).thenReturn(target);

		when(target.isAnnotationPresent(PostConstruct.class)).thenReturn(true);
		when(internalReflectionHelper.isStatic(target)).thenReturn(false);
		when(target.getParameterCount()).thenReturn(1);
		when(target.getParameterTypes()).thenReturn(new Class<?>[]{IEventBus.class});

		TestBean bean = new TestBean();
		BeanContext.BeanContextInternalInjector context = mock(BeanContext.BeanContextInternalInjector.class);
		ModContainer modContainer = mock(ModContainer.class);
		IEventBus bus = mock(IEventBus.class);
		when(modContainer.getEventBus()).thenReturn(bus);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData, bean, new AtomicReference<>()));

		verify(target, times(1)).trySetAccessible();
		verify(target, times(1)).invoke(bean, bus);
	}

	@Test
	public void processStatic() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, PostConstruct.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method(V)V", new HashMap<>())
		));

		when(internalReflectionHelper.getType(TestBean.class)).thenCallRealMethod();

		Method target = mock(Method.class);
		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method")).thenReturn(target);
		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method", IEventBus.class)).thenThrow(new NoSuchMethodException());

		when(target.isAnnotationPresent(PostConstruct.class)).thenReturn(true);
		when(internalReflectionHelper.isStatic(target)).thenReturn(true);
		when(target.getParameterCount()).thenReturn(0);

		TestBean bean = new TestBean();
		BeanContext.BeanContextInternalInjector context = mock(BeanContext.BeanContextInternalInjector.class);
		ModContainer modContainer = mock(ModContainer.class);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> instance.process(context, modContainer, scanData, bean, new AtomicReference<>()));

		assertEquals("@PostConstruct methods must be non-static", exception.getMessage());

		verify(target, never()).trySetAccessible();
		verify(target, never()).invoke(bean);
	}

	@Test
	public void processTooManyArgs() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, PostConstruct.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method(V)V", new HashMap<>())
		));

		when(internalReflectionHelper.getType(TestBean.class)).thenCallRealMethod();

		Method target = mock(Method.class);
		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method")).thenReturn(target);
		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method", IEventBus.class)).thenThrow(new NoSuchMethodException());

		when(target.isAnnotationPresent(PostConstruct.class)).thenReturn(true);
		when(internalReflectionHelper.isStatic(target)).thenReturn(false);
		when(target.getParameterCount()).thenReturn(2);

		TestBean bean = new TestBean();
		BeanContext.BeanContextInternalInjector context = mock(BeanContext.BeanContextInternalInjector.class);
		ModContainer modContainer = mock(ModContainer.class);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> instance.process(context, modContainer, scanData, bean, new AtomicReference<>()));

		assertEquals("@PostConstruct methods must not have parameters or only have one IEventBus parameter", exception.getMessage());

		verify(target, times(1)).trySetAccessible();
		verify(target, never()).invoke(bean);
	}

}
