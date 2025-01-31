package tamaized.beanification;

import java.lang.annotation.*;

/**
 * <b>Must be enabled with the gradle plugin, see the README for more information</b><p/>
 *
 * Annotated classes will have all of their constructors injected with {@link BeanContext#injectInto(Object)}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Configurable {

}
