package tamaized.beanification.internal;

import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.Type;
import tamaized.beanification.Autowired;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
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

	public boolean isStatic(Field field) {
		return Modifier.isStatic(field.getModifiers());
	}

	public Class<?> forName(String name) throws ClassNotFoundException {
		return Class.forName(name);
	}

	public Type getType(Class<?> c) {
		return Type.getType(c);
	}

}
