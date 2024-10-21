package tamaized.beanification.internal;

import cpw.mods.jarhandling.SecureJar;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.modscan.ModAnnotation;
import net.neoforged.fml.loading.modscan.Scanner;
import net.neoforged.neoforgespi.language.ModFileScanData;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

@ApiStatus.Internal
public class DistAnnotationRetriever {

	@Nullable
	private Supplier<ModFileScanData> cachedBeanificationScan;

	@SuppressWarnings("UnstableApiUsage")
	public final Optional<ModFileScanData> getBeanificationScanData() {
		if (cachedBeanificationScan != null) {
			return Optional.ofNullable(cachedBeanificationScan.get());
		}

		FMLLoader.getGameLayer().configuration().modules().stream().filter(r -> r.name().equals("beanification")).findAny().ifPresentOrElse(module -> {
			SecureJar jar = SecureJar.from(Path.of(module.reference().location().orElseThrow()));
			ModFile modFile = new ModFile(jar, file -> null, new ModFileDiscoveryAttributes(null, null, null, null));
			ModFileScanData result = new Scanner(modFile).scan();
			cachedBeanificationScan = () -> result;
		}, () -> cachedBeanificationScan = () -> null);

		return Optional.ofNullable(cachedBeanificationScan.get());
	}

	@SafeVarargs
	@SuppressWarnings({"UseBulkOperation", "ManualArrayToCollectionCopy"})
	public final Stream<ModFileScanData.AnnotationData> retrieve(ModFileScanData scanData, ElementType elementType, Class<? extends Annotation>... types) {
		List<Class<? extends Annotation>> t = new ArrayList<>();
		for (Class<? extends Annotation> type : types) {
			t.add(type);
		}
		return t.stream().flatMap(type -> {
			Stream<ModFileScanData.AnnotationData> modScan = scanData.getAnnotatedBy(type, elementType);
			Stream<ModFileScanData.AnnotationData> combinedScan = getBeanificationScanData().map(s -> Stream.concat(s.getAnnotatedBy(type, elementType), modScan)).orElse(modScan);
			return combinedScan.filter(annotation -> {
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
		});
	});
	}

}
