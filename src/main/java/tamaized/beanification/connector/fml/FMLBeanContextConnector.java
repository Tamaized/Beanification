package tamaized.beanification.connector.fml;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.jspecify.annotations.Nullable;
import tamaized.beanification.BeanContext;
import tamaized.beanification.ContextConnector;
import tamaized.beanification.InternalAutowired;
import tamaized.beanification.InternalBeanRegistrar;
import tamaized.beanification.scan.AnnotationScanData;
import tamaized.beanification.scan.ScanCondition;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class FMLBeanContextConnector implements ContextConnector {

	@InternalAutowired
	private ModScanDataConverter modScanDataConverter;

	@InternalAutowired
	private ModuleAnnotationRetriever moduleAnnotationRetriever;

	private final String modid;

	private final List<String> moduleNames;

	@Nullable
	private ModContainer cachedModContainer;

	@Nullable
	private AnnotationScanData cachedScanData = null;

	private FMLBeanContextConnector(String modid, String... additionalScanModules) {
		this.modid = modid;
		moduleNames = Stream.concat(
			modid.equals("beanification") ? Stream.empty() : Stream.of("beanification"),
			Stream.of(additionalScanModules)
		).toList();
	}

	public static void init(String modid, String... additionalScanModules) {
		BeanContext.registerConnectors(new FMLBeanContextConnector(modid, additionalScanModules));
		BeanContext.init(new Log4jLoggerWrapper());
	}

	@Override
	public void registerInternalBeans(InternalBeanRegistrar registrar) {
		registrar.register(ModScanDataConverter.class, null, new ModScanDataConverter());
		registrar.register(ModuleAnnotationRetriever.class, null, new ModuleAnnotationRetriever(moduleNames));
	}

	@Override
	public List<ScanCondition> conditions() {
		return List.of(new RequiresModsScanCondition(), new RequiresDistScanCondition());
	}

	public ModContainer getModContainer() {
		return Objects.requireNonNull(cachedModContainer);
	}

	@Override
	public AnnotationScanData scan(List<ScanCondition> conditions) {
		if (cachedScanData != null)
			return cachedScanData;

		ModContainer modContainer = ModList.get().getModContainerById(modid).orElseThrow(() -> new RuntimeException("Where is " + modid + "???!"));

		if (modContainer.getEventBus() == null)
			throw new RuntimeException("Mod EventBus is null");

		cachedModContainer = modContainer;

		ModFileScanData scanData = modContainer.getModInfo().getOwningFile().getFile().getScanResult();

		cachedScanData = new AnnotationScanData(
			moduleAnnotationRetriever,
			conditions,
			modScanDataConverter.convert(scanData)
		);
		return cachedScanData;
	}
}
