package tamaized.beanification.processors.inject;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.objectweb.asm.Type;
import tamaized.beanification.*;
import tamaized.beanification.directory.DirectoryOtherTestBean;
import tamaized.beanification.directory.DirectoryTestBean;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.junit.MockitoFixer;
import tamaized.beanification.junit.MockitoRunner;

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoFixer.class, MockitoRunner.class})
public class DirectoryAnnotationInjectBeanProcessorTest {

	@Mock
	private DistAnnotationRetriever distAnnotationRetriever;

	@Mock
	private InternalReflectionHelper internalReflectionHelper;

	@InjectMocks
	private DirectoryAnnotationInjectBeanProcessor instance;

	private Field mockField(boolean recursive) {
		Field f = mock(Field.class);
		when(f.isAnnotationPresent(Directory.class)).thenReturn(true);
		Directory annotation = mock(Directory.class);
		doReturn(TestBean.class).when(annotation).value();
		when(annotation.recursive()).thenReturn(recursive);
		when(f.getAnnotation(Directory.class)).thenReturn(annotation);
		return f;
	}

	@Test
	public void process() {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);

		Map<BeanDefinition<?>, Object> beanMap = new HashMap<>();
		when(context.beans()).thenReturn(Optional.of(beanMap));

		when(distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Directory.class)).thenReturn(Stream.empty());

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));
	}

	@Test
	public void processBeans() throws Throwable {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);

		TestBean bean = new TestBean();
		DirectoryTestBean recursiveBean = new DirectoryTestBean();
		DirectoryOtherTestBean recursiveOtherBean = new DirectoryOtherTestBean();
		Map<BeanDefinition<?>, Object> beanMap = new HashMap<>();
		beanMap.put(new BeanDefinition<>(TestBean.class, null), bean);
		beanMap.put(new BeanDefinition<>(DirectoryTestBean.class, null), recursiveBean);
		beanMap.put(new BeanDefinition<>(DirectoryOtherTestBean.class, null), recursiveOtherBean);
		when(context.beans()).thenReturn(Optional.of(beanMap));

		ModFileScanData.AnnotationData data = mock(ModFileScanData.AnnotationData.class);
		when(data.clazz()).thenReturn(Type.getType(TestBean.class));
		when(distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Directory.class)).thenAnswer(invocation -> Stream.of(data));

		when(internalReflectionHelper.classOrSuperEquals(Type.getType(TestBean.class), TestBean.class)).thenReturn(true);

		when(data.annotationData()).thenReturn(Map.of("value", Component.DEFAULT_VALUE));
		when(data.memberName()).thenReturn("memberName");

		Field field = mockField(false);
		when(internalReflectionHelper.getAllDirectoryFieldsIncludingSuper(TestBean.class, "memberName")).thenReturn(
			List.of(field)
		);
		when(internalReflectionHelper.getDeclaredField(TestBean.class, "memberName")).thenReturn(field);

		when(internalReflectionHelper.isStatic(field)).thenReturn(false);

		TestBean dep = new TestBean();
		when(context.injector()).thenReturn(Optional.of(def -> dep));

		when(context.currentInjection()).thenReturn(Optional.of(new AtomicReference<>()));

		ModFileScanData.ClassData classData = mock(ModFileScanData.ClassData.class);
		doReturn(Type.getType(TestBean.class)).when(classData).clazz();
		ModFileScanData.ClassData classDataRecursive = mock(ModFileScanData.ClassData.class);
		doReturn(Type.getType(DirectoryTestBean.class)).when(classDataRecursive).clazz();
		ModFileScanData.ClassData classDataRecursiveOther = mock(ModFileScanData.ClassData.class);
		doReturn(Type.getType(DirectoryOtherTestBean.class)).when(classDataRecursiveOther).clazz();
		when(scanData.getClasses()).thenReturn(Set.of(classData, classDataRecursive, classDataRecursiveOther));

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(field).set(bean, List.of(dep));
	}

	@Test
	public void processBeansRecursive() throws Throwable {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);

		TestBean bean = new TestBean();
		DirectoryTestBean recursiveBean = new DirectoryTestBean();
		DirectoryOtherTestBean recursiveOtherBean = new DirectoryOtherTestBean();
		Map<BeanDefinition<?>, Object> beanMap = new HashMap<>();
		beanMap.put(new BeanDefinition<>(TestBean.class, null), bean);
		beanMap.put(new BeanDefinition<>(DirectoryTestBean.class, null), recursiveBean);
		beanMap.put(new BeanDefinition<>(DirectoryOtherTestBean.class, null), recursiveOtherBean);
		when(context.beans()).thenReturn(Optional.of(beanMap));

		ModFileScanData.AnnotationData data = mock(ModFileScanData.AnnotationData.class);
		when(data.clazz()).thenReturn(Type.getType(TestBean.class));
		when(distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Directory.class)).thenAnswer(invocation -> Stream.of(data));

		when(internalReflectionHelper.classOrSuperEquals(Type.getType(TestBean.class), TestBean.class)).thenReturn(true);

		when(data.annotationData()).thenReturn(Map.of("value", Component.DEFAULT_VALUE));
		when(data.memberName()).thenReturn("memberName");

		Field field = mockField(true);
		when(internalReflectionHelper.getAllDirectoryFieldsIncludingSuper(TestBean.class, "memberName")).thenReturn(
			List.of(field)
		);
		when(internalReflectionHelper.getDeclaredField(TestBean.class, "memberName")).thenReturn(field);

		when(internalReflectionHelper.isStatic(field)).thenReturn(false);

		TestBean dep = new TestBean();
		when(context.injector()).thenReturn(Optional.of(def -> {
			if (def.equals(new BeanDefinition<>(DirectoryTestBean.class, null))) return recursiveBean;
			else if (def.equals(new BeanDefinition<>(DirectoryOtherTestBean.class, null))) return recursiveOtherBean;
			return dep;
		}));

		when(context.currentInjection()).thenReturn(Optional.of(new AtomicReference<>()));

		ModFileScanData.ClassData classData = mock(ModFileScanData.ClassData.class);
		doReturn(Type.getType(TestBean.class)).when(classData).clazz();
		ModFileScanData.ClassData classDataRecursive = mock(ModFileScanData.ClassData.class);
		doReturn(Type.getType(DirectoryTestBean.class)).when(classDataRecursive).clazz();
		ModFileScanData.ClassData classDataRecursiveOther = mock(ModFileScanData.ClassData.class);
		doReturn(Type.getType(DirectoryOtherTestBean.class)).when(classDataRecursiveOther).clazz();
		when(scanData.getClasses()).thenReturn(Set.of(classData, classDataRecursive, classDataRecursiveOther));

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(field).set(eq(bean), argThat(arg -> arg instanceof List<?> list && list.size() == 2 && list.contains(dep) && list.contains(recursiveBean)));
	}

	@Test
	public void processBeansRecord() {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);

		record TestBeanRecord() {

		}
		TestBeanRecord bean = new TestBeanRecord();
		Map<BeanDefinition<?>, Object> beanMap = new HashMap<>();
		beanMap.put(new BeanDefinition<>(TestBeanRecord.class, null), bean);
		when(context.beans()).thenReturn(Optional.of(beanMap));

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));
	}

	@Test
	public void processBeansStatic() throws Throwable {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);

		TestBean bean = new TestBean();
		Map<BeanDefinition<?>, Object> beanMap = new HashMap<>();
		beanMap.put(new BeanDefinition<>(TestBean.class, null), bean);
		when(context.beans()).thenReturn(Optional.of(beanMap));

		ModFileScanData.AnnotationData data = mock(ModFileScanData.AnnotationData.class);
		when(data.clazz()).thenReturn(Type.getType(TestBean.class));
		when(distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Directory.class)).thenAnswer(invocation -> Stream.of(data));

		when(internalReflectionHelper.classOrSuperEquals(Type.getType(TestBean.class), TestBean.class)).thenReturn(true);

		when(data.annotationData()).thenReturn(Map.of("value", Component.DEFAULT_VALUE));
		when(data.memberName()).thenReturn("memberName");

		Field field = mockField(false);
		when(internalReflectionHelper.getAllDirectoryFieldsIncludingSuper(TestBean.class, "memberName")).thenReturn(
			List.of(field)
		);
		when(internalReflectionHelper.getDeclaredField(TestBean.class, "memberName")).thenReturn(field);

		when(internalReflectionHelper.isStatic(field)).thenReturn(true);

		TestBean dep = new TestBean();
		when(context.injector()).thenReturn(Optional.of(def -> dep));

		when(context.currentInjection()).thenReturn(Optional.of(new AtomicReference<>()));

		ModFileScanData.ClassData classData = mock(ModFileScanData.ClassData.class);
		doReturn(Type.getType(TestBean.class)).when(classData).clazz();
		when(scanData.getClasses()).thenReturn(Set.of(classData));

		IllegalStateException result = assertThrows(IllegalStateException.class, () -> instance.process(context, modContainer, scanData));

		assertEquals("@Directory fields must be non-static inside Beans", result.getMessage());

		verify(field, never()).set(bean, dep);
	}

	@Test
	public void processStatic() throws Throwable {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);

		when(context.beans()).thenReturn(Optional.of(new HashMap<>()));

		ModFileScanData.AnnotationData data = mock(ModFileScanData.AnnotationData.class);
		when(data.clazz()).thenReturn(Type.getType(TestBean.class));
		when(distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Directory.class)).thenAnswer(invocation -> Stream.of(data));

		when(internalReflectionHelper.classOrSuperEquals(Type.getType(TestBean.class), TestBean.class)).thenReturn(true);

		when(data.annotationData()).thenReturn(Map.of("value", Component.DEFAULT_VALUE));
		when(data.memberName()).thenReturn("memberName");

		Field field = mockField(false);
		when(internalReflectionHelper.getAllDirectoryFieldsIncludingSuper(TestBean.class, "memberName")).thenReturn(
			List.of(field)
		);
		when(internalReflectionHelper.getDeclaredField(TestBean.class, "memberName")).thenReturn(field);

		when(internalReflectionHelper.isStatic(field)).thenReturn(true);

		TestBean dep = new TestBean();
		when(context.injector()).thenReturn(Optional.of(def -> dep));

		when(context.currentInjection()).thenReturn(Optional.of(new AtomicReference<>()));

		ModFileScanData.ClassData classData = mock(ModFileScanData.ClassData.class);
		doReturn(Type.getType(TestBean.class)).when(classData).clazz();
		when(scanData.getClasses()).thenReturn(Set.of(classData));

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(field).set(null, List.of(dep));
	}

}
