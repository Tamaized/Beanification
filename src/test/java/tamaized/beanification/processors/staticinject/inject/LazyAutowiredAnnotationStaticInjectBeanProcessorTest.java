package tamaized.beanification.processors.staticinject.inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import tamaized.beanification.*;
import tamaized.beanification.internal.FieldLocator;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.junit.MockitoRunner;
import tamaized.beanification.junit.TestScanData;
import tamaized.beanification.processors.BeanAnnotationProcessorMetadata;
import tamaized.beanification.processors.staticinject.LazyAutowiredAnnotationStaticFieldInjectBeanProcessor;
import tamaized.beanification.scan.AnnotationScanData;
import tamaized.beanification.util.SimpleMemoize;

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoRunner.class})
public class LazyAutowiredAnnotationStaticInjectBeanProcessorTest {

	@Mock
	private InternalReflectionHelper internalReflectionHelper;

	@Mock
	private FieldLocator fieldLocator;

	@InjectMocks
	private LazyAutowiredAnnotationStaticFieldInjectBeanProcessor instance;

	private Field mockField() {
		Field f = mock(Field.class);
		LazyAutowired annotation = mock(LazyAutowired.class);
		when(annotation.value()).thenReturn(Component.DEFAULT_VALUE);
		when(f.getAnnotation(LazyAutowired.class)).thenReturn(annotation);
		return f;
	}

	private List<AnnotationScanData> scanData() {
		return TestScanData.scan(TestScanData.raw(LazyAutowired.class, ElementType.FIELD, TestBean.class, "memberName", Map.of()));
	}

	@Test
	public void process() throws Throwable {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);

		Field field = mockField();
		when(fieldLocator.locate(eq(context), any(AnnotationScanData.AnnotationData.class))).thenReturn(field);

		when(internalReflectionHelper.isStatic(field)).thenReturn(true);

		TestBean dep = new TestBean();
		when(context.lazyInjector()).thenReturn(Optional.of(_ -> SimpleMemoize.memoize(() -> dep)));

		assertDoesNotThrow(() -> instance.process(context, scanData(), new BeanAnnotationProcessorMetadata()));

		verify(field).trySetAccessible();
		ArgumentCaptor<Supplier<TestBean>> captor = ArgumentCaptor.captor();
		verify(field).set(isNull(), captor.capture());
		assertSame(dep, captor.getValue().get());
	}

	@Test
	public void processNonStatic() throws Throwable {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);

		Field field = mockField();
		when(fieldLocator.locate(eq(context), any(AnnotationScanData.AnnotationData.class))).thenReturn(field);

		when(internalReflectionHelper.isStatic(field)).thenReturn(false);

		assertDoesNotThrow(() -> instance.process(context, scanData(), new BeanAnnotationProcessorMetadata()));

		verify(context, never()).lazyInjector();
		verify(field, never()).trySetAccessible();
		verify(field, never()).set(isNull(), any(TestBean.class));
	}

}
