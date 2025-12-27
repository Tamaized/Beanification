package tamaized.beanification.internal;

import net.neoforged.neoforgespi.language.ModFileScanData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.objectweb.asm.Type;
import tamaized.beanification.BeanContext;
import tamaized.beanification.BeanDefinition;
import tamaized.beanification.TestBean;
import tamaized.beanification.directory.DirectoryOtherTestBean;
import tamaized.beanification.directory.DirectoryTestBean;
import tamaized.beanification.junit.MockitoRunner;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;

@ExtendWith({MockitoRunner.class})
public class ListInjectorTests {

	@InjectMocks
	private ListInjector instance;

	@Test
	public void inject() {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModFileScanData scanData = mock(ModFileScanData.class);
		TestBean bean = new TestBean();

		TestBean dep = new TestBean();
		when(context.injector()).thenReturn(Optional.of(def -> dep));

		ModFileScanData.ClassData classData = mock(ModFileScanData.ClassData.class);
		doReturn(Type.getType(TestBean.class)).when(classData).clazz();
		ModFileScanData.ClassData classDataRecursive = mock(ModFileScanData.ClassData.class);
		doReturn(Type.getType(DirectoryTestBean.class)).when(classDataRecursive).clazz();
		ModFileScanData.ClassData classDataRecursiveOther = mock(ModFileScanData.ClassData.class);
		doReturn(Type.getType(DirectoryOtherTestBean.class)).when(classDataRecursiveOther).clazz();
		when(scanData.getClasses()).thenReturn(Set.of(classData, classDataRecursive, classDataRecursiveOther));

		List<?> result = instance.inject(context, scanData, bean.getClass(), TestBean.class, false);

		assertEquals(1, result.size());
		assertTrue(result.contains(dep));
	}

	@Test
	public void injectRecursive() {
		BeanContext.BeanLifeCycleContext context = mock(BeanContext.BeanLifeCycleContext.class);
		ModFileScanData scanData = mock(ModFileScanData.class);
		TestBean bean = new TestBean();

		TestBean dep = new TestBean();
		DirectoryTestBean recursiveBean = new DirectoryTestBean();
		DirectoryOtherTestBean recursiveOtherBean = new DirectoryOtherTestBean();
		when(context.injector()).thenReturn(Optional.of(def -> {
			if (def.equals(new BeanDefinition<>(DirectoryTestBean.class, null))) return recursiveBean;
			else if (def.equals(new BeanDefinition<>(DirectoryOtherTestBean.class, null))) return recursiveOtherBean;
			return dep;
		}));

		ModFileScanData.ClassData classData = mock(ModFileScanData.ClassData.class);
		doReturn(Type.getType(TestBean.class)).when(classData).clazz();
		ModFileScanData.ClassData classDataRecursive = mock(ModFileScanData.ClassData.class);
		doReturn(Type.getType(DirectoryTestBean.class)).when(classDataRecursive).clazz();
		ModFileScanData.ClassData classDataRecursiveOther = mock(ModFileScanData.ClassData.class);
		doReturn(Type.getType(DirectoryOtherTestBean.class)).when(classDataRecursiveOther).clazz();
		when(scanData.getClasses()).thenReturn(Set.of(classData, classDataRecursive, classDataRecursiveOther));

		List<?> result = instance.inject(context, scanData, bean.getClass(), TestBean.class, true);

		assertEquals(2, result.size());
		assertTrue(result.contains(dep));
		assertTrue(result.contains(recursiveBean));
	}

}
