# Beanification

### All Rights Reserved
- This library may only be used as a dependency in software projects.
- Modification is prohibited. (Pull Requests are allowed)
- This library is allowed to be repackaged, unmodified, into your own binaries for distribution.
  - Repackaging as shown below with the Gradle Shadow plugin is allowed, and encouraged.
- Redistribution of this library as a standalone product is prohibited.
- Attribution is not required.

## Gradle
- ModDevGradle is recommended.
  - (version 1.0.1 is being used for this example)
- The gradle shadow plugin is recommended.
```groovy
plugins {
	...
	id 'net.neoforged.moddev' version "${mdg_version}"
	id 'com.gradleup.shadow' version '8.3.3'
}

...

configurations {
	shade
}

...

shadowJar {
	archiveClassifier.set('')
	configurations = [project.configurations.shade]
	relocate 'tamaized.beanification', 'your.package.beanification'
}

...

repositories {
	...
	maven {
		name 'Beanification'
		url 'https://maven.tamaized.com/releases/'
	}
}

...

dependencies {
	...
	def beanification = "tamaized:beanification:${project.minecraft_version}-${project.beanification_version}"
	implementation beanification
	shade beanification
	testImplementation "${beanification}:tests"
	compileOnly "${beanification}:test-sources"
}

...

build.dependsOn shadowJar
```

## Setup
In your main `@Mod` class

```java
import tamaized.beanification.BeanContext;
import tamaized.beanification.Autowired;

import java.beans.beancontext.BeanContext;

@Mod("modid")
public class YourMod {

	static {
		// General Setup
		BeanContext.init();

		// This overload can be used instead to register beans directly
		BeanContext.init(context -> {
			context.register(YourComponent.class, YourComponent::new);
			context.register(YourComponent.class, "someName", YourExtendedComponent::new);
		});
	}

	@Autowired
	private YourComponent yourComponent; // Injected at runtime, not actually null.

	@Autowired("someName")
	private YourComponent yourNamedComponent; // Will be an instance of YourExtendedComponent

	public YourMod() {
		// Enables @Autowired to function with non-static fields in the main @Mod class
		BeanContext.enableMainModClassInjections();
	}

}
```
For further details, view the javadoc for:
- Autowired
- Bean
- Component
- PostConstruct
- Configurable

## Donations
https://ko-fi.com/tamaized

https://www.patreon.com/Tamaized
