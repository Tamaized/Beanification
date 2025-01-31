package tamaized.beanification.gradle.asm;

import groovyjarjarasm.asm.*;
import groovyjarjarasm.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class CompileTimeTransformer {

	public static byte[] transform(byte[] classBytes, String fileName) {
		ClassReader classReader = new ClassReader(classBytes);
		ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
		ClassNode classNode = new ClassNode();
		classReader.accept(classNode, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);

		ConfigurableTransformer.transform(classNode);

		classNode.accept(classWriter);
		return classWriter.toByteArray();
	}

	public static void processClassFile(File classFile) throws IOException {
		byte[] classBytes = Files.readAllBytes(classFile.toPath());
		byte[] modifiedBytes = transform(classBytes, classFile.getName());
		Files.write(classFile.toPath(), modifiedBytes);
	}

}
