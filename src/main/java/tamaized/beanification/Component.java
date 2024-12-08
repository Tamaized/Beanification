package tamaized.beanification;

import net.neoforged.api.distmarker.Dist;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Annotated classes will be automatically registered with {@link tamaized.beanification.BeanContext.BeanContextRegistrar#register(Class, Object)}.<br/>
 * If a value is set then {@link tamaized.beanification.BeanContext.BeanContextRegistrar#register(Class, String, Object)} is used instead.
 * </p>
 * <p>
 * Requires a no-arg constructor or a constructor with only {@link Autowired} parameters.<br/>
 * If two valid constructors are found then the constructor with {@link Autowired} parameters will be prioritized.<br/>
 * If multiple constructors contain {@link Autowired} parameters then {@link IllegalArgumentException} will be thrown.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Component {

	String DEFAULT_VALUE = "<beanification_shade_value>!beanification:internal:bean:DEFAULT!";

	String value() default DEFAULT_VALUE;

	Dist[] dist() default {};

}
