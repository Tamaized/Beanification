package tamaized.beanification.internal;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import tamaized.beanification.Autowired;
import tamaized.beanification.Directory;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@ApiStatus.Internal
public class InternalReflectionHelper {

	public boolean classOrSuperEquals(Type clazz, Class<?> c) {
		return clazz.equals(Type.getType(c)) || (c.getSuperclass() instanceof Class<?> sup && classOrSuperEquals(clazz, sup));
	}

	public List<Field> getAllAutowiredFieldsIncludingSuper(Class<?> c, String name, String value) {
		List<Field> list =  new ArrayList<>();
		getAllAutowiredFieldsIncludingSuper(c, name, value, list);
		return list;
	}

	private void getAllAutowiredFieldsIncludingSuper(Class<?> c, String name, String value, List<Field> list) {
		try {
			Field f = c.getDeclaredField(name);
			if (f.isAnnotationPresent(Autowired.class) && Objects.equals(f.getAnnotation(Autowired.class).value(), value))
				list.add(f);
		} catch (NoSuchFieldException ex) {
			// NO-OP
		}
		Class<?> sup = c.getSuperclass();
		if (sup != null)
			getAllAutowiredFieldsIncludingSuper(sup, name, value, list);
	}

	public List<Field> getAllDirectoryFieldsIncludingSuper(Class<?> c, String name) {
		List<Field> list =  new ArrayList<>();
		getAllDirectoryFieldsIncludingSuper(c, name, list);
		return list;
	}

	private void getAllDirectoryFieldsIncludingSuper(Class<?> c, String name, List<Field> list) {
		try {
			Field f = c.getDeclaredField(name);
			if (f.isAnnotationPresent(Directory.class))
				list.add(f);
		} catch (NoSuchFieldException ex) {
			// NO-OP
		}
		Class<?> sup = c.getSuperclass();
		if (sup != null)
			getAllDirectoryFieldsIncludingSuper(sup, name, list);
	}

	public boolean isStatic(Field field) {
		return Modifier.isStatic(field.getModifiers());
	}

	public boolean isStatic(Method method) {
		return Modifier.isStatic(method.getModifiers());
	}

	@SafeVarargs
	public final boolean isAnyAnnotationPresent(Class<?> clazz, Class<? extends Annotation>... annotations) {
		for (Class<? extends Annotation> annotation : annotations) {
			if (clazz.isAnnotationPresent(annotation))
				return true;
		}
		return false;
	}

	public <A extends Annotation> A getAnnotation(Class<?> clazz, Class<A> annotation) {
		return clazz.getAnnotation(annotation);
	}

	public Field getDeclaredField(Class<?> clazz, String name) throws NoSuchFieldException {
		return clazz.getDeclaredField(name);
	}

	private String trimMethodName(String raw) {
		return raw.split("\\(")[0];
	}

	public Method getDeclaredMethod(Class<?> clazz, String name, @Nullable Class<?>... args) throws NoSuchMethodException {
		return clazz.getDeclaredMethod(trimMethodName(name), args);
	}

	public List<Method> getDeclaredMethodsForName(Class<?> clazz, String name) {
		return Arrays.stream(clazz.getDeclaredMethods()).filter(m -> m.getName().equals(trimMethodName(name))).toList();
	}

	public Type getType(Class<?> c) {
		return Type.getType(c);
	}

	public Constructor<?>[] getConstructors(Class<?> c) {
		return c.getConstructors();
	}

	public Constructor<?> getConstructor(Class<?> c, Class<?>... p) throws NoSuchMethodException {
		return c.getConstructor(p);
	}

	public boolean allParametersHaveAnnotation(Annotation[][] parameterAnnotations, Class<? extends Annotation> annotationClass) {
		for (Annotation[] annotations : parameterAnnotations) {
			boolean hasAnnotation = false;
			for (Annotation annotation : annotations) {
				if (annotation.annotationType().equals(annotationClass)) {
					hasAnnotation = true;
					break;
				}
			}
			if (!hasAnnotation) {
				return false;
			}
		}
		return true;
	}

}
