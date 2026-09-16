package tamaized.beanification.internal;

import net.neoforged.neoforgespi.language.ModFileScanData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import tamaized.beanification.junit.MockitoRunner;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({MockitoRunner.class})
public class RequiredModIdsAnnotationDataPredicateFactoryTests {

	@InjectMocks
	private RequiredModIdsAnnotationDataPredicateFactory instance;

	@SafeVarargs
	private <T> ArrayList<T> list(T... elements) {
		return new ArrayList<>(Arrays.asList(elements));
	}

	@Test
	public void filterNoProp() {
		assertTrue(instance.filter(new ModFileScanData.AnnotationData(null, null, null, "test", Map.of())));
	}

	@Test
	public void filterEmptyList() {
		assertTrue(instance.filter(new ModFileScanData.AnnotationData(null, null, null, "test", Map.of("requiresModIdsLoaded", list()))));
	}

	@Test
	public void filterWrongType() {
		assertTrue(instance.filter(new ModFileScanData.AnnotationData(null, null, null, "test", Map.of("requiresModIdsLoaded", list(new Object())))));
	}

	@Test
	public void filterModNotLoaded() {
		assertFalse(instance.filter(new ModFileScanData.AnnotationData(null, null, null, "test", Map.of("requiresModIdsLoaded", list("mod")))));
	}

	@Test
	public void filterLoaded() {
		assertTrue(instance.filter(new ModFileScanData.AnnotationData(null, null, null, "test", Map.of("requiresModIdsLoaded", list("beanification")))));
	}

}
