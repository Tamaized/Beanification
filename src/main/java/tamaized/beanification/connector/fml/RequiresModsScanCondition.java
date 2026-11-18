package tamaized.beanification.connector.fml;

import net.neoforged.fml.ModList;
import tamaized.beanification.scan.AnnotationScanData;
import tamaized.beanification.scan.ScanCondition;

import java.util.List;
import java.util.Map;

public class RequiresModsScanCondition implements ScanCondition {

	@Override
	public boolean test(AnnotationScanData.ScannedMember member) {
		Map<String, Object> requiresMods = member.annotations().get(AnnotationScanData.TypeData.of(RequiresMods.class));
		if (requiresMods == null)
			return true;

		if (requiresMods.get("value") instanceof List<?> list) {
			for (Object o : list) {
				if (o instanceof String modId && !ModList.get().isLoaded(modId))
					return false;
			}
		}

		return true;
	}

}
