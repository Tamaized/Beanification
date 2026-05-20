package tamaized.beanification.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import tamaized.beanification.Autowired;
import tamaized.beanification.Directory;
import tamaized.beanification.TestBean;
import tamaized.beanification.junit.MockitoRunner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoRunner.class})
public class BeanConstructorLocaterTests {

	@Mock
	private InternalReflectionHelper internalReflectionHelper;

	@InjectMocks
	private BeanConstructorLocater instance;

	@Test
	public void locate() {
		Constructor<?> ctor = mock(Constructor.class);
		doReturn(new Constructor[] {
			ctor
		}).when(internalReflectionHelper).getConstructors(TestBean.class);

		when(ctor.getParameterCount()).thenReturn(0);

		Constructor<?> result = instance.locate(TestBean.class);

		assertSame(ctor, result);
	}

	@Test
	public void locateOneCtorWithArgs() {
		Constructor<?> ctor = mock(Constructor.class);
		doReturn(new Constructor[] {
			ctor
		}).when(internalReflectionHelper).getConstructors(TestBean.class);

		when(ctor.getParameterCount()).thenReturn(2);
		Annotation[][] params = new Annotation[][] {};
		when(ctor.getParameterAnnotations()).thenReturn(params);

		when(internalReflectionHelper.allParametersHaveAnnotation(params, Autowired.class, Directory.class)).thenReturn(true);

		Constructor<?> result = instance.locate(TestBean.class);

		assertSame(ctor, result);
	}

	@Test
	public void locateMultiCtorWithArgs() {
		Constructor<?> ctor = mock(Constructor.class);
		doReturn(new Constructor[] {
			ctor,
			ctor
		}).when(internalReflectionHelper).getConstructors(TestBean.class);

		when(ctor.getParameterCount()).thenReturn(2);
		Annotation[][] params = new Annotation[][] {};
		when(ctor.getParameterAnnotations()).thenReturn(params);

		when(internalReflectionHelper.allParametersHaveAnnotation(params, Autowired.class, Directory.class)).thenReturn(true);

		when(ctor.toGenericString()).thenReturn("A").thenReturn("B");

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> instance.locate(TestBean.class));

		assertEquals("Conflicting Constructors found: A and B", exception.getMessage());
	}

	@Test
	public void locateOneCtorWithInvalidArgs() {
		Constructor<?> ctor = mock(Constructor.class);
		doReturn(new Constructor[] {
			ctor
		}).when(internalReflectionHelper).getConstructors(TestBean.class);

		when(ctor.getParameterCount()).thenReturn(2);
		Annotation[][] params = new Annotation[][] {};
		when(ctor.getParameterAnnotations()).thenReturn(params);

		when(internalReflectionHelper.allParametersHaveAnnotation(params, Autowired.class, Directory.class)).thenReturn(false);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> instance.locate(TestBean.class));

		assertEquals("Could not find any Constructors in class: class tamaized.beanification.TestBean", exception.getMessage());
	}

}
