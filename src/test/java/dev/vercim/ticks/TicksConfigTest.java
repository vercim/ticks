package dev.vercim.ticks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TicksConfigTest {
	@Test
	void transitionTimeIsClampedToSupportedRange() {
		TicksConfig.setTransitionTimeMillis(-1);
		assertEquals(TicksConfig.MIN_TRANSITION_TIME_MILLIS, TicksConfig.getTransitionTimeMillis());

		TicksConfig.setTransitionTimeMillis(2_001);
		assertEquals(TicksConfig.MAX_TRANSITION_TIME_MILLIS, TicksConfig.getTransitionTimeMillis());
	}

	@Test
	void firstInitializationWritesDefaultConfiguration(@TempDir Path configDirectory) throws Exception {
		TicksConfig.initialize(configDirectory);

		Path configFile = configDirectory.resolve("ticks.json");
		assertTrue(Files.isRegularFile(configFile));
		try (Reader reader = Files.newBufferedReader(configFile)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			assertEquals(TicksConfig.DEFAULT_TRANSITION_TIME_MILLIS, root.get("transitionTimeMillis").getAsInt());
		}
	}
}
