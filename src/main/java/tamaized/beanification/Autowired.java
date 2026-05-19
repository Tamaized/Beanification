package tamaized.beanification;

import net.neoforged.api.distmarker.Dist;

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
 * Can also be applied to {@link Bean} method parameters and {@link Component} constructor parameters.
 * </p>
 * <p>
 * Record <b>fields</b> will be skipped, as a java record will apply annotations to both fields and constructor parameters.<br/>
 * Record constructor injection will still work.
 * </p>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Autowired {

	String value() default Component.DEFAULT_VALUE;

	Dist[] dist() default {};

}
