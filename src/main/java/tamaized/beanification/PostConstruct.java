package tamaized.beanification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * 	Annotated methods will be automatically invoked after bean construction has finished.<br/>
 * 	Methods must be no-arg.
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PostConstruct {

}
