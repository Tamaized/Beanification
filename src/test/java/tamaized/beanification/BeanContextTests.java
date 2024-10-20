package tamaized.beanification;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import tamaized.beanification.junit.MockitoRunner;
import tamaized.beanification.junit.TestConstants;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

	@Test
	@SuppressWarnings("ResultOfMethodCallIgnored")
	public void injectRenderers() throws NoSuchFieldException, IllegalAccessException {
		Minecraft minecraft = mock(Minecraft.class);

		EntityRenderDispatcher entityRenderDispatcher = mock(EntityRenderDispatcher.class);
		Field entityRenderDispatcherRenderers = EntityRenderDispatcher.class.getDeclaredField("renderers");
		entityRenderDispatcherRenderers.trySetAccessible();
		entityRenderDispatcherRenderers.set(entityRenderDispatcher, ImmutableMap.of());

		BlockEntityRenderDispatcher blockEntityRenderDispatcher = mock(BlockEntityRenderDispatcher.class);
		Field blockEntityRenderDispatcherRenderers = BlockEntityRenderDispatcher.class.getDeclaredField("renderers");
		blockEntityRenderDispatcherRenderers.trySetAccessible();
		blockEntityRenderDispatcherRenderers.set(blockEntityRenderDispatcher, ImmutableMap.of());

		when(minecraft.getEntityRenderDispatcher()).thenReturn(entityRenderDispatcher);
		when(minecraft.getBlockEntityRenderDispatcher()).thenReturn(blockEntityRenderDispatcher);

		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);

		try (MockedStatic<Minecraft> minecraftMockedStatic = mockStatic(Minecraft.class)) {
			minecraftMockedStatic.when(Minecraft::getInstance).thenReturn(minecraft);
			assertDoesNotThrow(() -> instance.injectRenderers(modContainer, scanData, new ArrayList<>()));
		}
	}

}
