package dev.vercim.ticks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SkyTimeInterpolatorTest {
	private static final int TRANSITION_TIME_MILLIS = 200;

	@Test
	void firstFrameStartsAtTheAuthoritativeFractionalTime() {
		SkyTimeInterpolator interpolator = new SkyTimeInterpolator();
		Object level = new Object();

		interpolator.update(level, 1_234.5, 100L, TRANSITION_TIME_MILLIS);

		assertTrue(interpolator.isTracking(level));
		assertEquals(1_234.5, interpolator.getRenderedTime(), 1.0e-9);
	}

	@Test
	void ordinaryProgressUsesTheAuthoritativeTimeWithoutLag() {
		SkyTimeInterpolator interpolator = new SkyTimeInterpolator();
		Object level = new Object();
		interpolator.update(level, 1_000.0, 0L, TRANSITION_TIME_MILLIS);

		interpolator.update(level, 1_000.5, 25_000_000L, TRANSITION_TIME_MILLIS);

		assertEquals(1_000.5, interpolator.getRenderedTime(), 1.0e-9);
	}

	@Test
	void timeJumpStartsFromThePreviousVisualTimeAndDecaysTowardTheTarget() {
		SkyTimeInterpolator interpolator = new SkyTimeInterpolator();
		Object level = new Object();
		interpolator.update(level, 1_000.0, 0L, TRANSITION_TIME_MILLIS);
		interpolator.markTimeJump(level);

		interpolator.update(level, 7_000.0, 0L, TRANSITION_TIME_MILLIS);
		assertEquals(1_000.0, interpolator.getRenderedTime(), 1.0e-9);

		interpolator.update(level, 7_000.0, 200_000_000L, TRANSITION_TIME_MILLIS);
		assertEquals(7_000.0 - 6_000.0 / Math.E, interpolator.getRenderedTime(), 1.0e-9);
	}

	@Test
	void midnightJumpUsesTheShortRouteAcrossTheDayBoundary() {
		SkyTimeInterpolator interpolator = new SkyTimeInterpolator();
		Object level = new Object();
		interpolator.update(level, 23_999.0, 0L, TRANSITION_TIME_MILLIS);
		interpolator.markTimeJump(level);

		interpolator.update(level, 1.0, 0L, TRANSITION_TIME_MILLIS);

		assertEquals(-1.0, interpolator.getRenderedTime(), 1.0e-9);
	}

	@Test
	void changingLevelsDoesNotCarryThePreviousLevelsOffset() {
		SkyTimeInterpolator interpolator = new SkyTimeInterpolator();
		Object firstLevel = new Object();
		Object secondLevel = new Object();
		interpolator.update(firstLevel, 1_000.0, 0L, TRANSITION_TIME_MILLIS);
		interpolator.markTimeJump(firstLevel);
		interpolator.update(firstLevel, 7_000.0, 0L, TRANSITION_TIME_MILLIS);

		interpolator.update(secondLevel, 15_000.0, 1L, TRANSITION_TIME_MILLIS);

		assertFalse(interpolator.isTracking(firstLevel));
		assertTrue(interpolator.isTracking(secondLevel));
		assertEquals(15_000.0, interpolator.getRenderedTime(), 1.0e-9);
	}

	@Test
	void zeroTransitionTimeAppliesTimeJumpImmediately() {
		SkyTimeInterpolator interpolator = new SkyTimeInterpolator();
		Object level = new Object();
		interpolator.update(level, 1_000.0, 0L, TRANSITION_TIME_MILLIS);
		interpolator.markTimeJump(level);

		interpolator.update(level, 7_000.0, 0L, 0);

		assertEquals(7_000.0, interpolator.getRenderedTime(), 1.0e-9);
	}

	@Test
	void backwardsFrameClockDoesNotMoveTheTransitionBackward() {
		SkyTimeInterpolator interpolator = new SkyTimeInterpolator();
		Object level = new Object();
		interpolator.update(level, 1_000.0, 100L, TRANSITION_TIME_MILLIS);
		interpolator.markTimeJump(level);

		interpolator.update(level, 7_000.0, 50L, TRANSITION_TIME_MILLIS);

		assertEquals(1_000.0, interpolator.getRenderedTime(), 1.0e-9);
	}
}
