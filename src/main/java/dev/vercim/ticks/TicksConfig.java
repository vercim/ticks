package dev.vercim.ticks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Stores the client-only visual-time preferences. */
public final class TicksConfig {
	public static final int DEFAULT_TRANSITION_TIME_MILLIS = 200;
	public static final int MIN_TRANSITION_TIME_MILLIS = 0;
	public static final int MAX_TRANSITION_TIME_MILLIS = 2_000;

	private static final Logger LOGGER = Logger.getLogger("ticks");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static Path file;
	private static int transitionTimeMillis = DEFAULT_TRANSITION_TIME_MILLIS;

	private TicksConfig() {
	}

	public static synchronized void initialize(Path configDirectory) {
		file = configDirectory.resolve("ticks.json");
		load();
		if (!Files.isRegularFile(file)) {
			save();
		}
	}

	public static int getTransitionTimeMillis() {
		return transitionTimeMillis;
	}

	public static void setTransitionTimeMillis(int value) {
		transitionTimeMillis = Math.max(MIN_TRANSITION_TIME_MILLIS, Math.min(MAX_TRANSITION_TIME_MILLIS, value));
	}

	public static synchronized void save() {
		if (file == null) {
			return;
		}

		JsonObject root = new JsonObject();
		root.addProperty("transitionTimeMillis", transitionTimeMillis);

		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file)) {
				GSON.toJson(root, writer);
			}
		} catch (IOException exception) {
			LOGGER.log(Level.WARNING, "Could not save Ticks configuration to " + file, exception);
		}
	}

	private static void load() {
		transitionTimeMillis = DEFAULT_TRANSITION_TIME_MILLIS;
		if (!Files.isRegularFile(file)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(file)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			if (root.has("transitionTimeMillis") && root.get("transitionTimeMillis").isJsonPrimitive()) {
				setTransitionTimeMillis(root.get("transitionTimeMillis").getAsInt());
			}
		} catch (IOException | RuntimeException exception) {
			LOGGER.log(Level.WARNING, "Could not read Ticks configuration from " + file + "; using defaults", exception);
		}
	}
}
