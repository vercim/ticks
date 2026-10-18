package dev.vercim.ticks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Scripted time-jump test for the dev client (never part of the mod: tools/selftest/run-selftest.ps1 copies it into
 * src/ for a run and removes it again). After the world has loaded it sets the time with server commands, records the
 * visual sky time of every frame, judges the recording and ends the game. Results: ticks-selftest.txt (verdicts) and
 * ticks-selftest.csv (frames) in the game directory.
 */
final class DevSelfTest {
	private static final boolean ENABLED = System.getProperty("ticks.selftest") != null;
	private static final String[] COMMANDS = {
		"gamerule doDaylightCycle true", "gamerule advance_time true",
		"time set 0", "time set 12000", "time add 6000", "time set 23000", "time set 1000", "time set 6000", "time set 18000"
	};
	private static final double SETTLE_SECONDS = 8.0;
	private static final double STEP_SECONDS = 3.0;
	private static final double DAY = SkyTimeMath.DAY_LENGTH;

	private static final List<double[]> FRAMES = new ArrayList<>();
	private static final List<Integer> EVENT_FRAMES = new ArrayList<>();
	private static final List<String> EVENT_NAMES = new ArrayList<>();
	private static long startNanos;
	private static int nextCommand;
	private static boolean finished;

	private DevSelfTest() {
	}

	static void onFrame(ClientLevel level, float partialTick) {
		if (!ENABLED || finished) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		MinecraftServer server = minecraft.getSingleplayerServer();
		if (minecraft.player == null || server == null) {
			return;
		}
		long now = System.nanoTime();
		if (startNanos == 0) {
			startNanos = now;
			System.out.println("[ticks-selftest] started");
		}
		double seconds = (now - startNanos) / 1.0e9;
		if (seconds >= SETTLE_SECONDS + nextCommand * STEP_SECONDS) {
			if (nextCommand >= COMMANDS.length) {
				finish(minecraft);
				return;
			}
			String command = COMMANDS[nextCommand++];
			EVENT_FRAMES.add(FRAMES.size());
			EVENT_NAMES.add(command);
			server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
		}
		Float rendered = readRendered();
		if (rendered == null) {
			return;
		}
		double override = Double.NaN;
		try {
			Method getTimeOfDay = level.getClass().getMethod("getTimeOfDay", float.class);
			override = invert((Float) getTimeOfDay.invoke(level, partialTick));
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			// 1.21.11 and newer read the angle from the render state, not from DimensionType.timeOfDay.
		}
		FRAMES.add(new double[] {seconds, dayTime(level) % DAY, invert(rendered), override});
	}

