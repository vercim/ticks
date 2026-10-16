package dev.vercim.ticks.mixin;

import dev.vercim.ticks.TicksController;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
//? if <1.21.4 {

/*import org.spongepowered.asm.mixin.Unique;
*///?}
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
abstract class ClientLevelMixin {
	//? if <26 {
	/*//? if >=1.21.4 {
	@Inject(method = "setTimeFromServer", at = @At("HEAD"))
	private void ticks$trackTimeUpdate(long gameTime, long dayTime, boolean tickDayTime, CallbackInfo callbackInfo) {
		TicksController.onDayTimeSet((ClientLevel) (Object) this, gameTime, dayTime, tickDayTime);
	}
	//?}
	//? if <1.21.4 {
	/^// The client advances its own game and day time every tick through the same setters the server packet uses.
	@Unique
	private boolean ticks$localTick;

	@Inject(method = "tickTime", at = @At("HEAD"))
	private void ticks$beginLocalTick(CallbackInfo callbackInfo) {
		ticks$localTick = true;
	}

	@Inject(method = "tickTime", at = @At("RETURN"))
	private void ticks$endLocalTick(CallbackInfo callbackInfo) {
		ticks$localTick = false;
	}

	@Inject(method = "setDayTime", at = @At("HEAD"))
	private void ticks$trackTimeUpdate(long dayTime, CallbackInfo callbackInfo) {
		ClientLevel level = (ClientLevel) (Object) this;
		if (!ticks$localTick) {
			TicksController.onDayTimeSet(level, level.getGameTime(), dayTime);
		}
	}
	^///?}
	*///?}
}
