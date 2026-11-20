package tamaized.beanification.processors.gather;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import tamaized.beanification.*;
import tamaized.beanification.internal.*;
import tamaized.beanification.junit.MockitoRunner;
import tamaized.beanification.junit.TestScanData;
import tamaized.beanification.processors.BeanAnnotationProcessorMetadata;
import tamaized.beanification.scan.AnnotationScanData;

import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoRunner.class})
public class ComponentAnnotationGatherBeanProcessorTests {

	@Mock
	private BeanConstructorLocater beanConstructorLocater;

	@Mock
	private ConjoinedParameterInjector conjoinedParameterInjector;

	@InjectMocks
	private ComponentAnnotationGatherBeanProcessor instance;

	private List<AnnotationScanData> scanData() {
		return TestScanData.scan(TestScanData.raw(Component.class, ElementType.TYPE, TestBean.class, "TestBean", Map.of()));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void processNoArgs() throws Throwable {
		BeanConstructorLocater.BeanConstructor beanConstructor = mock(BeanConstructorLocater.BeanConstructor.class);
		when(beanConstructorLocater.locate(any(AnnotationScanData.AnnotationData.class))).thenReturn(beanConstructor);

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		Map<BeanDefinition<?>, BeanContext.ThrowingSupplier<Object>> gatherMap = new HashMap<>();
		when(context.gather()).thenReturn(Optional.of(gatherMap));

		doReturn(new BeanDefinition<>(TestBean.class, null)).when(beanConstructor).definition();

		Constructor<TestBean> ctor = mock(Constructor.class);
		doReturn(ctor).when(beanConstructor).ctor();
		when(ctor.getParameterCount()).thenReturn(0);

		TestBean bean = new TestBean();
		when(ctor.newInstance()).thenReturn(bean);

		assertDoesNotThrow(() -> instance.process(context, scanData(), new BeanAnnotationProcessorMetadata()));

		assertEquals(1, gatherMap.size());
		assertSame(bean, gatherMap.get(new BeanDefinition<>(TestBean.class, null)).get());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void processWithArgs() throws Throwable {
		BeanConstructorLocater.BeanConstructor beanConstructor = mock(BeanConstructorLocater.BeanConstructor.class);
		when(beanConstructorLocater.locate(any(AnnotationScanData.AnnotationData.class))).thenReturn(beanConstructor);

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

		assertDoesNotThrow(() -> instance.process(context, scanData(), new BeanAnnotationProcessorMetadata()));

		assertEquals(1, gatherMap.size());
		assertSame(bean, gatherMap.get(new BeanDefinition<>(TestBean.class, null)).get());
	}

}
