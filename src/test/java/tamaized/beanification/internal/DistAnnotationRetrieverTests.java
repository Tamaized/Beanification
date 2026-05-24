package tamaized.beanification.internal;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.modscan.ModAnnotation;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import tamaized.beanification.Autowired;
import tamaized.beanification.junit.MockitoRunner;

import java.lang.annotation.ElementType;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoRunner.class})
public class DistAnnotationRetrieverTests {

	@Mock
	private AdditionalModuleNamesProvider additionalModuleNamesProvider;

	@InjectMocks
	private DistAnnotationRetriever instance;

	@SafeVarargs
	private <T> ArrayList<T> list(T... elements) {
		return new ArrayList<>(Arrays.asList(elements));
	}

	@Test
	@SuppressWarnings("UnstableApiUsage")
	public void retrieve() {
		ModFileScanData scanData = mock(ModFileScanData.class);
		when(scanData.getAnnotatedBy(Autowired.class, ElementType.FIELD)).thenReturn(Stream.of(
			new ModFileScanData.AnnotationData(null, null, null, "a", Map.of("dist", list())),
			new ModFileScanData.AnnotationData(null, null, null, "b", Map.of("dist", list(new Object()))),
			new ModFileScanData.AnnotationData(null, null, null, "c", Map.of("dist", list(new ModAnnotation.EnumHolder(null, FMLEnvironment.getDist().name())))),
			new ModFileScanData.AnnotationData(null, null, null, "d", Map.of("dist", list(new ModAnnotation.EnumHolder(null, Dist.values()[(FMLEnvironment.getDist().ordinal() + 1) % Dist.values().length].name())))),
			new ModFileScanData.AnnotationData(null, null, null, "e", Map.of("dist", list(new Object(), new ModAnnotation.EnumHolder(null, FMLEnvironment.getDist().name())))),
			new ModFileScanData.AnnotationData(null, null, null, "f", Map.of("dist", new Object())),
			new ModFileScanData.AnnotationData(null, null, null, "g", Map.of())
		));

		when(additionalModuleNamesProvider.getNames()).thenReturn(Collections.emptyList());

		List<ModFileScanData.AnnotationData> result = instance.retrieve(scanData, ElementType.FIELD, Autowired.class).toList();

		assertEquals(5, result.size());
		assertEquals("a", result.getFirst().memberName());
		assertEquals("c", result.get(1).memberName());
		assertEquals("e", result.get(2).memberName());
		assertEquals("f", result.get(3).memberName());
		assertEquals("g", result.get(4).memberName());
	}

}
