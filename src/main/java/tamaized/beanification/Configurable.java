package tamaized.beanification;

import java.lang.annotation.*;

/**
 * <p>
 * <b>Must be enabled with a Gradle plugin, it is the job of a connector to build such a plugin.</b><br/>
 * This annotation exists here as a means to reduce duplicate declarations between connectors.
 * </p>
 * <p>
 * Annotated classes will have all of their constructors injected with {@link BeanContext#injectInto(Object)}<br/>
 * The injection happens at the very end of each constructor. Specifically before each RETURN opcode<br/>
 * This even works for classes that have not actually defined a constructor in their source code.</p>
 * </p>
 * <p>
 * {@code @Configurable} classes are treated as beans during {@link BeanContext#injectInto(Object)} and so {@link Autowired} fields MUST be non-static!
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Configurable {

}
