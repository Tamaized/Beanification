package tamaized.beanification.processors;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.BeanContext;

public interface IBeanProcessor {

	void process(BeanContext.BeanLifeCycleContext context, ModContainer modContainer, ModFileScanData scanData) throws Throwable;

}
