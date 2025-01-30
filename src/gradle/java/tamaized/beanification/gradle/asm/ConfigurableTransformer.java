package tamaized.beanification.gradle.asm;

import groovyjarjarasm.asm.MethodVisitor;
import groovyjarjarasm.asm.Opcodes;

public class ConfigurableTransformer {

	public static void transform(MethodVisitor method) {
		method.visitVarInsn(Opcodes.ALOAD, 0);
		method.visitMethodInsn(Opcodes.INVOKESTATIC, "tamaized/beanification/BeanContext", "injectInto", "(Ljava/lang/Object;)V", false);
	}

}
