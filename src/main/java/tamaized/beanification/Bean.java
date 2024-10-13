package tamaized.beanification;

import net.neoforged.api.distmarker.Dist;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotated static methods will automatically be invoked and the returned object will be registered with {@link tamaized.beanification.BeanContext.BeanContextRegistrar#register(Class, Object)}.<br/>
 * If a value is set then {@link tamaized.beanification.BeanContext.BeanContextRegistrar#register(Class, String, Object)} is used instead.<p/>
 *
 * The returned object class can extend/implement the method return type. The method return type is what's used for bean identification.<br/>
 * This for example allows for interfaces to be used as beans.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Bean {

	String value() default Component.DEFAULT_VALUE;

	Dist[] dist() default {};

}
