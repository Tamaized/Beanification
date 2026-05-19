package tamaized.beanification;

import net.neoforged.api.distmarker.Dist;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotated static methods will automatically be invoked and the returned object will be registered to {@link tamaized.beanification.BeanContext}.<p/>
 *
 * The returned object class can extend/implement the method return type. The method return type is what's used for bean identification.<br/>
 * This for example allows for interfaces to be used as beans.<p/>
 *
 * Parameters must be annotated with {@link Autowired}
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Bean {

	/**
	 * Used to create a named bean.
	 */
	String value() default Component.DEFAULT_VALUE;

	Dist[] dist() default {};

	int priority() default 0;

}
