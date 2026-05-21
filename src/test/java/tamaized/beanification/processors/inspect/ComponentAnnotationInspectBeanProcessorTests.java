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
import tamaized.beanification.internal.DependencyInspector;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.junit.MockitoFixer;
import tamaized.beanification.junit.MockitoRunner;

import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
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

	@Mock
	private DependencyInspector dependencyInspector;

	@InjectMocks
	private ComponentAnnotationInspectBeanProcessor instance;

	@Test
	public void processNoArgs() {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "TestBean", new HashMap<>())
		));

		Component component = mock(Component.class);
		when(internalReflectionHelper.getAnnotation(TestBean.class, Component.class)).thenReturn(component);

		when(component.value()).thenReturn("A");

		Constructor<?> ctor = mock(Constructor.class);
		doReturn(ctor).when(beanConstructorLocater).locate(TestBean.class);

		when(ctor.getParameterCount()).thenReturn(0);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(context, never()).dependencies();
	}

	@Test
	public void processWithArgs() {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "TestBean", new HashMap<>())
		));

		Component component = mock(Component.class);
		when(internalReflectionHelper.getAnnotation(TestBean.class, Component.class)).thenReturn(component);

		when(component.value()).thenReturn("A");

		Constructor<?> ctor = mock(Constructor.class);
		doReturn(ctor).when(beanConstructorLocater).locate(TestBean.class);

		when(ctor.getParameterCount()).thenReturn(1);

		Map<BeanDefinition<?>, List<BeanDefinition<?>>> deps = new HashMap<>();
		when(context.dependencies()).thenReturn(Optional.of(deps));

		Parameter[] params = new Parameter[0];
		when(ctor.getParameters()).thenReturn(params);
		when(dependencyInspector.inspect(context, params)).thenReturn(List.of(
			new BeanDefinition<>(TestBean.class, "B")
		));

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		assertEquals(1, deps.size());
		assertEquals(new BeanDefinition<>(TestBean.class, "A"), deps.keySet().toArray()[0]);
		assertEquals(new BeanDefinition<>(TestBean.class, "B"), deps.values().toArray(List[]::new)[0].getFirst());
	}

}
