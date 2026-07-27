package dev.vercim.ticks.neoforge;

//? neoforge {
/*import dev.vercim.ticks.TicksConfig;
import dev.vercim.ticks.TicksConfigScreen;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod("ticks")
public final class TicksNeoForge {
	public TicksNeoForge(ModContainer container) {
		TicksConfig.initialize(FMLPaths.CONFIGDIR.get());
		if (ModList.get().isLoaded("cloth_config")) {
			container.registerExtensionPoint(IConfigScreenFactory.class,
					(minecraft, parent) -> TicksConfigScreen.create(parent));
		}
	}
}
*///?}
