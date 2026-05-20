package tamaized.beanification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 *     Annotated {@code List<?>} fields will be automatically injected with {@link BeanContext#inject(Class)}<br/>
 *     Beans are gathered by using the same package as the class using {@link Directory}.<br/>
 *     Only unnamed beans will be gathered.
 * </p>
 * <p>
 *     The value type will filter which classes to inject.
 * </p>
 * <p>
 *     By default, packages will be scanned recursively.
 * </p>
 * <p>
 *     Follows all the same rules as {@link Autowired}<br/>
 *     <b>Constructor injection does not work</b>
 * </p>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Directory {

	Class<?> value() default Object.class;

	boolean recursive() default true;

}
