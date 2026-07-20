package dev.skuto.smoothtime.mixin;

import dev.skuto.smoothtime.SmoothTimeController;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
abstract class ClientLevelMixin {
	@Inject(method = "setDayTime", at = @At("HEAD"))
	private void smoothTime$trackTimeJump(long dayTime, CallbackInfo callbackInfo) {
		SmoothTimeController.onDayTimeSet((ClientLevel) (Object) this, dayTime);
	}
}
