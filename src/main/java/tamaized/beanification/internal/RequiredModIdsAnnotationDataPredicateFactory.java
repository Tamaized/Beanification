package tamaized.beanification.internal;

import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;

@ApiStatus.Internal
public class RequiredModIdsAnnotationDataPredicateFactory {

	public final boolean filter(ModFileScanData.AnnotationData annotation) {
		if (annotation.annotationData().get("requiresModIdsLoaded") instanceof ArrayList<?> list) {
			if (list.isEmpty())
				return true;

			for (Object o : list) {
				if (o instanceof String modId && !ModList.get().isLoaded(modId)) {
					return false;
				}
			}
		}

		return true;
	}

}
