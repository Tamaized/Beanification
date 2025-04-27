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
		assertDoesNotThrow(() -> instance.initInternal("beanification"));
	}

}
