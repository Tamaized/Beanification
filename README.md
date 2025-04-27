# Beanification

## Minecraft version?
Each version should work on everything 1.21.1+
It's possible it can work on earlier versions as well, but they are untested.
If there's a Minecraft version with a breaking change that requires a new version of this library, then it will be stated here.

### All Rights Reserved
- This library may only be used as a dependency in software projects.
- Modification is prohibited. (Pull Requests are allowed)
- This library is allowed to be repackaged, unmodified, into your own binaries for distribution.
  - Repackaging as shown below with the Gradle Shadow plugin is allowed, and encouraged.
- Redistribution of this library as a standalone product is prohibited.
- Attribution is not required.

## Gradle
- ModDevGradle is recommended.
  - (version 2.0.76 is being used for this example)
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
	testCompileOnly "${beanification}:test-sources"
}

...

build.dependsOn shadowJar
```

## Setup
In your main `@Mod` class

```java
@Mod("modid")
public class YourMod {

	static {
		// (Optional) Configure the BeanContext, must be called before #init
		BeanContext.configure()
			.loggingSettings().enableInjectInto();

		// General Setup
		BeanContext.init();
	}

	@Autowired
	private YourComponent yourComponent; // Injected at runtime, not actually null.

	@Autowired("someName")
	private YourComponent yourNamedComponent; // Will be an instance of YourExtendedComponent

	public YourMod() {
		// Enables @Autowired to function with non-static fields in the main @Mod class
		BeanContext.injectInto(this);
		// You could instead use `@Configurable` to apply this automatically, more information below.
	}

	@Bean
	private static YourComponent yourComponent() {
		return new YourComponent();
	}

	@Bean("someName")
	private static YourComponent yourExtendedComponent() {
		return new YourExtendedComponent();
	}

}
```
For further details, view the javadoc for:
- Autowired
- Bean
- Component
- PostConstruct
- Configurable

## Enable `@Configurable`
### ModDevGradle 2.0.X is required

A gradle plugin is required for this annotation to work as it modifies bytecode during compile time to inject `BeanContext.injectInto(this)` into every constructor.

It even works if the class does not have any constructor defined.

The injection happens before each `RETURN (b1) (177)` opcode
```groovy
buildscript {
	repositories{
		maven {
			name 'Beanification'
			url 'https://maven.tamaized.com/releases/'
		}
	}
	dependencies {
		classpath "tamaized:beanification:${minecraft_version}-${beanification_version}:gradle"
	}
}

plugins {
	...
}

apply plugin: 'tamaized.beanification'
```

### This only works for Intellij run configs and the gradle classes (build) task! Other IDEs are unsupported at this time.

```java
@Configurable
public class MyItem extends Item {

	@Autowired
	private MyComponent myComponent;

	public MyItem() {
		super();
		z();
		// BeanContext.injectInto(this) <- compile time injection here
	}

	protected MyItem(int x) {
		this();
		x.y();
		z();
		// BeanContext.injectInto(this) <- compile time injection here
	}

	private void z() {
		// Will NPE
		// The injection happens at the very end of constructors
		// If you need beans during constructors themselves, use constructor injection
		myComponent.apply();
	}

	public void w() {
		// Will not NPE
		// Assuming this is invoked after the object constructor has finished
		myComponent.apply();
	}

}
```

## Donations
https://ko-fi.com/tamaized

https://www.patreon.com/Tamaized
