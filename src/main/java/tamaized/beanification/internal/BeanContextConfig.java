package tamaized.beanification.internal;

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

}
