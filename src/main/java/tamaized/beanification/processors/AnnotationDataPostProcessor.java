package tamaized.beanification.processors;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.BeanContext;

import java.util.concurrent.atomic.AtomicReference;

public interface AnnotationDataPostProcessor {

	void process(BeanContext.BeanContextInternalInjector context, ModContainer modContainer, ModFileScanData scanData, Object bean, AtomicReference<Object> currentInjectionTarget) throws Throwable;

	void process(BeanContext.BeanContextInternalInjector context, ModContainer modContainer, ModFileScanData scanData, AtomicReference<Object> currentInjectionTarget) throws Throwable;

}
