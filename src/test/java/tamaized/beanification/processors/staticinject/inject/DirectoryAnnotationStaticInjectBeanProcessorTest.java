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
import tamaized.beanification.internal.ListInjector;
import tamaized.beanification.junit.MockitoFixer;
import tamaized.beanification.junit.MockitoRunner;
import tamaized.beanification.processors.staticinject.DirectoryAnnotationStaticInjectBeanProcessor;

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoFixer.class, MockitoRunner.class})
public class DirectoryAnnotationStaticInjectBeanProcessorTest {

	@Mock
	private DistAnnotationRetriever distAnnotationRetriever;

	@Mock
	private InternalReflectionHelper internalReflectionHelper;

	@Mock
	private ListInjector listInjector;

	@InjectMocks
	private DirectoryAnnotationStaticInjectBeanProcessor instance;

	private Field mockField() {
		Field f = mock(Field.class);
		when(f.isAnnotationPresent(Directory.class)).thenReturn(true);
		Directory annotation = mock(Directory.class);
		doReturn(TestBean.class).when(annotation).value();
		when(annotation.recursive()).thenReturn(false);
		when(f.getAnnotation(Directory.class)).thenReturn(annotation);
		return f;
	}

	@Test
	@SuppressWarnings("unchecked")
	public void process() throws Throwable {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);

		TestBean dep = new TestBean();
		Map<BeanDefinition<?>, Object> beanMap = new HashMap<>();
		beanMap.put(new BeanDefinition<>(TestBean.class, null), dep);
		when(context.beans()).thenReturn(Optional.of(new HashMap<>()), Optional.of(beanMap));

		ModFileScanData.AnnotationData data = mock(ModFileScanData.AnnotationData.class);
		when(data.clazz()).thenReturn(Type.getType(TestBean.class));
		when(distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Directory.class)).thenAnswer(invocation -> Stream.of(data));

		when(internalReflectionHelper.classOrSuperEquals(Type.getType(TestBean.class), TestBean.class)).thenReturn(true);

		when(data.annotationData()).thenReturn(Map.of("value", Component.DEFAULT_VALUE));
		when(data.memberName()).thenReturn("memberName");

		Field field = mockField();
		when(internalReflectionHelper.getAllDirectoryFieldsIncludingSuper(TestBean.class, "memberName")).thenReturn(
			List.of(field)
		);
		when(internalReflectionHelper.getDeclaredField(TestBean.class, "memberName")).thenReturn(field);

		when(internalReflectionHelper.isStatic(field)).thenReturn(true);

		List<?> deps = List.of(new TestBean());
		doReturn(deps).when(listInjector).inject(context, scanData, TestBean.class, TestBean.class, false);

		when(context.currentInjection()).thenReturn(Optional.of(new AtomicReference<>()));

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(field).set(null, deps);
	}

}
