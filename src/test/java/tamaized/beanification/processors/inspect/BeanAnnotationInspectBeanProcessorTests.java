package tamaized.beanification.processors.inspect;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.objectweb.asm.Type;
import tamaized.beanification.*;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.internal.ListInjector;
import tamaized.beanification.junit.MockitoFixer;
import tamaized.beanification.junit.MockitoRunner;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
	private ListInjector listInjector;

	@InjectMocks
	private BeanAnnotationInspectBeanProcessor instance;

	private Parameter mockAutowiredParam(String name) {
		Parameter parameter = mock(Parameter.class);

		Autowired autowired = mock(Autowired.class);
		when(autowired.value()).thenReturn(name);
		when(parameter.isAnnotationPresent(Autowired.class)).thenReturn(true);
		when(parameter.isAnnotationPresent(Directory.class)).thenReturn(false);
		when(parameter.getAnnotation(Autowired.class)).thenReturn(autowired);

		doReturn(TestBean.class).when(parameter).getType();

		return parameter;
	}

	private Parameter mockDirectoryParam(String name) {
		Parameter parameter = mock(Parameter.class);

		Directory directory = mock(Directory.class);
		doReturn(TestBean.class).when(directory).value();
		when(directory.recursive()).thenReturn(true);
		when(parameter.isAnnotationPresent(Autowired.class)).thenReturn(false);
		when(parameter.isAnnotationPresent(Directory.class)).thenReturn(true);
		when(parameter.getAnnotation(Directory.class)).thenReturn(directory);

		doReturn(TestBean.class).when(parameter).getType();

		return parameter;
	}

	@Test
	public void processNoArgs() {
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));

		Method target = mock(Method.class);
		when(internalReflectionHelper.getDeclaredMethodsForName(TestBean.class, "method")).thenReturn(List.of(target));
		when(internalReflectionHelper.isStatic(target)).thenReturn(true);

		Bean bean = mock(Bean.class);
		when(bean.value()).thenReturn(Component.DEFAULT_VALUE);
		when(target.getAnnotation(Bean.class)).thenReturn(bean);
		when(target.isAnnotationPresent(Bean.class)).thenReturn(true);

		when(target.getParameterCount()).thenReturn(0);

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(context, never()).dependencies();
	}

	@Test
	public void processWithArgs() {
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));

		Method target = mock(Method.class);
		when(internalReflectionHelper.getDeclaredMethodsForName(TestBean.class, "method")).thenReturn(List.of(target));
		when(internalReflectionHelper.isStatic(target)).thenReturn(true);

		Bean bean = mock(Bean.class);
		when(bean.value()).thenReturn(Component.DEFAULT_VALUE);
		when(target.getAnnotation(Bean.class)).thenReturn(bean);
		when(target.isAnnotationPresent(Bean.class)).thenReturn(true);

		when(target.getParameterCount()).thenReturn(2);
		Parameter[] parameters = new Parameter[]{
			mockAutowiredParam("p1"),
			mockDirectoryParam("p2")
		};
		when(target.getParameters()).thenReturn(parameters);
		doReturn(TestBean.class).when(target).getReturnType();

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		Map<BeanDefinition<?>, List<BeanDefinition<?>>> depMap = new HashMap<>();
		when(context.dependencies()).thenReturn(Optional.of(depMap));

		when(target.getParameterAnnotations()).thenReturn(new Annotation[0][0]);
		when(internalReflectionHelper.allParametersHaveAnnotation(target.getParameterAnnotations(), Autowired.class, Directory.class)).thenReturn(true);

		doReturn(List.of(new BeanDefinition<>(TestBean.class, "d3"))).when(listInjector).gather(scanData, TestBean.class, TestBean.class, true);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		assertEquals(1, depMap.size());
		List<BeanDefinition<?>> depList = depMap.get(new BeanDefinition<>(TestBean.class, null));
		assertEquals(2, depList.size());
		assertEquals(new BeanDefinition<>(TestBean.class, "p1"), depList.getFirst());
		assertEquals(new BeanDefinition<>(TestBean.class, "d3"), depList.get(1));
	}

	@Test
	public void processWithArgsMissingAutowired() {
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));

		Method target = mock(Method.class);
		when(internalReflectionHelper.getDeclaredMethodsForName(TestBean.class, "method")).thenReturn(List.of(target));
		when(internalReflectionHelper.isStatic(target)).thenReturn(true);

		Bean bean = mock(Bean.class);
		when(bean.value()).thenReturn(Component.DEFAULT_VALUE);
		when(target.getAnnotation(Bean.class)).thenReturn(bean);
		when(target.isAnnotationPresent(Bean.class)).thenReturn(true);

		when(target.getParameterCount()).thenReturn(2);
		Parameter[] parameters = new Parameter[]{
			mockAutowiredParam("p1"),
			mockDirectoryParam("p2")
		};
		when(target.getParameters()).thenReturn(parameters);
		doReturn(TestBean.class).when(target).getReturnType();

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		Map<BeanDefinition<?>, List<BeanDefinition<?>>> depMap = new HashMap<>();
		when(context.dependencies()).thenReturn(Optional.of(depMap));

		when(target.getParameterAnnotations()).thenReturn(new Annotation[0][0]);
		when(internalReflectionHelper.allParametersHaveAnnotation(target.getParameterAnnotations(), Autowired.class, Directory.class)).thenReturn(false);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> instance.process(context, modContainer, scanData));

		assertEquals("@Bean method parameters must be annotated with @Autowired or @Directory", exception.getMessage());

		assertEquals(0, depMap.size());
	}

	@Test
	public void processNotStatic() {
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));

		Method target = mock(Method.class);
		when(target.isAnnotationPresent(Bean.class)).thenReturn(true);
		when(internalReflectionHelper.getDeclaredMethodsForName(TestBean.class, "method")).thenReturn(List.of(target));
		when(internalReflectionHelper.isStatic(target)).thenReturn(false);

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);

		IllegalStateException result = assertThrows(IllegalStateException.class, () -> instance.process(context, modContainer, scanData));

		assertEquals("@Bean methods must be static", result.getMessage());
	}

	@Test
	public void processMissingBeanAnnotation() {
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));

		Method target = mock(Method.class);
		when(internalReflectionHelper.getDeclaredMethodsForName(TestBean.class, "method")).thenReturn(List.of(target));
		when(internalReflectionHelper.isStatic(target)).thenReturn(true);

		when(target.isAnnotationPresent(Bean.class)).thenReturn(false);

		when(target.getParameterCount()).thenReturn(2);

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(context, never()).dependencies();
	}

}
