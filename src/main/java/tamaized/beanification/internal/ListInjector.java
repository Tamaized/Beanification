package tamaized.beanification.internal;

import net.neoforged.neoforgespi.language.ModFileScanData;
import org.jetbrains.annotations.ApiStatus;
import tamaized.beanification.BeanContext;
import tamaized.beanification.BeanDefinition;

import java.util.List;
import java.util.Objects;

@ApiStatus.Internal
public class ListInjector {

	public List<?> inject(BeanContext.BeanLifeCycleContext context, ModFileScanData scanData, Class<?> parent, Class<?> classFilter, boolean recursive) {
		return scanData.getClasses().stream()
			.filter(data -> {
				String pkg = data.clazz().getInternalName().replaceAll("/", ".");
				pkg = pkg.substring(0, pkg.lastIndexOf("."));
				return recursive ? pkg.contains(parent.getPackageName()) : pkg.equals(parent.getPackageName());
			})
			.map(data -> {
				try {
					return Class.forName(data.clazz().getClassName());
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			})
			.filter(classFilter::isAssignableFrom)
			.map(data -> new BeanDefinition<>(data, null))
			.map(data -> context.injector().orElseThrow().apply(data))
			.filter(Objects::nonNull)
			.toList();
	}

}
