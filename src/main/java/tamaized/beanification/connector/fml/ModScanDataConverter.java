package tamaized.beanification.connector.fml;

import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.scan.AnnotationScanData;

import java.util.stream.Stream;

public class ModScanDataConverter {

	public Stream<AnnotationScanData.RawAnnotationData> convert(ModFileScanData scanData) {
		return scanData.getAnnotations().stream().map(data -> new AnnotationScanData.RawAnnotationData(
			new AnnotationScanData.TypeData(data.annotationType().getClassName()),
			data.targetType(),
			new AnnotationScanData.TypeData(data.clazz().getClassName()),
			data.memberName(),
			data.annotationData()
		));
	}

}
