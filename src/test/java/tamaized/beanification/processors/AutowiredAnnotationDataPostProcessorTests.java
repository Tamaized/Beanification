package tamaized.beanification.processors;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.objectweb.asm.Type;
import tamaized.beanification.*;
import tamaized.beanification.internal.DistAnnotationRetriever;

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("rawtypes")
public class AutowiredAnnotationDataPostProcessorTests {

	private AutowiredAnnotationDataPostProcessor instance;

	@BeforeEach
	public void beforeEach() {
		instance = new AutowiredAnnotationDataPostProcessor();
		TestBean.beanC = null;
		TestBean.beanD = null;
	}

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

	/*@Test
	public void processBean() {
		try (MockedStatic<DistAnnotationRetriever> distAnnotationRetriever = mockStatic(DistAnnotationRetriever.class)) {
			ModFileScanData scanData = mock(ModFileScanData.class);
			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Autowired.class, ElementType.FIELD)).thenReturn(Stream.of(
				new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "beanA", new HashMap<>()),
				new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "beanB", Map.of("value", "test"))
			));

			TestBean beanA = new TestBean();
			TestBean beanB = new TestBean();

			BeanContext.BeanContextInternalInjector injector = mock(BeanContext.BeanContextInternalInjector.class);
			when(injector.inject(TestBean.class, null)).thenReturn(beanA);
			when(injector.inject(TestBean.class, "test")).thenReturn(beanB);

			TestBean bean = new TestBean();

			try (MockedStatic<Class> clazz = mockStatic(Class.class)) {
				clazz.when(() -> TestBean.class.getDeclaredField("beanA")).thenReturn(mockField(true));
				clazz.when(() -> TestBean.class.getDeclaredField("beanB")).thenReturn(mockField(true, "test"));

				assertDoesNotThrow(() -> instance.process(injector, null, scanData, bean, new AtomicReference<>()));
			}

			assertSame(beanA, bean.beanA);
			assertSame(beanB, bean.beanB);
			assertNull(TestBean.beanC);
			assertNull(TestBean.beanD);
		}
	}

	@Test
	public void processBeanStaticMember() {
		try (MockedStatic<DistAnnotationRetriever> distAnnotationRetriever = mockStatic(DistAnnotationRetriever.class)) {
			ModFileScanData scanData = mock(ModFileScanData.class);
			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Autowired.class, ElementType.FIELD)).thenReturn(Stream.of(
				new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "beanC", new HashMap<>())
			));

			BeanContext.BeanContextInternalInjector injector = mock(BeanContext.BeanContextInternalInjector.class);

			TestBean bean = new TestBean();

			try (MockedStatic<Class> clazz = mockStatic(Class.class)) {
				clazz.when(() -> TestBean.class.getDeclaredField("beanC")).thenReturn(mockField(true));

				IllegalStateException exception = assertThrows(IllegalStateException.class, () -> instance.process(injector, null, scanData, bean, new AtomicReference<>()));

				assertEquals("@Autowired fields must be non-static inside Beans", exception.getMessage());
			}

			assertNull(TestBean.beanC);
			assertNull(TestBean.beanD);
		}
	}

	@Test
	public void processBeanStaticMemberNamed() {
		try (MockedStatic<DistAnnotationRetriever> distAnnotationRetriever = mockStatic(DistAnnotationRetriever.class)) {
			ModFileScanData scanData = mock(ModFileScanData.class);
			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Autowired.class, ElementType.FIELD)).thenReturn(Stream.of(
				new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "beanD", Map.of("value", "test"))
			));

			BeanContext.BeanContextInternalInjector injector = mock(BeanContext.BeanContextInternalInjector.class);

			TestBean bean = new TestBean();

			try (MockedStatic<Class> clazz = mockStatic(Class.class)) {
				clazz.when(() -> TestBean.class.getDeclaredField("beanD")).thenReturn(mockField(true));

				IllegalStateException exception = assertThrows(IllegalStateException.class, () -> instance.process(injector, null, scanData, bean, new AtomicReference<>()));

				assertEquals("@Autowired fields must be non-static inside Beans", exception.getMessage());
			}

			assertNull(TestBean.beanC);
			assertNull(TestBean.beanD);
		}
	}

	@Test
	public void processNonBean() {
		try (MockedStatic<DistAnnotationRetriever> distAnnotationRetriever = mockStatic(DistAnnotationRetriever.class)) {
			ModFileScanData scanData = mock(ModFileScanData.class);

			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Configurable.class, ElementType.TYPE)).thenReturn(Stream.empty());
			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Component.class, ElementType.TYPE)).thenReturn(Stream.empty());
			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Mod.class, ElementType.TYPE)).thenReturn(Stream.empty());

			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Autowired.class, ElementType.FIELD)).thenReturn(Stream.of(
				new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "beanC", new HashMap<>()),
				new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "beanD", Map.of("value", "test"))
			));

			TestBean beanA = new TestBean();
			TestBean beanB = new TestBean();

			BeanContext.BeanContextInternalInjector injector = mock(BeanContext.BeanContextInternalInjector.class);
			when(injector.inject(TestBean.class, null)).thenReturn(beanA);
			when(injector.inject(TestBean.class, "test")).thenReturn(beanB);

			try (MockedStatic<Class> clazz = mockStatic(Class.class)) {
				clazz.when(() -> TestBean.class.getDeclaredField("beanC")).thenReturn(mockField(true));
				clazz.when(() -> TestBean.class.getDeclaredField("beanD")).thenReturn(mockField(true, "test"));

				assertDoesNotThrow(() -> instance.process(injector, null, scanData, new AtomicReference<>()));
			}

			assertSame(beanA, TestBean.beanC);
			assertSame(beanB, TestBean.beanD);
		}
	}

	@Test
	public void processNonBeanNonStaticMember() {
		try (MockedStatic<DistAnnotationRetriever> distAnnotationRetriever = mockStatic(DistAnnotationRetriever.class)) {
			ModFileScanData scanData = mock(ModFileScanData.class);

			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Configurable.class, ElementType.TYPE)).thenReturn(Stream.empty());
			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Component.class, ElementType.TYPE)).thenReturn(Stream.empty());
			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Mod.class, ElementType.TYPE)).thenReturn(Stream.empty());

			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Autowired.class, ElementType.FIELD)).thenReturn(Stream.of(
				new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "beanA", new HashMap<>())
			));

			BeanContext.BeanContextInternalInjector injector = mock(BeanContext.BeanContextInternalInjector.class);

			try (MockedStatic<Class<?>> clazz = mockStatic(TestBean.class.getClass())) {
				clazz.when(() -> TestBean.class.getDeclaredField("beanA")).thenReturn(mockField(true));

				IllegalStateException exception = assertThrows(IllegalStateException.class, () -> instance.process(injector, null, scanData, new AtomicReference<>()));

				assertEquals("@Autowired fields must be static outside of Beans", exception.getMessage());
			}

			assertNull(TestBean.beanC);
			assertNull(TestBean.beanD);
		}
	}

	@Test
	public void processNonBeanNonStaticMemberNamed() {
		try (MockedStatic<DistAnnotationRetriever> distAnnotationRetriever = mockStatic(DistAnnotationRetriever.class)) {
			ModFileScanData scanData = mock(ModFileScanData.class);

			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Configurable.class, ElementType.TYPE)).thenReturn(Stream.empty());
			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Component.class, ElementType.TYPE)).thenReturn(Stream.empty());
			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Mod.class, ElementType.TYPE)).thenReturn(Stream.empty());

			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Autowired.class, ElementType.FIELD)).thenReturn(Stream.of(
				new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "beanB", Map.of("value", "test"))
			));

			BeanContext.BeanContextInternalInjector injector = mock(BeanContext.BeanContextInternalInjector.class);

			try (MockedStatic<Class> clazz = mockStatic(Class.class)) {
				clazz.when(() -> TestBean.class.getDeclaredField("beanB")).thenReturn(mockField(true, "test"));

				IllegalStateException exception = assertThrows(IllegalStateException.class, () -> instance.process(injector, null, scanData, new AtomicReference<>()));

				assertEquals("@Autowired fields must be static outside of Beans", exception.getMessage());
			}

			assertNull(TestBean.beanC);
			assertNull(TestBean.beanD);
		}
	}*/

