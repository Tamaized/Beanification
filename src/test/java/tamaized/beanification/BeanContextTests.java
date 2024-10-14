package tamaized.beanification;

import org.junit.jupiter.api.Test;
import tamaized.beanification.junit.TestConstants;

public class BeanContextTests {

	@Test
	public void contextLoads() {
		BeanContext.init(TestConstants.MODID);
	}

	private static class TestBean {

	}

}
