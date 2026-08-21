package tamaized.beanification;

import net.neoforged.api.distmarker.Dist;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Annotated static fields will be automatically injected with {@link BeanContext#injectLazy(Class)}.<br/>
 * If a value is set then {@link BeanContext#injectLazy(Class, String)} is used instead.
 * </p>
 * <p>
 * Fields may be private.
 * </p>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface LazyAutowired {

	String value() default Component.DEFAULT_VALUE;

	Dist[] dist() default {};

}
