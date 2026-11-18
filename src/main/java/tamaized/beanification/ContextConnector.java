package tamaized.beanification;

import tamaized.beanification.scan.AnnotationScanData;
import tamaized.beanification.scan.ScanCondition;

import java.util.List;

public interface ContextConnector {

	/**
	 * Do not register {@code this}, ContextConnectors are auto registered last after all {@code #registerInternalBeans(InternalBeanRegistrar)} have run.
	 */
	void registerInternalBeans(InternalBeanRegistrar register);

	List<ScanCondition> conditions();

	AnnotationScanData scan(List<ScanCondition> conditions);

}
