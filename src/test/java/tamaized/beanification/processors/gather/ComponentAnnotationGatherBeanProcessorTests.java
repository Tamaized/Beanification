package tamaized.beanification.processors.gather;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.objectweb.asm.Type;
import tamaized.beanification.*;
import tamaized.beanification.internal.*;
import tamaized.beanification.junit.MockitoFixer;
import tamaized.beanification.junit.MockitoRunner;
import tamaized.beanification.processors.BeanAnnotationProcessorMetadata;

import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoFixer.class, MockitoRunner.class})
public class ComponentAnnotationGatherBeanProcessorTests {

	@Mock
	private DistAnnotationRetriever distAnnotationRetriever;

	@Mock
	private BeanConstructorLocater beanConstructorLocater;

	@Mock
	private ConjoinedParameterInjector conjoinedParameterInjector;

	@InjectMocks
	private ComponentAnnotationGatherBeanProcessor instance;

	@Test
	@SuppressWarnings("unchecked")
	public void processNoArgs() throws Throwable {
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);
		ModFileScanData.AnnotationData data = new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "TestBean", new HashMap<>());
		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class)).thenReturn(Stream.of(
			data
		));

		BeanConstructorLocater.BeanConstructor beanConstructor = mock(BeanConstructorLocater.BeanConstructor.class);
		when(beanConstructorLocater.locate(data)).thenReturn(beanConstructor);

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		Map<BeanDefinition<?>, BeanContext.ThrowingSupplier<Object>> gatherMap = new HashMap<>();
		when(context.gather()).thenReturn(Optional.of(gatherMap));

		doReturn(new BeanDefinition<>(TestBean.class, null)).when(beanConstructor).definition();

		Constructor<TestBean> ctor = mock(Constructor.class);
		doReturn(ctor).when(beanConstructor).ctor();
		when(ctor.getParameterCount()).thenReturn(0);

		TestBean bean = new TestBean();
		when(ctor.newInstance()).thenReturn(bean);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData, new BeanAnnotationProcessorMetadata()));

		assertEquals(1, gatherMap.size());
		assertSame(bean, gatherMap.get(new BeanDefinition<>(TestBean.class, null)).get());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void processWithArgs() throws Throwable {
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);
		ModFileScanData.AnnotationData data = new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "TestBean", new HashMap<>());
		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class)).thenReturn(Stream.of(
			data
		));

		BeanConstructorLocater.BeanConstructor beanConstructor = mock(BeanConstructorLocater.BeanConstructor.class);
		when(beanConstructorLocater.locate(data)).thenReturn(beanConstructor);

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		Map<BeanDefinition<?>, BeanContext.ThrowingSupplier<Object>> gatherMap = new HashMap<>();
		when(context.gather()).thenReturn(Optional.of(gatherMap));

		doReturn(new BeanDefinition<>(TestBean.class, null)).when(beanConstructor).definition();

		Constructor<TestBean> ctor = mock(Constructor.class);
		doReturn(ctor).when(beanConstructor).ctor();
		when(ctor.getParameterCount()).thenReturn(1);

		Parameter[] params = new Parameter[0];
		when(ctor.getParameters()).thenReturn(params);

		TestBean depBean = new TestBean();
		when(conjoinedParameterInjector.inject(context, params, ctor)).thenReturn(new Object[] {depBean});

		TestBean bean = new TestBean();
		when(ctor.newInstance(depBean)).thenReturn(bean);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData, new BeanAnnotationProcessorMetadata()));

		assertEquals(1, gatherMap.size());
		assertSame(bean, gatherMap.get(new BeanDefinition<>(TestBean.class, null)).get());
	}

}
