package tamaized.beanification.processors;

import tamaized.beanification.BeanDefinition;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

public class BeanAnnotationProcessorClassMetadata {

	public Map<Constructor<?>, MemberMetadata<Parameter>> constructors = new HashMap<>();

	public MemberMetadata<Field> fields = new MemberMetadata<>();

	public MemberMetadata<Method> methods = new MemberMetadata<>();

	public static class MemberMetadata<T> extends HashMap<T, AnnotationMemberMetadata> {

		@java.io.Serial
		private static final long serialVersionUID = 2396383925303816421L;

	}

	public static class AnnotationMemberMetadata extends HashMap<Class<? extends Annotation>, BeanDefinition<?>> {

		@java.io.Serial
		private static final long serialVersionUID = 3904741912730085724L;

	}

}
