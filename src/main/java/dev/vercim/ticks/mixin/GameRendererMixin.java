package dev.vercim.ticks.mixin;

import dev.vercim.ticks.TicksController;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
	@Inject(method = "render", at = @At("HEAD"))
	private void ticks$beginFrame(DeltaTracker deltaTracker, boolean tick, CallbackInfo callbackInfo) {
		TicksController.beginFrame(deltaTracker);
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void ticks$endFrame(DeltaTracker deltaTracker, boolean tick, CallbackInfo callbackInfo) {
		TicksController.endFrame();
	}
}
