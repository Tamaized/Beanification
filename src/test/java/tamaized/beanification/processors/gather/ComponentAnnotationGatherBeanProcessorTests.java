package tamaized.beanification.processors.gather;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.objectweb.asm.Type;
import tamaized.beanification.*;
import tamaized.beanification.internal.BeanConstructorLocater;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.junit.MockitoFixer;
import tamaized.beanification.junit.MockitoRunner;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoFixer.class, MockitoRunner.class})
public class ComponentAnnotationGatherBeanProcessorTests {

	@Mock
	private DistAnnotationRetriever distAnnotationRetriever;

	@Mock
	private InternalReflectionHelper internalReflectionHelper;

	@Mock
	private BeanConstructorLocater beanConstructorLocater;

	@InjectMocks
	private ComponentAnnotationGatherBeanProcessor instance;

	@Test
	@SuppressWarnings("unchecked")
	public void processNoArgs() throws Throwable {
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "TestBean", new HashMap<>())
		));

		Component bean = mock(Component.class);
		when(bean.value()).thenReturn(Component.DEFAULT_VALUE);
		when(internalReflectionHelper.getAnnotation(TestBean.class, Component.class)).thenReturn(bean);

		TestBean resultBean = new TestBean();
		Constructor<TestBean> constructor = mock(Constructor.class);
		when(constructor.getParameterCount()).thenReturn(0);
		when(constructor.newInstance()).thenReturn(resultBean);
		doReturn(constructor).when(beanConstructorLocater).locate(TestBean.class);

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		Map<BeanDefinition<?>, BeanContext.ThrowingSupplier<Object>> gatherMap = new HashMap<>();
		when(context.gather()).thenReturn(Optional.of(gatherMap));

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		assertEquals(1, gatherMap.size());
		assertSame(resultBean, gatherMap.get(new BeanDefinition<>(TestBean.class, null)).get());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void processNoArgsNamed() throws Throwable {
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "TestBean", new HashMap<>())
		));

		Component bean = mock(Component.class);
		when(bean.value()).thenReturn("test");
		when(internalReflectionHelper.getAnnotation(TestBean.class, Component.class)).thenReturn(bean);

		TestBean resultBean = new TestBean();
		Constructor<TestBean> constructor = mock(Constructor.class);
		when(constructor.getParameterCount()).thenReturn(0);
		when(constructor.newInstance()).thenReturn(resultBean);
		doReturn(constructor).when(beanConstructorLocater).locate(TestBean.class);

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		Map<BeanDefinition<?>, BeanContext.ThrowingSupplier<Object>> gatherMap = new HashMap<>();
		when(context.gather()).thenReturn(Optional.of(gatherMap));

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		assertEquals(1, gatherMap.size());
		assertSame(resultBean, gatherMap.get(new BeanDefinition<>(TestBean.class, "test")).get());
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
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "TestBean", new HashMap<>())
		));

		Component bean = mock(Component.class);
		when(bean.value()).thenReturn(Component.DEFAULT_VALUE);
		when(internalReflectionHelper.getAnnotation(TestBean.class, Component.class)).thenReturn(bean);

		TestBean depBean = new TestBean();
		TestBean resultBean = new TestBean();
		Constructor<TestBean> constructor = mock(Constructor.class);
		when(constructor.getParameterCount()).thenReturn(2);
		Parameter[] parameters = new Parameter[]{
			mockParam("p1"),
			mockParam("p2")
		};
		when(constructor.getParameters()).thenReturn(parameters);
		when(constructor.newInstance(depBean, depBean)).thenReturn(resultBean);
		doReturn(constructor).when(beanConstructorLocater).locate(TestBean.class);

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		Map<BeanDefinition<?>, BeanContext.ThrowingSupplier<Object>> gatherMap = new HashMap<>();
		when(context.gather()).thenReturn(Optional.of(gatherMap));
		AtomicReference<Object> refMock = mock(AtomicReference.class);
		when(context.currentInjection()).thenReturn(Optional.of(refMock));
		when(context.injector()).thenReturn(Optional.of(def -> depBean));

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		assertEquals(1, gatherMap.size());
		assertSame(resultBean, gatherMap.get(new BeanDefinition<>(TestBean.class, null)).get());
		verify(context, times(2)).currentInjection();
		verify(refMock, times(2)).set(constructor);
		verify(context, times(2)).injector();
	}

}
