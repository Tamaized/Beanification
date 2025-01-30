package tamaized.beanification.gradle.asm;

import groovyjarjarasm.asm.MethodVisitor;
import groovyjarjarasm.asm.Opcodes;

public class ConfigurableTransformer {

	public static void transform(MethodVisitor method, String className) {
		System.out.println("[Configurable] Transforming " + className);
		method.visitVarInsn(Opcodes.ALOAD, 0);
		method.visitMethodInsn(Opcodes.INVOKESTATIC, "tamaized/beanification/BeanContext", "injectInto", "(Ljava/lang/Object;)V", false);
	}

}
