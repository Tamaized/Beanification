package tamaized.beanification.processors.pre;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.objectweb.asm.Type;
import tamaized.beanification.Autowired;
import tamaized.beanification.BeanContext;
import tamaized.beanification.Component;
import tamaized.beanification.TestBean;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.junit.MockitoFixer;
import tamaized.beanification.junit.MockitoRunner;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoFixer.class, MockitoRunner.class})
public class ComponentAnnotationDataPreProcessorTests {

	@Mock
	private DistAnnotationRetriever distAnnotationRetriever;

	@Mock
	private InternalReflectionHelper internalReflectionHelper;

	@InjectMocks
	private ComponentAnnotationDataPreProcessor instance;

	@Test
	public void processNoArgs() {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "TestBean", new HashMap<>())
		));

		Component bean = mock(Component.class);
		when(bean.value()).thenReturn(Component.DEFAULT_VALUE);
		when(internalReflectionHelper.getAnnotation(TestBean.class, Component.class)).thenReturn(bean);

		Constructor<?> constructor = mock(Constructor.class);
		when(constructor.getParameterCount()).thenReturn(0);
		when(internalReflectionHelper.getConstructors(TestBean.class)).thenReturn(new Constructor[]{constructor});

		BeanContext.BeanContextInternalDependencyTreeAccumulator context = mock(BeanContext.BeanContextInternalDependencyTreeAccumulator.class);

		ModContainer modContainer = mock(ModContainer.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(context, never()).addDependency(any(), anyString(), any(), anyString());
	}

	@Test
	public void processNoArgsMultiCtor() {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "TestBean", new HashMap<>())
		));

		Component bean = mock(Component.class);
		when(bean.value()).thenReturn(Component.DEFAULT_VALUE);
		when(internalReflectionHelper.getAnnotation(TestBean.class, Component.class)).thenReturn(bean);

		Constructor<?> noArgCtor = mock(Constructor.class);
		when(noArgCtor.getParameterCount()).thenReturn(0);
		Constructor<?> oneArgCtorNoAnnotation = mock(Constructor.class);
		when(oneArgCtorNoAnnotation.getParameterCount()).thenReturn(1);
		Annotation[][] oneArgCtorAnnotationArray = new Annotation[][]{};
		when(oneArgCtorNoAnnotation.getParameterAnnotations()).thenReturn(oneArgCtorAnnotationArray);
		when(internalReflectionHelper.getConstructors(TestBean.class)).thenReturn(new Constructor[]{noArgCtor, oneArgCtorNoAnnotation});
		when(internalReflectionHelper.allParametersHaveAnnotation(oneArgCtorAnnotationArray, Autowired.class)).thenReturn(false);

		BeanContext.BeanContextInternalDependencyTreeAccumulator context = mock(BeanContext.BeanContextInternalDependencyTreeAccumulator.class);

		ModContainer modContainer = mock(ModContainer.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(context, never()).addDependency(any(), anyString(), any(), anyString());
	}

	@Test
	public void processWithArgsMultiCtor() {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "TestBean", new HashMap<>())
		));

		Component bean = mock(Component.class);
		when(bean.value()).thenReturn(Component.DEFAULT_VALUE);
		when(internalReflectionHelper.getAnnotation(TestBean.class, Component.class)).thenReturn(bean);

		Constructor<?> noArgCtor = mock(Constructor.class);
		when(noArgCtor.getParameterCount()).thenReturn(0);
		Constructor<?> oneArgCtorAnnotation = mock(Constructor.class);
		when(oneArgCtorAnnotation.getParameterCount()).thenReturn(1);
		Annotation[][] oneArgCtorAnnotationArray = new Annotation[][]{};
		when(oneArgCtorAnnotation.getParameterAnnotations()).thenReturn(oneArgCtorAnnotationArray);
		when(internalReflectionHelper.getConstructors(TestBean.class)).thenReturn(new Constructor[]{noArgCtor, oneArgCtorAnnotation});
		when(internalReflectionHelper.allParametersHaveAnnotation(oneArgCtorAnnotationArray, Autowired.class)).thenReturn(true);

		BeanContext.BeanContextInternalDependencyTreeAccumulator context = mock(BeanContext.BeanContextInternalDependencyTreeAccumulator.class);

		Parameter parameter = mock(Parameter.class);
		Autowired annotation = mock(Autowired.class);
		when(annotation.value()).thenReturn("test");
		when(parameter.getAnnotation(Autowired.class)).thenReturn(annotation);
		when(oneArgCtorAnnotation.getParameters()).thenReturn(new Parameter[]{parameter});

		ModContainer modContainer = mock(ModContainer.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(context, times(1)).addDependency(eq(TestBean.class), isNull(), isNull(), eq("test"));
	}

	@Test
	public void processWithArgs() {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "TestBean", new HashMap<>())
		));

		Component bean = mock(Component.class);
		when(bean.value()).thenReturn(Component.DEFAULT_VALUE);
		when(internalReflectionHelper.getAnnotation(TestBean.class, Component.class)).thenReturn(bean);

		Constructor<?> oneArgCtorAnnotation = mock(Constructor.class);
		when(oneArgCtorAnnotation.getParameterCount()).thenReturn(1);
		Annotation[][] oneArgCtorAnnotationArray = new Annotation[][]{};
		when(oneArgCtorAnnotation.getParameterAnnotations()).thenReturn(oneArgCtorAnnotationArray);
		when(internalReflectionHelper.getConstructors(TestBean.class)).thenReturn(new Constructor[]{oneArgCtorAnnotation});
		when(internalReflectionHelper.allParametersHaveAnnotation(oneArgCtorAnnotationArray, Autowired.class)).thenReturn(true);

		BeanContext.BeanContextInternalDependencyTreeAccumulator context = mock(BeanContext.BeanContextInternalDependencyTreeAccumulator.class);

		Parameter parameter = mock(Parameter.class);
		Autowired annotation = mock(Autowired.class);
		when(annotation.value()).thenReturn("test");
		when(parameter.getAnnotation(Autowired.class)).thenReturn(annotation);
		when(oneArgCtorAnnotation.getParameters()).thenReturn(new Parameter[]{parameter});

		ModContainer modContainer = mock(ModContainer.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(context, times(1)).addDependency(eq(TestBean.class), isNull(), isNull(), eq("test"));
	}

	@Test
	public void processWithTwoArgs() {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "TestBean", new HashMap<>())
		));

		Component bean = mock(Component.class);
		when(bean.value()).thenReturn(Component.DEFAULT_VALUE);
		when(internalReflectionHelper.getAnnotation(TestBean.class, Component.class)).thenReturn(bean);

		Constructor<?> oneArgCtorAnnotation = mock(Constructor.class);
		when(oneArgCtorAnnotation.getParameterCount()).thenReturn(1);
		Annotation[][] oneArgCtorAnnotationArray = new Annotation[][]{};
		when(oneArgCtorAnnotation.getParameterAnnotations()).thenReturn(oneArgCtorAnnotationArray);
		when(internalReflectionHelper.getConstructors(TestBean.class)).thenReturn(new Constructor[]{oneArgCtorAnnotation});
		when(internalReflectionHelper.allParametersHaveAnnotation(oneArgCtorAnnotationArray, Autowired.class)).thenReturn(true);

		BeanContext.BeanContextInternalDependencyTreeAccumulator context = mock(BeanContext.BeanContextInternalDependencyTreeAccumulator.class);

		Parameter parameter = mock(Parameter.class);
		Autowired annotation = mock(Autowired.class);
		when(annotation.value()).thenReturn("test");
		when(parameter.getAnnotation(Autowired.class)).thenReturn(annotation);
		when(oneArgCtorAnnotation.getParameters()).thenReturn(new Parameter[]{parameter, parameter});

		ModContainer modContainer = mock(ModContainer.class);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(context, times(2)).addDependency(eq(TestBean.class), isNull(), isNull(), eq("test"));
	}

	@Test
	public void processWithArgsConflict() {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, Type.getType(TestBean.class), "TestBean", new HashMap<>())
		));

		Component bean = mock(Component.class);
		when(bean.value()).thenReturn(Component.DEFAULT_VALUE);
		when(internalReflectionHelper.getAnnotation(TestBean.class, Component.class)).thenReturn(bean);

		Constructor<?> ctorOne = mock(Constructor.class);
		when(ctorOne.getParameterCount()).thenReturn(1);
		Constructor<?> ctorTwo = mock(Constructor.class);
		when(ctorTwo.getParameterCount()).thenReturn(1);
		Annotation[][] oneArgCtorAnnotationArray = new Annotation[][]{};
		when(ctorOne.getParameterAnnotations()).thenReturn(oneArgCtorAnnotationArray);
		when(ctorTwo.getParameterAnnotations()).thenReturn(oneArgCtorAnnotationArray);
		when(internalReflectionHelper.getConstructors(TestBean.class)).thenReturn(new Constructor[]{ctorOne, ctorTwo});
		when(internalReflectionHelper.allParametersHaveAnnotation(oneArgCtorAnnotationArray, Autowired.class)).thenReturn(true);

		BeanContext.BeanContextInternalDependencyTreeAccumulator context = mock(BeanContext.BeanContextInternalDependencyTreeAccumulator.class);

		Parameter parameter = mock(Parameter.class);
		Autowired annotation = mock(Autowired.class);
		when(annotation.value()).thenReturn("test");
		when(parameter.getAnnotation(Autowired.class)).thenReturn(annotation);
		when(ctorOne.getParameters()).thenReturn(new Parameter[]{parameter});
		when(ctorTwo.getParameters()).thenReturn(new Parameter[]{parameter});

		ModContainer modContainer = mock(ModContainer.class);

		assertThrows(IllegalArgumentException.class, () -> instance.process(context, modContainer, scanData));
	}

}
