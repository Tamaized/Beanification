package tamaized.beanification.internal;

import net.neoforged.neoforgespi.language.ModFileScanData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.objectweb.asm.Type;
import tamaized.beanification.*;
import tamaized.beanification.directory.DirectoryOtherTestBean;
import tamaized.beanification.directory.DirectoryTestBean;
import tamaized.beanification.junit.MockitoRunner;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoRunner.class})
public class ConjoinedParameterInjectorTests {

	@Mock
	private AutowiredParameterInjector autowiredParameterInjector;

	@Mock
	private ListInjector listInjector;

	@InjectMocks
	private ConjoinedParameterInjector instance;

	@Test
	public void inject() {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModFileScanData scanData = mock(ModFileScanData.class);

		Parameter autowiredParam = mock(Parameter.class);
		when(autowiredParam.isAnnotationPresent(Autowired.class)).thenReturn(true);
		when(autowiredParam.isAnnotationPresent(Directory.class)).thenReturn(false);

		Parameter directoryParam = mock(Parameter.class);
		when(directoryParam.isAnnotationPresent(Autowired.class)).thenReturn(false);
		when(directoryParam.isAnnotationPresent(Directory.class)).thenReturn(true);
		Directory directoryAnnotation = mock(Directory.class);
		when(directoryAnnotation.recursive()).thenReturn(true);
		doReturn(TestBean.class).when(directoryAnnotation).value();
		when(directoryParam.getAnnotation(Directory.class)).thenReturn(directoryAnnotation);

		Parameter[] params = new Parameter[] {
			autowiredParam,
			directoryParam
		};

		TestBean objRef = new TestBean();

		assertDoesNotThrow(() -> instance.inject(context, scanData, TestBean.class, params, objRef));

		ArgumentCaptor<Parameter[]> captor = ArgumentCaptor.forClass(Parameter[].class);
		verify(autowiredParameterInjector).inject(eq(context), captor.capture(), eq(objRef));
		assertEquals(1, captor.getValue().length);
		assertEquals(autowiredParam, captor.getValue()[0]);

		verify(listInjector).inject(context, scanData, TestBean.class, TestBean.class, true);
	}

	@Test
	public void injectMissingAnnotation() {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModFileScanData scanData = mock(ModFileScanData.class);

		Parameter param = mock(Parameter.class);
		when(param.getName()).thenReturn("dep");
		when(param.isAnnotationPresent(Autowired.class)).thenReturn(false);
		when(param.isAnnotationPresent(Directory.class)).thenReturn(false);

		Parameter[] params = new Parameter[] {
			param
		};

		TestBean objRef = new TestBean();

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> instance.inject(context, scanData, TestBean.class, params, objRef));

		assertEquals("Could not find any @Autowired or @Directory annotation on parameter: dep", exception.getMessage());

		verify(autowiredParameterInjector, never()).inject(eq(context), any(Parameter[].class), eq(objRef));
		verify(listInjector, never()).inject(eq(context), eq(scanData), eq(TestBean.class), any(), anyBoolean());
	}

}
