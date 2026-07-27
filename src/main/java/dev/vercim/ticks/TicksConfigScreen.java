package dev.vercim.ticks;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Creates the single-category Cloth Config screen used by every supported loader. */
public final class TicksConfigScreen {
	private TicksConfigScreen() {
	}

	public static Screen create(Screen parent) {
		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.translatable("title.ticks.config"));
		ConfigCategory category = builder.getOrCreateCategory(Component.translatable("category.ticks.general"));
		ConfigEntryBuilder entries = builder.entryBuilder();

		category.addEntry(entries.startBooleanToggle(Component.translatable("option.ticks.enabled"), TicksConfig.isEnabled())
				.setDefaultValue(true)
				.setSaveConsumer(TicksConfig::setEnabled)
				.build());
		category.addEntry(entries.startIntField(
						Component.translatable("option.ticks.transition_time"), TicksConfig.getTransitionTimeMillis())
				.setDefaultValue(TicksConfig.DEFAULT_TRANSITION_TIME_MILLIS)
				.setMin(TicksConfig.MIN_TRANSITION_TIME_MILLIS)
				.setMax(TicksConfig.MAX_TRANSITION_TIME_MILLIS)
				.setSaveConsumer(TicksConfig::setTransitionTimeMillis)
				.build());
		builder.setSavingRunnable(TicksConfig::save);
		return builder.build();
	}
}
