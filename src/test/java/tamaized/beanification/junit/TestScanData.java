package tamaized.beanification.junit;

import tamaized.beanification.scan.AnnotationScanData;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class TestScanData {

	private TestScanData() {

	}

	public static AnnotationScanData.RawAnnotationData raw(Class<? extends Annotation> annotation, ElementType targetType, Class<?> owner, String memberName, Map<String, Object> values) {
		return new AnnotationScanData.RawAnnotationData(
			AnnotationScanData.TypeData.of(annotation),
			targetType,
			type(owner),
			memberName,
			values
		);
	}

	public static AnnotationScanData.TypeData type(Class<?> c) {
		return new AnnotationScanData.TypeData(c.getName());
	}

	public static List<AnnotationScanData> scan(AnnotationScanData.RawAnnotationData... entries) {
		return List.of(new AnnotationScanData(Stream.of(entries)));
	}

}
