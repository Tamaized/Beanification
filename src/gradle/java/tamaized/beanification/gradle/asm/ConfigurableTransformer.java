package tamaized.beanification.gradle.asm;

import groovyjarjarasm.asm.MethodVisitor;
import groovyjarjarasm.asm.Opcodes;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

public class ConfigurableTransformer {

	private static final Logger logger = Logging.getLogger(ConfigurableTransformer.class);

	public static void transform(MethodVisitor method, String className, String memberName, String descriptor) {
		logger.lifecycle("Transforming {} {} {}", className, memberName, descriptor);
		method.visitVarInsn(Opcodes.ALOAD, 0);
		method.visitMethodInsn(Opcodes.INVOKESTATIC, "tamaized/beanification/BeanContext", "injectInto", "(Ljava/lang/Object;)V", false);
	}

}
