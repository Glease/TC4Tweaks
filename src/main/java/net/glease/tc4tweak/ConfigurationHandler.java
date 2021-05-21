package net.glease.tc4tweak;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.glease.tc4tweak.modules.FlushableCache;
import net.minecraftforge.common.config.Configuration;

import java.io.File;

public enum ConfigurationHandler {
	INSTANCE;

	private Configuration config;
	private boolean inverted;
	private int updateInterval;
	private boolean checkWorkbenchRecipes;
	private int arcaneCraftingHistorySize;
	private boolean addTooltip;
	private float browserScale;
	private boolean limitBookSearchToCategory;

	private int browserHeight = 230;
	private int browserWidth = 256;

	ConfigurationHandler() {
		FMLCommonHandler.instance().bus().register(this);
	}

	void init(File f) {
		config = new Configuration(f);
		loadConfig(false);
	}

	@SubscribeEvent
	public void onConfigChange(ConfigChangedEvent.PostConfigChangedEvent e) {
		if (e.modID.equals(TC4Tweak.MOD_ID)) {
			loadConfig(true);
			FlushableCache.enableAll(false);
		}
	}

	private void loadConfig(boolean send) {
		inverted = config.getBoolean("inverted", "general", false, "Flip it if you find the scrolling unintuitive");
		updateInterval = config.getInt("updateInterval", "general", 4, 0, 40, "How often should Arcane Workbench update displayed crafting result. Unit is in game ticks.");
		checkWorkbenchRecipes = config.getBoolean("checkWorkbenchRecipes", "general", true, "When false, Arcane Workbench will not perform vanilla crafting bench recipes. Arcane Workbench GUI will behave slightly awkwardly if the client has it false but not on server, but nothing would be broken.");
		arcaneCraftingHistorySize = config.getInt("arcaneCraftingHistorySize", "general", 16, 0, 256, "The maximum size of arcane crafting cache. 0 will effectively turn off the cache. It is suggested to keep a size of at least 1 to ensure shift crafting does not lag the server.");
		addTooltip = config.getBoolean("addTooltip", "general", true, "If false, no tooltip will be added.");
		browserScale = config.getFloat("browserScale", "general", 1, 1, 2, "Tweak the size of the book gui.");
		limitBookSearchToCategory = config.getBoolean("limitBookSearchToCategory", "general", false, "Whether the book gui search should search current tab only.");
		browserWidth = (int) (browserScale * 256);
		browserHeight = (int) (browserScale * 230);
		// if allow checking (vanilla behavior) no need to force client to have this mod
		TC4Tweak.INSTANCE.setAllowAll(checkWorkbenchRecipes);
		if (send)
			TC4Tweak.INSTANCE.detectAndSendConfigChanges();
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

	public int getArcaneCraftingHistorySize() {
		return arcaneCraftingHistorySize;
	}

	public boolean isAddTooltip() {
		return addTooltip;
	}

	public float getBrowserScale() {
		return browserScale;
	}

	public int getBrowserHeight() {
		return browserHeight;
	}

	public int getBrowserWidth() {
		return browserWidth;
	}

	public boolean isLimitBookSearchToCategory() {
		return limitBookSearchToCategory;
	}
}
