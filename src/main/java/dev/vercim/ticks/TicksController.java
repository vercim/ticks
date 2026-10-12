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
	private static Float renderedSkyAngle;
	private static final SkyTimeInterpolator INTERPOLATOR = new SkyTimeInterpolator();
	private static final ServerTimeEstimator SERVER_TIME = new ServerTimeEstimator();
	private static ClientLevel serverTimeLevel;
	//? >=1.21.4 {
	/*private static double dayTimeRate = 1.0;
	*///?}

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
		boolean paused = minecraft.isPaused();
		double target;
		if (serverTimeLevel == level && SERVER_TIME.isSynchronized()) {
			target = SERVER_TIME.estimate(now, !paused && runsNormally(level), getTickRate(level), getDayTimeRate(level));
		} else {
			target = getLocalTime(level, partialTick);
		}

		INTERPOLATOR.update(level, target, now, TicksConfig.getTransitionTimeMillis(), paused);
		trackedLevel = level;
		renderedSkyAngle = SkyTimeMath.skyAngle(INTERPOLATOR.getRenderedTime());
	}

	public static void endFrame() {
		rendering = false;
	}

	//? >=26 {
	/*public static void onClockUpdate(ClientLevel level, long gameTime, long newTime, float partialTick, float rate) {
		trackServerTime(level, gameTime, newTime + (double) partialTick);
		dayTimeRate = rate;
	}

	public static void onGameTimeUpdate(ClientLevel level, long gameTime) {
		if (serverTimeLevel == level && SERVER_TIME.onGameTimeUpdate(System.nanoTime(), gameTime)) {
			INTERPOLATOR.markTimeJump(level);
		}
	}
	*///?}
	//? >=1.21.4 {
	//? <26 {
	/*public static void onDayTimeSet(ClientLevel level, long gameTime, long newDayTime, boolean tickDayTime) {
		dayTimeRate = tickDayTime ? 1.0 : 0.0;
		trackServerTime(level, gameTime, newDayTime);
	}
	*///?}
	//?}
	//? <1.21.4 {
	public static void onDayTimeSet(ClientLevel level, long gameTime, long newDayTime) {
		// A negative time is how the server tells the client that the day does not advance.
		trackServerTime(level, gameTime, Math.abs(newDayTime));
	}
	//?}

	private static void trackServerTime(ClientLevel level, long gameTime, double dayTime) {
		if (serverTimeLevel != level) {
			SERVER_TIME.reset();
			serverTimeLevel = level;
		}
		if (SERVER_TIME.onTimeUpdate(System.nanoTime(), gameTime, dayTime)) {
			INTERPOLATOR.markTimeJump(level);
		}
	}

	public static Float getSkyAngleOverride(DimensionType dimensionType, long vanillaDayTime) {
		ClientLevel level = trackedLevel;
		if (!rendering || !INTERPOLATOR.isTracking(level) || level == null || level.dimensionType() != dimensionType
				|| getSkyTime(level) != vanillaDayTime || dimensionType.hasFixedTime()) {
			return null;
		}
		return renderedSkyAngle;
	}

	/** The time the client itself keeps, used until the first server time packet of a level has arrived. */
	private static double getLocalTime(ClientLevel level, float partialTick) {
		//? >=26 {
		/*return level.getOverworldClockTime();
		*///?}
		//? <26 {
		return level.getDayTime() + partialTick * getDayTimeRate(level);
		//?}
	}

	private static boolean runsNormally(ClientLevel level) {
		//? >=1.21.1 {
		return level.tickRateManager().runsNormally();
		//?}
		//? <1.21.1 {
		/*return true;
		*///?}
	}

	private static double getTickRate(ClientLevel level) {
		//? >=1.21.1 {
		return level.tickRateManager().tickrate();
		//?}
		//? <1.21.1 {
		/*return 20.0;
		*///?}
	}

	private static double getDayTimeRate(ClientLevel level) {
		//? >=1.21.4 {
		/*return dayTimeRate;
		*///?}
		//? <1.21.4 {
		return level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT) ? 1.0 : 0.0;
		//?}
	}

	private static long getSkyTime(ClientLevel level) {
		//? >=26 {
		/*return level.getOverworldClockTime();
		*///?}
		//? <26 {
		return level.getDayTime();
		//?}
	}

	private static void reset() {
		rendering = false;
		trackedLevel = null;
		serverTimeLevel = null;
		renderedSkyAngle = null;
		SERVER_TIME.reset();
		INTERPOLATOR.reset();
		//? >=1.21.4 {
		/*dayTimeRate = 1.0;
		*///?}
	}
}
