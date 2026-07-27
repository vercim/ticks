package dev.vercim.ticks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TicksConfigTest {
	@AfterEach
	void restoreDefaults() {
		TicksConfig.setEnabled(true);
		TicksConfig.setTransitionTimeMillis(TicksConfig.DEFAULT_TRANSITION_TIME_MILLIS);
	}

	@Test
	void transitionTimeIsClampedToSupportedRange() {
		TicksConfig.setTransitionTimeMillis(-1);
		assertEquals(TicksConfig.MIN_TRANSITION_TIME_MILLIS, TicksConfig.getTransitionTimeMillis());

		TicksConfig.setTransitionTimeMillis(2_001);
		assertEquals(TicksConfig.MAX_TRANSITION_TIME_MILLIS, TicksConfig.getTransitionTimeMillis());
	}

	@Test
	void enabledFlagCanBeChangedWithoutTouchingSimulation() {
		TicksConfig.setEnabled(false);
		assertFalse(TicksConfig.isEnabled());

		TicksConfig.setEnabled(true);
		assertTrue(TicksConfig.isEnabled());
	}
}
