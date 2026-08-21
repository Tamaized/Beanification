package tamaized.beanification;

import java.lang.annotation.*;

/**
 * <p>
 * <b>Must be enabled with the Gradle plugin, see the README for more information.</b>
 * </p>
 * <p>
 * Annotated classes will have all of their constructors injected with {@link BeanContext#injectInto(Object)}<br/>
 * The injection happens at the very end of each constructor. Specifically before each RETURN opcode<br/>
 * This even works for classes that have not actually defined a constructor in their source code.</p>
 * </p>
 * <p>
 * {@code @Configurable} classes may use both static and non-static autowired as these classes are NOT true beans.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Configurable {

}
