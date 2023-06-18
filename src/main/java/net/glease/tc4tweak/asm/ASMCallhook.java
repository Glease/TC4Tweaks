package net.glease.tc4tweak.asm;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.glease.tc4tweak.ClientProxy;
import net.glease.tc4tweak.ClientUtils;
import net.glease.tc4tweak.ConfigurationHandler;
import net.glease.tc4tweak.modules.researchBrowser.BrowserPaging;
import net.glease.tc4tweak.modules.researchBrowser.DrawResearchBrowserBorders;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.util.ResourceLocation;
import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.client.gui.GuiResearchBrowser;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.client.gui.GuiResearchTable;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.tiles.TileMagicWorkbench;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.glease.tc4tweak.TC4Tweak.log;

public class ASMCallhook {
    private static final WeakHashMap<TileMagicWorkbench, Void> postponed = new WeakHashMap<>();
    private static final AtomicBoolean cacheUsed = new AtomicBoolean(false);
    // workbench throttling
    private static long lastUpdate = 0;
    private static boolean priorityChanged = false;
    private static long start;
    private static Field fieldParticleTexture;

    private ASMCallhook() {
    }

    /**
     * Called from {@link thaumcraft.client.gui.GuiResearchRecipe#getFromCache(int)}
     */
    @Callhook
    public static void onCacheLookupHead() {
        cacheUsed.lazySet(true);
    }

    /**
     * Called from {@link thaumcraft.client.gui.MappingThread#run()}
     */
    @Callhook
    public static void onMappingStart(Map<String, Integer> mapping) {
        if (ConfigurationHandler.INSTANCE.isMappingThreadNice())
            Thread.currentThread().setPriority(1);
        else
            priorityChanged = true;
        log.info("TC4 Mapping start. {} entries to work with.", mapping.size());
        start = System.nanoTime();
    }

    /**
     * Called from {@link thaumcraft.client.gui.MappingThread#run()}
     */
    @Callhook
    public static void onMappingDidWork() {
        if (!priorityChanged && cacheUsed.get()) {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
            priorityChanged = true;
        }
    }

    /**
     * Called from {@link thaumcraft.client.gui.MappingThread#run()}
     */
    @Callhook
    public static void onMappingFinished() {
        if (ConfigurationHandler.INSTANCE.isMappingThreadNice())
            log.info("TC4 Mapping finish. Took {}ns.", System.nanoTime() - start);
        else
            log.info("TC4 Mapping finish. Took {}ns. Priority boosted: {}", System.nanoTime() - start, priorityChanged);
    }

    public static void updatePostponed() {
        synchronized (postponed) {
            for (Map.Entry<TileMagicWorkbench, Void> workbench : postponed.entrySet()) {
                TileMagicWorkbench tile = workbench.getKey();
                if (tile != null && tile.eventHandler != null && !tile.isInvalid() && tile.hasWorldObj()) {
                    // best effort guess on whether tile is valid
                    tile.eventHandler.onCraftMatrixChanged(tile);
                }
            }
            postponed.clear();
        }
    }

    /**
     * called from GuiResearchTable. first arg is this
     */
    @Callhook
    public static void handleMouseInput(GuiResearchTable screen) {
        ClientProxy.handleMouseInput(screen);
    }

    /**
     * called from GuiResearchRecipe. first arg is this
     */
    @Callhook
    public static void handleMouseInput(GuiResearchRecipe screen) {
        ClientProxy.handleMouseInput(screen);
    }

    /**
     * Throttle the amount of arcane workbench update on client side
     * called from TileMagicWorkbench.
     */
    @Callhook
    public static void updateCraftingMatrix(TileMagicWorkbench self) {
        if (!self.getWorldObj().isRemote) {
            self.eventHandler.onCraftMatrixChanged(self);
            return;
        }
        long oldUpdate = lastUpdate;
        lastUpdate = System.currentTimeMillis();
        if (lastUpdate - oldUpdate > 1000 / 5) {
            self.eventHandler.onCraftMatrixChanged(self); // 5 times per second at max
        } else {
            synchronized (postponed) {
                postponed.put(self, null);
            }
        }
    }

