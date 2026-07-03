package tamaized.beanification.processors.staticinject.inject;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import tamaized.beanification.*;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.FieldLocator;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.junit.MockitoFixer;
import tamaized.beanification.junit.MockitoRunner;
import tamaized.beanification.processors.BeanAnnotationProcessorMetadata;
import tamaized.beanification.processors.staticinject.AutowiredAnnotationStaticFieldInjectBeanProcessor;

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoFixer.class, MockitoRunner.class})
public class AutowiredAnnotationStaticInjectBeanProcessorTest {

	@Mock
	private DistAnnotationRetriever distAnnotationRetriever;

	@Mock
	private InternalReflectionHelper internalReflectionHelper;

	@Mock
	private FieldLocator fieldLocator;

	@InjectMocks
	private AutowiredAnnotationStaticFieldInjectBeanProcessor instance;

	private Field mockField() {
		Field f = mock(Field.class);
		Autowired annotation = mock(Autowired.class);
		when(annotation.value()).thenReturn(Component.DEFAULT_VALUE);
		when(f.getAnnotation(Autowired.class)).thenReturn(annotation);
		return f;
	}

	@Test
	public void process() throws Throwable {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);

		ModFileScanData.AnnotationData data = mock(ModFileScanData.AnnotationData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Autowired.class)).thenAnswer(_ -> Stream.of(data));

		Field field = mockField();
		when(fieldLocator.locate(context, data)).thenReturn(field);

		when(internalReflectionHelper.isStatic(field)).thenReturn(true);

		TestBean dep = new TestBean();
		when(context.strictInjector()).thenReturn(Optional.of(_ -> dep));

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData, new BeanAnnotationProcessorMetadata()));

		verify(field).trySetAccessible();
		verify(field).set(null, dep);
	}

	@Test
	public void processNonStatic() throws Throwable {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);

		ModFileScanData.AnnotationData data = mock(ModFileScanData.AnnotationData.class);
		when(distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Autowired.class)).thenAnswer(_ -> Stream.of(data));

		Field field = mockField();
		when(fieldLocator.locate(context, data)).thenReturn(field);

		when(internalReflectionHelper.isStatic(field)).thenReturn(false);

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData, new BeanAnnotationProcessorMetadata()));

		verify(context, never()).strictInjector();
		verify(field, never()).trySetAccessible();
		verify(field, never()).set(isNull(), any(TestBean.class));
	}

}
