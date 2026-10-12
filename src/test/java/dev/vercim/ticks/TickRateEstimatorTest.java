package dev.vercim.ticks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TickRateEstimatorTest {
	private static final long SECOND = 1_000_000_000L;

	@Test
	void reportsNothingUntilTwoPacketsSpanEnoughTime() {
		TickRateEstimator estimator = new TickRateEstimator();
		assertTrue(Double.isNaN(estimator.getTicksPerSecond()));

		estimator.add(0L, 1_000L);
		assertTrue(Double.isNaN(estimator.getTicksPerSecond()));

		estimator.add(10_000_000L, 1_020L);
		assertTrue(Double.isNaN(estimator.getTicksPerSecond()));
	}

	@Test
	void measuresAServerRunningAtTheDefaultRate() {
		TickRateEstimator estimator = new TickRateEstimator();

		for (int packet = 0; packet < 6; packet++) {
			estimator.add(packet * SECOND, 1_000L + packet * 20L);
		}

		assertEquals(20.0, estimator.getTicksPerSecond(), 1.0e-9);
	}

	@Test
	void measuresARateTheNominalRateDoesNotReach() {
		TickRateEstimator estimator = new TickRateEstimator();

		for (int packet = 0; packet < 40; packet++) {
			estimator.add(Math.round(packet * 20 / 150.0 * SECOND), packet * 20L);
		}

		assertEquals(150.0, estimator.getTicksPerSecond(), 0.5);
	}

	@Test
	void forgetsAnOlderSpeedOnceItLeavesTheWindow() {
		TickRateEstimator estimator = new TickRateEstimator();
		long gameTime = 0L;
		long nanos = 0L;

		for (int packet = 0; packet <= 5; packet++) {
			estimator.add(nanos, gameTime);
			nanos += SECOND;
			gameTime += 20L;
		}
		nanos -= SECOND;
		gameTime -= 20L;
		for (int packet = 0; packet < 25; packet++) {
			nanos += SECOND / 5;
			gameTime += 20L;
			estimator.add(nanos, gameTime);
		}

		assertEquals(100.0, estimator.getTicksPerSecond(), 1.0e-9);
	}

	@Test
	void packetsProcessedTogetherAfterAStallAverageOverTheStall() {
		TickRateEstimator estimator = new TickRateEstimator();

		for (int packet = 0; packet <= 5; packet++) {
			estimator.add(packet * SECOND / 5, packet * 20L);
		}
		for (int packet = 6; packet <= 10; packet++) {
			estimator.add(2 * SECOND, packet * 20L);
		}

		assertEquals(100.0, estimator.getTicksPerSecond(), 1.0e-9);
	}

	@Test
	void packetsProcessedAtTheSameInstantAreNotAnInfiniteRate() {
		TickRateEstimator estimator = new TickRateEstimator();

		estimator.add(SECOND, 100L);
		estimator.add(SECOND, 120L);
		estimator.add(SECOND, 140L);

		assertTrue(Double.isNaN(estimator.getTicksPerSecond()));
	}

	@Test
	void keepsTwoPacketsOfAServerThatSendsOneEveryFewSeconds() {
		TickRateEstimator estimator = new TickRateEstimator();

		estimator.add(0L, 0L);
		estimator.add(10 * SECOND, 20L);
		estimator.add(20 * SECOND, 40L);

		assertEquals(2.0, estimator.getTicksPerSecond(), 1.0e-9);
	}

	@Test
	void aClockThatGoesBackwardsStartsOver() {
		TickRateEstimator estimator = new TickRateEstimator();
		estimator.add(0L, 0L);
		estimator.add(SECOND, 20L);

		estimator.add(2 * SECOND, 5L);
		assertTrue(Double.isNaN(estimator.getTicksPerSecond()));

		estimator.add(3 * SECOND, 45L);
		assertEquals(40.0, estimator.getTicksPerSecond(), 1.0e-9);
	}

	@Test
	void clearingForgetsEverything() {
		TickRateEstimator estimator = new TickRateEstimator();
		estimator.add(0L, 0L);
		estimator.add(SECOND, 20L);

		estimator.clear();

		assertTrue(Double.isNaN(estimator.getTicksPerSecond()));
	}
}
