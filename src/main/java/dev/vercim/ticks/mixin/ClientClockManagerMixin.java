package dev.vercim.ticks.mixin;

//? >=26 {
/*import java.util.Map;

import dev.vercim.ticks.TicksController;
import net.minecraft.client.ClientClockManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientClockManager.class)
abstract class ClientClockManagerMixin {
	@Inject(method = "handleUpdates", at = @At("HEAD"))
	private void ticks$trackClockUpdate(long gameTime,
			Map<Holder<WorldClock>, ClockNetworkState> updates, CallbackInfo callbackInfo) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return;
		}

		for (Map.Entry<Holder<WorldClock>, ClockNetworkState> entry : updates.entrySet()) {
			if (entry.getKey().is(WorldClocks.OVERWORLD)) {
				ClockNetworkState state = entry.getValue();
				TicksController.onClockUpdate(
						level, gameTime, state.totalTicks(), state.partialTick(), state.rate());
				return;
			}
		}
	}
}
*///?}
