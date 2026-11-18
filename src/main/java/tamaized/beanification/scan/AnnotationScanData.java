package tamaized.beanification.scan;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AnnotationScanData {

	private final AnnotationScanFilterStrategy filterStrategy;

	private final List<ScanCondition> conditions;

	private final Set<AnnotationData> annotations = new LinkedHashSet<>();

	public AnnotationScanData(AnnotationScanFilterStrategy filterStrategy, List<ScanCondition> conditions, Stream<RawAnnotationData> data) {
		this.filterStrategy = filterStrategy;
		this.conditions = conditions;
		data.collect(Collectors.groupingBy(
			raw -> new MemberMetadata(raw.targetType(), raw.clazz(), raw.memberName())
		)).forEach((key, group) -> {
			ScannedMember member = new ScannedMember(key, group.stream().collect(Collectors.toUnmodifiableMap(
				RawAnnotationData::annotationType,
				RawAnnotationData::annotationData
			)));
			group.forEach(raw -> annotations.add(new AnnotationData(raw.annotationType(), member)));
		});
	}

	public AnnotationScanData(Stream<RawAnnotationData> data) {
		this(
			(scanData, elementType, types) -> types.stream().flatMap(type -> scanData.getAnnotatedBy(type, elementType)),
			List.of(),
			data
		);
	}

	public Stream<AnnotationData> getAnnotatedBy(Class<? extends Annotation> type, ElementType elementType) {
		TypeData t = TypeData.of(type);
		return annotations.stream().filter(data -> data.member().metadata().targetType() == elementType && data.annotationType.equals(t));
	}

	@SafeVarargs
	@SuppressWarnings({"ManualArrayToCollectionCopy", "UseBulkOperation"})
	public final Stream<AnnotationData> filtered(ElementType elementType, Class<? extends Annotation>... types) {
		List<Class<? extends Annotation>> typeList = new ArrayList<>();
		for (Class<? extends Annotation> type : types) {
			typeList.add(type);
		}
		return filterStrategy
			.filter(this, elementType, typeList)
			.filter(data -> conditions.stream().allMatch(condition -> condition.test(data.member())));
	}

	@FunctionalInterface
	public interface AnnotationScanFilterStrategy {

		Stream<AnnotationData> filter(
			AnnotationScanData scanData,
			ElementType elementType,
			List<Class<? extends Annotation>> types
		);

	}

	public record RawAnnotationData(
		TypeData annotationType,
		ElementType targetType,
		TypeData clazz,
		String memberName,
		Map<String, Object> annotationData
	) {

	}

	public record MemberMetadata(
		ElementType targetType,
		TypeData clazz,
		String memberName
	) {

	}

	public record ScannedMember(
		MemberMetadata metadata,
		Map<TypeData, Map<String, Object>> annotations
	) {

	}

	public record AnnotationData(
		TypeData annotationType,
		ScannedMember member
	) {

		public Map<String, Object> annotationData() {
			return member.annotations().get(annotationType);
		}

	}

	public record TypeData(String className) {

		public static TypeData of(Class<? extends Annotation> type) {
			return new TypeData(type.getName());
		}

	}

}
