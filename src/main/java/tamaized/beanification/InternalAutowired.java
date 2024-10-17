package tamaized.beanification;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Must be used with {@link tamaized.beanification.InternalBeanContext#injectInto(Object)}
 */
@Nullable
@ApiStatus.Internal
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InternalAutowired {

	String value() default Component.DEFAULT_VALUE;

}
