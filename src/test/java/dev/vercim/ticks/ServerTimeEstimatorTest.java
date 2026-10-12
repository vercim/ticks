package dev.vercim.ticks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Random;

import org.junit.jupiter.api.Test;

class ServerTimeEstimatorTest {
	private static final long SECOND = 1_000_000_000L;

	@Test
	void startsFromTheLowerOfTheDefaultAndTheNominalRate() {
		ServerTimeEstimator unlimited = new ServerTimeEstimator();
		unlimited.onTimeUpdate(0L, 1_000L, 6_000.0);
		unlimited.estimate(0L, true, 10_000.0, 1.0);
		assertEquals(6_020.0, unlimited.estimate(SECOND, true, 10_000.0, 1.0), 1.0e-9);

		ServerTimeEstimator slow = new ServerTimeEstimator();
		slow.onTimeUpdate(0L, 1_000L, 6_000.0);
		slow.estimate(0L, true, 5.0, 1.0);
		assertEquals(6_005.0, slow.estimate(SECOND, true, 5.0, 1.0), 1.0e-9);
	}

	@Test
	void gameTimeOnlyPacketsContinueTheDayTimeAtItsRate() {
		ServerTimeEstimator estimator = new ServerTimeEstimator();
		assertFalse(estimator.onGameTimeUpdate(0L, 1_000L));
		assertFalse(estimator.isSynchronized());

		estimator.onTimeUpdate(0L, 1_000L, 6_000.0);
		estimator.estimate(0L, true, 20.0, 0.5);
		assertFalse(estimator.onGameTimeUpdate(SECOND, 1_020L));

		assertEquals(6_010.0, estimator.estimate(SECOND, true, 20.0, 0.5), 1.0e-9);
	}

	@Test
	void aSmallerGameTimeIsANewWorld() {
		ServerTimeEstimator estimator = new ServerTimeEstimator();
		estimator.onTimeUpdate(0L, 50_000L, 6_000.0);
		estimator.estimate(0L, true, 20.0, 1.0);
		estimator.onTimeUpdate(SECOND, 50_020L, 6_020.0);

		assertTrue(estimator.onTimeUpdate(2 * SECOND, 10L, 18_000.0));

		assertEquals(18_000.0, estimator.estimate(2 * SECOND, true, 20.0, 1.0), 1.0e-9);
	}

	@Test
	void resetForgetsTheServer() {
		ServerTimeEstimator estimator = new ServerTimeEstimator();
		estimator.onTimeUpdate(0L, 1_000L, 6_000.0);

		estimator.reset();

		assertFalse(estimator.isSynchronized());
	}

	@Test
	void tracksAServerRunningAtTheDefaultRate() {
		Simulation simulation = new Simulation();

		simulation.run(10.0, 3.0);

		assertTrue(simulation.maxError < 1.5, "max error " + simulation.maxError);
		assertEquals(20.0, simulation.estimator.getTickRate(), 0.5);
	}

	@Test
	void ordinaryProgressNeverAsksForBlending() {
		Simulation simulation = new Simulation();
		simulation.run(5.0, 5.0);
		simulation.blends = 0;

		simulation.run(30.0, 0.0);

		assertEquals(0, simulation.blends);
	}

	@Test
	void followsAServerThatCannotReachItsNominalTickRate() {
		Simulation simulation = new Simulation();
		simulation.serverTickRate = 150.0;
		simulation.nominalTickRate = 10_000.0;

		simulation.run(10.0, 1.0);

		assertTrue(simulation.maxError < 8.0, "max error " + simulation.maxError);
		assertEquals(150.0, simulation.estimator.getTickRate(), 3.0);
	}

	@Test
	void doesNotOvershootWhenTheTickRateIsRaisedBeyondWhatTheServerDoes() {
		Simulation simulation = new Simulation();
		simulation.run(5.0, 5.0);

		simulation.serverTickRate = 150.0;
		simulation.nominalTickRate = 10_000.0;
		simulation.run(0.5, 0.0);
		assertTrue(simulation.maxError < 50.0, "max error " + simulation.maxError);

		simulation.run(3.0, 1.5);
		assertTrue(simulation.maxError < 8.0, "max error " + simulation.maxError);
	}

	@Test
	void slowsDownAtOnceWhenTheTickRateIsLowered() {
		Simulation simulation = new Simulation();
		simulation.run(5.0, 5.0);

		simulation.serverTickRate = 5.0;
		simulation.nominalTickRate = 5.0;
		simulation.run(2.0, 0.0);

		assertTrue(simulation.maxError < 3.0, "max error " + simulation.maxError);
	}

	@Test
	void followsALaggingServer() {
		Simulation simulation = new Simulation();
		simulation.run(5.0, 5.0);

		simulation.serverTickRate = 8.0;
		simulation.run(12.0, 8.0);

		assertTrue(simulation.maxError < 2.0, "max error " + simulation.maxError);
		assertEquals(8.0, simulation.estimator.getTickRate(), 0.5);
	}

	@Test
	void aLagSpikeIsBlendedRatherThanSnappedAndSettles() {
		Simulation simulation = new Simulation();
		simulation.run(5.0, 5.0);
		simulation.blends = 0;

		simulation.serverRunning = false;
		simulation.run(3.0, 0.0);
		simulation.serverRunning = true;
		simulation.run(2.0, 0.0);
		assertTrue(simulation.blends >= 1, "blends " + simulation.blends);

		simulation.run(6.0, 3.0);
		assertTrue(simulation.maxError < 1.5, "max error " + simulation.maxError);
		assertEquals(20.0, simulation.estimator.getTickRate(), 0.5);
	}

