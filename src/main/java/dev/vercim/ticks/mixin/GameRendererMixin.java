package dev.vercim.ticks.mixin;

import dev.vercim.ticks.TicksController;
//? >=1.21.1 {
/*import net.minecraft.client.DeltaTracker;
*///?}
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
	//? >=1.21.1 {
	/*@Inject(method = "render", at = @At("HEAD"))
	private void ticks$beginFrame(DeltaTracker deltaTracker, boolean tick, CallbackInfo callbackInfo) {
		TicksController.beginFrame(deltaTracker.getGameTimeDeltaPartialTick(false));
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void ticks$endFrame(DeltaTracker deltaTracker, boolean tick, CallbackInfo callbackInfo) {
		TicksController.endFrame();
	}
	*///?}
	//? <1.21.1 {
	@Inject(method = "render", at = @At("HEAD"))
	private void ticks$beginFrame(float partialTick, long nanoTime, boolean tick, CallbackInfo callbackInfo) {
		TicksController.beginFrame(partialTick);
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void ticks$endFrame(float partialTick, long nanoTime, boolean tick, CallbackInfo callbackInfo) {
		TicksController.endFrame();
	}
	//?}
}
