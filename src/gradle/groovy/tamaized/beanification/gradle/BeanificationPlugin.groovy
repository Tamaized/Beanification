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

			def transformTask = project.tasks.register("beanificationTransformClasses") {
				it.dependsOn "classes"

				it.doLast {
//					def classFileTree = project.fileTree(instrumentedDir.get()).matching {
//						include '**/*.class'
//					}
//					classFileTree.each { file ->
//						CompileTimeTransformer.processClassFile(file)
//					}
				}
			}

			project.tasks.register("beanificationCopyClasses", Copy) {
				it.dependsOn "classes"
				it.from project.layout.buildDirectory.dir("classes/java/main")
				it.into instrumentedDir
				it.finalizedBy transformTask
			}
	}

}