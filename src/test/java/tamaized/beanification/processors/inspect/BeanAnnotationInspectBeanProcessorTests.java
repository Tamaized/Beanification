package tamaized.beanification.processors.inspect;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.objectweb.asm.Type;
import tamaized.beanification.*;
import tamaized.beanification.internal.DependencyInspector;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.junit.MockitoFixer;
import tamaized.beanification.junit.MockitoRunner;
import tamaized.beanification.processors.BeanAnnotationProcessorMetadata;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoFixer.class, MockitoRunner.class})
public class BeanAnnotationInspectBeanProcessorTests {

	@Mock
	private DistAnnotationRetriever distAnnotationRetriever;

	@Mock
	private InternalReflectionHelper internalReflectionHelper;

	@Mock
	private DependencyInspector dependencyInspector;

	@InjectMocks
	private BeanAnnotationInspectBeanProcessor instance;

	@Test
	public void processNoArgs() {
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));

		Method target = mock(Method.class);

		when(internalReflectionHelper.getDeclaredMethodsForName(TestBean.class, "method")).thenReturn(List.of(target));
		when(target.isAnnotationPresent(Bean.class)).thenReturn(true);

		when(internalReflectionHelper.isStatic(target)).thenReturn(true);

		Bean bean = mock(Bean.class);
		when(target.getAnnotation(Bean.class)).thenReturn(bean);
		when(bean.value()).thenReturn("test");

		when(target.getParameterCount()).thenReturn(0);

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData, new BeanAnnotationProcessorMetadata()));

		verify(target).trySetAccessible();

		verify(context, never()).dependencies();
	}

	@Test
	public void processWithArgs() {
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>()),
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));

		Method target = mock(Method.class);

		when(internalReflectionHelper.getDeclaredMethodsForName(TestBean.class, "method")).thenReturn(List.of(target));
		when(target.isAnnotationPresent(Bean.class)).thenReturn(true);

		when(internalReflectionHelper.isStatic(target)).thenReturn(true);

		Bean bean = mock(Bean.class);
		when(target.getAnnotation(Bean.class)).thenReturn(bean);
		when(bean.value()).thenReturn("A", "A", "B", "B"); // #value is called twice per iteration

		when(target.getParameterCount()).thenReturn(1);

		Annotation[][] paramAnnotations = new Annotation[0][0];
		when(target.getParameterAnnotations()).thenReturn(paramAnnotations);
		when(internalReflectionHelper.allParametersHaveAnnotation(paramAnnotations, Autowired.class, Directory.class)).thenReturn(true);

		when(bean.priority()).thenReturn(5, 3);
		doReturn(TestBean.class).when(target).getReturnType();

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		Parameter[] params = new Parameter[0];
		when(target.getParameters()).thenReturn(params);
		doReturn(
			List.of(new BeanDefinition<>(TestBean.class, "C")),
			List.of(new BeanDefinition<>(TestBean.class, "D"))
		).when(dependencyInspector).inspect(context, params);

		LinkedHashMap<BeanDefinition<?>, List<BeanDefinition<?>>> map = new LinkedHashMap<>();
		when(context.dependencies()).thenReturn(Optional.of(map));

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData, new BeanAnnotationProcessorMetadata()));

		verify(target, times(2)).trySetAccessible();

		assertEquals(2, map.size());

		assertEquals("B", map.sequencedEntrySet().getFirst().getKey().name());
		assertEquals(1, map.sequencedEntrySet().getFirst().getValue().size());
		assertEquals("D", map.sequencedEntrySet().getFirst().getValue().getFirst().name());

		assertEquals("A", map.sequencedEntrySet().getLast().getKey().name());
		assertEquals(1, map.sequencedEntrySet().getLast().getValue().size());
		assertEquals("C", map.sequencedEntrySet().getLast().getValue().getFirst().name());
	}

	@Test
	public void processNonStatic() {
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));

		Method target = mock(Method.class);

		when(internalReflectionHelper.getDeclaredMethodsForName(TestBean.class, "method")).thenReturn(List.of(target));
		when(target.isAnnotationPresent(Bean.class)).thenReturn(true);

		when(internalReflectionHelper.isStatic(target)).thenReturn(false);

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> instance.process(context, modContainer, scanData, new BeanAnnotationProcessorMetadata()));

		assertEquals("@Bean methods must be static", exception.getMessage());
	}

	@Test
	public void processInvalidArgs() {
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>()),
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));

		Method target = mock(Method.class);

		when(internalReflectionHelper.getDeclaredMethodsForName(TestBean.class, "method")).thenReturn(List.of(target));
		when(target.isAnnotationPresent(Bean.class)).thenReturn(true);

		when(internalReflectionHelper.isStatic(target)).thenReturn(true);

		Bean bean = mock(Bean.class);
		when(target.getAnnotation(Bean.class)).thenReturn(bean);
		when(bean.value()).thenReturn("A", "A", "B", "B"); // #value is called twice per iteration

		when(target.getParameterCount()).thenReturn(1);

		Annotation[][] paramAnnotations = new Annotation[0][0];
		when(target.getParameterAnnotations()).thenReturn(paramAnnotations);
		when(internalReflectionHelper.allParametersHaveAnnotation(paramAnnotations, Autowired.class, Directory.class)).thenReturn(false);

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> instance.process(context, modContainer, scanData, new BeanAnnotationProcessorMetadata()));

		assertEquals("@Bean method parameters must be annotated with @Autowired or @Directory", exception.getMessage());
	}

	@Test
	public void processDuplicateArgs() {
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>()),
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));

		Method target = mock(Method.class);

		when(internalReflectionHelper.getDeclaredMethodsForName(TestBean.class, "method")).thenReturn(List.of(target));
		when(target.isAnnotationPresent(Bean.class)).thenReturn(true);

		when(internalReflectionHelper.isStatic(target)).thenReturn(true);

		Bean bean = mock(Bean.class);
		when(target.getAnnotation(Bean.class)).thenReturn(bean);
		when(bean.value()).thenReturn("A");

		when(target.getParameterCount()).thenReturn(1);

		Annotation[][] paramAnnotations = new Annotation[0][0];
		when(target.getParameterAnnotations()).thenReturn(paramAnnotations);
		when(internalReflectionHelper.allParametersHaveAnnotation(paramAnnotations, Autowired.class, Directory.class)).thenReturn(true);

		when(bean.priority()).thenReturn(5, 3);
		doReturn(TestBean.class).when(target).getReturnType();

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		Parameter[] params = new Parameter[0];
		when(target.getParameters()).thenReturn(params);
		doReturn(
			List.of(new BeanDefinition<>(TestBean.class, "C")),
			List.of(new BeanDefinition<>(TestBean.class, "D"))
		).when(dependencyInspector).inspect(context, params);

		LinkedHashMap<BeanDefinition<?>, List<BeanDefinition<?>>> map = new LinkedHashMap<>();
		when(context.dependencies()).thenReturn(Optional.of(map));

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> instance.process(context, modContainer, scanData, new BeanAnnotationProcessorMetadata()));

		assertEquals("Duplicate bean detected - {Type: Ltamaized/beanification/TestBean;, Name: A}", exception.getMessage());
	}

}
