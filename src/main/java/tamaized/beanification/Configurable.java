package tamaized.beanification;

import java.lang.annotation.*;

/**
 * Supported class types will be handled as pseudo-beans for {@link tamaized.beanification.processors.AnnotationDataPostProcessor}.<p/>
 *
 * This annotation is {@link Inherited} to subclasses.<p/>
 *
 * Supported Types:<br/>
 * <ul>
 *     <li>Registry Objects</li>
 *     <li>Registered {@link net.minecraft.client.renderer.blockentity.BlockEntityRenderer}</li>
 *     <li>Registered {@link net.minecraft.client.renderer.entity.EntityRenderer}</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Configurable {

}
