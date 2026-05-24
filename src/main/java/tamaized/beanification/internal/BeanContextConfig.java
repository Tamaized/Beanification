package tamaized.beanification.internal;

import java.util.ArrayList;
import java.util.List;

public class BeanContextConfig {

	private final LoggingSettings loggingSettings = new LoggingSettings();

	public LoggingSettings loggingSettings() {
		return loggingSettings;
	}

	public class LoggingSettings {

		private boolean injectInto = false;

		/**
		 * Should only be used for debugging purposes. This can cause log spam and degraded performance
		 */
		public BeanContextConfig enableInjectInto() {
			injectInto = true;
			return BeanContextConfig.this;
		}

		public boolean isInjectIntoEnabled() {
			return injectInto;
		}

	}

	private final ScanSettings scanSettings = new ScanSettings();

	public ScanSettings scanSettings() {
		return scanSettings;
	}

	public class ScanSettings {

		private final List<String> additionalScanModuleNames = new ArrayList<>();

		/**
		 * Used to add shaded dependencies to the scan path during development environments
		 */
		public BeanContextConfig addAdditionalComponentScanModuleName(String moduleName) {
			additionalScanModuleNames.add(moduleName);
			return BeanContextConfig.this;
		}

		public List<String> getAdditionalComponentScanModuleNames() {
			return additionalScanModuleNames;
		}

	}

}
