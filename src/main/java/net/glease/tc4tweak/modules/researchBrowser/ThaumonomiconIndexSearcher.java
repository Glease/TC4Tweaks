package net.glease.tc4tweak.modules.researchBrowser;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.glease.tc4tweak.ConfigurationHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;
import thaumcraft.client.gui.GuiResearchBrowser;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.config.ConfigItems;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Adapted from Witching Gadgets
 * https://github.com/GTNewHorizons/WitchingGadgets/blob/1.2.13-GTNH/src/main/java/witchinggadgets/client/ThaumonomiconIndexSearcher.java
 */
public class ThaumonomiconIndexSearcher {
    private static final int mouseBufferIdent = 17;
    private static final int selectedCategoryIdent = 21;
    public static ThaumonomiconIndexSearcher instance;
    private static ByteBuffer mouseBuffer;
    private static Field f_selectedCategory = null;
    private static GuiTextField thaumSearchField;
    private static int listDisplayOffset = 0;
    private static String searchCategory;
    private static List<SearchQuery> searchResults = new ArrayList<>();

    public static void init() {
        instance = new ThaumonomiconIndexSearcher();
        MinecraftForge.EVENT_BUS.register(instance);
        FMLCommonHandler.instance().bus().register(instance);

        if (mouseBuffer == null)
            try {
                Field f_buf = Mouse.class.getDeclaredFields()[mouseBufferIdent];
                if (!f_buf.getName().equalsIgnoreCase("readBuffer"))
                    f_buf = Mouse.class.getDeclaredField("readBuffer");
                f_buf.setAccessible(true);
                mouseBuffer = (ByteBuffer) f_buf.get(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        try {
            f_selectedCategory = GuiResearchBrowser.class.getDeclaredFields()[selectedCategoryIdent];
            if (!f_selectedCategory.getName().equalsIgnoreCase("selectedCategory"))
                f_selectedCategory = GuiResearchBrowser.class.getDeclaredField("selectedCategory");
            f_selectedCategory.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static int getResultDisplayAreaWidth(GuiScreen gui) {
        return Math.min(gui.width - getResultDisplayAreaX(gui), 224);
    }

    private static int getResultDisplayAreaX(GuiScreen gui) {
        return gui.width / 2 + ConfigurationHandler.INSTANCE.getBrowserWidth() / 2 + (ResearchCategories.researchCategories.size() > BrowserPaging.getTabPerSide() ? 24 : 0);
    }

    private static void buildEntryList(String query) {
        if (query == null || query.isEmpty()) {
            searchResults.clear();
            return;
        }
        query = query.toLowerCase();
        List<SearchQuery> valids = new ArrayList<>();
        Set<String> keys;
        if (searchCategory != null && !searchCategory.isEmpty())
            keys = ResearchCategories.getResearchList(searchCategory).research.keySet();
        else {
            keys = new HashSet<>();
            for (ResearchCategoryList cat : ResearchCategories.researchCategories.values())
                keys.addAll(cat.research.keySet());
        }

        Set<SearchQuery> recipeBased = new HashSet<>();
        Set<String> usedResearches = new HashSet<>();
        for (String key : keys)
            if (key != null && !key.isEmpty() && ResearchCategories.getResearch(key) != null && ThaumcraftApiHelper.isResearchComplete(Minecraft.getMinecraft().thePlayer.getCommandSenderName(), key)) {
                if (ResearchCategories.getResearch(key).getName().startsWith("tc.research_name"))
                    continue;
                recipeBased.clear();
                ResearchPage[] pages = ResearchCategories.getResearch(key).getPages();
                if (pages != null)
                    for (ResearchPage page : pages) {
                        if (page.recipeOutput != null && page.recipeOutput.getDisplayName().toLowerCase().contains(query)) {
                            String dn;
                            if (page.recipeOutput.getItem() == ConfigItems.itemGolemCore) {
                                StringBuilder sb = new StringBuilder();
                                for (Object info : page.recipeOutput.getTooltip(Minecraft.getMinecraft().thePlayer, false))
                                    sb.append(info).append(" ");
                                dn = sb.toString();
                            } else {
                                dn = page.recipeOutput.getDisplayName();
                            }
                            if (!usedResearches.contains(dn)) {
                                recipeBased.add(new SearchQuery(key, "Item: " + dn));
                                usedResearches.add(dn);
                            }
                        }
                    }
                boolean rAdded = false;
                if (recipeBased.size() <= 1) {
                    if (!usedResearches.contains(ResearchCategories.getResearch(key).getName()))
                        if (key.toLowerCase().contains(query) || ResearchCategories.getResearch(key).getName().toLowerCase().contains(query)) {
                            valids.add(new SearchQuery(key, null));
                            usedResearches.add(ResearchCategories.getResearch(key).getName());
                            rAdded = true;
                        }
                }
                if (!rAdded)
                    valids.addAll(recipeBased);
            }
        valids.sort(ResearchSorter.instance);
        searchResults = valids;
    }

    private static String getActiveCategory() {
        String s = null;
        try {
            s = (String) f_selectedCategory.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s;
    }

    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        searchResults.clear();
        if (event.gui.getClass().getName().endsWith("GuiResearchBrowser")) {
            int width = ConfigurationHandler.INSTANCE.getBrowserWidth();
            int height = ConfigurationHandler.INSTANCE.getBrowserHeight();
            thaumSearchField = new GuiTextField(event.gui.mc.fontRenderer, event.gui.width / 2, event.gui.height / 2 - height / 2 + 5, Math.min(width / 2 - 20, 120), 13);
            thaumSearchField.setTextColor(-1);
            thaumSearchField.setDisabledTextColour(-1);
            thaumSearchField.setEnableBackgroundDrawing(false);
            thaumSearchField.setMaxStringLength(40);
            Keyboard.enableRepeatEvents(true);
        }
    }

    @SubscribeEvent
    public void onGuiPreDraw(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (thaumSearchField != null) {
            boolean cont = mouseBuffer.hasRemaining();
            if (Mouse.isCreated())
                if (cont) {
                    int mx = Mouse.getEventX() * event.gui.width / event.gui.mc.displayWidth;
                    int my = event.gui.height - Mouse.getEventY() * event.gui.height / event.gui.mc.displayHeight - 1;
                    int button = Mouse.getEventButton();
                    int wheel = Mouse.getEventDWheel();
                    if (Mouse.getEventButtonState()) {
                        thaumSearchField.mouseClicked(mx, my, button);
                        if (thaumSearchField.isFocused() && button == 1) {
                            thaumSearchField.setText("");
                            searchResults.clear();
                        } else if (mx > (event.gui.width / 2 + ConfigurationHandler.INSTANCE.getBrowserWidth() / 2 + (ResearchCategories.researchCategories.size() > BrowserPaging.getTabPerSide() ? 24 : 2)) && my > event.gui.height / 2 - ConfigurationHandler.INSTANCE.getBrowserHeight() / 2 && my < event.gui.height / 2 + ConfigurationHandler.INSTANCE.getBrowserHeight() / 2) {
                            int clicked = my - (event.gui.height / 2 - ConfigurationHandler.INSTANCE.getBrowserHeight() / 2 + 6);
                            clicked /= 11;
                            int selected = clicked + listDisplayOffset;
                            if (selected < searchResults.size()) {
                                ResearchItem item = ResearchCategories.getResearch(searchResults.get(selected).research);
                                event.gui.mc.displayGuiScreen(new GuiResearchRecipe(item, 0, item.displayColumn, item.displayRow));
                            }
                        }
                    } else if (wheel != 0 && mx > (event.gui.width / 2 + ConfigurationHandler.INSTANCE.getBrowserWidth() / 2 + (ResearchCategories.researchCategories.size() > BrowserPaging.getTabPerSide() ? 24 : 2))) {
                        if (wheel < 0)
                            listDisplayOffset++;
                        else
                            listDisplayOffset--;
                        if (listDisplayOffset > searchResults.size() - 20)
                            listDisplayOffset = searchResults.size() - 20;
                        if (listDisplayOffset < 0)
                            listDisplayOffset = 0;
                    }
                }
        }
    }

    @SubscribeEvent
    public void onGuiPostDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (thaumSearchField != null) {
            int x = getResultDisplayAreaX(event.gui);
            int y = event.gui.height / 2 - ConfigurationHandler.INSTANCE.getBrowserHeight() / 2;
            int maxWidth = getResultDisplayAreaWidth(event.gui);

            if (!searchResults.isEmpty()) {
                UtilsFX.bindTexture("textures/misc/parchment3.png");
                GL11.glEnable(GL11.GL_BLEND);
                Tessellator tes = Tessellator.instance;
                tes.startDrawingQuads();
                tes.setColorOpaque_I(0xffffff);
                tes.addVertexWithUV(x, y + 230, 0, 0, 150 / 256f);
                tes.addVertexWithUV(x + maxWidth, y + 230, 0, 150 / 256f, 150 / 256f);
                tes.addVertexWithUV(x + maxWidth, y, 0, 150 / 256f, 0);
                tes.addVertexWithUV(x, y, 0, 0, 0);
                tes.draw();
            }
            UtilsFX.bindTexture("textures/gui/guiresearchtable2.png");
            event.gui.drawTexturedModalRect(thaumSearchField.xPosition - 2, thaumSearchField.yPosition - 4, 94, 8, thaumSearchField.width + 8, thaumSearchField.height);
            event.gui.drawTexturedModalRect(thaumSearchField.xPosition - 2, thaumSearchField.yPosition + thaumSearchField.height - 4, 138, 158, thaumSearchField.width + 8, 2);
            event.gui.drawTexturedModalRect(thaumSearchField.xPosition + thaumSearchField.width + 6, thaumSearchField.yPosition - 4, 244, 136, 2, thaumSearchField.height + 2);

            if ((searchResults == null || searchResults.isEmpty()) && !thaumSearchField.isFocused())
                event.gui.drawString(event.gui.mc.fontRenderer, StatCollector.translateToLocal("tc4tweaks.gui.search"), thaumSearchField.xPosition, thaumSearchField.yPosition, 0x777777);
            else
                for (int i = 0; i < 20; i++)
                    if (i + listDisplayOffset < searchResults.size()) {
                        String name = searchResults.get(listDisplayOffset + i).display != null ? searchResults.get(listDisplayOffset + i).display : ResearchCategories.getResearch(searchResults.get(listDisplayOffset + i).research).getName();
                        name = searchResults.get(listDisplayOffset + i).modifier + event.gui.mc.fontRenderer.trimStringToWidth(name, maxWidth - 10);
                        event.gui.mc.fontRenderer.drawString(name, x + 6, y + 6 + i * 11, 0xffffff, false);
                    }

            thaumSearchField.drawTextBox();
        }
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (thaumSearchField != null) {
            thaumSearchField = null;
            Keyboard.enableRepeatEvents(false);
        }
    }

    @SubscribeEvent
    public void renderTick(TickEvent.ClientTickEvent event) {
        if (thaumSearchField != null)
            if (Keyboard.isCreated() && thaumSearchField.isFocused())
                while (Keyboard.next())
                    if (Keyboard.getEventKeyState()) {
                        if (Keyboard.getEventKey() == 1)
                            Minecraft.getMinecraft().displayGuiScreen(null);
                        else {
                            thaumSearchField.textboxKeyTyped(Keyboard.getEventCharacter(), Keyboard.getEventKey());
                            listDisplayOffset = 0;
                            if (ConfigurationHandler.INSTANCE.isLimitBookSearchToCategory())
                                searchCategory = getActiveCategory();
                            buildEntryList(thaumSearchField.getText());
                        }
                    }
    }

    private static class ResearchSorter implements Comparator<SearchQuery> {
        static final ResearchSorter instance = new ResearchSorter();

        @Override
        public int compare(SearchQuery o1, SearchQuery o2) {
            String c1 = o1.display != null ? o1.display : ResearchCategories.getResearch(o1.research).getName();
            String c2 = o2.display != null ? o2.display : ResearchCategories.getResearch(o2.research).getName();
            return c1.compareToIgnoreCase(c2);
        }
    }

    private static class SearchQuery {
        public final String research;
        public final String display;
        public final String modifier;

        public SearchQuery(String research, String display) {
            this.research = research;
            this.display = display;
            modifier = display != null ? EnumChatFormatting.DARK_GRAY.toString() : "";
        }
    }
}