package tamaized.beanification.gradle.asm;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

public class AsmInsnListUtil {

	static InsnList of(AbstractInsnNode... nodes) {
		InsnList list = new InsnList();
		for (AbstractInsnNode node : nodes) {
			list.add(node);
		}
		return list;
	}

}
