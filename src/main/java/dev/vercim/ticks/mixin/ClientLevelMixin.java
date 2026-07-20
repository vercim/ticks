package dev.vercim.ticks.mixin;

import dev.vercim.ticks.TicksController;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
abstract class ClientLevelMixin {
	@Inject(method = "setDayTime", at = @At("HEAD"))
	private void ticks$trackTimeJump(long dayTime, CallbackInfo callbackInfo) {
		TicksController.onDayTimeSet((ClientLevel) (Object) this, dayTime);
	}
}
