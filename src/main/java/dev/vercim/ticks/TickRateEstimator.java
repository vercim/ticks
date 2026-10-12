package dev.vercim.ticks;

/**
 * Measures how many game ticks the server really runs per second, from the game times of its time packets.
 * The nominal tick rate is only an upper bound: a lagging server, or {@code /tick rate} above what the machine
 * can sustain, runs slower than it.
 */
final class TickRateEstimator {
	private static final long WINDOW_NANOS = 2_000_000_000L;
	private static final long MIN_SPAN_NANOS = 50_000_000L;
	private static final int CAPACITY = 512;

	private final long[] times = new long[CAPACITY];
	private final long[] gameTimes = new long[CAPACITY];
	private int oldest;
	private int size;

	void add(long nowNanos, long gameTime) {
		if (size > 0) {
			int newest = index(size - 1);
			if (nowNanos < times[newest] || gameTime < gameTimes[newest]) {
				clear();
			}
		}
		if (size == CAPACITY) {
			dropOldest();
		}

		int slot = index(size);
		times[slot] = nowNanos;
		gameTimes[slot] = gameTime;
		size++;

		// Always keep two samples so a slow server (one packet every few seconds) still has a baseline.
		while (size > 2 && nowNanos - times[oldest] > WINDOW_NANOS) {
			dropOldest();
		}
	}

	/**
	 * Returns the measured ticks per second, or NaN while the history is too short to tell. The slope is taken
	 * over the whole window rather than between neighbouring packets, so packets that were processed together
	 * after a stall cannot produce an absurd rate.
	 */
	double getTicksPerSecond() {
		if (size < 2) {
			return Double.NaN;
		}

		int newest = index(size - 1);
		long spanNanos = times[newest] - times[oldest];
		if (spanNanos < MIN_SPAN_NANOS) {
			return Double.NaN;
		}
		return (gameTimes[newest] - gameTimes[oldest]) * 1.0e9 / spanNanos;
	}

	void clear() {
		oldest = 0;
		size = 0;
	}

	private int index(int offset) {
		return (oldest + offset) % CAPACITY;
	}

	private void dropOldest() {
		oldest = (oldest + 1) % CAPACITY;
		size--;
	}
}
