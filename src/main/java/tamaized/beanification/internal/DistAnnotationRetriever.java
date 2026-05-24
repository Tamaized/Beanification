package tamaized.beanification.internal;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.modscan.ModAnnotation;
import net.neoforged.fml.loading.modscan.Scanner;
import net.neoforged.neoforgespi.language.ModFileScanData;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import tamaized.beanification.InternalAutowired;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

@ApiStatus.Internal
public class DistAnnotationRetriever {

	@InternalAutowired
	private AdditionalModuleNamesProvider additionalModuleNamesProvider;

	private final Map<String, Supplier<ModFileScanData>> cachedModuleScan = new HashMap<>();

	@SuppressWarnings("UnstableApiUsage")
	public final Optional<ModFileScanData> getModuleScanData(String moduleName) {
		if (cachedModuleScan.containsKey(moduleName)) {
			return Optional.ofNullable(cachedModuleScan.get(moduleName).get());
		}

		FMLLoader.getCurrent().getGameLayer().configuration().modules().stream().filter(r -> r.name().equals(moduleName)).findAny().ifPresentOrElse(module -> {
			try {
				JarContents jar = JarContents.ofPath(Path.of(module.reference().location().orElseThrow()));
				ModFile modFile = new ModFile(jar, _ -> null, new ModFileDiscoveryAttributes(null, null, null, null));
				ModFileScanData result = new Scanner(modFile).scan();
				cachedModuleScan.put(moduleName, () -> result);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}, () -> cachedModuleScan.put(moduleName, () -> null));

		return Optional.ofNullable(cachedModuleScan.get(moduleName).get());
	}

	@SafeVarargs
	@SuppressWarnings({"UseBulkOperation", "ManualArrayToCollectionCopy", "UnstableApiUsage"})
	public final Stream<ModFileScanData.AnnotationData> retrieve(ModFileScanData scanData, ElementType elementType, Class<? extends Annotation>... types) {
		List<Class<? extends Annotation>> t = new ArrayList<>();
		for (Class<? extends Annotation> type : types) {
			t.add(type);
		}
		return t.stream().flatMap(type -> {
			Stream<ModFileScanData.AnnotationData> combinedScan = Stream.concat(
				scanData.getAnnotatedBy(type, elementType),
				Stream.concat(
					Stream.of("beanification"),
					additionalModuleNamesProvider.getNames().stream()
				).flatMap(moduleName -> getModuleScanData(moduleName)
					.map(s -> s.getAnnotatedBy(type, elementType))
					.orElseGet(Stream::empty)
				)
			);
			return combinedScan.filter(annotation -> {
			if (annotation.annotationData().get("dist") instanceof ArrayList<?> list) {
				if (list.isEmpty())
					return true;
				for (Object o : list) {
					if (o instanceof ModAnnotation.EnumHolder e && Dist.valueOf(e.value()) == FMLEnvironment.getDist()) {
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
