package tamaized.beanification.connector.fml;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 *  This is an extension of {@link tamaized.beanification.PostConstruct}<br/>
 *  Annotated methods will run after {@link tamaized.beanification.PostConstruct} annotated methods.<p/>
 * 	Must have a single IEventBus parameter for the Mod EventBus.<br/>
 * 	The parameter can be configured to be the Game EventBus type instead via {@code @EventBusPostConstruct(EventBusPostConstruct.Bus.GAME)}<br/>
 * 	If there are two IEventBus parameters then the first type will be what is defined as {@link EventBusPostConstruct#value()} and the second will be the alternative.
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventBusPostConstruct {

	Bus value() default Bus.MOD;

	enum Bus {
		MOD, GAME
	}

}