	/*@Test
	public void processNonBeanConfigurableIgnored() {
		try (MockedStatic<DistAnnotationRetriever> distAnnotationRetriever = mockStatic(DistAnnotationRetriever.class)) {
			ModFileScanData scanData = mock(ModFileScanData.class);

			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Configurable.class, ElementType.TYPE)).thenReturn(Stream.of(
				new ModFileScanData.AnnotationData(null, null, Type.getType(ConfigurableTestBean.class), "test", new HashMap<>())
			));
			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Component.class, ElementType.TYPE)).thenReturn(Stream.empty());
			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Mod.class, ElementType.TYPE)).thenReturn(Stream.empty());

			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Autowired.class, ElementType.FIELD)).thenReturn(Stream.of(
				new ModFileScanData.AnnotationData(null, null, Type.getType(ConfigurableTestBean.class), "test", new HashMap<>())
			));

			BeanContext.BeanContextInternalInjector injector = mock(BeanContext.BeanContextInternalInjector.class);
			when(injector.inject(TestBean.class, null)).thenReturn(new TestBean());

			assertDoesNotThrow(() -> instance.process(injector, null, scanData, new AtomicReference<>()));

			assertNull(ConfigurableTestBean.test);
		}
	}

	@Test
	public void processNonBeanConfigurablePresent() {
		try (MockedStatic<DistAnnotationRetriever> distAnnotationRetriever = mockStatic(DistAnnotationRetriever.class)) {
			ModFileScanData scanData = mock(ModFileScanData.class);

			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Configurable.class, ElementType.TYPE)).thenReturn(Stream.empty());
			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Component.class, ElementType.TYPE)).thenReturn(Stream.empty());
			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Mod.class, ElementType.TYPE)).thenReturn(Stream.empty());

			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Autowired.class, ElementType.FIELD)).thenReturn(Stream.of(
				new ModFileScanData.AnnotationData(null, null, Type.getType(ConfigurableTestBean.class), "test", new HashMap<>())
			));

			BeanContext.BeanContextInternalInjector injector = mock(BeanContext.BeanContextInternalInjector.class);
			when(injector.inject(TestBean.class, null)).thenReturn(new TestBean());

			assertDoesNotThrow(() -> instance.process(injector, null, scanData, new AtomicReference<>()));

			assertNull(ConfigurableTestBean.test);
		}
	}

	@Test
	public void processNonBeanComponentIgnored() {
		try (MockedStatic<DistAnnotationRetriever> distAnnotationRetriever = mockStatic(DistAnnotationRetriever.class)) {
			ModFileScanData scanData = mock(ModFileScanData.class);

			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Configurable.class, ElementType.TYPE)).thenReturn(Stream.empty());
			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Component.class, ElementType.TYPE)).thenReturn(Stream.of(
				new ModFileScanData.AnnotationData(null, null, Type.getType(ComponentTestBean.class), "test", new HashMap<>())
			));
			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Mod.class, ElementType.TYPE)).thenReturn(Stream.empty());

			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Autowired.class, ElementType.FIELD)).thenReturn(Stream.of(
				new ModFileScanData.AnnotationData(null, null, Type.getType(ComponentTestBean.class), "test", new HashMap<>())
			));

			BeanContext.BeanContextInternalInjector injector = mock(BeanContext.BeanContextInternalInjector.class);
			when(injector.inject(TestBean.class, null)).thenReturn(new TestBean());

			assertDoesNotThrow(() -> instance.process(injector, null, scanData, new AtomicReference<>()));

			assertNull(ComponentTestBean.test);
		}
	}

	@Test
	public void processNonBeanComponentPresent() {
		try (MockedStatic<DistAnnotationRetriever> distAnnotationRetriever = mockStatic(DistAnnotationRetriever.class)) {
			ModFileScanData scanData = mock(ModFileScanData.class);

			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Configurable.class, ElementType.TYPE)).thenReturn(Stream.empty());
			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Component.class, ElementType.TYPE)).thenReturn(Stream.empty());
			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Mod.class, ElementType.TYPE)).thenReturn(Stream.empty());

			distAnnotationRetriever.when(() -> DistAnnotationRetriever.retrieve(scanData, Autowired.class, ElementType.FIELD)).thenReturn(Stream.of(
				new ModFileScanData.AnnotationData(null, null, Type.getType(ComponentTestBean.class), "test", new HashMap<>())
			));

			BeanContext.BeanContextInternalInjector injector = mock(BeanContext.BeanContextInternalInjector.class);
			when(injector.inject(TestBean.class, null)).thenReturn(new TestBean());

			assertDoesNotThrow(() -> instance.process(injector, null, scanData, new AtomicReference<>()));

			assertNull(ComponentTestBean.test);
		}
	}*/

	private static class TestBean {

		private TestBean beanA;

		private TestBean beanB;

		private static TestBean beanC;

		private static TestBean beanD;

	}

}
