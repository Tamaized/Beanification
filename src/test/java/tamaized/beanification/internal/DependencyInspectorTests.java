package tamaized.beanification.internal;

import org.apache.groovy.util.Maps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import tamaized.beanification.*;
import tamaized.beanification.junit.MockitoRunner;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoRunner.class})
public class DependencyInspectorTests {

	@InjectMocks
	private DependencyInspector instance;

	@Test
	public void inspect() {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);

		Parameter autowiredParam = mock(Parameter.class);
		when(autowiredParam.isAnnotationPresent(Autowired.class)).thenReturn(true);
		when(autowiredParam.isAnnotationPresent(Directory.class)).thenReturn(false);
		Autowired autowiredAnnotation = mock(Autowired.class);
		when(autowiredAnnotation.value()).thenReturn("test");
		when(autowiredParam.getAnnotation(Autowired.class)).thenReturn(autowiredAnnotation);
		doReturn(TestBean.class).when(autowiredParam).getType();

		Parameter directoryParam = mock(Parameter.class);
		when(directoryParam.isAnnotationPresent(Autowired.class)).thenReturn(false);
		when(directoryParam.isAnnotationPresent(Directory.class)).thenReturn(true);
		Directory directoryAnnotation = mock(Directory.class);
		doReturn(TestBean.class).when(directoryAnnotation).value();
		when(directoryParam.getAnnotation(Directory.class)).thenReturn(directoryAnnotation);

		Parameter invalidParam = mock(Parameter.class);
		when(invalidParam.isAnnotationPresent(Autowired.class)).thenReturn(false);
		when(invalidParam.isAnnotationPresent(Directory.class)).thenReturn(false);

		Parameter[] params = new Parameter[] {
			autowiredParam,
			directoryParam,
			invalidParam
		};

		TestBean depBean = new TestBean();

		when(context.gather()).thenReturn(Optional.of(Maps.of(
			new BeanDefinition<>(TestBean.class, "A"), () -> depBean,
			new BeanDefinition<>(TestBean.class, "B"), () -> depBean
		)));

		List<BeanDefinition<?>> result = instance.inspect(context, params);

		assertEquals(3, result.size());

		assertSame(TestBean.class, result.getFirst().type());
		assertEquals("test", result.getFirst().name());

		assertSame(TestBean.class, result.get(1).type());
		assertEquals("A", result.get(1).name());

		assertSame(TestBean.class, result.get(2).type());
		assertEquals("B", result.get(2).name());

	}

}
