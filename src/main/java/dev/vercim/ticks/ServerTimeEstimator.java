package dev.vercim.ticks;

/**
 * Estimates the server's day time between its time packets, without Minecraft classes so it stays unit-testable.
 *
 * <p>The client's own tick counter cannot stand in for it: the client ticks at the nominal tick rate, while the
 * server may run slower (a lagging server, or {@code /tick rate} above what the machine can sustain). Instead the
 * estimate starts from the last packet and advances at the measured speed of the server's game clock. Every
 * packet re-anchors it, so a wrong speed never accumulates into drift.
 */
final class ServerTimeEstimator {
	private static final double DEFAULT_TICK_RATE = 20.0;
	/** Largest difference from the expected day time that still counts as ordinary progress. */
	private static final double CONTINUITY_TOLERANCE_TICKS = 0.5;
	/** Corrections below this many ticks, or this much motion, are applied at once instead of blended. */
	private static final double MIN_BLEND_TICKS = 1.0;
	private static final double MIN_BLEND_SECONDS = 0.05;
	private static final double NANOS_PER_SECOND = 1.0e9;

	private final TickRateEstimator tickRateEstimator = new TickRateEstimator();
	private boolean synchronizedWithServer;
	private long sampleGameTime;
	private double sampleDayTime;
	private double anchorDayTime;
	private long anchorNanos;
	private double tickRate = DEFAULT_TICK_RATE;
	private double nominalTickRate = Double.NaN;
	private double dayTimeRate = 1.0;
	private boolean advancing = true;

	boolean isSynchronized() {
		return synchronizedWithServer;
	}

	double getTickRate() {
		return tickRate;
	}

	/**
	 * Records the game time and day time of a server time packet.
	 *
	 * @return whether the packet moved the estimate enough (a time jump, or a large correction) that the rendered
	 *         time should blend toward it instead of following it at once
	 */
	boolean onTimeUpdate(long nowNanos, long gameTime, double dayTime) {
		if (!synchronizedWithServer || gameTime < sampleGameTime) {
			synchronizedWithServer = true;
			tickRateEstimator.clear();
			if (advancing) {
				tickRateEstimator.add(nowNanos, gameTime);
			}
			anchor(nowNanos, gameTime, dayTime);
			return true;
		}

		double expectedDayTime = sampleDayTime + (gameTime - sampleGameTime) * dayTimeRate;
		boolean continuous = Math.abs(dayTime - expectedDayTime) <= CONTINUITY_TOLERANCE_TICKS;
		double correction = Math.abs(dayTime - currentEstimate(nowNanos));
		double blendThreshold = Math.max(MIN_BLEND_TICKS, tickRate * dayTimeRate * MIN_BLEND_SECONDS);

		if (advancing) {
			tickRateEstimator.add(nowNanos, gameTime);
			double measured = tickRateEstimator.getTicksPerSecond();
			if (!Double.isNaN(measured)) {
				tickRate = measured;
			}
		}
		anchor(nowNanos, gameTime, dayTime);
		return !continuous || correction > blendThreshold;
	}

	/**
	 * Records a time packet that carries the game time only (the day time keeps running at its current rate).
	 *
	 * @return the same as {@link #onTimeUpdate}
	 */
	boolean onGameTimeUpdate(long nowNanos, long gameTime) {
		if (!synchronizedWithServer) {
			return false;
		}
		return onTimeUpdate(nowNanos, gameTime, sampleDayTime + (gameTime - sampleGameTime) * dayTimeRate);
	}

	/**
	 * Returns the estimated server day time for a frame.
	 *
	 * @param advancing whether time is running (false while paused or frozen); the estimate holds still otherwise
	 * @param nominalTickRate the tick rate the server is asked to run at
	 * @param dayTimeRate day-time ticks per game tick (1 normally, 0 when the day does not advance)
	 */
	double estimate(long nowNanos, boolean advancing, double nominalTickRate, double dayTimeRate) {
		boolean nominalChanged = nominalTickRate != this.nominalTickRate;
		if (nominalChanged || advancing != this.advancing || dayTimeRate != this.dayTimeRate) {
			// Whatever changes the speed applies from now on, so first fix the position reached so far.
			anchorDayTime = currentEstimate(nowNanos);
			anchorNanos = nowNanos;

			if (nominalChanged) {
				// A lowered limit applies at once. A raised one is not trusted until it has been measured: the
				// server may not be able to reach it, and overshooting by the whole nominal rate is far worse
				// than lagging.
				tickRate = Math.min(tickRate, nominalTickRate);
				this.nominalTickRate = nominalTickRate;
				tickRateEstimator.clear();
			}
			if (advancing != this.advancing) {
				this.advancing = advancing;
				tickRateEstimator.clear();
			}
			this.dayTimeRate = dayTimeRate;
		}
		return currentEstimate(nowNanos);
	}

	void reset() {
		tickRateEstimator.clear();
		synchronizedWithServer = false;
		sampleGameTime = 0L;
		sampleDayTime = 0.0;
		anchorDayTime = 0.0;
		anchorNanos = 0L;
		tickRate = DEFAULT_TICK_RATE;
		nominalTickRate = Double.NaN;
		dayTimeRate = 1.0;
		advancing = true;
	}

	private void anchor(long nowNanos, long gameTime, double dayTime) {
		sampleGameTime = gameTime;
		sampleDayTime = dayTime;
		anchorDayTime = dayTime;
		anchorNanos = nowNanos;
	}

	private double currentEstimate(long nowNanos) {
		if (!advancing) {
			return anchorDayTime;
		}
		long elapsedNanos = Math.max(0L, nowNanos - anchorNanos);
		return anchorDayTime + tickRate * dayTimeRate * elapsedNanos / NANOS_PER_SECOND;
	}
}
