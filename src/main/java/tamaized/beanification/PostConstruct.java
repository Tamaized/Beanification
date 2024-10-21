package tamaized.beanification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * 	Annotated methods will be automatically invoked after bean construction has finished.<br/>
 * 	Runs after {@link tamaized.beanification.processors.AnnotationDataPostProcessor}
 * </p>
 * <p>
 * 	Must have no arguments or have a single IEventBus parameter for the Mod EventBus.<br/>
 * 	The parameter can be configured to be the Game EventBus type instead via {@code @PostConstruct(PostConstruct.Bus.GAME)}
 * </p>
 * <p>
 * 	Works for {@link Configurable}
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PostConstruct {

	Bus value() default Bus.MOD;

	enum Bus {
		MOD, GAME
	}

}
