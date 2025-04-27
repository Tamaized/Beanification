package tamaized.beanification.processors.construct;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.objectweb.asm.Type;
import tamaized.beanification.*;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.junit.MockitoFixer;
import tamaized.beanification.junit.MockitoRunner;
import tamaized.beanification.processors.gather.BeanAnnotationGatherBeanProcessor;

import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoFixer.class, MockitoRunner.class})
public class ConstructBeanProcessorTests {

	@InjectMocks
	private ConstructBeanProcessor instance;

	@Test
	@SuppressWarnings("unchecked")
	public void process() {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);

		when(context.dependencies()).thenReturn(Optional.of(new HashMap<>()));

		TestBean beanA = new TestBean();
		TestBean beanB = new TestBean();
		Map<BeanDefinition<?>, BeanContext.ThrowingSupplier<Object>> gatherMap = new HashMap<>();
		gatherMap.put(new BeanDefinition<>(TestBean.class, "A"), () -> beanA);
		gatherMap.put(new BeanDefinition<>(TestBean.class, "B"), () -> beanB);
		when(context.gather()).thenReturn(Optional.of(gatherMap));

		BiConsumer<BeanDefinition<?>, Object> consumer = mock(BiConsumer.class);
		when(context.register()).thenReturn(Optional.of(consumer));

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		verify(consumer).accept(new BeanDefinition<>(TestBean.class, "A"), beanA);
		verify(consumer).accept(new BeanDefinition<>(TestBean.class, "B"), beanB);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void processWithDeps() {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModContainer modContainer = mock(ModContainer.class);
		ModFileScanData scanData = mock(ModFileScanData.class);

		Map<BeanDefinition<?>, List<BeanDefinition<?>>> depMap = new HashMap<>();
		depMap.put(new BeanDefinition<>(TestBean.class, "A"), List.of(new BeanDefinition<>(TestBean.class, "B")));
		when(context.dependencies()).thenReturn(Optional.of(depMap));

		TestBean beanA = new TestBean();
		TestBean beanB = new TestBean();
		Map<BeanDefinition<?>, BeanContext.ThrowingSupplier<Object>> gatherMap = new HashMap<>();
		gatherMap.put(new BeanDefinition<>(TestBean.class, "A"), () -> beanA);
		gatherMap.put(new BeanDefinition<>(TestBean.class, "B"), () -> beanB);
		when(context.gather()).thenReturn(Optional.of(gatherMap));

		BiConsumer<BeanDefinition<?>, Object> consumer = mock(BiConsumer.class);
		when(context.register()).thenReturn(Optional.of(consumer));

		assertDoesNotThrow(() -> instance.process(context, modContainer, scanData));

		InOrder verifyInOrder = inOrder(consumer);
		verifyInOrder.verify(consumer).accept(new BeanDefinition<>(TestBean.class, "B"), beanB);
		verifyInOrder.verify(consumer).accept(new BeanDefinition<>(TestBean.class, "A"), beanA);
	}

}
