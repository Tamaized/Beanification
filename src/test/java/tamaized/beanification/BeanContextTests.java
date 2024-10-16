package tamaized.beanification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import tamaized.beanification.junit.MockitoRunner;
import tamaized.beanification.junit.TestConstants;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({MockitoRunner.class})
public class BeanContextTests {

	@InjectMocks
	private BeanContext instance;

	@Test
	public void contextLoads() {
		assertDoesNotThrow(() -> instance.initInternal(TestConstants.MODID, registrar -> {}, false));
	}

	@Test
	public void unknownModContainer() {
		assertThrows(NoSuchElementException.class, () -> instance.initInternal("missingno", registrar -> {}, false));
	}

	@Test
	public void directBeanRegistration() {
		TestBean bean = new TestBean();
		TestBean namedBean = new TestBean();
		instance.initInternal(TestConstants.MODID, registrar -> {
			registrar.register(TestBean.class, bean);
			registrar.register(TestBean.class, "named", namedBean);
		}, false);

		assertSame(bean, instance.injectInternal(TestBean.class, null));
		assertSame(namedBean, instance.injectInternal(TestBean.class, "named"));
	}

	private static class TestBean {

	}

}
