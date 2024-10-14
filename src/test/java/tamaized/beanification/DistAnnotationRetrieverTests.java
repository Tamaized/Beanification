package tamaized.beanification;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.modscan.ModAnnotation;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DistAnnotationRetrieverTests {

	@SafeVarargs
	private <T> ArrayList<T> list(T... elements) {
		return new ArrayList<>(Arrays.asList(elements));
	}

	@Test
	public void retrieve() {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(scanData.getAnnotatedBy(Autowired.class, ElementType.FIELD)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, null, "a", Map.of("dist", list())),
			new ModFileScanData.AnnotationData(null, null, null, "b", Map.of("dist", list(new Object()))),
			new ModFileScanData.AnnotationData(null, null, null, "c", Map.of("dist", list(new ModAnnotation.EnumHolder(null, FMLEnvironment.dist.name())))),
			new ModFileScanData.AnnotationData(null, null, null, "d", Map.of("dist", list(new ModAnnotation.EnumHolder(null, Dist.values()[(FMLEnvironment.dist.ordinal() + 1) % Dist.values().length].name())))),
			new ModFileScanData.AnnotationData(null, null, null, "e", Map.of("dist", list(new Object(), new ModAnnotation.EnumHolder(null, FMLEnvironment.dist.name())))),
			new ModFileScanData.AnnotationData(null, null, null, "f", Map.of("dist", new Object())),
			new ModFileScanData.AnnotationData(null, null, null, "g", Map.of())
		));

		List<ModFileScanData.AnnotationData> result = DistAnnotationRetriever.retrieve(scanData, Autowired.class, ElementType.FIELD).toList();

		assertEquals(5, result.size());
		assertEquals("a", result.getFirst().memberName());
		assertEquals("c", result.get(1).memberName());
		assertEquals("e", result.get(2).memberName());
		assertEquals("f", result.get(3).memberName());
		assertEquals("g", result.get(4).memberName());
	}

}
