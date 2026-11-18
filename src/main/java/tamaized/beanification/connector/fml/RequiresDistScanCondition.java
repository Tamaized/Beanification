package tamaized.beanification.connector.fml;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.modscan.ModAnnotation;
import tamaized.beanification.scan.AnnotationScanData;
import tamaized.beanification.scan.ScanCondition;

import java.util.Map;

public class RequiresDistScanCondition implements ScanCondition {

	@Override
	@SuppressWarnings("UnstableApiUsage")
	public boolean test(AnnotationScanData.ScannedMember member) {
		Map<String, Object> requiresDist = member.annotations().get(AnnotationScanData.TypeData.of(RequiresDist.class));
		if (requiresDist == null)
			return true;

		return requiresDist.get("value") instanceof ModAnnotation.EnumHolder holder && Dist.valueOf(holder.value()) == FMLEnvironment.getDist();
	}

}
