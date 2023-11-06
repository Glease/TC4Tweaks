package net.glease.tc4tweak.modules.researchBrowser;

import java.lang.reflect.Field;

import net.glease.tc4tweak.CommonUtils;
import thaumcraft.client.gui.GuiResearchBrowser;

class Utils {
    private static final int selectedCategoryIdent = 21;
    private static final Field f_selectedCategory = CommonUtils.getField(GuiResearchBrowser.class, "selectedCategory", selectedCategoryIdent);

    static String getActiveCategory() {
        return CommonUtils.reflectGet(f_selectedCategory, null);
    }
}
