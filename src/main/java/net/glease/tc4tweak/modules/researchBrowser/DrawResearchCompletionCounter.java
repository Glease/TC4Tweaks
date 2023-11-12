package net.glease.tc4tweak.modules.researchBrowser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.glease.tc4tweak.ClientUtils;
import net.glease.tc4tweak.ConfigurationHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.client.gui.GuiResearchBrowser;

import static net.glease.tc4tweak.modules.researchBrowser.DrawResearchBrowserBorders.BORDER_HEIGHT;
import static net.glease.tc4tweak.modules.researchBrowser.DrawResearchBrowserBorders.BORDER_WIDTH;
import static thaumcraft.client.gui.GuiResearchBrowser.completedResearch;

public class DrawResearchCompletionCounter {
    public static void init() {
        MinecraftForge.EVENT_BUS.register(EventHandler.INSTANCE);
    }

    private static boolean canUnlockResearch(ResearchItem res) {
        String playerName = Minecraft.getMinecraft().thePlayer.getCommandSenderName();
        if (res.parents != null) {
            for (String pt : res.parents) {
                ResearchItem parent = ResearchCategories.getResearch(pt);
                if (parent != null && !completedResearch.get(playerName).contains(parent.key)) {
                    return false;
                }
            }
        }

        if (res.parentsHidden != null) {
            for (String pt : res.parentsHidden) {
                ResearchItem parent = ResearchCategories.getResearch(pt);
                if (parent != null && !completedResearch.get(playerName).contains(parent.key)) {
                    return false;
                }
            }
        }
        return true;
    }

    static void drawCompletionCounter(GuiResearchBrowser gui, int x, int y, int mx, int my) {
        ConfigurationHandler.CompletionCounterStyle style = ConfigurationHandler.INSTANCE.getResearchCounterStyle();
        if (style == ConfigurationHandler.CompletionCounterStyle.None)
            return;
        // draw completion text progress text
        ResearchCategoryList category = ResearchCategories.getResearchList(Utils.getActiveCategory());
        // filter away stuff that are auto unlocked but never shown. probably should just filter away virtual research,
        // but I'm not entirely sure how that field is actually used in practice, so let's be conservative for now
        Map<String, ResearchItem> all = category.research.entrySet().stream().filter(e -> !(e.getValue().isAutoUnlock() && e.getValue().isVirtual())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        int total = all.size();
        ArrayList<String> completedKeys = completedResearch.get(Minecraft.getMinecraft().thePlayer.getCommandSenderName());
        long completed = completedKeys.stream().filter(all::containsKey).count();
        long revealed = all.entrySet().stream().filter(e -> completedKeys.contains(e.getKey()) || completedKeys.contains("@" + e.getKey()) || !(e.getValue().isLost() || e.getValue().isHidden() && !completedKeys.contains("@" + e.getValue().key) || e.getValue().isConcealed() && !canUnlockResearch(e.getValue()))).count();
        String tooltip;
        if (style == ConfigurationHandler.CompletionCounterStyle.Current && revealed < total) {
            tooltip = I18n.format("tc4tweaks.gui.progress.partial", completed, revealed);
        } else {
            tooltip = I18n.format("tc4tweaks.gui.progress", completed, total);
        }

        FontRenderer fontRenderer = gui.mc.fontRenderer;
        int lblX = x + BORDER_WIDTH + 2;
        int lblY = y + (BORDER_HEIGHT - fontRenderer.FONT_HEIGHT) / 2;
        fontRenderer.drawString(tooltip, lblX, lblY, 0x777777, true);
        if (mx >= lblX && mx <= lblX + fontRenderer.getStringWidth(tooltip) && my >= lblY && my <= lblY + fontRenderer.FONT_HEIGHT) {
            String hover;
            if (revealed < total) {
                hover = I18n.format("tc4tweaks.gui.progress.partial.tooltip");
            } else {
                hover = I18n.format("tc4tweaks.gui.progress.tooltip");
            }
            ClientUtils.drawMultilineTip(fontRenderer, mx, my, Collections.singletonList(hover));
        }
    }

    public enum EventHandler {
        INSTANCE;

        @SubscribeEvent
        public void onPostGuiDraw(GuiScreenEvent.DrawScreenEvent.Post e) {
            if (!(e.gui instanceof GuiResearchBrowser)) return;
            drawCompletionCounter((GuiResearchBrowser) e.gui, DrawResearchBrowserBorders.guiX, DrawResearchBrowserBorders.guiY, e.mouseX, e.mouseY);
        }
    }
}
