package tamaized.beanification.gradle.asm;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class ConfigurableHotswapAgent implements ClassFileTransformer {

	public static void premain(String args, Instrumentation instrumentation) {
		instrumentation.addTransformer(new ConfigurableHotswapAgent());
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
		if (classBeingRedefined == null)
			return null;
		return CompileTimeTransformer.transform(classfileBuffer, System.out::println);
	}

}
