package tamaized.beanification.gradle.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class CompileTimeTransformer {

	public static byte[] transform(byte[] classBytes, String fileName) {
		ClassReader classReader = new ClassReader(classBytes);
		ClassNode classNode = new ClassNode();
		classReader.accept(classNode, 0);

		boolean modified = ConfigurableTransformer.transform(classNode);

		if (!modified)
			return null;

		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		classNode.accept(classWriter);
		return classWriter.toByteArray();
	}

	public static void processClassFile(File classFile) throws IOException {
		byte[] classBytes = Files.readAllBytes(classFile.toPath());
		byte[] modifiedBytes = transform(classBytes, classFile.getName());
		if (modifiedBytes != null)
			Files.write(classFile.toPath(), modifiedBytes);
	}

}
