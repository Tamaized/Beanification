package tamaized.beanification.processors;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.objectweb.asm.Type;
import tamaized.beanification.BeanContext;
import tamaized.beanification.Component;
import tamaized.beanification.TestBean;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.junit.MockitoFixer;
import tamaized.beanification.junit.MockitoRunner;

import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoFixer.class, MockitoRunner.class})
public class ComponentAnnotationDataProcessorTests {

	@Mock
	private DistAnnotationRetriever distAnnotationRetriever;

	@Mock
	private InternalReflectionHelper internalReflectionHelper;

	@InjectMocks
	private ComponentAnnotationDataProcessor instance;

	@Test
	public void process() {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "TestBean", new HashMap<>())
		));

		Component bean = mock(Component.class);
		when(bean.value()).thenReturn(Component.DEFAULT_VALUE);
		when(internalReflectionHelper.getAnnotation(TestBean.class, Component.class)).thenReturn(bean);

		BeanContext.BeanContextInternalRegistrar context = mock(BeanContext.BeanContextInternalRegistrar.class);

		ModContainer modContainer = mock(ModContainer.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(context, times(1)).register(eq(TestBean.class), isNull(), isNotNull());
	}

	@Test
	public void processNamed() {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "TestBean", new HashMap<>())
		));

		Component bean = mock(Component.class);
		when(bean.value()).thenReturn("test");
		when(internalReflectionHelper.getAnnotation(TestBean.class, Component.class)).thenReturn(bean);

		BeanContext.BeanContextInternalRegistrar context = mock(BeanContext.BeanContextInternalRegistrar.class);

		ModContainer modContainer = mock(ModContainer.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(context, times(1)).register(eq(TestBean.class), eq("test"), isNotNull());
	}

}
