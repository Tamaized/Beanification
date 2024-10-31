package tamaized.beanification.internal;

public class BeanContextConfig {

	private final ConfigurableSettings configurableSettings = new ConfigurableSettings();

	public ConfigurableSettings configurableSettings() {
		return configurableSettings;
	}

	public class ConfigurableSettings {

		private boolean registry = true;
		private boolean renderer = true;
		private boolean entity = true;

		public BeanContextConfig disableRegistry() {
			registry = false;
			return BeanContextConfig.this;
		}

		public boolean isRegistryEnabled() {
			return registry;
		}

		public BeanContextConfig disableRenderer() {
			renderer = false;
			return BeanContextConfig.this;
		}

		public boolean isRendererEnabled() {
			return renderer;
		}

		public BeanContextConfig disableEntity() {
			entity = false;
			return BeanContextConfig.this;
		}

		public boolean isEntityEnabled() {
			return entity;
		}

	}

}
