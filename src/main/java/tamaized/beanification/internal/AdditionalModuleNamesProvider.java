package tamaized.beanification.internal;

import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ApiStatus.Internal
public class AdditionalModuleNamesProvider {

	private final List<String> names = new ArrayList<>();

	public List<String> getNames() {
		return Collections.unmodifiableList(names);
	}

	public void setup(List<String> moduleNames) {
		names.clear();
		names.addAll(moduleNames);
	}

}
