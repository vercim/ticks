package dev.vercim.ticks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
//? <1.21.4 {
/*
import net.minecraft.world.level.GameRules;
*///?}
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Owns the client-only visual time. The world continues to hold the authoritative
 * whole-tick time; only calls made while a frame is rendered see this value.
 */
public final class TicksController {
	private static ClientLevel trackedLevel;
	private static boolean rendering;
	private static boolean dayTimeAdvances = true;
	private static Float renderedSkyAngle;
	private static final SkyTimeInterpolator INTERPOLATOR = new SkyTimeInterpolator();

	private TicksController() {
	}

	public static void beginFrame(float partialTick) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		rendering = true;

		if (level == null) {
			reset();
			return;
		}

		long now = System.nanoTime();
		double target = level.getDayTime();
		//? >=1.21.4 {
		/*if (dayTimeAdvances) {
			target += partialTick;
		}
		*///?}
		//? <1.21.4 {
		if (level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
			target += partialTick;
		}
		//?}

		INTERPOLATOR.update(level, target, now, TicksConfig.getTransitionTimeMillis());
		trackedLevel = level;
		renderedSkyAngle = SkyTimeMath.skyAngle(INTERPOLATOR.getRenderedTime());
	}

	public static void endFrame() {
		rendering = false;
	}

	//? >=1.21.4 {
	/*public static void onDayTimeSet(ClientLevel level, long newDayTime, boolean tickDayTime) {
		dayTimeAdvances = tickDayTime;
		trackDayTimeSet(level, newDayTime);
	}
	*///?}
	//? <1.21.4 {
	public static void onDayTimeSet(ClientLevel level, long newDayTime) {
		trackDayTimeSet(level, newDayTime);
	}
	//?}

	private static void trackDayTimeSet(ClientLevel level, long newDayTime) {
		if (!INTERPOLATOR.isTracking(level)) {
			return;
		}

		long previousDayTime = level.getDayTime();
		long delta = newDayTime - previousDayTime;
		if (delta != 0L && delta != 1L) {
			INTERPOLATOR.markTimeJump(level);
		}
	}

	public static Float getSkyAngleOverride(DimensionType dimensionType, long vanillaDayTime) {
		ClientLevel level = trackedLevel;
		if (!rendering || !INTERPOLATOR.isTracking(level) || level == null || level.dimensionType() != dimensionType
				|| level.getDayTime() != vanillaDayTime || dimensionType.hasFixedTime()) {
			return null;
		}
		return renderedSkyAngle;
	}

	private static void reset() {
		rendering = false;
		trackedLevel = null;
		dayTimeAdvances = true;
		renderedSkyAngle = null;
		INTERPOLATOR.reset();
	}
}
