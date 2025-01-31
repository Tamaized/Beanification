package tamaized.beanification;

import java.lang.annotation.*;

/**
 * <b>Must be enabled with the gradle plugin, see the README for more information</b><p/>
 *
 * Annotated classes will have all of their constructors injected with {@link BeanContext#injectInto(Object)}<br/>
 * The injection happens at the very bottom of each constructor. Specifically before each RETURN opcode<br/>
 * This even works for classes that have not actually defined a constructor in their source code.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Configurable {

}
