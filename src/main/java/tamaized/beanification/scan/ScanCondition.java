package tamaized.beanification.scan;

@FunctionalInterface
public interface ScanCondition {

	boolean test(AnnotationScanData.ScannedMember member);

}
