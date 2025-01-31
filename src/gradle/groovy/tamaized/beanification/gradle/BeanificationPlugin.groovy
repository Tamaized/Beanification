package tamaized.beanification.gradle

import net.neoforged.moddevgradle.internal.IntelliJOutputDirectoryValueSource
import org.gradle.api.Plugin
import org.gradle.api.Project
import tamaized.beanification.gradle.asm.CompileTimeTransformer

class BeanificationPlugin implements Plugin<Project> {

	@Override
	void apply(Project project) {
		def taskIdea = project.tasks.register("beanificationTransformClassesIdea") {
			def outputDir = project.providers.provider {
				project.sourceSets.main.output.classesDirs
			}

			//noinspection GroovyAccessibility
			def ideaOut = IntelliJOutputDirectoryValueSource.getIntellijOutputDirectory(project)?.apply(project)?.toPath()?.resolve('production')?.toAbsolutePath()?.toString()
			def modClasses = project.providers.provider {
				ideaOut == null ? null : project.fileTree(ideaOut)
			}

			it.doLast {
				def tree = modClasses.orElse(outputDir).get().asFileTree
				println tree
				tree.matching {
					it.include '**/*.class'
				}.each { file ->
					CompileTimeTransformer.processClassFile(file)
				}
			}
		}

		project.neoForge {
			runs {
				configureEach {
					taskBefore taskIdea
				}
			}
		}

		def task = project.tasks.register("beanificationTransformClasses") {
			mustRunAfter 'classes'

			inputs.files(project.fileTree("src/main/java"))
			def outputFile = project.layout.buildDirectory.file("customTaskOutput.txt").get().asFile
			outputs.file(outputFile)

			def outputDir = project.providers.provider {
				project.sourceSets.main.output.classesDirs
			}
			it.doLast {
				def tree = outputDir.get().asFileTree
				println tree
				tree.matching {
					it.include '**/*.class'
				}.each { file ->
					CompileTimeTransformer.processClassFile(file)
				}
				outputFile.text = "${new Date()}"
			}
		}
		def classesTask = project.tasks.named('classes')
		project.afterEvaluate {
			classesTask.get().finalizedBy(task)
		}
	}

}