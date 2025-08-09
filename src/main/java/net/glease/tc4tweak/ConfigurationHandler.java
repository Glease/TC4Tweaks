package net.glease.tc4tweak;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableList;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.FMLLaunchHandler;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import net.glease.tc4tweak.modules.infusionRecipe.InfusionOreDictMode;
import net.glease.tc4tweak.config.StringOrderingEntry;
import net.glease.tc4tweak.modules.FlushableCache;
import net.glease.tc4tweak.modules.researchBrowser.BrowserPaging;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public enum ConfigurationHandler {
    INSTANCE;

    private Configuration config;
    private boolean inverted;
    private int updateInterval;
    private boolean checkWorkbenchRecipes;
    private int arcaneCraftingHistorySize;
    private boolean addTooltip;
    private boolean mappingThreadNice;
    private float browserScale;
    private boolean limitBookSearchToCategory;
    private float nodeVisualSizeLimit;
    private boolean inferBrowserScale;
    private float inferBrowserScaleUpperBound;
    private float inferBrowserScaleLowerBound;
    private boolean inferBrowserScaleConsiderSearch;
    private boolean smallerJars;
    private boolean moreRandomizedLoot;
    private boolean dispenserShootPrimalArrow;
    private boolean addClearButton;
    private boolean addResearchSearch;
    private int decantMaxBlocks;
    private boolean savedLinkSaveWholeLink;
    private boolean savedLink;
    private boolean savedLinkDebug;
    private boolean alternativeAddStack;
    private boolean sendSupplementaryS35;

    private int browserHeight = 230;
    private int browserWidth = 256;
    private InfusionOreDictMode infusionOreDictMode = InfusionOreDictMode.Default;
    private List<String> categoryOrder = new ArrayList<>();
    private CompletionCounterStyle counterStyle = CompletionCounterStyle.Current;
    private EarthShockHarmMode earthShockHarmMode = EarthShockHarmMode.OnlyLiving;
    private final TObjectDoubleMap<UUID> championMods = new TObjectDoubleHashMap<>();

    ConfigurationHandler() {
        FMLCommonHandler.instance().bus().register(this);
    }

    void init(File f) {
        config = new Configuration(f, ConfigurationVersion.latest().getVersionMarker());
        ConfigurationVersion.migrateToLatest(config);
        loadConfig(false);
        setLanguageKeys();
    }

    private void breakLongCommentLines() {
        for (String categoryName : config.getCategoryNames()) {
            ConfigCategory category = config.getCategory(categoryName);
            for (Map.Entry<String, Property> entry : category.entrySet()) {
                entry.getValue().comment = entry.getValue().comment.replace(". ", ".\n");
            }
        }
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
    public void onConfigChange(ConfigChangedEvent.OnConfigChangedEvent e) {
        if (e.modID.equals(TC4Tweak.MOD_ID)) {
            loadConfig(true);
            FlushableCache.enableAll(false);
            CommonUtils.sortResearchCategories(true);
        }
    }

    private void loadConfig(boolean send) {
        checkWorkbenchRecipes = config.getBoolean("checkWorkbenchRecipes", "general", true, "When false, Arcane Workbench will not perform vanilla crafting bench recipes. Arcane Workbench GUI will behave slightly awkwardly if the client has it false but not on server, but nothing would be broken.");
        arcaneCraftingHistorySize = config.getInt("arcaneCraftingHistorySize", "general", 16, 0, 256, "The maximum size of arcane crafting cache. 0 will effectively turn off the cache. It is suggested to keep a size of at least 1 to ensure shift crafting does not lag the server.");

        inverted = config.getBoolean("inverted", "client", false, "Flip it if you find the scrolling unintuitive");
        updateInterval = config.getInt("updateInterval", "client", 4, 0, 40, "How often should Arcane Workbench update displayed crafting result. Unit is in game ticks.");
        addTooltip = config.getBoolean("addTooltip", "client", true, "If false, no tooltip will be added.");
        int mappingThreadNiceType = config.getInt("mappingThreadNiceType", "client", 0, 0, 2, "Whether to adjust mapping thread priority. 0 means auto. 1 means force enable. 2 means force disable.");
        switch (mappingThreadNiceType) {
            case 1:
                mappingThreadNice = true;
                break;
            case 2:
                mappingThreadNice = false;
                break;
            default:
                // 1 for integrated server. 1 for client thread. both of these are critical foreground tasks
                // so if less than 3 hardware threads available, let's be nice. otherwise, being nice only cause more
                // preemption and does no good to preserve good CPU time for foreground tasks
                mappingThreadNice = Runtime.getRuntime().availableProcessors() < 3;
                break;
        }
        browserScale = config.getFloat("scale", "client.browser_scale", 1, 1, 4, "Tweak the size of the book gui. No longer works if inferBrowserScale is set to true.");
        limitBookSearchToCategory = config.getBoolean("limitBookSearchToCategory", "client", false, "Whether the book gui search should search current tab only.");
        nodeVisualSizeLimit = config.getFloat("limitOversizedNodeRender", "client", 1, 0.5f, 1e10f, "The upper limit on how big nodes can be rendered. This is purely a visual thing and will not affect how big your node can actually grow. Setting a value like 10000.0 will effectively turn off this functionality, i.e. not limit the rendered size.");
        inferBrowserScale = config.getBoolean("infer", "client.browser_scale", true, "Tweak the size of the book gui based on screen size automatically. The value of browserScale set manually will not function any more.");
        inferBrowserScaleUpperBound = config.getFloat("maximum", "client.browser_scale", 4, 1, 16, "The minimum inferred scale. Cannot be smaller than the value of inferBrowserScaleLowerBound. This shouldn't be too high as a huge browser would be rendered with really poor image quality.");
        inferBrowserScaleLowerBound = config.getFloat("minimum", "client.browser_scale", 1, 1, 16, "The maximum inferred scale. Cannot be bigger than the value of inferBrowserScaleUpperBound.");
        inferBrowserScaleConsiderSearch = config.getBoolean("considerSearchArea", "client.browser_scale", true, "The search result area, even if it's not disabled, will be considered while inferring browserScale.");
        smallerJars = config.getBoolean("smallerJars", "general", FMLLaunchHandler.side().isServer(), "If true, jars (brain in jar, essentia jars, etc) will have a collision box the same as block outline. Otherwise it will have a collision box of 1x1x1, which is the vanilla tc4 behavior.");
        moreRandomizedLoot = config.getBoolean("moreRandomizedLoot", "general", true, "If true, enchanted books will have randomized enchantment and vis stone will have different vis stored even without server restart.");
        infusionOreDictMode = InfusionOreDictMode.get(config.getString("infusionOreDictMode", "general", infusionOreDictMode.name(), "Select the infusion oredict mode. Default: vanilla TC4 behavior. Strict: all oredict names must match to count as oredict substitute. Relaxed: oredict names needs only overlaps to count as oredict substitute. None: no oredict substitute at all.", Arrays.stream(InfusionOreDictMode.values()).map(Enum::name).toArray(String[]::new)));
        categoryOrder = ImmutableList.copyOf(config.getStringList("categoryOrder", "client", new String[]{"BASICS", "THAUMATURGY", "ALCHEMY", "ARTIFICE", "GOLEMANCY", "ELDRITCH",}, "Specify a full sorting order of research tabs. An empty list here means the feature is disabled. any research tab not listed here will be appended to the end in their original order. Use NEI utility to dump a list of all research tabs. Default is the list of all vanilla thaumcraft tabs."));
        dispenserShootPrimalArrow = config.getBoolean("dispenserShootPrimalArrow", "general", false, "If true, dispenser will shoot primal arrow instead of dropping it into world.");
        addClearButton = config.getBoolean("addClearButton", "client", true, "If true, a button will be shown when there is any amount of tc4 notifications AND when sending chat.");
        addResearchSearch = config.getBoolean("addResearchSearch", "client", true, "If true, a search box will appear on the top right bar of thaumonomicon gui. This feature is taken from WitchingGadgets due to the said GUI is being upsized by this mod and without modifying its code, the search box would not be positioned correctly. Will disable WitchingGadget's search feature (if it is present) regardless of whether this is true.");
        counterStyle = CompletionCounterStyle.get(config.getString("completionCounterStyle", "client", counterStyle.name(), "Select the style of completion counter. None: disable completion progress counter. Current: display how many you have completed already, and only show the total count for this tab when everything here has been learnt. All: show all counters at all times.", Arrays.stream(CompletionCounterStyle.values()).map(Enum::name).toArray(String[]::new)));
        decantMaxBlocks = config.getInt("decantMaxBlocks", "general", 1000, 1, Integer.MAX_VALUE, "Max blocks in queue of decant golem. High values can lead to severe server lag if golem is marked to empty very large body of fluid and has very long visibility range");
        earthShockHarmMode = EarthShockHarmMode.get(config.getString("earthShockHarmMode", "general", earthShockHarmMode.name(), "Select the entities to be damaged by earth shock. Note: certain entity (e.g. most projectiles) cannot be damaged even if it's on the list. OnlyLiving: only harm living entities, e.g. cows, players, zombies, the most intuitive behavior. ExceptItemXp: harm everything except items and xp orbs, e.g. item frames, all living entities like mentioned before. AllEntity: harm everything, like thaumcraft does out of box", Arrays.stream(EarthShockHarmMode.values()).map(Enum::name).toArray(String[]::new)));
        savedLink = config.getBoolean("enable", "general.saved_link", true, "Enable this feature. When disabled, no additional data will be saved to disk");
        savedLinkSaveWholeLink = config.getBoolean("saveCompleteLink", "general.saved_link", false, "When enabled, save the entire link up to source node. There is no actual benefit of this beyond more debug info. You probably don't want to change this.");
        savedLinkDebug = config.getBoolean("debug", "general.saved_link", false, "When enabled, print more debug info for this feature. You probably don't want to change this.");
        alternativeAddStack = config.getBoolean("alternativeAddStack", "general", true, "When enabled, using a phial will cause the new stack to be added to current player inventory using an alternative rule that prefers partial stacks and current slot.");
        sendSupplementaryS35 = config.getBoolean("sendSupplementaryS35", "general", true, "When enabled, will try harder at sending server side states for some particular tile entities. Enabling this might leads to very slight bandwidth usage increase, but can fix some rare desync.");

        String[][] championMods = new String[][]{
                {"a62bef38-48cc-42a6-ac5e-ef913841c4fd", "Champion health buff", "Champion health buff. Plain add.",},
                {"a340d2db-d881-4c25-ac62-f0ad14cd63b0", "Champion damage buff", "Champion damage buff. +x%, i.e. if configured as 1, the mob will deal double damage.",},
                {"4b1edd33-caa9-47ae-a702-d86c05701037", "Bold speed boost", "Bold speed boost. Multiplier, i.e. if configured as 2, the mob will have double speed.",},
                {"7163897f-07f5-49b3-9ce4-b74beb83d2d3", "Mighty damage boost", "Mighty damage boost. +x%, i.e. if configured as 1, the mob will deal double damage.",},
                {"54d621c1-dd4d-4b43-8bd2-5531c8875797", "HEALTH BUFF 1", "Boss health buff when at least 1 player present when spawned. Plain add.",},
                {"f51257dc-b7fa-4f7a-92d7-75d68e8592c4", "HEALTH BUFF 2", "Boss health buff when at least 2 player present when spawned. Plain add.",},
                {"3d6b2e42-4141-4364-b76d-0e8664bbd0bb", "HEALTH BUFF 3", "Boss health buff when at least 3 player present when spawned. Plain add.",},
                {"02c97a08-801c-4131-afa2-1427a6151934", "HEALTH BUFF 4", "Boss health buff when at least 4 player present when spawned. Plain add.",},
                {"0f354f6a-33c5-40be-93be-81b1338567f1", "HEALTH BUFF 5", "Boss health buff when at least 5 player present when spawned. Plain add.",},
                {"534f8c57-929a-48cf-bbd6-0fd851030748", "DAMAGE BUFF 1", "Boss damage buff when at least 1 player present when spawned. Plain add.",},
                {"d317a76e-0e7c-4c61-acfd-9fa286053b32", "DAMAGE BUFF 2", "Boss damage buff when at least 2 player present when spawned. Plain add.",},
                {"ff462d63-26a2-4363-830e-143ed97e2a4f", "DAMAGE BUFF 3", "Boss damage buff when at least 3 player present when spawned. Plain add.",},
                {"cf1eb39e-0c67-495f-887c-0d3080828d2f", "DAMAGE BUFF 4", "Boss damage buff when at least 4 player present when spawned. Plain add.",},
                {"3cfab9da-2701-43d8-ac07-885f16fa4117", "DAMAGE BUFF 5", "Boss damage buff when at least 5 player present when spawned. Plain add.",},
        };
        double[] defaults = new double[]{
                30.0D, 2.0D, 0.3D, 3.0D, 50.0D, 50.0D, 50.0D, 50.0D, 50.0D, 0.5D, 0.5D, 0.5D, 0.5D, 0.5D,
        };

        for (int i = 0; i < championMods.length; i++) {
            Property p = config.get("general.champion_mods", championMods[i][1], defaults[i], championMods[i][2]);
            p.setMinValue(0.0D);
            this.championMods.put(UUID.fromString(championMods[i][0]), p.getDouble());
        }
        config.getCategory("general.champion_mods").setComment("Tweak the stat buffs applied to champion mobs. Do note that those boss buffs can stack. If 5 player is present then all of DAMAGE BUFF 1 to DAMAGE BUFF 5 will be applied!");
        config.getCategory("general.saved_link").setComment("Persist the parents of vis relays into save file and use it as a hint for ");

        // validation
        if (inferBrowserScaleLowerBound > inferBrowserScaleUpperBound)
            config.getCategory("client").get("inferBrowserScaleLowerBound").set(inferBrowserScaleUpperBound);

        browserWidth = (int) (browserScale * 256);
        browserHeight = (int) (browserScale * 230);
        // it has been proven that the lack of this mod on client side is not a concern at all, for now
        TC4Tweak.INSTANCE.setAllowAll(true);
        if (send) {
            TC4Tweak.INSTANCE.detectAndSendConfigChanges();
            BrowserPaging.flushCache();
        }
        breakLongCommentLines();
        config.save();
    }

    void setGUISettings() {
        // config GUI stuff
        config.getCategory("client").get("categoryOrder").setArrayEntryClass(StringOrderingEntry.class);
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

    public boolean isMappingThreadNice() {
        return mappingThreadNice;
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

    public boolean isSmallerJars() {
        return smallerJars;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isMoreRandomizedLoot() {
        return moreRandomizedLoot;
    }

    public boolean isDispenserShootPrimalArrow() {
        return dispenserShootPrimalArrow;
    }

    public InfusionOreDictMode getInfusionOreDictMode() {
        return infusionOreDictMode;
    }

    public List<String> getCategoryOrder() {
        return categoryOrder;
    }

    public boolean isAddClearButton() {
        return addClearButton;
    }

    public boolean isAddResearchSearch() {
        return addResearchSearch;
    }

    public CompletionCounterStyle getResearchCounterStyle() {
        return counterStyle;
    }

    public double getChampionModValue(UUID configKey, double old) {
        double v = championMods.get(configKey);
        if (v == championMods.getNoEntryValue())
            return old;
        return v;
    }

    public int getDecantMaxBlocks() {
        return decantMaxBlocks;
    }

    public EarthShockHarmMode getEarthShockHarmMode() {
        return earthShockHarmMode;
    }

    public boolean isSavedLinkSaveWholeLink() {
        return savedLinkSaveWholeLink;
    }

    public boolean isSavedLinkEnabled() {
        return savedLink;
    }

    public boolean isSavedLinkDebugEnabled() {
        return savedLinkDebug;
    }

    public boolean isAlternativeAddStack() {
        return alternativeAddStack;
    }

    public boolean isSendSupplementaryS35() {
        return sendSupplementaryS35;
    }

    public enum CompletionCounterStyle {
        None,
        Current,
        All,
        ;

        public static CompletionCounterStyle get(String name) {
            for (CompletionCounterStyle value : values()) {
                if (value.name().equals(name))
                    return value;
            }
            return Current;
        }
    }

    public enum EarthShockHarmMode {
        OnlyLiving,
        ExceptItemXp,
        AllEntity,
        ;

        public static EarthShockHarmMode get(String name) {
            for (EarthShockHarmMode value : values()) {
                if (value.name().equals(name))
                    return value;
            }
            return OnlyLiving;
        }
    }
}
