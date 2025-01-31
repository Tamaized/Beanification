package tamaized.beanification.gradle.asm;

import groovyjarjarasm.asm.tree.AbstractInsnNode;
import groovyjarjarasm.asm.tree.InsnList;

public class AsmInsnListUtil {

	static InsnList of(AbstractInsnNode... nodes) {
		InsnList list = new InsnList();
		for (AbstractInsnNode node : nodes) {
			list.add(node);
		}
		return list;
	}

}
