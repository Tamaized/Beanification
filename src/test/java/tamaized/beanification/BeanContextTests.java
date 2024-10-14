package tamaized.beanification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tamaized.beanification.junit.TestConstants;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.spy;

public class BeanContextTests {

	@BeforeEach
	public void beforeEach() {
		BeanContext.INSTANCE = spy(BeanContext.class);
	}

	@Test
	public void contextLoads() {
		assertDoesNotThrow(() -> BeanContext.init(TestConstants.MODID));
	}

	@Test
	public void unknownModContainer() {
		assertThrows(NoSuchElementException.class, () -> BeanContext.init("missingno"));
	}

	@Test
	public void directBeanRegistration() {
		TestBean bean = new TestBean();
		TestBean namedBean = new TestBean();
		BeanContext.init(TestConstants.MODID, beanContextRegistrar -> {
			beanContextRegistrar.register(TestBean.class, bean);
			beanContextRegistrar.register(TestBean.class, "named", namedBean);
		});

		assertSame(bean, BeanContext.inject(TestBean.class));
		assertSame(namedBean, BeanContext.inject(TestBean.class, "named"));
	}

	private static class TestBean {

	}

}
