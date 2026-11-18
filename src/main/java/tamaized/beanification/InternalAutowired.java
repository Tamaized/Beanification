package tamaized.beanification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Must be used with {@link tamaized.beanification.InternalBeanContext#injectInto(Object)}
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InternalAutowired {

	String value() default Component.DEFAULT_VALUE;

}
