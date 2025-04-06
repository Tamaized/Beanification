package tamaized.beanification.event;

import net.neoforged.neoforge.common.NeoForge;
import org.junit.jupiter.api.Test;
import tamaized.beanification.BeanContext;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertSame;

public class OverrideBeanEventIntegrationTest {

	@Test
	public void override() {
		final Object override = new Object();
		final Object namedOverride = new Object();
		Consumer<OverrideBeanEvent> listener = event -> {
			event.override(Object.class, null, override);
			event.override(Object.class, "namedOverride", namedOverride);
		};
		NeoForge.EVENT_BUS.addListener(OverrideBeanEvent.class, listener);

		final Object named = new Object();
		BeanContext.init("beanification", context -> {
			context.register(Object.class, new Object());
			context.register(Object.class, "named", named);
			context.register(Object.class, "namedOverride", new Object());
		});

		assertSame(override, BeanContext.inject(Object.class));
		assertSame(named, BeanContext.inject(Object.class, "named"));
		assertSame(namedOverride, BeanContext.inject(Object.class, "namedOverride"));

		NeoForge.EVENT_BUS.unregister(listener);
	}

}
