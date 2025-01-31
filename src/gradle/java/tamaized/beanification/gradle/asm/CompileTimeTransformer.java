package tamaized.beanification.gradle.asm;

import groovyjarjarasm.asm.*;
import groovyjarjarasm.asm.commons.AdviceAdapter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class CompileTimeTransformer {

	public static byte[] transform(byte[] classBytes, String className) {
		ClassReader classReader = new ClassReader(classBytes);
		ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
		ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM9, classWriter) {
			private final List<String> annotations = new ArrayList<>();

			@Override
			public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				annotations.add(descriptor);
				return super.visitAnnotation(descriptor, visible);
			}

			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
				if ("<init>".equals(name)) {
					return new AdviceAdapter(Opcodes.ASM9, mv, access, name, descriptor) {
						@Override
						protected void onMethodEnter() {
							if (annotations.contains("Ltamaized/beanification/Configurable;"))
								ConfigurableTransformer.transform(mv, className);
						}
					};
				}
				return mv;
			}
		};
		classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
		return classWriter.toByteArray();
	}

	public static void processClassFile(File classFile) throws IOException {
		byte[] classBytes = Files.readAllBytes(classFile.toPath());
		byte[] modifiedBytes = transform(classBytes, classFile.getName());
		Files.write(classFile.toPath(), modifiedBytes);
	}

}
