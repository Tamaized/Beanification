package tamaized.beanification.processors.gather;

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
import tamaized.beanification.junit.MockitoFixer;
import tamaized.beanification.junit.MockitoRunner;

import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
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

		TestBean beanInstance = new TestBean();
		when(target.getParameterCount()).thenReturn(0);
		when(target.invoke(null)).thenReturn(beanInstance);
		doReturn(TestBean.class).when(target).getReturnType();

		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method")).thenReturn(target);
		when(internalReflectionHelper.isStatic(target)).thenReturn(true);

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		Map<BeanDefinition<?>, BeanContext.ThrowingSupplier<Object>> gatherMap = new HashMap<>();
		when(context.gather()).thenReturn(Optional.of(gatherMap));

		ModContainer modContainer = mock(ModContainer.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		assertEquals(1, gatherMap.size());
		assertSame(beanInstance, gatherMap.get(new BeanDefinition<>(TestBean.class, null)).get());
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

		TestBean beanInstance = new TestBean();
		when(target.getParameterCount()).thenReturn(0);
		when(target.invoke(null)).thenReturn(beanInstance);
		doReturn(TestBean.class).when(target).getReturnType();

		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method")).thenReturn(target);
		when(internalReflectionHelper.isStatic(target)).thenReturn(true);

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		Map<BeanDefinition<?>, BeanContext.ThrowingSupplier<Object>> gatherMap = new HashMap<>();
		when(context.gather()).thenReturn(Optional.of(gatherMap));

		ModContainer modContainer = mock(ModContainer.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		assertEquals(1, gatherMap.size());
		assertSame(beanInstance, gatherMap.get(new BeanDefinition<>(TestBean.class, "test")).get());
	}

	private Parameter mockParam(String name) {
		Parameter parameter = mock(Parameter.class);

		Autowired autowired = mock(Autowired.class);
		when(autowired.value()).thenReturn(name);
		when(parameter.getAnnotation(Autowired.class)).thenReturn(autowired);

		doReturn(TestBean.class).when(parameter).getType();

		return parameter;
	}

	@Test
	@SuppressWarnings("unchecked")
	public void processWithArgs() throws Throwable {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));

		Method target = mock(Method.class);

		Bean bean = mock(Bean.class);
		when(bean.value()).thenReturn(Component.DEFAULT_VALUE);
		when(target.getAnnotation(Bean.class)).thenReturn(bean);

		TestBean depBean = new TestBean();
		TestBean beanInstance = new TestBean();
		when(target.getParameterCount()).thenReturn(2);
		Parameter[] parameters = new Parameter[]{
			mockParam("p1"),
			mockParam("p2")
		};
		when(target.getParameters()).thenReturn(parameters);
		when(target.invoke(null, depBean, depBean)).thenReturn(beanInstance);
		doReturn(TestBean.class).when(target).getReturnType();

		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method")).thenReturn(target);
		when(internalReflectionHelper.isStatic(target)).thenReturn(true);

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		Map<BeanDefinition<?>, BeanContext.ThrowingSupplier<Object>> gatherMap = new HashMap<>();
		when(context.gather()).thenReturn(Optional.of(gatherMap));
		AtomicReference<Object> refMock = mock(AtomicReference.class);
		when(context.currentInjection()).thenReturn(Optional.of(refMock));
		when(context.injector()).thenReturn(Optional.of(def -> depBean));

		ModContainer modContainer = mock(ModContainer.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		assertEquals(1, gatherMap.size());
		assertSame(beanInstance, gatherMap.get(new BeanDefinition<>(TestBean.class, null)).get());
		verify(context, times(2)).currentInjection();
		verify(refMock, times(2)).set(target);
		verify(context, times(2)).injector();
	}

	@Test
	public void processEmpty() throws Throwable {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class)).thenReturn(Stream.empty());

		Method target = mock(Method.class);

		Bean bean = mock(Bean.class);
		when(bean.value()).thenReturn(Component.DEFAULT_VALUE);
		when(target.getAnnotation(Bean.class)).thenReturn(bean);

		TestBean beanInstance = new TestBean();
		when(target.getParameterCount()).thenReturn(0);
		when(target.invoke(null)).thenReturn(beanInstance);
		doReturn(TestBean.class).when(target).getReturnType();

		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method")).thenReturn(target);
		when(internalReflectionHelper.isStatic(target)).thenReturn(true);

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		Map<BeanDefinition<?>, BeanContext.ThrowingSupplier<Object>> gatherMap = new HashMap<>();
		when(context.gather()).thenReturn(Optional.of(gatherMap));

		ModContainer modContainer = mock(ModContainer.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

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

		TestBean beanInstance = new TestBean();
		when(target.getParameterCount()).thenReturn(0);
		when(target.invoke(null)).thenReturn(beanInstance);
		doReturn(TestBean.class).when(target).getReturnType();

		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method")).thenReturn(target);
		when(internalReflectionHelper.isStatic(target)).thenReturn(false);

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		Map<BeanDefinition<?>, BeanContext.ThrowingSupplier<Object>> gatherMap = new HashMap<>();
		when(context.gather()).thenReturn(Optional.of(gatherMap));

		ModContainer modContainer = mock(ModContainer.class);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> instance.process(context, modContainer, scanData));

		assertEquals("@Bean methods must be static", exception.getMessage());

		assertTrue(gatherMap.isEmpty());
	}

}
