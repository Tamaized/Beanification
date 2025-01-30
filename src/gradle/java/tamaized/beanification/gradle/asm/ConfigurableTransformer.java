package tamaized.beanification.gradle.asm;

import groovyjarjarasm.asm.MethodVisitor;
import groovyjarjarasm.asm.Opcodes;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

public class ConfigurableTransformer {

	private static final Logger logger = Logging.getLogger(ConfigurableTransformer.class);

	public static void transform(MethodVisitor method, String className) {
		logger.info("Transforming {}", className);
		method.visitVarInsn(Opcodes.ALOAD, 0);
		method.visitMethodInsn(Opcodes.INVOKESTATIC, "tamaized/beanification/BeanContext", "injectInto", "(Ljava/lang/Object;)V", false);
	}

}
