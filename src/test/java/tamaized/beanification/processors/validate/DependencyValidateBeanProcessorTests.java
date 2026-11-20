package tamaized.beanification.processors.validate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import tamaized.beanification.BeanContext;
import tamaized.beanification.BeanDefinition;
import tamaized.beanification.CircularDependencyException;
import tamaized.beanification.TestBean;
import tamaized.beanification.junit.MockitoRunner;
import tamaized.beanification.processors.BeanAnnotationProcessorMetadata;
import tamaized.beanification.processors.construct.ConstructBeanProcessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoRunner.class})
public class DependencyValidateBeanProcessorTests {

	@InjectMocks
	private DependencyValidateBeanProcessor instance;

	@Test
	public void process() {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);

		when(context.dependencies()).thenReturn(Optional.of(new HashMap<>()));

		assertDoesNotThrow(() -> instance.process(context, List.of(), new BeanAnnotationProcessorMetadata()));
	}

	@Test
	public void processWithDeps() {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);

		Map<BeanDefinition<?>, List<BeanDefinition<?>>> depMap = new HashMap<>();
		depMap.put(new BeanDefinition<>(TestBean.class, "A"), List.of(new BeanDefinition<>(TestBean.class, "B")));
		depMap.put(new BeanDefinition<>(TestBean.class, "B"), List.of(new BeanDefinition<>(TestBean.class, "C")));
		when(context.dependencies()).thenReturn(Optional.of(depMap));

		assertDoesNotThrow(() -> instance.process(context, List.of(), new BeanAnnotationProcessorMetadata()));
	}

	@Test
	public void processWithDepsCircular() {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);

		Map<BeanDefinition<?>, List<BeanDefinition<?>>> depMap = new HashMap<>();
		depMap.put(new BeanDefinition<>(TestBean.class, "A"), List.of(new BeanDefinition<>(TestBean.class, "B")));
		depMap.put(new BeanDefinition<>(TestBean.class, "B"), List.of(new BeanDefinition<>(TestBean.class, "A")));
		when(context.dependencies()).thenReturn(Optional.of(depMap));

		assertThrows(CircularDependencyException.class, () -> instance.process(context, List.of(), new BeanAnnotationProcessorMetadata()));
	}

}
