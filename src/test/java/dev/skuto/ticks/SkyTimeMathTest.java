package dev.skuto.ticks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SkyTimeMathTest {
	@Test
	void midnightUsesTheShortForwardRoute() {
		assertEquals(2.0, SkyTimeMath.shortestDifference(23_999.0, 1.0), 1.0e-9);
		assertEquals(-2.0, SkyTimeMath.shortestDifference(1.0, 23_999.0), 1.0e-9);
	}

	@Test
	void oppositeSidesChooseOneDeterministicHalfDayRoute() {
		assertEquals(12_000.0, SkyTimeMath.shortestDifference(0.0, 12_000.0), 1.0e-9);
	}

	@Test
	void celestialCurveIsCyclicForFractionalTimes() {
		assertEquals(SkyTimeMath.skyAngle(1234.5), SkyTimeMath.skyAngle(25_234.5), 1.0e-6);
	}

	@Test
	void exponentialDecayIsFrameRateIndependent() {
		double sixtyFrames = decayOverOneSecond(60);
		double oneHundredFortyFourFrames = decayOverOneSecond(144);
		assertEquals(sixtyFrames, oneHundredFortyFourFrames, 1.0e-12);
		assertTrue(sixtyFrames < 0.01);
	}

	private static double decayOverOneSecond(int frames) {
		double value = 1.0;
		for (int frame = 0; frame < frames; frame++) {
			value *= Math.exp(-(1.0 / frames) / 0.20);
		}
		return value;
	}
}
