package tamaized.beanification.processors.staticinject.inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import tamaized.beanification.*;
import tamaized.beanification.internal.FieldLocator;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.junit.MockitoRunner;
import tamaized.beanification.junit.TestScanData;
import tamaized.beanification.processors.BeanAnnotationProcessorMetadata;
import tamaized.beanification.processors.staticinject.AutowiredAnnotationStaticFieldInjectBeanProcessor;
import tamaized.beanification.scan.AnnotationScanData;

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoRunner.class})
public class AutowiredAnnotationStaticInjectBeanProcessorTest {

	@Mock
	private InternalReflectionHelper internalReflectionHelper;

	@Mock
	private FieldLocator fieldLocator;

	@InjectMocks
	private AutowiredAnnotationStaticFieldInjectBeanProcessor instance;

	private Field mockField() {
		Field f = mock(Field.class);
		Autowired annotation = mock(Autowired.class);
		when(annotation.value()).thenReturn(Component.DEFAULT_VALUE);
		when(f.getAnnotation(Autowired.class)).thenReturn(annotation);
		return f;
	}

	private List<AnnotationScanData> scanData() {
		return TestScanData.scan(TestScanData.raw(Autowired.class, ElementType.FIELD, TestBean.class, "memberName", Map.of()));
	}

	@Test
	public void process() throws Throwable {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);

		Field field = mockField();
		when(fieldLocator.locate(eq(context), any(AnnotationScanData.AnnotationData.class))).thenReturn(field);

		when(internalReflectionHelper.isStatic(field)).thenReturn(true);

		TestBean dep = new TestBean();
		when(context.strictInjector()).thenReturn(Optional.of(_ -> dep));

		assertDoesNotThrow(() -> instance.process(context, scanData(), new BeanAnnotationProcessorMetadata()));

		verify(field).trySetAccessible();
		verify(field).set(null, dep);
	}

	@Test
	public void processNonStatic() throws Throwable {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);

		Field field = mockField();
		when(fieldLocator.locate(eq(context), any(AnnotationScanData.AnnotationData.class))).thenReturn(field);

		when(internalReflectionHelper.isStatic(field)).thenReturn(false);

		assertDoesNotThrow(() -> instance.process(context, scanData(), new BeanAnnotationProcessorMetadata()));

		verify(context, never()).strictInjector();
		verify(field, never()).trySetAccessible();
		verify(field, never()).set(isNull(), any(TestBean.class));
	}

}
