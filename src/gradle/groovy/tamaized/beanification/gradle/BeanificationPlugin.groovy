package tamaized.beanification.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import tamaized.beanification.gradle.asm.CompileTimeTransformer

class BeanificationPlugin implements Plugin<Project> {

	@Override
	void apply(Project project) {
		def task = project.tasks.register("beanificationTransformClasses") {
			it.dependsOn 'classes'
			it.mustRunAfter 'classes'

			def workDir = project.layout.buildDirectory.dir("classes/java/main")

			it.doLast {
				workDir.get().asFileTree.matching {
					it.include '**/*.class'
				}.each { file ->
					CompileTimeTransformer.processClassFile(file)
				}
			}
		}
		def classesTask = project.tasks.named('classes')
		project.afterEvaluate {
			classesTask.get().finalizedBy(task)
		}
	}

}