package tamaized.beanification.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import tamaized.beanification.junit.MockitoRunner;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

}
