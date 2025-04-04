package tamaized.beanification;

import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import tamaized.beanification.junit.MockitoRunner;
import tamaized.beanification.junit.TestConstants;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({MockitoRunner.class})
public class BeanContextTests {

	@InjectMocks
	private BeanContext instance;

	@BeforeEach
	public void beforeEach() {
		ModLoadingContext.get().setActiveContainer(ModList.get().getModContainerById(TestConstants.MODID).orElseThrow());
	}

	@Test
	public void contextLoads() {
		assertDoesNotThrow(() -> instance.initInternal("beanification", registrar -> {}));
	}

	@Test
	public void directBeanRegistration() {
		TestBean bean = new TestBean();
		TestBean namedBean = new TestBean();
		instance.initInternal("beanification", registrar -> {
			registrar.register(TestBean.class, bean);
			registrar.register(TestBean.class, "named", namedBean);
		});

		assertSame(bean, instance.injectInternal(TestBean.class, null));
		assertSame(namedBean, instance.injectInternal(TestBean.class, "named"));
	}

}
