package dev.vercim.ticks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Owns the client-only visual time. The world continues to hold the authoritative
 * whole-tick time; only calls made while a frame is rendered see this value.
 */
public final class TicksController {
	private static final double TRANSITION_TIME_CONSTANT_SECONDS = 0.20;

	private static ClientLevel trackedLevel;
	private static boolean rendering;
	private static boolean initialized;
	private static boolean pendingJump;
	private static double renderedTime;
	private static double offset;
	private static long lastFrameNanos;

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
		if (level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
			target += partialTick;
		}

		if (!initialized || trackedLevel != level) {
			trackedLevel = level;
			initialized = true;
			pendingJump = false;
			offset = 0.0;
			renderedTime = target;
			lastFrameNanos = now;
			return;
		}

		if (pendingJump) {
			offset = SkyTimeMath.shortestDifference(target, renderedTime);
			pendingJump = false;
		}

		double elapsedSeconds = Math.max(0.0, (now - lastFrameNanos) / 1_000_000_000.0);
		lastFrameNanos = now;
		offset *= Math.exp(-elapsedSeconds / TRANSITION_TIME_CONSTANT_SECONDS);
		renderedTime = target + offset;
	}

	public static void endFrame() {
		rendering = false;
	}

	public static void onDayTimeSet(ClientLevel level, long newDayTime) {
		if (!initialized || trackedLevel != level) {
			return;
		}

		long previousDayTime = level.getDayTime();
		long delta = newDayTime - previousDayTime;
		if (delta != 0L && delta != 1L) {
			pendingJump = true;
		}
	}

	public static Float getSkyAngleOverride(DimensionType dimensionType, long vanillaDayTime) {
		ClientLevel level = trackedLevel;
		if (!rendering || !initialized || level == null || level.dimensionType() != dimensionType
				|| level.getDayTime() != vanillaDayTime || dimensionType.hasFixedTime()) {
			return null;
		}
		return SkyTimeMath.skyAngle(renderedTime);
	}

	private static void reset() {
		trackedLevel = null;
		initialized = false;
		pendingJump = false;
		offset = 0.0;
		lastFrameNanos = 0L;
	}
}
