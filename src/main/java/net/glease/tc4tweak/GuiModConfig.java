package net.glease.tc4tweak;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;

import java.util.ArrayList;
import java.util.List;

public class GuiModConfig extends GuiConfig {
	public GuiModConfig(GuiScreen guiScreen) {
		super(guiScreen, getConfigElements(), TC4Tweak.MOD_ID, false, false, GuiConfig.getAbridgedConfigPath(ConfigurationHandler.INSTANCE.getConfig().toString()));
	}

	@SuppressWarnings("rawtypes")
	private static List<IConfigElement> getConfigElements() {
		List<IConfigElement> elements = new ArrayList<>();
		final Configuration config = ConfigurationHandler.INSTANCE.getConfig();
		for (String name : config.getCategoryNames()) {
			elements.add(new ConfigElement(config.getCategory(name).setLanguageKey("tcscrolling.category." + name)));
		}
		return elements;
	}
}
