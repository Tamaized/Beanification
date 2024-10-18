package tamaized.beanification.processors;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.objectweb.asm.Type;
import tamaized.beanification.*;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.junit.MockitoFixer;
import tamaized.beanification.junit.MockitoRunner;

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoFixer.class, MockitoRunner.class})
public class AutowiredAnnotationDataPostProcessorTests {

	@Mock
	private DistAnnotationRetriever distAnnotationRetriever;

	@Mock
	private InternalReflectionHelper internalReflectionHelper;

	@InjectMocks
	private AutowiredAnnotationDataPostProcessor instance;

	private Field mockField(boolean autowired) {
		return mockField(autowired, Component.DEFAULT_VALUE);
	}

	private Field mockField(boolean autowired, String value) {
		Field f = mock(Field.class);
		when(f.isAnnotationPresent(Autowired.class)).thenReturn(autowired);
		if (autowired) {
			Autowired annotation = mock(Autowired.class);
			when(annotation.value()).thenReturn(value);
			when(f.getAnnotation(Autowired.class)).thenReturn(annotation);
		}
		return f;
	}

	@Test
	public void processBean() throws IllegalAccessException {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Autowired.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "target", new HashMap<>())
		));

		TestBean bean = new TestBean();
		TestBean dependencyBean = new TestBean();

		when(internalReflectionHelper.classOrSuperEquals(Type.getType(TestBean.class), bean.getClass())).thenReturn(true);

		Field target = mockField(true);
		when(internalReflectionHelper.getAllAutowiredFieldsIncludingSuper(bean.getClass(), "target", Component.DEFAULT_VALUE)).thenReturn(List.of(target));

		when(internalReflectionHelper.isStatic(target)).thenReturn(false);

		BeanContext.BeanContextInternalInjector injector = mock(BeanContext.BeanContextInternalInjector.class);
		doReturn(dependencyBean).when(injector).inject(isNull(), isNull());

		assertDoesNotThrow(() -> instance.process(injector, null, scanData, bean, new AtomicReference<>()));

		verify(target, times(1)).trySetAccessible();
		verify(target, times(1)).set(bean, dependencyBean);
	}

	@Test
	public void processBeanNamed() throws IllegalAccessException {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Autowired.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "target", Map.of("value", "test"))
		));

		TestBean bean = new TestBean();
		TestBean dependencyBean = new TestBean();

		when(internalReflectionHelper.classOrSuperEquals(Type.getType(TestBean.class), bean.getClass())).thenReturn(true);

		Field target = mockField(true, "test");
		when(internalReflectionHelper.getAllAutowiredFieldsIncludingSuper(bean.getClass(), "target", "test")).thenReturn(List.of(target));

		when(internalReflectionHelper.isStatic(target)).thenReturn(false);

		BeanContext.BeanContextInternalInjector injector = mock(BeanContext.BeanContextInternalInjector.class);
		doReturn(dependencyBean).when(injector).inject(isNull(), eq("test"));

		assertDoesNotThrow(() -> instance.process(injector, null, scanData, bean, new AtomicReference<>()));

		verify(target, times(1)).trySetAccessible();
		verify(target, times(1)).set(bean, dependencyBean);
	}

	@Test
	public void processBeanStaticMember() throws IllegalAccessException {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Autowired.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "target", new HashMap<>())
		));

		TestBean bean = new TestBean();
		TestBean dependencyBean = new TestBean();

		when(internalReflectionHelper.classOrSuperEquals(Type.getType(TestBean.class), bean.getClass())).thenReturn(true);

		Field target = mockField(true);
		when(internalReflectionHelper.getAllAutowiredFieldsIncludingSuper(bean.getClass(), "target", Component.DEFAULT_VALUE)).thenReturn(List.of(target));

		when(internalReflectionHelper.isStatic(target)).thenReturn(true);

		BeanContext.BeanContextInternalInjector injector = mock(BeanContext.BeanContextInternalInjector.class);
		doReturn(dependencyBean).when(injector).inject(isNull(), isNull());

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> instance.process(injector, null, scanData, bean, new AtomicReference<>()));

		assertEquals("@Autowired fields must be non-static inside Beans", exception.getMessage());

		verify(target, never()).trySetAccessible();
		verify(target, never()).set(bean, dependencyBean);
	}

	@Test
	public void processNonBean() throws NoSuchFieldException, IllegalAccessException {
		ModFileScanData scanData = mock(ModFileScanData.class);

		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Configurable.class, Component.class, Mod.class)).thenReturn(Stream.empty());

		when(distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Autowired.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "target", new HashMap<>())
		));

		TestBean dependencyBean = new TestBean();

		when(internalReflectionHelper.isAnyAnnotationPresent(TestBean.class, Configurable.class, Component.class, Mod.class)).thenReturn(false);

		Field target = mockField(true);
		doReturn(TestBean.class).when(target).getType();
		when(internalReflectionHelper.getDeclaredField(TestBean.class, "target")).thenReturn(target);
		when(internalReflectionHelper.isStatic(target)).thenReturn(true);

		BeanContext.BeanContextInternalInjector injector = mock(BeanContext.BeanContextInternalInjector.class);
		when(injector.inject(TestBean.class, null)).thenReturn(dependencyBean);

		assertDoesNotThrow(() -> instance.process(injector, null, scanData, new AtomicReference<>()));

		verify(target, times(1)).trySetAccessible();
		verify(target, times(1)).set(null, dependencyBean);
	}

	@Test
	public void processNonBeanNamed() throws NoSuchFieldException, IllegalAccessException {
		ModFileScanData scanData = mock(ModFileScanData.class);

		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Configurable.class, Component.class, Mod.class)).thenReturn(Stream.empty());

		when(distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Autowired.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "target", Map.of("value", "test"))
		));

		TestBean dependencyBean = new TestBean();

		when(internalReflectionHelper.isAnyAnnotationPresent(TestBean.class, Configurable.class, Component.class, Mod.class)).thenReturn(false);

		Field target = mockField(true, "test");
		doReturn(TestBean.class).when(target).getType();
		when(internalReflectionHelper.getDeclaredField(TestBean.class, "target")).thenReturn(target);
		when(internalReflectionHelper.isStatic(target)).thenReturn(true);

		BeanContext.BeanContextInternalInjector injector = mock(BeanContext.BeanContextInternalInjector.class);
		when(injector.inject(TestBean.class, "test")).thenReturn(dependencyBean);

		assertDoesNotThrow(() -> instance.process(injector, null, scanData, new AtomicReference<>()));

		verify(target, times(1)).trySetAccessible();
		verify(target, times(1)).set(null, dependencyBean);
	}

	@Test
	public void processNonBeanNonStatic() throws NoSuchFieldException, IllegalAccessException {
		ModFileScanData scanData = mock(ModFileScanData.class);

		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Configurable.class, Component.class, Mod.class)).thenReturn(Stream.empty());

		when(distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Autowired.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "target", new HashMap<>())
		));

		TestBean dependencyBean = new TestBean();

		when(internalReflectionHelper.isAnyAnnotationPresent(TestBean.class, Configurable.class, Component.class, Mod.class)).thenReturn(false);

		Field target = mockField(true);
		doReturn(TestBean.class).when(target).getType();
		when(internalReflectionHelper.getDeclaredField(TestBean.class, "target")).thenReturn(target);
		when(internalReflectionHelper.isStatic(target)).thenReturn(false);

		BeanContext.BeanContextInternalInjector injector = mock(BeanContext.BeanContextInternalInjector.class);
		when(injector.inject(TestBean.class, null)).thenReturn(dependencyBean);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> instance.process(injector, null, scanData, new AtomicReference<>()));

		assertEquals("@Autowired fields must be static outside of Beans", exception.getMessage());

		verify(target, never()).trySetAccessible();
		verify(target, never()).set(null, dependencyBean);
	}

	@Test
	public void processNonBeanNonStaticContextContains() throws NoSuchFieldException, IllegalAccessException {
		ModFileScanData scanData = mock(ModFileScanData.class);

		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Configurable.class, Component.class, Mod.class)).thenReturn(Stream.empty());

		when(distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Autowired.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "target", new HashMap<>())
		));

		TestBean dependencyBean = new TestBean();

		when(internalReflectionHelper.isAnyAnnotationPresent(TestBean.class, Configurable.class, Component.class, Mod.class)).thenReturn(false);

		Field target = mockField(true);
		doReturn(TestBean.class).when(target).getType();
		when(internalReflectionHelper.getDeclaredField(TestBean.class, "target")).thenReturn(target);
		when(internalReflectionHelper.isStatic(target)).thenReturn(false);

		BeanContext.BeanContextInternalInjector injector = mock(BeanContext.BeanContextInternalInjector.class);
		when(injector.inject(TestBean.class, null)).thenReturn(dependencyBean);

		when(injector.contains(TestBean.class, null)).thenReturn(true);

		assertDoesNotThrow(() -> instance.process(injector, null, scanData, new AtomicReference<>()));

		verify(target, never()).trySetAccessible();
		verify(target, never()).set(null, dependencyBean);
	}

	@Test
	public void processNonBeanNonStaticIgnored() throws NoSuchFieldException, IllegalAccessException {
		ModFileScanData scanData = mock(ModFileScanData.class);

		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Configurable.class, Component.class, Mod.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "target", new HashMap<>())
		));

		when(distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Autowired.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "target", new HashMap<>())
		));

		TestBean dependencyBean = new TestBean();

		when(internalReflectionHelper.isAnyAnnotationPresent(TestBean.class, Configurable.class, Component.class, Mod.class)).thenReturn(false);

		Field target = mockField(true);
		doReturn(TestBean.class).when(target).getType();
		when(internalReflectionHelper.getDeclaredField(TestBean.class, "target")).thenReturn(target);
		when(internalReflectionHelper.isStatic(target)).thenReturn(true);

		BeanContext.BeanContextInternalInjector injector = mock(BeanContext.BeanContextInternalInjector.class);
		when(injector.inject(TestBean.class, null)).thenReturn(dependencyBean);

		when(injector.contains(TestBean.class, null)).thenReturn(true);

		assertDoesNotThrow(() -> instance.process(injector, null, scanData, new AtomicReference<>()));

		verify(target, never()).trySetAccessible();
		verify(target, never()).set(null, dependencyBean);
	}

	@Test
	public void processNonBeanNonStaticPresent() throws NoSuchFieldException, IllegalAccessException {
		ModFileScanData scanData = mock(ModFileScanData.class);

		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Configurable.class, Component.class, Mod.class)).thenReturn(Stream.empty());

		when(distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Autowired.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "target", new HashMap<>())
		));

		TestBean dependencyBean = new TestBean();

		when(internalReflectionHelper.isAnyAnnotationPresent(TestBean.class, Configurable.class, Component.class, Mod.class)).thenReturn(true);

		Field target = mockField(true);
		doReturn(TestBean.class).when(target).getType();
		when(internalReflectionHelper.getDeclaredField(TestBean.class, "target")).thenReturn(target);
		when(internalReflectionHelper.isStatic(target)).thenReturn(true);

		BeanContext.BeanContextInternalInjector injector = mock(BeanContext.BeanContextInternalInjector.class);
		when(injector.inject(TestBean.class, null)).thenReturn(dependencyBean);

		when(injector.contains(TestBean.class, null)).thenReturn(true);

		assertDoesNotThrow(() -> instance.process(injector, null, scanData, new AtomicReference<>()));

		verify(target, never()).trySetAccessible();
		verify(target, never()).set(null, dependencyBean);
	}

}
