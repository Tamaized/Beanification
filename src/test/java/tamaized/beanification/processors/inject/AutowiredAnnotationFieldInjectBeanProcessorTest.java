package tamaized.beanification.processors.inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import tamaized.beanification.*;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.junit.MockitoRunner;
import tamaized.beanification.junit.TestScanData;
import tamaized.beanification.processors.BeanAnnotationProcessorMetadata;
import tamaized.beanification.scan.AnnotationScanData;

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoRunner.class})
public class AutowiredAnnotationFieldInjectBeanProcessorTest {

	@Mock
	private InternalReflectionHelper internalReflectionHelper;

	@InjectMocks
	private AutowiredAnnotationFieldInjectBeanProcessor instance;

	private Field mockField() {
		return mockField(Component.DEFAULT_VALUE);
	}

	private Field mockField(String value) {
		Field f = mock(Field.class);
		when(f.isAnnotationPresent(Autowired.class)).thenReturn(true);
		Autowired annotation = mock(Autowired.class);
		when(annotation.value()).thenReturn(value);
		when(f.getAnnotation(Autowired.class)).thenReturn(annotation);
		return f;
	}

	private List<AnnotationScanData> scanData() {
		return TestScanData.scan(TestScanData.raw(Autowired.class, ElementType.FIELD, TestBean.class, "memberName", Map.of("value", Component.DEFAULT_VALUE)));
	}

	@Test
	public void process() {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);

		Map<BeanDefinition<?>, Object> beanMap = new HashMap<>();
		when(context.beansToProcess()).thenReturn(Optional.of(beanMap));

		assertDoesNotThrow(() -> instance.process(context, TestScanData.scan(), new BeanAnnotationProcessorMetadata()));
	}

	@Test
	public void processBeans() throws Throwable {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);

		TestBean bean = new TestBean();
		Map<BeanDefinition<?>, Object> beanMap = new HashMap<>();
		beanMap.put(new BeanDefinition<>(TestBean.class, null), bean);
		when(context.beansToProcess()).thenReturn(Optional.of(beanMap));

		when(internalReflectionHelper.classOrSuperEquals(TestScanData.type(TestBean.class), TestBean.class)).thenReturn(true);

		Field field = mockField();
		when(internalReflectionHelper.getAllAutowiredFieldsIncludingSuper(TestBean.class, "memberName", Component.DEFAULT_VALUE)).thenReturn(
			List.of(field)
		);
		when(internalReflectionHelper.getDeclaredField(TestBean.class, "memberName")).thenReturn(field);

		when(internalReflectionHelper.isStatic(field)).thenReturn(false);

		TestBean dep = new TestBean();
		when(context.strictInjector()).thenReturn(Optional.of(def -> dep));

		when(context.currentInjection()).thenReturn(Optional.of(new AtomicReference<>()));

		assertDoesNotThrow(() -> instance.process(context, scanData(), new BeanAnnotationProcessorMetadata()));

		verify(field).set(bean, dep);
	}

	@Test
	public void processBeansRecord() {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);

		record TestBeanRecord() {

		}
		TestBeanRecord bean = new TestBeanRecord();
		Map<BeanDefinition<?>, Object> beanMap = new HashMap<>();
		beanMap.put(new BeanDefinition<>(TestBeanRecord.class, null), bean);
		when(context.beansToProcess()).thenReturn(Optional.of(beanMap));

		assertDoesNotThrow(() -> instance.process(context, scanData(), new BeanAnnotationProcessorMetadata()));
	}

	@Test
	public void processBeansStatic() throws Throwable {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);

		TestBean bean = new TestBean();
		Map<BeanDefinition<?>, Object> beanMap = new HashMap<>();
		beanMap.put(new BeanDefinition<>(TestBean.class, null), bean);
		when(context.beansToProcess()).thenReturn(Optional.of(beanMap));

		when(internalReflectionHelper.classOrSuperEquals(TestScanData.type(TestBean.class), TestBean.class)).thenReturn(true);

		Field field = mockField();
		when(internalReflectionHelper.getAllAutowiredFieldsIncludingSuper(TestBean.class, "memberName", Component.DEFAULT_VALUE)).thenReturn(
			List.of(field)
		);
		when(internalReflectionHelper.getDeclaredField(TestBean.class, "memberName")).thenReturn(field);

		when(internalReflectionHelper.isStatic(field)).thenReturn(true);

		TestBean dep = new TestBean();
		when(context.strictInjector()).thenReturn(Optional.of(def -> dep));

		when(context.currentInjection()).thenReturn(Optional.of(new AtomicReference<>()));

		IllegalStateException result = assertThrows(IllegalStateException.class, () -> instance.process(context, scanData(), new BeanAnnotationProcessorMetadata()));

		assertEquals("@Autowired fields must be non-static inside Beans", result.getMessage());

		verify(field, never()).set(bean, dep);
	}

}
