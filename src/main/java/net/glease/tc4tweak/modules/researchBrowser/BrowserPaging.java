package net.glease.tc4tweak.modules.researchBrowser;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.glease.tc4tweak.ClientUtils;
import net.glease.tc4tweak.ConfigurationHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.opengl.GL11;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.client.gui.GuiResearchBrowser;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.lib.research.ResearchManager;

import static net.glease.tc4tweak.modules.researchBrowser.DrawResearchBrowserBorders.*;

public class BrowserPaging {
    public static final int BUTTON_HEIGHT = 8;
    public static final int BUTTON_WIDTH = 24;
    private static final Field fieldPlayer = ReflectionHelper.findField(GuiResearchBrowser.class, "player");
    private static final int NAVIGATION_BUTTON_Z_LEVEL = -50;
    public static int currentPageIndex;
    public static int maxPageIndex;
    public static LinkedHashMap<String, ResearchCategoryList> currentPageTabs;

    public static int getTabPerSide() {
        return ConfigurationHandler.INSTANCE.getBrowserHeight() / BUTTON_WIDTH;
    }

    private static boolean isEldritchUnlocked(GuiResearchBrowser gui) {
        try {
            return ResearchManager.isResearchComplete((String) fieldPlayer.get(gui), "ELDRITCHMINOR");
        } catch (ReflectiveOperationException ignored) {
            return true; //assume unlocked
        }
    }

    private static void updateMaxPageIndex(GuiResearchBrowser gui) {
        int tabsPerPage = getTabPerSide() * 2;
        int newMaxPageIndex = (ResearchCategories.researchCategories.size() - (isEldritchUnlocked(gui) ? 0 : 1) + tabsPerPage) / tabsPerPage - 1;
        if (newMaxPageIndex != maxPageIndex) {
            maxPageIndex = newMaxPageIndex;
            currentPageIndex = Math.min(currentPageIndex, BrowserPaging.maxPageIndex);
            currentPageTabs = null;
        }
    }

    public static LinkedHashMap<String, ResearchCategoryList> getTabsOnCurrentPage(String player) {
        if (currentPageTabs == null) {
            int tabsPerPage = getTabPerSide() * 2;
            // reset in case tab count changed
            if (currentPageIndex > maxPageIndex)
                currentPageIndex = 0;
            currentPageTabs = new LinkedHashMap<>();
            int toSkip = tabsPerPage * currentPageIndex;
            for (Map.Entry<String, ResearchCategoryList> e : ResearchCategories.researchCategories.entrySet()) {
                // pretend eldritch tab doesn't exist if ELDRITCHMINOR is not complete
                if ("ELDRITCH".equals(e.getKey()) && !ResearchManager.isResearchComplete(player, "ELDRITCHMINOR"))
                    continue;
                // skip tabs not on page
                if (toSkip > 0) {
                    toSkip--;
                    continue;
                }
                currentPageTabs.put(e.getKey(), e.getValue());
                // stop if collection full
                if (currentPageTabs.size() == tabsPerPage) break;
            }
        }
        return currentPageTabs;
    }

    public static void nextPage() {
        int tabsPerPage = getTabPerSide() * 2;
        currentPageIndex = Math.min(currentPageIndex + 1, (ResearchCategories.researchCategories.size() + tabsPerPage) / tabsPerPage - 1);
        currentPageTabs = null;
    }

    public static void prevPage() {
        currentPageIndex = Math.max(currentPageIndex - 1, 0);
        currentPageTabs = null;
    }

    public static void setPage(int page) {
        currentPageIndex = Math.max(0, Math.min(page, maxPageIndex));
        currentPageTabs = null;
    }

    public static void init() {
        EventHandler eventHandler = new EventHandler();
        FMLCommonHandler.instance().bus().register(eventHandler);
        MinecraftForge.EVENT_BUS.register(eventHandler);
    }

    public static void flushCache() {
        currentPageTabs = null;
    }

    // region Poor man's way of hooking into research browser.
    // Surely I could just add lines via asm, but that should be used as sparingly as possible.
    private static class ButtonPrevPage extends GuiButton {
        public ButtonPrevPage(int id, int x, int y) {
            super(id, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, "");
            zLevel = NAVIGATION_BUTTON_Z_LEVEL;
        }

        private void updateState() {
            visible = currentPageIndex > 0;
        }

        @Override
        public void drawButton(Minecraft p_146112_1_, int p_146112_2_, int p_146112_3_) {
            updateState();
            if (visible) {
                GL11.glColor4f(1, 1, 1, 1);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                UtilsFX.bindTexture("textures/gui/guiresearchtable2.png");
                ClientUtils.drawRectTextured(xPosition, xPosition + width, yPosition, yPosition + height, 184, 184 + 24, 208, 208 + 8, zLevel);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
            }
        }

        @Override
        public boolean mousePressed(Minecraft p_146116_1_, int p_146116_2_, int p_146116_3_) {
            updateState();
            if (super.mousePressed(p_146116_1_, p_146116_2_, p_146116_3_)) {
                prevPage();
                return true;
            } else {
                return false;
            }
        }
    }

    private static class ButtonNextPage extends GuiButton {

