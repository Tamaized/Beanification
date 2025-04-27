package tamaized.beanification.processors.inspect;

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

import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
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
public class ComponentAnnotationInspectBeanProcessorTests {

	@Mock
	private DistAnnotationRetriever distAnnotationRetriever;

	@Mock
	private InternalReflectionHelper internalReflectionHelper;

	@Mock
	private BeanConstructorLocater beanConstructorLocater;

	@InjectMocks
	private ComponentAnnotationInspectBeanProcessor instance;

	private Parameter mockParam(String name) {
		Parameter parameter = mock(Parameter.class);

		Autowired autowired = mock(Autowired.class);
		when(autowired.value()).thenReturn(name);
		when(parameter.getAnnotation(Autowired.class)).thenReturn(autowired);

		doReturn(TestBean.class).when(parameter).getType();

		return parameter;
	}

	@Test
	public void processNoArgs() {
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "TestBean", new HashMap<>())
		));

		Component annotation = mock(Component.class);
		when(internalReflectionHelper.getAnnotation(TestBean.class, Component.class)).thenReturn(annotation);
		when(annotation.value()).thenReturn(Component.DEFAULT_VALUE);

		Constructor<?> ctor = mock(Constructor.class);
		doReturn(ctor).when(beanConstructorLocater).locate(TestBean.class);
		when(ctor.getParameterCount()).thenReturn(0);

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));
	}

	@Test
	public void processWithArgs() {
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "TestBean", new HashMap<>())
		));

		Component annotation = mock(Component.class);
		when(internalReflectionHelper.getAnnotation(TestBean.class, Component.class)).thenReturn(annotation);
		when(annotation.value()).thenReturn(Component.DEFAULT_VALUE);

		Constructor<?> ctor = mock(Constructor.class);
		doReturn(ctor).when(beanConstructorLocater).locate(TestBean.class);
		when(ctor.getParameterCount()).thenReturn(2);

		Parameter[] parameters = new Parameter[] {
			mockParam("p1"),
			mockParam("p2")
		};
		when(ctor.getParameters()).thenReturn(parameters);

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);

		Map<BeanDefinition<?>, List<BeanDefinition<?>>> depMap = new HashMap<>();
		when(context.dependencies()).thenReturn(Optional.of(depMap));

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));
		assertEquals(1, depMap.size());
		List<BeanDefinition<?>> depList = depMap.get(new BeanDefinition<>(TestBean.class, null));
		assertNotNull(depList);
		assertEquals(2, depList.size());
		assertEquals("p1", depList.getFirst().name());
		assertEquals("p2", depList.get(1).name());
	}

}
