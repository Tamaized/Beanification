package tamaized.beanification.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import tamaized.beanification.Autowired;
import tamaized.beanification.BeanContext;
import tamaized.beanification.BeanDefinition;
import tamaized.beanification.TestBean;
import tamaized.beanification.junit.MockitoRunner;

import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoRunner.class})
public class AutowiredParameterInjectorTests {

	@InjectMocks
	private AutowiredParameterInjector instance;

	private Parameter mockParam(String name) {
		Parameter parameter = mock(Parameter.class);

		Autowired autowired = mock(Autowired.class);
		when(autowired.value()).thenReturn(name);
		when(parameter.getAnnotation(Autowired.class)).thenReturn(autowired);

		doReturn(TestBean.class).when(parameter).getType();

		return parameter;
	}

	@Test
	public void inject() {
		TestBean depBean = new TestBean();

		Parameter[] parameters = new Parameter[]{
			mockParam("p1"),
			mockParam("p2")
		};

		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		Map<BeanDefinition<?>, BeanContext.ThrowingSupplier<Object>> gatherMap = new HashMap<>();
		when(context.gather()).thenReturn(Optional.of(gatherMap));
		AtomicReference<Object> refMock = mock(AtomicReference.class);
		when(context.currentInjection()).thenReturn(Optional.of(refMock));
		when(context.injector()).thenReturn(Optional.of(_ -> depBean));

		TestBean refObj = new TestBean();

		Object[] result = assertDoesNotThrow(() -> instance.inject(context, parameters, refObj));

		assertNotNull(result);
		assertEquals(2, result.length);
		assertSame(depBean, result[0]);
		assertSame(depBean, result[1]);

		verify(context, times(2)).currentInjection();
		verify(refMock, times(2)).set(refObj);
		verify(context, times(2)).injector();
	}

}
