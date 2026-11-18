package tamaized.beanification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 *     Annotated {@code List<?>} fields will be automatically injected with {@link BeanContext#inject(Class)} and all registered subtypes of the {@link Class}.<br/>
 *     Both unnamed and named beans will be gathered.
 * </p>
 * <p>
 *     Follows all the same rules as {@link Autowired}
 * </p>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Directory {

	Class<?> value();

}
