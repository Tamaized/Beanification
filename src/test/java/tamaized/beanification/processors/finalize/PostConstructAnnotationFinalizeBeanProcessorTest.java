package tamaized.beanification.processors.finalize;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import tamaized.beanification.BeanContext;
import tamaized.beanification.BeanDefinition;
import tamaized.beanification.PostConstruct;
import tamaized.beanification.TestBean;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.junit.MockitoRunner;
import tamaized.beanification.junit.TestScanData;
import tamaized.beanification.processors.BeanAnnotationProcessorMetadata;
import tamaized.beanification.scan.AnnotationScanData;

import java.lang.annotation.ElementType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoRunner.class})
public class PostConstructAnnotationFinalizeBeanProcessorTest {

	@Mock
	private InternalReflectionHelper internalReflectionHelper;

	@InjectMocks
	private PostConstructAnnotationFinalizeBeanProcessor instance;

	private List<AnnotationScanData> scanData() {
		return TestScanData.scan(TestScanData.raw(PostConstruct.class, ElementType.METHOD, TestBean.class, "method()V", Map.of()));
	}

	@Test
	public void process() {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);

		when(context.beansToProcess()).thenReturn(Optional.of(Map.of(new BeanDefinition<>(TestBean.class, null), new TestBean())));

		when(internalReflectionHelper.getDeclaredMethodsForName(TestBean.class, "method()V")).thenReturn(List.of());

		assertDoesNotThrow(() -> instance.process(context, scanData(), new BeanAnnotationProcessorMetadata()));

		verify(context, never()).currentInjection();
		verify(internalReflectionHelper, never()).isStatic(any(Method.class));
	}

	@Test
	public void processNoArgs() throws InvocationTargetException, IllegalAccessException {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);

		TestBean bean = new TestBean();
		when(context.beansToProcess()).thenReturn(Optional.of(Map.of(new BeanDefinition<>(TestBean.class, null), bean)));

		Method method = mock(Method.class);
		when(method.isAnnotationPresent(PostConstruct.class)).thenReturn(true);
		when(method.getParameterCount()).thenReturn(0);
		when(internalReflectionHelper.getDeclaredMethodsForName(TestBean.class, "method()V")).thenReturn(List.of(method));

		when(context.currentInjection()).thenReturn(Optional.of(new AtomicReference<>()));

		when(internalReflectionHelper.isStatic(method)).thenReturn(false);

		assertDoesNotThrow(() -> instance.process(context, scanData(), new BeanAnnotationProcessorMetadata()));

		verify(method).trySetAccessible();
		verify(method).invoke(bean);
	}

	@Test
	public void processStatic() throws InvocationTargetException, IllegalAccessException {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);

		TestBean bean = new TestBean();
		when(context.beansToProcess()).thenReturn(Optional.of(Map.of(new BeanDefinition<>(TestBean.class, null), bean)));

		Method method = mock(Method.class);
		when(method.isAnnotationPresent(PostConstruct.class)).thenReturn(true);
		when(method.getParameterCount()).thenReturn(0);
		when(internalReflectionHelper.getDeclaredMethodsForName(TestBean.class, "method()V")).thenReturn(List.of(method));

		when(context.currentInjection()).thenReturn(Optional.of(new AtomicReference<>()));

		when(internalReflectionHelper.isStatic(method)).thenReturn(true);

		IllegalStateException result = assertThrows(IllegalStateException.class, () -> instance.process(context, scanData(), new BeanAnnotationProcessorMetadata()));

		assertEquals("@PostConstruct methods must be non-static", result.getMessage());
		verify(method, never()).trySetAccessible();
		verify(method, never()).invoke(bean);
	}

	@Test
	public void processWithParameters() throws InvocationTargetException, IllegalAccessException {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);

		TestBean bean = new TestBean();
		when(context.beansToProcess()).thenReturn(Optional.of(Map.of(new BeanDefinition<>(TestBean.class, null), bean)));

		Method method = mock(Method.class);
		when(method.isAnnotationPresent(PostConstruct.class)).thenReturn(true);
		when(method.getParameterCount()).thenReturn(1);
		when(internalReflectionHelper.getDeclaredMethodsForName(TestBean.class, "method()V")).thenReturn(List.of(method));

		when(context.currentInjection()).thenReturn(Optional.of(new AtomicReference<>()));

		when(internalReflectionHelper.isStatic(method)).thenReturn(false);

		IllegalStateException result = assertThrows(IllegalStateException.class, () -> instance.process(context, scanData(), new BeanAnnotationProcessorMetadata()));

		assertEquals("@PostConstruct methods must not have parameters", result.getMessage());
		verify(method, never()).trySetAccessible();
		verify(method, never()).invoke(bean);
	}

}
