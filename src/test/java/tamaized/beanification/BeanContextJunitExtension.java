package tamaized.beanification;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.commons.support.AnnotationSupport;
import org.mockito.MockedStatic;
import tamaized.beanification.junit.MockBean;
import tamaized.beanification.junit.TestConstants;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class BeanContextJunitExtension implements BeforeAllCallback, AfterAllCallback, TestInstancePostProcessor {

	private static final Logger LOGGER = LogManager.getLogger(BeanContextJunitExtension.class);

	@Nullable
	private MockedStatic<BeanContext> beanContext;

	@Nullable
	private BeanContext beanContextInstance;

	private final Map<BeanContext.BeanDefinition<?>, Object> mockedBeans = new HashMap<>();

	private <T> T mockBean(Class<T> type) {
		return mockBean(type, null);
	}

	private <T> T mockBean(Class<T> type, @Nullable String name) {
		assertNotNull(beanContext);
		assertNotNull(beanContextInstance);
		T bean = mock(type);
		if (name == null)
			beanContext.when(() -> BeanContext.inject(type)).thenReturn(bean);
		beanContext.when(() -> BeanContext.inject(type, name)).thenReturn(bean);
		doReturn(bean).when(beanContextInstance).injectInternal(type, name);
		return bean;
	}

	@Override
	public void beforeAll(ExtensionContext context) {
		LOGGER.info("Dirtied BeanContext");
		beanContextInstance = spy(BeanContext.class);
		beanContext = mockStatic(BeanContext.class);
	}

	@Override
	public void afterAll(ExtensionContext context) {
		assertNotNull(beanContext);
		beanContext.close();
	}

	@Override
	public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
		for (Field field : AnnotationSupport.findAnnotatedFields(testInstance.getClass(), MockBean.class)) {
			field.trySetAccessible();
			if (field.get(testInstance) == null) {
				String name = field.getAnnotation(MockBean.class).value();
				field.set(testInstance, mockedBeans.computeIfAbsent(
					new BeanContext.BeanDefinition<>(field.getType(), name),
					k -> name.equals(MockBean.DEFAULT_VALUE) ? mockBean(field.getType()) : mockBean(field.getType(), name)
				));
			}
		}

		assertNotNull(beanContextInstance);
		if (beanContextInstance.isFrozen())
			return;
		Field f = BeanContext.class.getDeclaredField("INSTANCE");
		f.trySetAccessible();
		f.set(null, beanContextInstance);
		Method init = BeanContext.class.getDeclaredMethod("initInternal", String.class, Consumer.class, boolean.class);
		init.trySetAccessible();
		init.invoke(beanContextInstance, TestConstants.MODID, null, true);
	}
}
