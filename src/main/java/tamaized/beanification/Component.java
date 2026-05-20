package tamaized.beanification;

import net.neoforged.api.distmarker.Dist;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Annotated classes will be automatically registered to {@link tamaized.beanification.BeanContext}.
 * </p>
 * <p>
 * Requires a no-arg constructor or a constructor with only {@link Autowired} or {@link Directory} parameters.<br/>
 * If two valid constructors are found then the constructor with {@link Autowired} or {@link Directory} parameters will be prioritized.<br/>
 * If multiple constructors contain {@link Autowired} or {@link Directory} parameters then {@link IllegalArgumentException} will be thrown.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Component {

	String DEFAULT_VALUE = "<beanification_shade_value>!beanification:internal:bean:DEFAULT!";

	/**
	 * Used to create a named bean.
	 */
	String value() default DEFAULT_VALUE;

	Dist[] dist() default {};

}
