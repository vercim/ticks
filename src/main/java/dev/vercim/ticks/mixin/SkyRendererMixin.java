package dev.vercim.ticks.mixin;

//? >=1.21.11 {
/*import dev.vercim.ticks.TicksController;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.SkyRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.SkyRenderer")
abstract class SkyRendererMixin {
	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void ticks$beginSkyFrame(ClientLevel level, float partialTick, Camera camera, SkyRenderState renderState,
			CallbackInfo callbackInfo) {
		TicksController.beginFrame(partialTick);
	}

	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void ticks$useSmoothedSkyAngles(ClientLevel level, float partialTick, Camera camera, SkyRenderState renderState,
			CallbackInfo callbackInfo) {
		Float angle = TicksController.getSkyAngleOverride(level.dimensionType(), level.getDayTime());
		if (angle != null) {
			float sunAndStarAngle = angle * ((float) Math.PI * 2.0F);
			renderState.sunAngle = sunAndStarAngle;
			renderState.starAngle = sunAndStarAngle;
			renderState.moonAngle = sunAndStarAngle + (float) Math.PI;
		}
		TicksController.endFrame();
	}
}
*///?}
