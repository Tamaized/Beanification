package tamaized.beanification.processors.construct;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.BeanContext;
import tamaized.beanification.BeanDefinition;
import tamaized.beanification.BeanLifeCycle;
import tamaized.beanification.processors.BeanProcessor;
import tamaized.beanification.processors.IBeanProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@BeanProcessor(BeanLifeCycle.Construct)
public class ConstructBeanProcessor implements IBeanProcessor {

	@Override
	public void process(BeanContext.BeanLifeCycleContext context, ModContainer modContainer, ModFileScanData scanData) throws Throwable {
		var deps = new HashMap<>(context.dependencies().orElseThrow());
		deps.replaceAll((_, v) -> new ArrayList<>(v));

		var toProcess = new HashMap<>(context.gather().orElseThrow());
		while (!toProcess.isEmpty()) {
			List<BeanDefinition<?>> toRemove = new ArrayList<>();
			toProcess.forEach((def, factory) -> {
				List<BeanDefinition<?>> currentDeps = deps.get(def);
				if (currentDeps == null || toProcess.keySet().stream().noneMatch(currentDeps::contains)) {
					toRemove.add(def);
					try {
						context.register().orElseThrow().accept(def, factory.get());
					} catch (Throwable ex) {
						throw new RuntimeException("Failed to construct Bean: " + def, ex);
					}
				}
			});
			toRemove.forEach(toProcess::remove);
		}
	}

}
