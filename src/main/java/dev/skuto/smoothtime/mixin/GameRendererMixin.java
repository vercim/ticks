package dev.skuto.smoothtime.mixin;

import dev.skuto.smoothtime.SmoothTimeController;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
	@Inject(method = "render", at = @At("HEAD"))
	private void smoothTime$beginFrame(DeltaTracker deltaTracker, boolean tick, CallbackInfo callbackInfo) {
		SmoothTimeController.beginFrame(deltaTracker);
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void smoothTime$endFrame(DeltaTracker deltaTracker, boolean tick, CallbackInfo callbackInfo) {
		SmoothTimeController.endFrame();
	}
}