	@Test
	void packetsProcessedTogetherAfterAClientStallDoNotPoisonTheSpeed() {
		Simulation simulation = new Simulation();
		simulation.serverTickRate = 150.0;
		simulation.nominalTickRate = 150.0;
		simulation.run(5.0, 5.0);

		simulation.stallClient(1.0);
		simulation.run(0.2, 0.0);

		assertEquals(150.0, simulation.estimator.getTickRate(), 15.0);
	}

	@Test
	void holdsStillWhileTimeIsNotAdvancingAndResumesWithoutDrift() {
		Simulation simulation = new Simulation();
		simulation.run(5.0, 5.0);

		simulation.advancing = false;
		simulation.serverRunning = false;
		simulation.run(10.0, 0.0);
		assertTrue(simulation.maxError < 1.5, "max error while frozen " + simulation.maxError);

		simulation.advancing = true;
		simulation.serverRunning = true;
		simulation.maxError = 0.0;
		simulation.run(3.0, 0.0);
		assertTrue(simulation.maxError < 3.0, "max error after resuming " + simulation.maxError);
		assertEquals(20.0, simulation.estimator.getTickRate(), 1.0);
	}

	@Test
	void aDayThatDoesNotAdvanceKeepsTheMeasuredTickRate() {
		Simulation simulation = new Simulation();
		simulation.run(5.0, 5.0);

		simulation.dayTimeRate = 0.0;
		simulation.serverDayTimeRate = 0.0;
		simulation.run(5.0, 0.0);
		assertTrue(simulation.maxError < 1.5, "max error " + simulation.maxError);
		assertEquals(20.0, simulation.estimator.getTickRate(), 0.5);

		simulation.dayTimeRate = 1.0;
		simulation.serverDayTimeRate = 1.0;
		simulation.maxError = 0.0;
		simulation.run(1.0, 0.0);
		assertTrue(simulation.maxError < 1.5, "max error after resuming " + simulation.maxError);
	}

	@Test
	void timeJumpIsReportedAndFollowedWithoutDisturbingTheSpeed() {
		Simulation simulation = new Simulation();
		simulation.run(5.0, 5.0);
		simulation.blends = 0;

		simulation.dayTime += 6_000.0;
		simulation.run(3.0, 2.0);

		assertEquals(1, simulation.blends);
		assertTrue(simulation.maxError < 1.5, "max error " + simulation.maxError);
		assertEquals(20.0, simulation.estimator.getTickRate(), 0.5);
	}

	/** A server that sends its game time and day time every 20 of its ticks, observed by a client at 60 FPS. */
	private static final class Simulation {
		private static final long STEP_NANOS = 1_000_000L;
		private static final long FRAME_NANOS = 16_666_667L;
		private static final double MAX_JITTER_NANOS = 10_000_000.0;

		final ServerTimeEstimator estimator = new ServerTimeEstimator();
		final Random random = new Random(7L);
		final ArrayDeque<Packet> inFlight = new ArrayDeque<>();

		double serverTickRate = 20.0;
		double nominalTickRate = 20.0;
		double serverDayTimeRate = 1.0;
		double dayTimeRate = 1.0;
		boolean serverRunning = true;
		boolean advancing = true;
		double dayTime = 6_000.0;
		int blends;
		double maxError;

		private long now;
		private long nextFrame = FRAME_NANOS;
		private long lastArrival;
		private double gameTime = 1_000.0;
		private long lastPacketTick = 1_000L;
		private boolean clientStalled;
		private double measureFromSeconds;
		private double elapsedSeconds;

		/** Runs for a while, tracking the largest error of the frames after {@code measureAfterSeconds}. */
		void run(double seconds, double measureAfterSeconds) {
			maxError = 0.0;
			measureFromSeconds = measureAfterSeconds;
			elapsedSeconds = 0.0;
			long end = now + Math.round(seconds * SECOND);
			while (now < end) {
				now += STEP_NANOS;
				elapsedSeconds += STEP_NANOS / (double) SECOND;
				stepServer();
				if (now >= nextFrame) {
					nextFrame += FRAME_NANOS;
					if (!clientStalled) {
						frame();
					}
				}
			}
		}

		/** The client processes nothing for a while; its packets queue up and are handled in one frame. */
		void stallClient(double seconds) {
			clientStalled = true;
			run(seconds, Double.MAX_VALUE);
			clientStalled = false;
		}

		private void stepServer() {
			if (!serverRunning) {
				return;
			}

			double ticks = serverTickRate * STEP_NANOS / SECOND;
			gameTime += ticks;
			dayTime += ticks * serverDayTimeRate;
			while (gameTime >= lastPacketTick + 20) {
				lastPacketTick += 20;
				double dayTimeAtPacket = dayTime - (gameTime - lastPacketTick) * serverDayTimeRate;
				lastArrival = Math.max(lastArrival, now + (long) (random.nextDouble() * MAX_JITTER_NANOS));
				inFlight.add(new Packet(lastArrival, lastPacketTick, dayTimeAtPacket));
			}
		}

		private void frame() {
			while (!inFlight.isEmpty() && inFlight.peek().arrival <= now) {
				Packet packet = inFlight.poll();
				if (estimator.onTimeUpdate(now, packet.gameTime, packet.dayTime)) {
					blends++;
				}
			}

			double estimate = estimator.estimate(now, advancing, nominalTickRate, dayTimeRate);
			if (estimator.isSynchronized() && elapsedSeconds >= measureFromSeconds) {
				maxError = Math.max(maxError, Math.abs(estimate - dayTime));
			}
		}

		private record Packet(long arrival, long gameTime, double dayTime) {
		}
	}
}
