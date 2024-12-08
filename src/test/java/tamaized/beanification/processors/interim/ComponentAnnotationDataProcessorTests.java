package tamaized.beanification.processors.interim;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.objectweb.asm.Type;
import tamaized.beanification.*;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.junit.MockitoFixer;
import tamaized.beanification.junit.MockitoRunner;

import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
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
	public void processNoDeps() {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "TestBean", new HashMap<>())
		));

		Component bean = mock(Component.class);
		when(bean.value()).thenReturn(Component.DEFAULT_VALUE);
		when(internalReflectionHelper.getAnnotation(TestBean.class, Component.class)).thenReturn(bean);

		BeanContext.BeanContextInternalRegistrar context = mock(BeanContext.BeanContextInternalRegistrar.class);
		when(context.getDependencies(TestBean.class, null)).thenReturn(Collections.emptyList());

		ModContainer modContainer = mock(ModContainer.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(context, times(1)).register(eq(TestBean.class), isNull(), isNotNull());
	}

	@Test
	public void processNamedNoDeps() {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "TestBean", new HashMap<>())
		));

		Component bean = mock(Component.class);
		when(bean.value()).thenReturn("test");
		when(internalReflectionHelper.getAnnotation(TestBean.class, Component.class)).thenReturn(bean);

		BeanContext.BeanContextInternalRegistrar context = mock(BeanContext.BeanContextInternalRegistrar.class);
		when(context.getDependencies(TestBean.class, null)).thenReturn(Collections.emptyList());

		ModContainer modContainer = mock(ModContainer.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(context, times(1)).register(eq(TestBean.class), eq("test"), isNotNull());
	}

	@Test
	public void processDeps() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "TestBean", new HashMap<>())
		));

		Component bean = mock(Component.class);
		when(bean.value()).thenReturn(Component.DEFAULT_VALUE);
		when(internalReflectionHelper.getAnnotation(TestBean.class, Component.class)).thenReturn(bean);

		BeanContext.BeanContextInternalRegistrar context = mock(BeanContext.BeanContextInternalRegistrar.class);
		when(context.getDependencies(TestBean.class, null)).thenReturn(Collections.singletonList(new BeanDefinition<>(TestBean.class, "dep")));
		when(context.getUnfrozenBean(new BeanDefinition<>(TestBean.class, "dep"))).thenReturn(new TestBean());
		Constructor<?> ctor = mock(Constructor.class);
		doReturn(new TestBean()).when(ctor).newInstance(any());
		doReturn(ctor).when(internalReflectionHelper).getConstructor(TestBean.class, TestBean.class);

		ModContainer modContainer = mock(ModContainer.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(context, times(1)).register(eq(TestBean.class), isNull(), isNotNull());
	}

	@Test
	public void processDepsCircular() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "TestBean", new HashMap<>())
		));

		Component bean = mock(Component.class);
		when(bean.value()).thenReturn(Component.DEFAULT_VALUE);
		when(internalReflectionHelper.getAnnotation(TestBean.class, Component.class)).thenReturn(bean);

		BeanContext.BeanContextInternalRegistrar context = mock(BeanContext.BeanContextInternalRegistrar.class);
		when(context.getDependencies(TestBean.class, null)).thenReturn(Collections.singletonList(new BeanDefinition<>(TestBean.class, null)));
		Constructor<?> ctor = mock(Constructor.class);
		doReturn(new TestBean()).when(ctor).newInstance(any());
		doReturn(ctor).when(internalReflectionHelper).getConstructor(TestBean.class, TestBean.class);

		ModContainer modContainer = mock(ModContainer.class);

		assertThrows(CircularDependencyException.class, () -> instance.process(context, modContainer, scanData));

		verify(context, never()).register(eq(TestBean.class), isNull(), isNotNull());
	}

	@Test
	public void processDepsRetry() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "TestBean", new HashMap<>())
		));

		Component bean = mock(Component.class);
		when(bean.value()).thenReturn(Component.DEFAULT_VALUE);
		when(internalReflectionHelper.getAnnotation(TestBean.class, Component.class)).thenReturn(bean);

		BeanContext.BeanContextInternalRegistrar context = mock(BeanContext.BeanContextInternalRegistrar.class);
		when(context.getDependencies(TestBean.class, null)).thenReturn(Collections.singletonList(new BeanDefinition<>(TestBean.class, "dep")));
		when(context.getUnfrozenBean(new BeanDefinition<>(TestBean.class, "dep"))).thenReturn(null, new TestBean());
		Constructor<?> ctor = mock(Constructor.class);
		doReturn(new TestBean()).when(ctor).newInstance(any());
		doReturn(ctor).when(internalReflectionHelper).getConstructor(TestBean.class, TestBean.class);

		ModContainer modContainer = mock(ModContainer.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(context, times(1)).register(eq(TestBean.class), isNull(), isNotNull());
		verify(context, times(2)).getUnfrozenBean(new BeanDefinition<>(TestBean.class, "dep"));
	}

}
