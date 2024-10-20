package tamaized.beanification.processors;

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
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoFixer.class, MockitoRunner.class})
public class BeanAnnotationDataProcessorTests {

	@Mock
	private DistAnnotationRetriever distAnnotationRetriever;

	@Mock
	private InternalReflectionHelper internalReflectionHelper;

	@InjectMocks
	private BeanAnnotationDataProcessor instance;

	@Test
	public void process() throws Throwable {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));

		Method target = mock(Method.class);

		Bean bean = mock(Bean.class);
		when(bean.value()).thenReturn(Component.DEFAULT_VALUE);
		when(target.getAnnotation(Bean.class)).thenReturn(bean);

		TestBean beanInstance = new TestBean();
		when(target.invoke(null)).thenReturn(beanInstance);
		doReturn(TestBean.class).when(target).getReturnType();

		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method")).thenReturn(target);
		when(internalReflectionHelper.isStatic(target)).thenReturn(true);

		BeanContext.BeanContextInternalRegistrar context = mock(BeanContext.BeanContextInternalRegistrar.class);

		ModContainer modContainer = mock(ModContainer.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(context, times(1)).register(TestBean.class, null, beanInstance);
	}

	@Test
	public void processNamed() throws Throwable {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", Map.of("value", "test"))
		));

		Method target = mock(Method.class);

		Bean bean = mock(Bean.class);
		when(bean.value()).thenReturn("test");
		when(target.getAnnotation(Bean.class)).thenReturn(bean);

		TestBean beanInstance = new TestBean();
		when(target.invoke(null)).thenReturn(beanInstance);
		doReturn(TestBean.class).when(target).getReturnType();

		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method")).thenReturn(target);
		when(internalReflectionHelper.isStatic(target)).thenReturn(true);

		BeanContext.BeanContextInternalRegistrar context = mock(BeanContext.BeanContextInternalRegistrar.class);

		ModContainer modContainer = mock(ModContainer.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(context, times(1)).register(TestBean.class, "test", beanInstance);
	}

	@Test
	public void processEmpty() throws Throwable {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class)).thenReturn(Stream.empty());

		Method target = mock(Method.class);

		Bean bean = mock(Bean.class);
		when(bean.value()).thenReturn(Component.DEFAULT_VALUE);
		when(target.getAnnotation(Bean.class)).thenReturn(bean);

		TestBean beanInstance = new TestBean();
		when(target.invoke(null)).thenReturn(beanInstance);
		doReturn(TestBean.class).when(target).getReturnType();

		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method")).thenReturn(target);
		when(internalReflectionHelper.isStatic(target)).thenReturn(true);

		BeanContext.BeanContextInternalRegistrar context = mock(BeanContext.BeanContextInternalRegistrar.class);

		ModContainer modContainer = mock(ModContainer.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(context, never()).register(TestBean.class, null, beanInstance);
	}

	@Test
	public void processNotStatic() throws Throwable {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "method", new HashMap<>())
		));

		Method target = mock(Method.class);

		Bean bean = mock(Bean.class);
		when(bean.value()).thenReturn(Component.DEFAULT_VALUE);
		when(target.getAnnotation(Bean.class)).thenReturn(bean);

		TestBean beanInstance = new TestBean();
		when(target.invoke(null)).thenReturn(beanInstance);
		doReturn(TestBean.class).when(target).getReturnType();

		when(internalReflectionHelper.getDeclaredMethod(TestBean.class, "method")).thenReturn(target);
		when(internalReflectionHelper.isStatic(target)).thenReturn(false);

		BeanContext.BeanContextInternalRegistrar context = mock(BeanContext.BeanContextInternalRegistrar.class);

		ModContainer modContainer = mock(ModContainer.class);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> instance.process(context, modContainer, scanData));

		assertEquals("@Bean methods must be static", exception.getMessage());

		verify(context, never()).register(TestBean.class, null, beanInstance);
	}

}
