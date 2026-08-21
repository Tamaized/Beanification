package tamaized.beanification.processors.staticinject.inject;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import tamaized.beanification.*;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.FieldLocator;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.junit.MockitoFixer;
import tamaized.beanification.junit.MockitoRunner;
import tamaized.beanification.processors.BeanAnnotationProcessorMetadata;
import tamaized.beanification.processors.staticinject.LazyAutowiredAnnotationStaticFieldInjectBeanProcessor;

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoFixer.class, MockitoRunner.class})
public class LazyAutowiredAnnotationStaticInjectBeanProcessorTest {

	@Mock
	private DistAnnotationRetriever distAnnotationRetriever;

	@Mock
	private InternalReflectionHelper internalReflectionHelper;

	@Mock
	private FieldLocator fieldLocator;

	@InjectMocks
	private LazyAutowiredAnnotationStaticFieldInjectBeanProcessor instance;

	private Field mockField() {
		Field f = mock(Field.class);
		LazyAutowired annotation = mock(LazyAutowired.class);
		when(annotation.value()).thenReturn(Component.DEFAULT_VALUE);
		when(f.getAnnotation(LazyAutowired.class)).thenReturn(annotation);
		return f;
	}

	@Test
	public void process() throws Throwable {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);

		ModFileScanData.AnnotationData data = mock(ModFileScanData.AnnotationData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, LazyAutowired.class)).thenAnswer(_ -> Stream.of(data));

		Field field = mockField();
		when(fieldLocator.locate(context, data)).thenReturn(field);

		when(internalReflectionHelper.isStatic(field)).thenReturn(true);

		TestBean dep = new TestBean();
		when(context.lazyInjector()).thenReturn(Optional.of(_ -> Suppliers.memoize(() -> dep)));

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData, new BeanAnnotationProcessorMetadata()));

		verify(field).trySetAccessible();
		ArgumentCaptor<Supplier<TestBean>> captor = ArgumentCaptor.captor();
		verify(field).set(isNull(), captor.capture());
		assertSame(dep, captor.getValue().get());
	}

	@Test
	public void processNonStatic() throws Throwable {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);

		ModFileScanData.AnnotationData data = mock(ModFileScanData.AnnotationData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, LazyAutowired.class)).thenAnswer(_ -> Stream.of(data));

		Field field = mockField();
		when(fieldLocator.locate(context, data)).thenReturn(field);

		when(internalReflectionHelper.isStatic(field)).thenReturn(false);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData, new BeanAnnotationProcessorMetadata()));

		verify(context, never()).lazyInjector();
		verify(field, never()).trySetAccessible();
		verify(field, never()).set(isNull(), any(TestBean.class));
	}

}
