package net.glease.tc4tweak;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.FMLLaunchHandler;
import gnu.trove.set.hash.TIntHashSet;
import net.glease.tc4tweak.modules.FlushableCache;
import net.glease.tc4tweak.modules.researchBrowser.BrowserPaging;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.oredict.OreDictionary;
import thaumcraft.api.ThaumcraftApiHelper;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

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

    private int browserHeight = 230;
    private int browserWidth = 256;
    private InfusionOreDictMode infusionOreDictMode = InfusionOreDictMode.Default;

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
    public void onConfigChange(ConfigChangedEvent.OnConfigChangedEvent e) {
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

    public boolean isMoreRandomizedLoot() {
        return moreRandomizedLoot;
    }

    public InfusionOreDictMode getInfusionOreDictMode() {
        return InfusionOreDictMode.Default; // TODO
    }

    public enum InfusionOreDictMode {
        Default {
            @SuppressWarnings("deprecation")
            @Override
            public boolean test(ItemStack playerInput, ItemStack recipeSpec) {
                int od = OreDictionary.getOreID(playerInput);
                if (od == -1) return false;
                ItemStack[] ores = OreDictionary.getOres(od).toArray(new ItemStack[0]);
                return ThaumcraftApiHelper.containsMatch(false, new ItemStack[]{recipeSpec}, ores);
            }
        },
        Strict {
            @Override
            public boolean test(ItemStack playerInput, ItemStack recipeSpec) {
                return new TIntHashSet(OreDictionary.getOreIDs(playerInput)).equals(new TIntHashSet(OreDictionary.getOreIDs(recipeSpec)));
            }
        },
        Relaxed {
            @Override
            public boolean test(ItemStack playerInput, ItemStack recipeSpec) {
                TIntHashSet set = new TIntHashSet(OreDictionary.getOreIDs(playerInput));
                for (int i : OreDictionary.getOreIDs(recipeSpec)) {
                    if (set.contains(i))
                        return true;
                }
                return false;
            }
        },
        None {
            @Override
            public boolean test(ItemStack playerInput, ItemStack recipeSpec) {
                return false;
            }
        };

        public abstract boolean test(ItemStack playerInput, ItemStack recipeSpec);

        public static InfusionOreDictMode get(String name) {
            for (InfusionOreDictMode value : values()) {
                if (value.name().equals(name))
                    return value;
            }
            return Default;
        }
    }
}
