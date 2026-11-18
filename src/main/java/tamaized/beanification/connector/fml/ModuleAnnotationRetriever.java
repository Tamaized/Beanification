package tamaized.beanification.connector.fml;

import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.modscan.Scanner;
import net.neoforged.neoforgespi.language.ModFileScanData;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import tamaized.beanification.InternalAutowired;
import tamaized.beanification.scan.AnnotationScanData;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ModuleAnnotationRetriever implements AnnotationScanData.AnnotationScanFilterStrategy {

	@InternalAutowired
	private ModScanDataConverter modScanDataConverter;

	private final List<String> moduleNames;

	private final Map<String, Supplier<AnnotationScanData>> cachedModuleScan = new HashMap<>();

	public ModuleAnnotationRetriever(List<String> moduleNames) {
		this.moduleNames = moduleNames;
	}

	@SuppressWarnings("UnstableApiUsage")
	public final Optional<AnnotationScanData> getModuleScanData(String moduleName) {
		if (cachedModuleScan.containsKey(moduleName)) {
			return Optional.ofNullable(cachedModuleScan.get(moduleName).get());
		}

		FMLLoader.getCurrent()
			.getGameLayer()
			.configuration()
			.modules()
			.stream()
			.filter(r -> r.name().equals(moduleName))
			.findAny()
			.ifPresentOrElse(module -> {
				try {
					JarContents jar = JarContents.ofPath(Path.of(module.reference().location().orElseThrow()));
					ModFile modFile = new ModFile(jar, _ -> null, new ModFileDiscoveryAttributes(null, null, null, null));
					ModFileScanData result = new Scanner(modFile).scan();
					cachedModuleScan.put(moduleName, () -> new AnnotationScanData(modScanDataConverter.convert(result)));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}, () -> cachedModuleScan.put(moduleName, () -> null));

		return Optional.ofNullable(cachedModuleScan.get(moduleName).get());
	}

	@Override
	public final Stream<AnnotationScanData.AnnotationData> filter(AnnotationScanData scanData, ElementType elementType, List<Class<? extends Annotation>> types) {
		return types.stream().flatMap(type -> Stream.concat(
			scanData.getAnnotatedBy(type, elementType),
			moduleNames.stream().flatMap(moduleName -> getModuleScanData(moduleName)
				.map(s -> s.getAnnotatedBy(type, elementType))
				.orElseGet(Stream::empty)
			)
		));
	}

}
