package dev.vercim.ticks.mixin;

import dev.vercim.ticks.TicksController;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DimensionType.class)
abstract class DimensionTypeMixin {
	@Inject(method = "timeOfDay", at = @At("HEAD"), cancellable = true)
	private void ticks$useFractionalTime(long dayTime, CallbackInfoReturnable<Float> callbackInfo) {
		Float angle = TicksController.getSkyAngleOverride((DimensionType) (Object) this, dayTime);
		if (angle != null) {
			callbackInfo.setReturnValue(angle);
		}
	}
}