    @Callhook
    public static void renderFacingStrip(double px, double py, double pz, float angle, float scale, float alpha, int frames, int strip, int frame, float partialTicks, int color) {
        UtilsFX.renderFacingStrip(px, py, pz, angle, Math.min(scale, ConfigurationHandler.INSTANCE.getNodeVisualSizeLimit()), alpha, frames, strip, frame, partialTicks, color);
    }

    @Callhook
    public static void renderAnimatedQuadStrip(float scale, float alpha, int frames, int strip, int cframe, float partialTicks, int color) {
        UtilsFX.renderAnimatedQuadStrip(Math.min(scale, ConfigurationHandler.INSTANCE.getNodeVisualSizeLimit()), alpha, frames, strip, cframe, partialTicks, color);
    }

    /**
     * Draw research browser borders. Called from GuiResearchBrowser#genResearchBackground
     */
    @Callhook
    public static void drawResearchBrowserBorders(GuiResearchBrowser gui, int x, int y, int u, int v, int width, int height) {
        DrawResearchBrowserBorders.drawResearchBrowserBorders(gui, x, y, u, v, width, height);
    }

    @Callhook
    public static void drawResearchBrowserBackground(GuiResearchBrowser gui, int x, int y, int u, int v, int width, int height) {
        DrawResearchBrowserBorders.drawResearchBrowserBackground(gui, x, y, u, v, width, height);
    }

    @Callhook
    public static int getResearchBrowserHeight() {
        return ConfigurationHandler.INSTANCE.getBrowserHeight();
    }

    @Callhook
    public static int getResearchBrowserWidth() {
        return ConfigurationHandler.INSTANCE.getBrowserWidth();
    }

    @Callhook
    public static int getTabDistance() {
        // why is this 8?
        return ConfigurationHandler.INSTANCE.getBrowserWidth() + 8;
    }

    @Callhook
    public static int getTabIconDistance() {
        // why is this 24?
        return ConfigurationHandler.INSTANCE.getBrowserWidth() + 24;
    }

    @Callhook
    public static int getNewGuiMapTop(int oldVal) {
        return (int) (oldVal - 85 * (ConfigurationHandler.INSTANCE.getBrowserScale() - 1));
    }

    @Callhook
    public static int getNewGuiMapLeft(int oldVal) {
        return (int) (oldVal - 112 * (ConfigurationHandler.INSTANCE.getBrowserScale() - 1));
    }

    @Callhook
    public static int getNewGuiMapBottom(int oldVal) {
        return (int) (oldVal - 112 * (ConfigurationHandler.INSTANCE.getBrowserScale() - 1));
    }

    @Callhook
    public static int getNewGuiMapRight(int oldVal) {
        return (int) (oldVal - 61 * (ConfigurationHandler.INSTANCE.getBrowserScale() - 1));
    }

    @Callhook
    public static int getTabPerSide() {
        return BrowserPaging.getTabPerSide();
    }

    @Callhook
    public static LinkedHashMap<String, ResearchCategoryList> getTabsOnCurrentPage(String player) {
        return BrowserPaging.getTabsOnCurrentPage(player);
    }

    @Callhook
    public static void drawResearchCategoryHintParticles(int x, int y, int u, int v, int width, int height, double zLevel, GuiResearchBrowser gui) {
        if (x < gui.width / 2)
            UtilsFX.drawTexturedQuad(x, y, u, v, width, height, zLevel);
        else {
            x += 16;
            ClientUtils.drawRectTextured(x, x + width, y, y + height, u + width, u, v + height, v, zLevel);
        }
    }

    @Callhook
    public static ResourceLocation getParticleTexture() {
        try {
            if (fieldParticleTexture == null)
                fieldParticleTexture = ReflectionHelper.findField(EffectRenderer.class, "particleTextures", "b", "field_110737_b");
            return (ResourceLocation) fieldParticleTexture.get(null);
        } catch (Exception ignored) {
            return null;
        }
    }
}
