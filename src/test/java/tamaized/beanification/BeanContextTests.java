package tamaized.beanification;

import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import tamaized.beanification.junit.MockitoRunner;
import tamaized.beanification.junit.TestConstants;
import tamaized.beanification.processors.BeanAnnotationProcessorMetadata;

import java.util.List;

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
		assertDoesNotThrow(() -> instance.initInternal("beanification", (_) -> {}));
	}

	// @Test
	public void simpleProfileInjectInto() {
		instance.initInternal("beanification", (register) -> register.accept(SimpleDepBean.class, null, new SimpleDepBean()));
		BeanContext.INSTANCE = instance;
		double k1 = 0;
		for (int k = 0; k < 2; k++) {
			System.out.println("============");
			System.out.println(k);
			long sum = 0;
			for (int j = 0; j < 10; j++) {
				long ms = System.currentTimeMillis();
				for (int i = 0; i < 500000; i++) {
					if (k == 1) {
						instance.beanAnnotationProcessorMetadata = new BeanAnnotationProcessorMetadata();
					}
					new SimpleTestBean();
				}
				long result = System.currentTimeMillis() - ms;
				System.out.printf("%s ms elapsed%n", result);
				sum += result;
			}
			double avg = sum / 10D;
			System.out.printf("Avg: %s%n", avg);
			if (k == 0)
				k1 = avg;
			else
				System.out.printf("Perf: %s%%%n", 1D - (k1 / avg));
		}
	}

	public static class SimpleDepBean {

	}

	public static class SimpleTestBean {

		@Autowired
		private SimpleDepBean simpleDepBean;

		@Directory(SimpleDepBean.class)
		private List<SimpleTestBean> deps;

		public SimpleTestBean() {
			BeanContext.injectInto(this);
		}

	}

}
