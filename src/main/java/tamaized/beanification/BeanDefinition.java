package tamaized.beanification;

import javax.annotation.Nullable;
import java.util.Objects;

record BeanDefinition<T>(Class<T> type, @Nullable String name) {

	@Override
	public int hashCode() {
		return Objects.hash(type, name);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof BeanDefinition<?>(Class<?> typeOther, String nameOther) && type.equals(typeOther) && Objects.equals(name, nameOther);
	}
}
