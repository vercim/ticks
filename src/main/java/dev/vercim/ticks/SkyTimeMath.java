package dev.vercim.ticks;

/** Pure math used to keep the visual sky clock continuous around midnight. */
public final class SkyTimeMath {
	public static final double DAY_LENGTH = 24_000.0;

	private SkyTimeMath() {
	}

	/** Returns the shortest signed difference from {@code from} to {@code to}. */
	public static double shortestDifference(double from, double to) {
		double difference = positiveModulo(to - from + DAY_LENGTH / 2.0, DAY_LENGTH) - DAY_LENGTH / 2.0;
		return difference == -DAY_LENGTH / 2.0 ? DAY_LENGTH / 2.0 : difference;
	}

	public static double positiveModulo(double value, double modulus) {
		double result = value % modulus;
		return result < 0.0 ? result + modulus : result;
	}

	/** Matches Minecraft's non-linear celestial-angle curve, but accepts fractional ticks. */
	public static float skyAngle(double dayTime) {
		double fraction = positiveModulo(dayTime / DAY_LENGTH - 0.25, 1.0);
		double eased = 0.5 - Math.cos(fraction * Math.PI) / 2.0;
		return (float) ((fraction * 2.0 + eased) / 3.0);
	}
}
