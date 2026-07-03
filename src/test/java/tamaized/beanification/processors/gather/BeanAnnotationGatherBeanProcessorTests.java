package tamaized.beanification.processors.gather;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.objectweb.asm.Type;
import tamaized.beanification.*;
import tamaized.beanification.internal.AutowiredParameterInjector;
import tamaized.beanification.internal.ConjoinedParameterInjector;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.junit.MockitoFixer;
import tamaized.beanification.junit.MockitoRunner;
import tamaized.beanification.processors.BeanAnnotationProcessorMetadata;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoFixer.class, MockitoRunner.class})
public class BeanAnnotationGatherBeanProcessorTests {

	@Mock
	private DistAnnotationRetriever distAnnotationRetriever;

	@Mock
	private InternalReflectionHelper internalReflectionHelper;

	@Mock
	private ConjoinedParameterInjector conjoinedParameterInjector;

	@InjectMocks
	private BeanAnnotationGatherBeanProcessor instance;

	@Test
	public void process() throws Throwable {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));

		Method target = mock(Method.class);

		Bean bean = mock(Bean.class);
		when(bean.value()).thenReturn(Component.DEFAULT_VALUE);
		when(target.getAnnotation(Bean.class)).thenReturn(bean);
		when(target.isAnnotationPresent(Bean.class)).thenReturn(true);

		TestBean beanInstance = new TestBean();
		when(target.getParameterCount()).thenReturn(0);
		when(target.invoke(null)).thenReturn(beanInstance);
		doReturn(TestBean.class).when(target).getReturnType();

		when(internalReflectionHelper.getDeclaredMethodsForName(TestBean.class, "method")).thenReturn(List.of(target));
		when(internalReflectionHelper.isStatic(target)).thenReturn(true);

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		Map<BeanDefinition<?>, BeanContext.ThrowingSupplier<Object>> gatherMap = new HashMap<>();
		when(context.gather()).thenReturn(Optional.of(gatherMap));

		ModContainer modContainer = mock(ModContainer.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData, new BeanAnnotationProcessorMetadata()));

		assertEquals(1, gatherMap.size());
		assertSame(beanInstance, gatherMap.get(new BeanDefinition<>(TestBean.class, null)).get());
	}

	@Test
	public void processDuplicate() throws Throwable {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));

		Method target = mock(Method.class);

		Bean bean = mock(Bean.class);
		when(bean.value()).thenReturn(Component.DEFAULT_VALUE);
		when(target.getAnnotation(Bean.class)).thenReturn(bean);
		when(target.isAnnotationPresent(Bean.class)).thenReturn(true);

		TestBean beanInstance = new TestBean();
		when(target.getParameterCount()).thenReturn(0);
		when(target.invoke(null)).thenReturn(beanInstance);
		doReturn(TestBean.class).when(target).getReturnType();

		when(internalReflectionHelper.getDeclaredMethodsForName(TestBean.class, "method")).thenReturn(List.of(target, target));
		when(internalReflectionHelper.isStatic(target)).thenReturn(true);

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		Map<BeanDefinition<?>, BeanContext.ThrowingSupplier<Object>> gatherMap = new HashMap<>();
		when(context.gather()).thenReturn(Optional.of(gatherMap));

		ModContainer modContainer = mock(ModContainer.class);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> instance.process(context, modContainer, scanData, new BeanAnnotationProcessorMetadata()));

		assertEquals("Duplicate bean detected - {Type: Ltamaized/beanification/TestBean;, Name: null}", exception.getMessage());

		assertFalse(gatherMap.isEmpty());
	}

	@Test
	public void processNamed() throws Throwable {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", Map.of("value", "test"))
		));

		Method target = mock(Method.class);

		Bean bean = mock(Bean.class);
		when(bean.value()).thenReturn("test");
		when(target.getAnnotation(Bean.class)).thenReturn(bean);
		when(target.isAnnotationPresent(Bean.class)).thenReturn(true);

		TestBean beanInstance = new TestBean();
		when(target.getParameterCount()).thenReturn(0);
		when(target.invoke(null)).thenReturn(beanInstance);
		doReturn(TestBean.class).when(target).getReturnType();

		when(internalReflectionHelper.getDeclaredMethodsForName(TestBean.class, "method")).thenReturn(List.of(target));
		when(internalReflectionHelper.isStatic(target)).thenReturn(true);

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		Map<BeanDefinition<?>, BeanContext.ThrowingSupplier<Object>> gatherMap = new HashMap<>();
		when(context.gather()).thenReturn(Optional.of(gatherMap));

		ModContainer modContainer = mock(ModContainer.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData, new BeanAnnotationProcessorMetadata()));

		assertEquals(1, gatherMap.size());
		assertSame(beanInstance, gatherMap.get(new BeanDefinition<>(TestBean.class, "test")).get());
	}

	private Parameter mockAutowiredParam(String name) {
		Parameter parameter = mock(Parameter.class);

		Autowired autowired = mock(Autowired.class);
		when(autowired.value()).thenReturn(name);
		when(parameter.getAnnotation(Autowired.class)).thenReturn(autowired);

		doReturn(TestBean.class).when(parameter).getType();

		return parameter;
	}

	private Parameter mockDirectoryParam(String name) {
		Parameter parameter = mock(Parameter.class);

		Directory directory = mock(Directory.class);
		doReturn(TestBean.class).when(directory).value();
		when(parameter.getAnnotation(Directory.class)).thenReturn(directory);

		doReturn(TestBean.class).when(parameter).getType();

		return parameter;
	}

	@Test
	public void processWithArgs() throws Throwable {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));

		Method target = mock(Method.class);

		Bean bean = mock(Bean.class);
		when(bean.value()).thenReturn(Component.DEFAULT_VALUE);
		when(target.getAnnotation(Bean.class)).thenReturn(bean);
		when(target.isAnnotationPresent(Bean.class)).thenReturn(true);

		TestBean depBean = new TestBean();
		TestBean beanInstance = new TestBean();
		when(target.getParameterCount()).thenReturn(2);
		Parameter[] parameters = new Parameter[]{
			mockAutowiredParam("p1"),
			mockDirectoryParam("p2")
		};
		when(target.getParameters()).thenReturn(parameters);
		when(target.invoke(null, depBean, depBean)).thenReturn(beanInstance);
		doReturn(TestBean.class).when(target).getReturnType();

		when(internalReflectionHelper.getDeclaredMethodsForName(TestBean.class, "method")).thenReturn(List.of(target));
		when(internalReflectionHelper.isStatic(target)).thenReturn(true);

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		Map<BeanDefinition<?>, BeanContext.ThrowingSupplier<Object>> gatherMap = new HashMap<>();
		when(context.gather()).thenReturn(Optional.of(gatherMap));

		when(conjoinedParameterInjector.inject(context, parameters, target)).thenReturn(new Object[] {depBean, depBean});

		when(target.getParameterAnnotations()).thenReturn(new Annotation[0][0]);
		when(internalReflectionHelper.allParametersHaveAnnotation(target.getParameterAnnotations(), Autowired.class, Directory.class)).thenReturn(true);

		ModContainer modContainer = mock(ModContainer.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData, new BeanAnnotationProcessorMetadata()));

		assertEquals(1, gatherMap.size());
		assertSame(beanInstance, gatherMap.get(new BeanDefinition<>(TestBean.class, null)).get());
	}

	@Test
	public void processWithArgsMissingAutowired() throws Throwable {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));

		Method target = mock(Method.class);

		Bean bean = mock(Bean.class);
		when(bean.value()).thenReturn(Component.DEFAULT_VALUE);
		when(target.getAnnotation(Bean.class)).thenReturn(bean);
		when(target.isAnnotationPresent(Bean.class)).thenReturn(true);

		TestBean depBean = new TestBean();
		TestBean beanInstance = new TestBean();
		when(target.getParameterCount()).thenReturn(2);
		Parameter[] parameters = new Parameter[]{
			mockAutowiredParam("p1"),
			mockDirectoryParam("p2")
		};
		when(target.getParameters()).thenReturn(parameters);
		when(target.invoke(null, depBean, depBean)).thenReturn(beanInstance);
		doReturn(TestBean.class).when(target).getReturnType();

		when(internalReflectionHelper.getDeclaredMethodsForName(TestBean.class, "method")).thenReturn(List.of(target));
		when(internalReflectionHelper.isStatic(target)).thenReturn(true);

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		Map<BeanDefinition<?>, BeanContext.ThrowingSupplier<Object>> gatherMap = new HashMap<>();
		when(context.gather()).thenReturn(Optional.of(gatherMap));

		when(conjoinedParameterInjector.inject(context, parameters, target)).thenReturn(new Object[] {depBean, depBean});

		when(target.getParameterAnnotations()).thenReturn(new Annotation[0][0]);
		when(internalReflectionHelper.allParametersHaveAnnotation(target.getParameterAnnotations(), Autowired.class, Directory.class)).thenReturn(false);

		ModContainer modContainer = mock(ModContainer.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData, new BeanAnnotationProcessorMetadata()));

		assertEquals(1, gatherMap.size());

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> gatherMap.get(new BeanDefinition<>(TestBean.class, null)).get());

		assertEquals("@Bean method parameters must be annotated with @Autowired or @Directory", exception.getMessage());
	}

	@Test
	public void processEmpty() throws Throwable {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class)).thenReturn(Stream.empty());

		Method target = mock(Method.class);

		Bean bean = mock(Bean.class);
		when(bean.value()).thenReturn(Component.DEFAULT_VALUE);
		when(target.getAnnotation(Bean.class)).thenReturn(bean);
		when(target.isAnnotationPresent(Bean.class)).thenReturn(true);

		TestBean beanInstance = new TestBean();
		when(target.getParameterCount()).thenReturn(0);
		when(target.invoke(null)).thenReturn(beanInstance);
		doReturn(TestBean.class).when(target).getReturnType();

		when(internalReflectionHelper.getDeclaredMethodsForName(TestBean.class, "method")).thenReturn(List.of(target));
		when(internalReflectionHelper.isStatic(target)).thenReturn(true);

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		Map<BeanDefinition<?>, BeanContext.ThrowingSupplier<Object>> gatherMap = new HashMap<>();
		when(context.gather()).thenReturn(Optional.of(gatherMap));

		ModContainer modContainer = mock(ModContainer.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData, new BeanAnnotationProcessorMetadata()));

		assertTrue(gatherMap.isEmpty());
	}

	@Test
	public void processNotStatic() throws Throwable {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));

		Method target = mock(Method.class);

		Bean bean = mock(Bean.class);
		when(bean.value()).thenReturn(Component.DEFAULT_VALUE);
		when(target.getAnnotation(Bean.class)).thenReturn(bean);
		when(target.isAnnotationPresent(Bean.class)).thenReturn(true);

		TestBean beanInstance = new TestBean();
		when(target.getParameterCount()).thenReturn(0);
		when(target.invoke(null)).thenReturn(beanInstance);
		doReturn(TestBean.class).when(target).getReturnType();

		when(internalReflectionHelper.getDeclaredMethodsForName(TestBean.class, "method")).thenReturn(List.of(target));
		when(internalReflectionHelper.isStatic(target)).thenReturn(false);

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		Map<BeanDefinition<?>, BeanContext.ThrowingSupplier<Object>> gatherMap = new HashMap<>();
		when(context.gather()).thenReturn(Optional.of(gatherMap));

		ModContainer modContainer = mock(ModContainer.class);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> instance.process(context, modContainer, scanData, new BeanAnnotationProcessorMetadata()));

		assertEquals("@Bean methods must be static", exception.getMessage());

		assertTrue(gatherMap.isEmpty());
	}

	@Test
	public void processMissingBeanAnnotation() {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));

		Method target = mock(Method.class);

		when(target.isAnnotationPresent(Bean.class)).thenReturn(false);

		when(internalReflectionHelper.getDeclaredMethodsForName(TestBean.class, "method")).thenReturn(List.of(target));
		when(internalReflectionHelper.isStatic(target)).thenReturn(false);

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		Map<BeanDefinition<?>, BeanContext.ThrowingSupplier<Object>> gatherMap = new HashMap<>();
		when(context.gather()).thenReturn(Optional.of(gatherMap));

		ModContainer modContainer = mock(ModContainer.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData, new BeanAnnotationProcessorMetadata()));

		assertTrue(gatherMap.isEmpty());
	}

}
