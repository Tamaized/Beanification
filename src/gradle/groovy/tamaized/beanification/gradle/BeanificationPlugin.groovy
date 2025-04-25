package tamaized.beanification.gradle

import groovy.transform.TupleConstructor
import groovyjarjarantlr4.v4.runtime.misc.Nullable
import net.neoforged.moddevgradle.dsl.ModDevExtension
import net.neoforged.moddevgradle.internal.IntelliJOutputDirectoryValueSource
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.plugins.JavaPluginExtension
import tamaized.beanification.gradle.asm.CompileTimeTransformer

class BeanificationPlugin implements Plugin<Project> {

	@Override
	void apply(Project project) {
		def taskIdea = project.tasks.register("beanificationTransformClassesIdea") {
			def sourceSets = project.extensions.getByType(JavaPluginExtension).sourceSets

			def outputDirs = project.providers.provider {
				sourceSets.toList().collect { sourceSet ->
					//noinspection GroovyAccessibility
					def ideaOutput = IntelliJOutputDirectoryValueSource.getIntellijOutputDirectory(project)?.apply(project)?.toPath()?.resolve(
						sourceSet.name == "main" ? 'production' : sourceSet.name
					)?.toAbsolutePath()?.toString()
					return new NamedIdeOutputDirs(
						sourceSet.name,
						sourceSet.output.classesDirs,
						ideaOutput == null ? null : project.fileTree(ideaOutput)
					)
				}
			}

			it.doLast {
				outputDirs.get().each {outputDir ->
					println "Processing ${outputDir.name}"
					FileTree tree = outputDir.ideOutputDir != null ? outputDir.ideOutputDir : outputDir.gradleOutputDir.asFileTree
					println tree
					tree.matching {
						it.include '**/*.class'
					}.each { file ->
						CompileTimeTransformer.processClassFile(file)
					}
				}
			}
		}

		project.extensions.getByType(ModDevExtension).runs {
			configureEach {
				taskBefore taskIdea
			}
		}

		def task = project.tasks.register("beanificationTransformClasses") {
			mustRunAfter 'classes'

			// inputs.files(project.fileTree("src/main/java"))
			// def outputFile = project.layout.buildDirectory.file("beanificationTaskOutput.txt").get().asFile
			// outputs.file(outputFile)
			outputs.upToDateWhen {
				false
			}

			def outputDir = project.providers.provider {
				project.extensions.getByType(JavaPluginExtension).sourceSets.main.output.classesDirs
			}
			it.doLast {
				def tree = outputDir.get().asFileTree
				println tree
				tree.matching {
					it.include '**/*.class'
				}.each { file ->
					CompileTimeTransformer.processClassFile(file)
				}
				//outputFile.text = "${new Date()}"
			}
		}
		def classesTask = project.tasks.named('classes')
		project.afterEvaluate {
			classesTask.get().finalizedBy(task)
		}
	}

	@TupleConstructor
	class NamedIdeOutputDirs {
		String name
		FileCollection gradleOutputDir
		@Nullable FileTree ideOutputDir

	}

}