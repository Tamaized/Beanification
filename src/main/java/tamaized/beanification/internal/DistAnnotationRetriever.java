package tamaized.beanification.internal;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.modscan.ModAnnotation;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@ApiStatus.Internal
public class DistAnnotationRetriever {

	@SafeVarargs
	@SuppressWarnings({"UseBulkOperation", "ManualArrayToCollectionCopy"})
	public final Stream<ModFileScanData.AnnotationData> retrieve(ModFileScanData scanData, ElementType elementType, Class<? extends Annotation>... types) {
		List<Class<? extends Annotation>> t = new ArrayList<>();
		for (Class<? extends Annotation> type : types) {
			t.add(type);
		}
		return t.stream().flatMap(type -> scanData.getAnnotatedBy(type, elementType).filter(annotation -> {
			if (annotation.annotationData().get("dist") instanceof ArrayList<?> list) {
				if (list.isEmpty())
					return true;
				for (Object o : list) {
					if (o instanceof ModAnnotation.EnumHolder e && Dist.valueOf(e.value()) == FMLEnvironment.dist) {
						return true;
					}
				}
			} else {
				return true;
			}
			return false;
		}));
	}

}
