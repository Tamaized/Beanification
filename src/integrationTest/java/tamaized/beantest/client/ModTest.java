package tamaized.beantest.client;

import net.neoforged.fml.common.Mod;
import tamaized.beanification.Autowired;
import tamaized.beanification.BeanContext;
import tamaized.beanification.Component;
import tamaized.beanification.connector.fml.FMLBeanContextConnector;

@Mod("beantest")
public class ModTest {

	static {
		BeanContext.configure().loggingSettings().enableInjectInto();
		FMLBeanContextConnector.init("beantest");
	}

	@Component
	public static class IntegrationComponentTestBean {

		public boolean debug() {
			return true;
		}

	}

	@Autowired
	private IntegrationComponentTestBean test;

	public ModTest() {
		BeanContext.injectInto(this);

		if (!test.debug())
			throw new IllegalStateException("IntegrationTestBean#debug must return true");
	}

}