        public ButtonNextPage(int id, int x, int y) {
            // draw an additional black line
            super(id, x, y, BUTTON_WIDTH + 1, BUTTON_HEIGHT, "");
            zLevel = NAVIGATION_BUTTON_Z_LEVEL;
        }

        private void updateState() {
            visible = currentPageIndex < maxPageIndex;
        }

        @Override
        public void drawButton(Minecraft p_146112_1_, int p_146112_2_, int p_146112_3_) {
            updateState();
            if (visible) {
                GL11.glColor4f(1, 1, 1, 1);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                UtilsFX.bindTexture("textures/gui/guiresearchtable2.png");
                ClientUtils.drawRectTextured(xPosition, xPosition + width, yPosition, yPosition + height, 207, 207 + 25, 208, 208 + 8, zLevel);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
            }
        }

        @Override
        public boolean mousePressed(Minecraft p_146116_1_, int p_146116_2_, int p_146116_3_) {
            updateState();
            if (super.mousePressed(p_146116_1_, p_146116_2_, p_146116_3_)) {
                nextPage();
                return true;
            } else {
                return false;
            }
        }
    }
    // endregion

    public static class EventHandler {
        private int ticks;
        private boolean updated = false;

        @SubscribeEvent
        public void onClientTickEnd(TickEvent.ClientTickEvent e) {
            if (e.phase == TickEvent.Phase.START) {
                ticks++;
                updated = false;
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onGuiInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
            if (e.gui instanceof GuiResearchBrowser && ConfigurationHandler.INSTANCE.isInferBrowserScale()) {
                // factors to consider:
                // width: browser itself. tab icons on both sides. search area. a visual gap (BORDER_WIDTH * 2) between the furthest element and window border
                // height: browser itself. a visual gap (BORDER_HEIGHT * 2) between the furthest element and window border
                // search area width affects and depends on browser scale by width at the same time
                // tabs per side affects browser scale by height and depends on browser scale by widh at the same time
                // so in order to not make this iteratively smaller/bigger on each reopen we simulate this process (Glease/TC4Tweaks#40)
                // the process is rather complicated since >3 factors are affecting each other
                // simulating the process is the most straightforward and most bugfree way
                int iterations = 0;
                float oldScale;
                do {
                    oldScale = ConfigurationHandler.INSTANCE.getBrowserScale();
                    int searchAreaSizeTimesTwo = ConfigurationHandler.INSTANCE.isInferBrowserScaleConsiderSearch() ? ThaumonomiconIndexSearcher.getResultDisplayAreaWidth(e.gui) * 2 : 0;
                    float factorByWidth = ((float) e.gui.width - BORDER_WIDTH * 2 - searchAreaSizeTimesTwo - 24 * Math.min(2, ResearchCategories.researchCategories.size() / getTabPerSide())) / TEXTURE_WIDTH;
                    float factorByHeight = ((float) e.gui.height - BORDER_HEIGHT * 2) / TEXTURE_HEIGHT;
                    ConfigurationHandler.INSTANCE.setBrowserScale(Math.max(1, Math.min(factorByWidth, factorByHeight)));
                } while (Math.abs(oldScale - ConfigurationHandler.INSTANCE.getBrowserScale()) > 1e-4 && iterations++ < 1000);
            }
        }

        @SubscribeEvent
        @SuppressWarnings("unchecked")
        public void onGuiInitPost(GuiScreenEvent.InitGuiEvent.Post e) {
            if (e.gui instanceof GuiResearchBrowser) {
                GuiResearchBrowser gui = (GuiResearchBrowser) e.gui;
                // 5 here is small gap
                int x1 = gui.width / 2 - ConfigurationHandler.INSTANCE.getBrowserWidth() / 2 + BORDER_WIDTH + 5;
                // draw an additional black line
                int x2 = gui.width / 2 + ConfigurationHandler.INSTANCE.getBrowserWidth() / 2 - BORDER_WIDTH - 5 - 1 - BUTTON_WIDTH;
                int y = gui.height / 2 + ConfigurationHandler.INSTANCE.getBrowserHeight() / 2 - BORDER_HEIGHT / 2 - BUTTON_HEIGHT / 2;
                e.buttonList.add(new ButtonPrevPage(0, x1, y));
                e.buttonList.add(new ButtonNextPage(1, x2, y));


                ReflectionHelper.setPrivateValue(GuiResearchBrowser.class, gui, ConfigurationHandler.INSTANCE.getBrowserWidth(), "paneWidth");
                ReflectionHelper.setPrivateValue(GuiResearchBrowser.class, gui, ConfigurationHandler.INSTANCE.getBrowserHeight(), "paneHeight");

                currentPageTabs = null;
                updateMaxPageIndex(gui);
            }
        }

        @SubscribeEvent
        public void onGuiPreDraw(GuiScreenEvent.DrawScreenEvent.Pre e) {
            if (e.gui instanceof GuiResearchBrowser) {
                if (ticks % 10 == 0 && !updated) {
                    updated = true;
                    updateMaxPageIndex((GuiResearchBrowser) e.gui);
                }
            }
        }
    }
}
