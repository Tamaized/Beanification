package tamaized.beanification.processors.inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import tamaized.beanification.*;
import tamaized.beanification.directory.DirectoryOtherTestBean;
import tamaized.beanification.directory.DirectoryTestBean;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.junit.MockitoFixer;
import tamaized.beanification.junit.MockitoRunner;
import tamaized.beanification.junit.TestScanData;
import tamaized.beanification.processors.BeanAnnotationProcessorMetadata;
import tamaized.beanification.scan.AnnotationScanData;

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoFixer.class, MockitoRunner.class})
public class DirectoryAnnotationFieldInjectBeanProcessorTest {

	@Mock
	private InternalReflectionHelper internalReflectionHelper;

	@InjectMocks
	private DirectoryAnnotationFieldInjectBeanProcessor instance;

	private Field mockField() {
		Field f = mock(Field.class);
		when(f.isAnnotationPresent(Directory.class)).thenReturn(true);
		Directory annotation = mock(Directory.class);
		doReturn(TestBean.class).when(annotation).value();
		when(f.getAnnotation(Directory.class)).thenReturn(annotation);
		return f;
	}

	private List<AnnotationScanData> scanData() {
		return TestScanData.scan(TestScanData.raw(Directory.class, ElementType.FIELD, TestBean.class, "memberName", Map.of("value", Component.DEFAULT_VALUE)));
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
		DirectoryTestBean recursiveBean = new DirectoryTestBean();
		DirectoryOtherTestBean recursiveOtherBean = new DirectoryOtherTestBean();
		Map<BeanDefinition<?>, Object> beanMap = new HashMap<>();
		beanMap.put(new BeanDefinition<>(TestBean.class, null), bean);
		beanMap.put(new BeanDefinition<>(DirectoryTestBean.class, null), recursiveBean);
		beanMap.put(new BeanDefinition<>(DirectoryOtherTestBean.class, null), recursiveOtherBean);
		when(context.beansToProcess()).thenReturn(Optional.of(beanMap));

		when(internalReflectionHelper.classOrSuperEquals(TestScanData.type(TestBean.class), TestBean.class)).thenReturn(true);

		Field field = mockField();
		when(internalReflectionHelper.getAllDirectoryFieldsIncludingSuper(TestBean.class, "memberName")).thenReturn(
			List.of(field)
		);
		when(internalReflectionHelper.getDeclaredField(TestBean.class, "memberName")).thenReturn(field);

		when(internalReflectionHelper.isStatic(field)).thenReturn(false);

		when(context.currentInjection()).thenReturn(Optional.of(new AtomicReference<>()));

		List<?> deps = List.of(new TestBean());

		when(context.fuzzyInjector()).thenReturn(Optional.of(_ -> deps));

		assertDoesNotThrow(() -> instance.process(context, scanData(), new BeanAnnotationProcessorMetadata()));

		verify(field).set(bean, deps);
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
		TestBean dep = new TestBean();
		Map<BeanDefinition<?>, Object> beanMap = new HashMap<>();
		beanMap.put(new BeanDefinition<>(TestBean.class, "source"), bean);
		beanMap.put(new BeanDefinition<>(TestBean.class, null), dep);
		when(context.beansToProcess()).thenReturn(Optional.of(beanMap));

		when(internalReflectionHelper.classOrSuperEquals(TestScanData.type(TestBean.class), TestBean.class)).thenReturn(true);

		Field field = mockField();
		when(internalReflectionHelper.getAllDirectoryFieldsIncludingSuper(TestBean.class, "memberName")).thenReturn(
			List.of(field)
		);
		when(internalReflectionHelper.getDeclaredField(TestBean.class, "memberName")).thenReturn(field);

		when(internalReflectionHelper.isStatic(field)).thenReturn(true);

		when(context.strictInjector()).thenReturn(Optional.of(def -> dep));

		when(context.currentInjection()).thenReturn(Optional.of(new AtomicReference<>()));

		IllegalStateException result = assertThrows(IllegalStateException.class, () -> instance.process(context, scanData(), new BeanAnnotationProcessorMetadata()));

		assertEquals("@Directory fields must be non-static inside Beans", result.getMessage());

		verify(field, never()).set(bean, dep);
	}

}
