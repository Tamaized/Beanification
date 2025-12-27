package tamaized.beanification.processors.staticinject.inject;

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
import tamaized.beanification.processors.staticinject.AutowiredAnnotationStaticInjectBeanProcessor;

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoFixer.class, MockitoRunner.class})
public class AutowiredAnnotationStaticInjectBeanProcessorTest {

	@Mock
	private DistAnnotationRetriever distAnnotationRetriever;

	@Mock
	private InternalReflectionHelper internalReflectionHelper;

	@InjectMocks
	private AutowiredAnnotationStaticInjectBeanProcessor instance;

	private Field mockField() {
		Field f = mock(Field.class);
		when(f.isAnnotationPresent(Autowired.class)).thenReturn(true);
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

		when(context.beans()).thenReturn(Optional.of(new HashMap<>()));

		ModFileScanData.AnnotationData data = mock(ModFileScanData.AnnotationData.class);
		when(data.clazz()).thenReturn(Type.getType(TestBean.class));
		when(distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Autowired.class)).thenAnswer(invocation -> Stream.of(data));

		when(internalReflectionHelper.classOrSuperEquals(Type.getType(TestBean.class), TestBean.class)).thenReturn(true);

		when(data.annotationData()).thenReturn(Map.of("value", Component.DEFAULT_VALUE));
		when(data.memberName()).thenReturn("memberName");

		Field field = mockField();
		when(internalReflectionHelper.getAllAutowiredFieldsIncludingSuper(TestBean.class, "memberName", Component.DEFAULT_VALUE)).thenReturn(
			List.of(field)
		);
		when(internalReflectionHelper.getDeclaredField(TestBean.class, "memberName")).thenReturn(field);

		when(internalReflectionHelper.isStatic(field)).thenReturn(true);

		TestBean dep = new TestBean();
		when(context.injector()).thenReturn(Optional.of(def -> dep));

		when(context.currentInjection()).thenReturn(Optional.of(new AtomicReference<>()));

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(field).set(null, dep);
	}

}