	private static Float readRendered() {
		try {
			Field field = TicksController.class.getDeclaredField("renderedSkyAngle");
			field.setAccessible(true);
			return (Float) field.get(null);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}

	private static double dayTime(ClientLevel level) {
		for (String name : new String[] {"getOverworldClockTime", "getDayTime"}) {
			try {
				return ((Number) level.getClass().getMethod(name).invoke(level)).doubleValue();
			} catch (ReflectiveOperationException ignored) {
				// try the next name
			}
		}
		throw new IllegalStateException("no day time accessor");
	}

	/** Inverse of {@link SkyTimeMath#skyAngle}: the day time in ticks that gives the angle. */
	private static double invert(double angle) {
		double low = 0.0;
		double high = 1.0;
		for (int i = 0; i < 60; i++) {
			double mid = (low + high) / 2.0;
			if ((mid * 2.0 + 0.5 - Math.cos(mid * Math.PI) / 2.0) / 3.0 < angle) {
				low = mid;
			} else {
				high = mid;
			}
		}
		return SkyTimeMath.positiveModulo((low + high) / 2.0 + 0.25, 1.0) * DAY;
	}

	private static void finish(Minecraft minecraft) {
		finished = true;
		List<String> report = new ArrayList<>();
		boolean ok = judge(report);
		report.add("RESULT " + (ok ? "PASS" : "FAIL"));
		try {
			List<String> csv = new ArrayList<>();
			csv.add("seconds,dayTime,renderedTime,overrideTime,event");
			int event = 0;
			for (int i = 0; i < FRAMES.size(); i++) {
				String name = "";
				while (event < EVENT_FRAMES.size() && EVENT_FRAMES.get(event) <= i) {
					name = EVENT_NAMES.get(event++);
				}
				double[] f = FRAMES.get(i);
				csv.add(String.format(Locale.ROOT, "%.4f,%.2f,%.3f,%.3f,%s", f[0], f[1], f[2], f[3], name));
			}
			Files.write(Path.of("ticks-selftest.csv"), csv);
			Files.write(Path.of("ticks-selftest.txt"), report);
		} catch (IOException exception) {
			exception.printStackTrace();
		}
		report.forEach(line -> System.out.println("[ticks-selftest] " + line));
		minecraft.stop();
	}

	private static boolean judge(List<String> report) {
		boolean ok = true;
		int first = EVENT_FRAMES.isEmpty() ? 0 : EVENT_FRAMES.get(0);
		if (FRAMES.size() < 200) {
			report.add("FAIL frames: only " + FRAMES.size() + " frames recorded");
			return false;
		}
		report.add("PASS frames: " + FRAMES.size() + " frames in " + String.format(Locale.ROOT, "%.0f", FRAMES.get(FRAMES.size() - 1)[0]) + " s");

		// Steady state: the visual time must advance every frame, not once per tick.
		int moving = 0;
		int counted = 0;
		int backwards = 0;
		double worstBack = 0.0;
		int overrideMatches = 0;
		int overrideCounted = 0;
		for (int i = 1; i < first; i++) {
			double[] a = FRAMES.get(i - 1);
			double[] b = FRAMES.get(i);
			if (b[0] < 3.0 || b[0] - a[0] > 0.05) {
				continue;
			}
			double step = SkyTimeMath.shortestDifference(a[2], b[2]);
			counted++;
			if (Math.abs(step) > 1.0e-4) {
				moving++;
			}
			if (step < -1.0e-3) {
				backwards++;
				worstBack = Math.min(worstBack, step);
			}
			if (!Double.isNaN(b[3])) {
				overrideCounted++;
				if (Math.abs(SkyTimeMath.shortestDifference(b[2], b[3])) < 0.05) {
					overrideMatches++;
				}
			}
		}
		double movingShare = counted == 0 ? 0.0 : moving / (double) counted;
		boolean smooth = movingShare >= 0.9 && worstBack > -3.0;
		ok &= smooth;
		report.add((smooth ? "PASS" : "FAIL") + String.format(Locale.ROOT,
			" steady: sky moves in %.0f%% of %d frames, %d tiny backward steps (worst %.2f ticks)",
			movingShare * 100.0, counted, backwards, worstBack));
		if (overrideCounted > 0) {
			boolean hook = overrideMatches >= overrideCounted * 0.95;
			ok &= hook;
			report.add((hook ? "PASS" : "FAIL") + " dimension-hook: DimensionType.timeOfDay returns the visual time in "
				+ overrideMatches + " of " + overrideCounted + " frames");
		} else {
			report.add("SKIP dimension-hook: this version has no DimensionType.timeOfDay path");
		}

		// Every jump: blended along the shortest route, never snapped, settled in time.
		for (int k = 0; k < EVENT_FRAMES.size(); k++) {
			String name = EVENT_NAMES.get(k);
			if (!name.startsWith("time")) {
				continue;
			}
			int from = EVENT_FRAMES.get(k);
			int to = k + 1 < EVENT_FRAMES.size() ? EVENT_FRAMES.get(k + 1) : FRAMES.size();
			int onset = -1;
			for (int i = Math.max(from, 1); i < to; i++) {
				if (Math.abs(SkyTimeMath.shortestDifference(FRAMES.get(i - 1)[1], FRAMES.get(i)[1])) > 100.0) {
					onset = i;
					break;
				}
			}
			if (onset < 0) {
				ok = false;
				report.add("FAIL " + name + ": the client never saw the time change");
				continue;
			}
			double jump = SkyTimeMath.shortestDifference(FRAMES.get(onset - 1)[2], FRAMES.get(onset)[1]);
			double t0 = FRAMES.get(onset)[0];
			double maxStep = 0.0;
			double wrongWay = 0.0;
			double settled = -1.0;
			double remaining100 = Double.NaN;
			for (int i = onset; i < to && FRAMES.get(i)[0] - t0 <= 3.0; i++) {
				double step = SkyTimeMath.shortestDifference(FRAMES.get(i - 1)[2], FRAMES.get(i)[2]);
				maxStep = Math.max(maxStep, Math.abs(step));
				// The part of the step that is not the world's own 20 ticks per second must go towards the new time.
				double blendStep = step - 20.0 * (FRAMES.get(i)[0] - FRAMES.get(i - 1)[0]);
				wrongWay = Math.min(wrongWay, blendStep * Math.signum(jump));
				double error = Math.abs(SkyTimeMath.shortestDifference(FRAMES.get(i)[2], FRAMES.get(i)[1]));
				if (settled < 0.0 && error < 10.0) {
					settled = FRAMES.get(i)[0] - t0;
				}
				if (Double.isNaN(remaining100) && FRAMES.get(i)[0] - t0 >= 0.1) {
					remaining100 = error / Math.abs(jump);
				}
			}
			boolean small = Math.abs(jump) < 500.0;
			boolean blended = small || (maxStep <= 0.35 * Math.abs(jump) && remaining100 > 0.2 && remaining100 < 0.9);
			boolean direction = wrongWay > -3.0;
			boolean done = settled >= 0.0 && settled <= 2.5;
			boolean pass = blended && direction && done;
			ok &= pass;
			report.add((pass ? "PASS" : "FAIL") + String.format(Locale.ROOT,
				" %s: jump %+.0f ticks, biggest frame step %.0f%%, %.0f%% left after 100 ms, wrong-way %.1f ticks, settled after %.2f s",
				name, jump, maxStep / Math.abs(jump) * 100.0, remaining100 * 100.0, wrongWay, settled));
		}
		return ok;
	}
}
