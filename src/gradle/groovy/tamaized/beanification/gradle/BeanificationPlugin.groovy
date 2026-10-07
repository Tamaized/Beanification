package tamaized.beanification.gradle

import groovy.transform.TupleConstructor
import groovyjarjarantlr4.v4.runtime.misc.Nullable
import net.neoforged.moddevgradle.dsl.ModDevExtension
import net.neoforged.moddevgradle.internal.IntelliJOutputDirectoryValueSource
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
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
					println "Processing Dir: ${outputDir.name}"
					FileTree tree = outputDir.ideOutputDir != null ? outputDir.ideOutputDir : outputDir.gradleOutputDir.asFileTree
					println "Tree (IDEA): ${tree}"
					tree.matching {
						it.include '**/*.class'
					}.each { file ->
						println "Processing: ${file.getName()}"
						CompileTimeTransformer.processClassFile(file) { Logging.getLogger(BeanificationPlugin).lifecycle(it) }
					}
				}
			}
		}

		def pluginJar = new File(BeanificationPlugin.class.protectionDomain.codeSource.location.toURI())

		project.extensions.getByType(ModDevExtension).runs {
			configureEach {
				taskBefore taskIdea
				if (pluginJar.name.endsWith('.jar'))
					jvmArgument "-javaagent:${pluginJar.absolutePath}"
			}
		}

		project.tasks.withType(JavaCompile).configureEach {
			def outputDir = it.destinationDirectory
			it.doLast {
				def tree = outputDir.get().asFileTree
				println "Tree (Gradle-JavaCompile): ${tree}"
				tree.matching {
					it.include '**/*.class'
				}.each { file ->
					println "Processing: ${file.getName()}"
					CompileTimeTransformer.processClassFile(file) { Logging.getLogger(BeanificationPlugin).lifecycle(it) }
				}
			}
		}
	}

	@TupleConstructor
	class NamedIdeOutputDirs {
		String name
		FileCollection gradleOutputDir
		@Nullable FileTree ideOutputDir

	}

}