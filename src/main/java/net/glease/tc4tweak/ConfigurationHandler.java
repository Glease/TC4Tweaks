package net.glease.tc4tweak;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.config.Configuration;

import java.io.File;

public enum ConfigurationHandler {
	INSTANCE;

	private Configuration config;
	private boolean inverted;
	private int updateInterval;
	private boolean checkWorkbenchRecipes;

	ConfigurationHandler() {
		FMLCommonHandler.instance().bus().register(this);
	}

	void init(File f) {
		config = new Configuration(f);
		loadConfig();
	}

	@SubscribeEvent
	public void onConfigChange(ConfigChangedEvent.PostConfigChangedEvent e) {
		if (e.modID.equals(TC4Tweak.MOD_ID)) {
			loadConfig();
		}
	}

	private void loadConfig() {
		inverted = config.getBoolean("inverted", "general", false, "Flip it if you find the scrolling unintuitive");
		updateInterval = config.getInt("updateInterval", "general", 4, 0, 40, "How often should Arcane Workbench update displayed crafting result. Unit is in game ticks.");
		checkWorkbenchRecipes = config.getBoolean("checkWorkbenchRecipes", "general", true, "When false, Arcane Workbench will not perform vanilla crafting bench recipes. Arcane Workbench GUI will behave slightly awkwardly if the client has it false but not on server, but nothing would be broken.");
		// if allow checking (vanilla behavior) no need to force client to have this mod
		TC4Tweak.INSTANCE.setAllowAll(!checkWorkbenchRecipes);
		config.save();
	}

	public boolean isInverted() {
		return inverted;
	}

	public int getUpdateInterval() {
		return updateInterval;
	}

	public Configuration getConfig() {
		return config;
	}

	public boolean isCheckWorkbenchRecipes() {
		return checkWorkbenchRecipes;
	}
}
