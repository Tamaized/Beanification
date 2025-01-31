package tamaized.beanification.gradle.asm;

import groovyjarjarasm.asm.Opcodes;
import groovyjarjarasm.asm.tree.ClassNode;
import groovyjarjarasm.asm.tree.MethodInsnNode;
import groovyjarjarasm.asm.tree.VarInsnNode;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

public class ConfigurableTransformer {

	private static final Logger logger = Logging.getLogger(ConfigurableTransformer.class);

	public static boolean transform(ClassNode classNode) {
		AtomicBoolean flag = new AtomicBoolean(false);
		if (classNode.visibleAnnotations != null && classNode.visibleAnnotations.stream().anyMatch(node -> node.desc.equals("Ltamaized/beanification/Configurable;"))) {
			classNode.methods.stream()
				.filter(node -> node.name.equals("<init>"))
				.forEach(methodNode -> {
					if (StreamSupport.stream(methodNode.instructions.spliterator(), false).anyMatch(
						insn -> insn instanceof MethodInsnNode methodInsn &&
								methodInsn.getOpcode() == Opcodes.INVOKESTATIC &&
								methodInsn.owner.equals("tamaized/beanification/BeanContext") &&
								methodInsn.name.equals("injectInto") &&
								methodInsn.desc.equals("(Ljava/lang/Object;)V")
					)) {
						logger.lifecycle("[ConfigurableTransformer] Skipping, already injected {} {} {}", classNode.name, methodNode.name, methodNode.desc);
					} else {
						logger.lifecycle("[ConfigurableTransformer] Transforming {} {} {}", classNode.name, methodNode.name, methodNode.desc);
						methodNode.instructions.insertBefore(methodNode.instructions.getFirst(), AsmInsnListUtil.of(
							new VarInsnNode(Opcodes.ALOAD, 0),
							new MethodInsnNode(Opcodes.INVOKESTATIC, "tamaized/beanification/BeanContext", "injectInto", "(Ljava/lang/Object;)V", false)
						));
						flag.set(true);
					}
				});
		}
		return flag.get();
	}

}
