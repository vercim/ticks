package dev.vercim.ticks;

/** Maintains the rendered visual time independently of Minecraft client classes. */
final class SkyTimeInterpolator {
	private Object trackedLevel;
	private boolean initialized;
	private boolean pendingJump;
	private double renderedTime;
	private double offset;
	private long lastFrameNanos;

	void update(Object level, double target, long nowNanos, int transitionTimeMillis, boolean paused) {
		if (!initialized || trackedLevel != level) {
			trackedLevel = level;
			initialized = true;
			pendingJump = false;
			offset = 0.0;
			renderedTime = target;
			lastFrameNanos = nowNanos;
			return;
		}

		if (pendingJump) {
			offset = SkyTimeMath.shortestDifference(target, renderedTime);
			pendingJump = false;
		}

		double elapsedSeconds = paused
				? 0.0
				: Math.max(0.0, (nowNanos - lastFrameNanos) / 1_000_000_000.0);
		lastFrameNanos = nowNanos;
		double transitionTimeSeconds = transitionTimeMillis / 1_000.0;
		if (transitionTimeSeconds == 0.0) {
			offset = 0.0;
		} else {
			offset *= Math.exp(-elapsedSeconds / transitionTimeSeconds);
		}
		renderedTime = target + offset;
	}

	boolean isTracking(Object level) {
		return initialized && trackedLevel == level;
	}

	void markTimeJump(Object level) {
		if (isTracking(level)) {
			pendingJump = true;
		}
	}

	double getRenderedTime() {
		return renderedTime;
	}

	void reset() {
		trackedLevel = null;
		initialized = false;
		pendingJump = false;
		renderedTime = 0.0;
		offset = 0.0;
		lastFrameNanos = 0L;
	}
}
