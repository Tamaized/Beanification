package tamaized.beanification.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import tamaized.beanification.gradle.asm.CompileTimeTransformer

class BeanificationPlugin implements Plugin<Project> {

	@Override
	void apply(Project project) {
		project.tasks.register("beanificationTransformClasses") {
			it.dependsOn 'classes'

			def workDir = project.layout.buildDirectory.dir("classes/java/main")

			it.doLast {
				workDir.get().asFileTree.matching {
					it.include '**/*.class'
				}.each { file ->
					CompileTimeTransformer.processClassFile(file)
				}
			}
		}
	}

}