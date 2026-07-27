package dev.vercim.ticks.forge;

//? forge {
/*import dev.vercim.ticks.TicksConfig;
import dev.vercim.ticks.TicksConfigScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod("ticks")
public final class TicksForge {
	public TicksForge() {
		TicksConfig.initialize(FMLPaths.CONFIGDIR.get());
		if (ModList.get().isLoaded("cloth_config")) {
			ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
					() -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> TicksConfigScreen.create(parent)));
		}
	}
}
*///?}
