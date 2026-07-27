package dev.vercim.ticks.fabric;

import dev.vercim.ticks.TicksConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class TicksFabric implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		TicksConfig.initialize(FabricLoader.getInstance().getConfigDir());
	}
}
