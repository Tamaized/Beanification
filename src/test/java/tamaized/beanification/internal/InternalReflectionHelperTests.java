package tamaized.beanification.internal;

import net.neoforged.api.distmarker.Dist;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import tamaized.beanification.Autowired;
import tamaized.beanification.Component;
import tamaized.beanification.junit.MockitoRunner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({MockitoRunner.class})
public class InternalReflectionHelperTests {

	@InjectMocks
	private InternalReflectionHelper instance;

	@Test
	public void getDeclaredMethod() {
		Method method = assertDoesNotThrow(() -> instance.getDeclaredMethod(InternalReflectionHelperTests.class, "testMethod(LRandomJunk;)V"));

		assertEquals("testMethod", method.getName());
	}

	@SuppressWarnings("unused")
	private void testMethod() {

	}

	@Test
	public void allParametersHaveAnnotationTrue() {
		assertTrue(instance.allParametersHaveAnnotation(new Annotation[][]{
			new Annotation[]{makeAutowired()},
			new Annotation[]{makeAutowired(), makeComponent()},
			new Annotation[]{makeComponent(), makeAutowired()},
		}, Autowired.class));
	}

	@Test
	public void allParametersHaveAnnotationFalse() {
		assertFalse(instance.allParametersHaveAnnotation(new Annotation[][]{
			new Annotation[]{},
			new Annotation[]{makeAutowired(), makeComponent()},
			new Annotation[]{makeComponent(), makeAutowired()},
		}, Autowired.class));

		assertFalse(instance.allParametersHaveAnnotation(new Annotation[][]{
			new Annotation[]{makeAutowired()},
			new Annotation[]{makeComponent()},
			new Annotation[]{makeComponent(), makeAutowired()},
		}, Autowired.class));

		assertFalse(instance.allParametersHaveAnnotation(new Annotation[][]{
			new Annotation[]{makeAutowired()},
			new Annotation[]{makeAutowired(), makeComponent()},
			new Annotation[]{makeComponent()},
		}, Autowired.class));
	}

	private Autowired makeAutowired() {
		return new Autowired() {

			@Override
			public Class<? extends Annotation> annotationType() {
				return Autowired.class;
			}

			@Override
			public String value() {
				return "";
			}

			@Override
			public Dist[] dist() {
				return new Dist[0];
			}
		};
	}

	private Component makeComponent() {
		return new Component() {

			@Override
			public Class<? extends Annotation> annotationType() {
				return Component.class;
			}

			@Override
			public String value() {
				return "";
			}

			@Override
			public Dist[] dist() {
				return new Dist[0];
			}
		};
	}

}
