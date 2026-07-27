package dev.vercim.ticks.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.vercim.ticks.TicksConfigScreen;
import net.fabricmc.loader.api.FabricLoader;

public final class TicksModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		if (!FabricLoader.getInstance().isModLoaded("cloth-config2")) {
			return null;
		}
		return TicksConfigScreen::create;
	}
}
