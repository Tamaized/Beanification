package tamaized.beanification.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import tamaized.beanification.*;
import tamaized.beanification.junit.MockitoRunner;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoRunner.class})
public class ConjoinedParameterInjectorTests {

	@Mock
	private AutowiredParameterInjector autowiredParameterInjector;

	@InjectMocks
	private ConjoinedParameterInjector instance;

	@Test
	public void inject() {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);

		Parameter autowiredParam = mock(Parameter.class);
		when(autowiredParam.isAnnotationPresent(Autowired.class)).thenReturn(true);
		when(autowiredParam.isAnnotationPresent(Directory.class)).thenReturn(false);

		Parameter directoryParam = mock(Parameter.class);
		when(directoryParam.isAnnotationPresent(Autowired.class)).thenReturn(false);
		when(directoryParam.isAnnotationPresent(Directory.class)).thenReturn(true);
		Directory directoryAnnotation = mock(Directory.class);
		doReturn(TestBean.class).when(directoryAnnotation).value();
		when(directoryParam.getAnnotation(Directory.class)).thenReturn(directoryAnnotation);

		Parameter[] params = new Parameter[] {
			autowiredParam,
			directoryParam
		};

		TestBean objRef = new TestBean();

		AtomicReference<Object> ref = new AtomicReference<>();
		when(context.currentInjection()).thenReturn(Optional.of(ref));

		List<?> depBeanList = List.of(new TestBean());
		when(context.fuzzyInjector()).thenReturn(Optional.of(_ -> depBeanList));

		Object[] result = assertDoesNotThrow(() -> instance.inject(context, params, objRef));

		ArgumentCaptor<Parameter> captor = ArgumentCaptor.forClass(Parameter.class);
		verify(autowiredParameterInjector).injectSingle(eq(context), captor.capture(), eq(objRef));
		assertEquals(autowiredParam, captor.getValue());

		assertSame(objRef, ref.get());
		assertNotNull(result);
		assertEquals(2, result.length);
		assertSame(depBeanList, result[1]);
	}

	@Test
	public void injectMissingAnnotation() {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);

		Parameter param = mock(Parameter.class);
		when(param.getName()).thenReturn("dep");
		when(param.isAnnotationPresent(Autowired.class)).thenReturn(false);
		when(param.isAnnotationPresent(Directory.class)).thenReturn(false);

		Parameter[] params = new Parameter[] {
			param
		};

		TestBean objRef = new TestBean();

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> instance.inject(context, params, objRef));

		assertEquals("Could not find any @Autowired or @Directory annotation on parameter: dep", exception.getMessage());

		verify(autowiredParameterInjector, never()).inject(eq(context), any(Parameter[].class), eq(objRef));
	}

}
