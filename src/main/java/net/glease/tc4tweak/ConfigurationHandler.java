package net.glease.tc4tweak;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.glease.tc4tweak.modules.FlushableCache;
import net.glease.tc4tweak.modules.researchBrowser.BrowserPaging;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;
import java.util.Map;

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
    private float nodeVisualSizeLimit;
    private boolean inferBrowserScale;
    private float inferBrowserScaleUpperBound;
    private float inferBrowserScaleLowerBound;
    private boolean inferBrowserScaleConsiderSearch;

    private int browserHeight = 230;
    private int browserWidth = 256;

    ConfigurationHandler() {
        FMLCommonHandler.instance().bus().register(this);
    }

    void init(File f) {
        config = new Configuration(f, ConfigurationVersion.latest().getVersionMarker());
        ConfigurationVersion.migrateToLatest(config);
        loadConfig(false);
        setLanguageKeys();
    }

    private void setLanguageKeys() {
        for (String categoryName : config.getCategoryNames()) {
            ConfigCategory category = config.getCategory(categoryName);
            category.setLanguageKey("tc4tweaks.config." + categoryName);
            for (Map.Entry<String, Property> entry : category.entrySet()) {
                entry.getValue().setLanguageKey(String.format("%s.%s", category.getLanguagekey(), entry.getKey()));
            }
        }
    }

    @SubscribeEvent
    public void onConfigChange(ConfigChangedEvent.PostConfigChangedEvent e) {
        if (e.modID.equals(TC4Tweak.MOD_ID)) {
            loadConfig(true);
            FlushableCache.enableAll(false);
        }
    }

    private void loadConfig(boolean send) {
        checkWorkbenchRecipes = config.getBoolean("checkWorkbenchRecipes", "general", true, "When false, Arcane Workbench will not perform vanilla crafting bench recipes. Arcane Workbench GUI will behave slightly awkwardly if the client has it false but not on server, but nothing would be broken.");
        arcaneCraftingHistorySize = config.getInt("arcaneCraftingHistorySize", "general", 16, 0, 256, "The maximum size of arcane crafting cache. 0 will effectively turn off the cache. It is suggested to keep a size of at least 1 to ensure shift crafting does not lag the server.");

        inverted = config.getBoolean("inverted", "client", false, "Flip it if you find the scrolling unintuitive");
        updateInterval = config.getInt("updateInterval", "client", 4, 0, 40, "How often should Arcane Workbench update displayed crafting result. Unit is in game ticks.");
        addTooltip = config.getBoolean("addTooltip", "client", true, "If false, no tooltip will be added.");
        browserScale = config.getFloat("scale", "client.browser_scale", 1, 1, 4, "Tweak the size of the book gui. No longer works if inferBrowserScale is set to true.");
        limitBookSearchToCategory = config.getBoolean("limitBookSearchToCategory", "client", false, "Whether the book gui search should search current tab only.");
        nodeVisualSizeLimit = config.getFloat("limitOversizedNodeRender", "client", 1, 0.5f, 1e10f, "The upper limit on how big nodes can be rendered. This is purely a visual thing and will not affect how big your node can actually grow. Setting a value like 10000.0 will effectively turn off this functionality, i.e. not limit the rendered size.");
        inferBrowserScale = config.getBoolean("infer", "client.browser_scale", true, "Tweak the size of the book gui based on screen size automatically. The value of browserScale set manually will not function any more.");
        inferBrowserScaleUpperBound = config.getFloat("maximum", "client.browser_scale", 4, 1, 16, "The minimum inferred scale. Cannot be smaller than the value of inferBrowserScaleLowerBound. This shouldn't be too high as a huge browser would be rendered with really poor image quality.");
        inferBrowserScaleLowerBound = config.getFloat("minimum", "client.browser_scale", 1, 1, 16, "The maximum inferred scale. Cannot be bigger than the value of inferBrowserScaleUpperBound.");
        inferBrowserScaleConsiderSearch = config.getBoolean("considerSearchArea", "client.browser_scale", true, "The search result area, even if it's not disabled, will be considered while inferring browserScale.");

        // validation
        if (inferBrowserScaleLowerBound > inferBrowserScaleUpperBound)
            config.getCategory("client").get("inferBrowserScaleLowerBound").set(inferBrowserScaleUpperBound);

        browserWidth = (int) (browserScale * 256);
        browserHeight = (int) (browserScale * 230);
        // if allow checking (vanilla behavior) no need to force client to have this mod
        TC4Tweak.INSTANCE.setAllowAll(checkWorkbenchRecipes);
        if (send) {
            TC4Tweak.INSTANCE.detectAndSendConfigChanges();
            BrowserPaging.flushCache();
        }
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

    public void setBrowserScale(float browserScale) {
        this.browserScale = Math.max(Math.min(browserScale, inferBrowserScaleUpperBound), inferBrowserScaleLowerBound);
        browserWidth = (int) (browserScale * 256);
        browserHeight = (int) (browserScale * 230);
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

    public float getNodeVisualSizeLimit() {
        return nodeVisualSizeLimit;
    }

    public boolean isInferBrowserScale() {
        return inferBrowserScale;
    }

    public boolean isInferBrowserScaleConsiderSearch() {
        return inferBrowserScaleConsiderSearch;
    }
}
