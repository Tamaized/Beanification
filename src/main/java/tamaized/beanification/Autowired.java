package tamaized.beanification;

import net.neoforged.api.distmarker.Dist;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Annotated fields will be automatically injected with {@link BeanContext#inject(Class)}.<br/>
 * If a value is set then {@link BeanContext#inject(Class, String)} is used instead.
 * </p>
 * <p>
 * Fields may be private.<br/>
 * When used inside a Bean or {@link Configurable} the field must be non-static.<br/>
 * When used outside a Bean, the field <b>must</b> be <b>static</b>!
 * <p>
 * Can also be applied to <strike>{@link Bean} method parameters and</strike> {@link Component} constructor parameters.<br/>
 * Note: {@link Bean} capabilities are not yet implemented.
 * </p>
 */
@Nullable
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Autowired {

	String value() default Component.DEFAULT_VALUE;

	Dist[] dist() default {};

}
