package tamaized.beanification.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import tamaized.beanification.gradle.asm.CompileTimeTransformer

class BeanificationPlugin implements Plugin<Project> {

	private static final String LOCATION = "beanification-instrumented-classes"

	@Override
	void apply(Project project) {
		def instrumentedDir = project.layout.buildDirectory.dir(LOCATION)

		def copyTask = project.tasks.register("beanificationCopyClasses", Copy) {
			it.dependsOn "classes"
			it.from project.layout.buildDirectory.dir("classes/java/main")
			it.into instrumentedDir
		}

		project.tasks.register("beanificationTransformClasses") {
			it.dependsOn copyTask

			it.doLast {
				logger.info("????")
				def tree = instrumentedDir.get().asFileTree
				if (tree.isEmpty()) {
					logger.warn("EMPTY!!!!!!!!!!!!!!!")
				}
				tree.each { file ->
					logger.info(file.name)
				}
				tree.matching {
					it.include '**/*.class'
				}.each { file ->
					logger.info(file.name)
					CompileTimeTransformer.processClassFile(file)
				}
			}
		}
	}

}