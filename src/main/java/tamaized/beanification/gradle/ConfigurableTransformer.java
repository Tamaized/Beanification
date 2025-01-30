package tamaized.beanification.gradle;

import org.objectweb.asm.*;

public class ConfigurableTransformer {

	public static void transform(MethodVisitor method) {
		method.visitVarInsn(Opcodes.ALOAD, 0);
		method.visitMethodInsn(Opcodes.INVOKESTATIC, "tamaized/beanification/BeanContext", "injectInto", "(Ljava/lang/Object;)V", false);
	}

}
