/* SPDX-License-Identifier: MIT */
/**
 * TC4Tweaks API
 * Copyright (c) 2025 Glease
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package net.glease.tc4tweak.api;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.client.gui.GuiResearchBrowser;
import thaumcraft.common.lib.research.ResearchManager;

class DummyBrowserPagingAPI implements BrowserPagingAPI {
    private final WeakHashMap<GuiResearchBrowser, Map.Entry<String, Boolean>> lastPlayer = new WeakHashMap<>();
    private final Map<String, ResearchCategoryList> categories = new LinkedHashMap<>();
    private final Map<String, ResearchCategoryList> view = Collections.unmodifiableMap(categories);

    private static boolean isEldritchUnlocked(String player) {
        return ResearchManager.isResearchComplete(player, "ELDRITCHMINOR");
    }

    @Override
    public Map<String, ResearchCategoryList> getTabsOnCurrentPage(GuiResearchBrowser gui, String player) {
        boolean eldritchUnlocked = isEldritchUnlocked(player);
        Map.Entry<String, Boolean> accessed = lastPlayer.get(gui);
        if (accessed == null || !accessed.getKey().equals(player) || eldritchUnlocked != accessed.getValue()) {
            categories.clear();
            categories.putAll(ResearchCategories.researchCategories);
            if (!eldritchUnlocked) {
                categories.remove("ELDRITCH");
            }
            lastPlayer.put(gui, new AbstractMap.SimpleImmutableEntry<>(player, eldritchUnlocked));
        }
        return view;
    }

    @Override
    public void setPage(GuiResearchBrowser gui, int page) {
    }

    @Override
    public void moveNextPage(GuiResearchBrowser gui) {
    }

    @Override
    public void movePreviousPage(GuiResearchBrowser gui) {
    }

    @Override
    public int getCurrentPage(GuiResearchBrowser gui) {
        return 0;
    }

    @Override
    public int getTotalPages(GuiResearchBrowser gui) {
        return 1;
    }

    @Override
    public int getTabsPerPage(GuiResearchBrowser gui) {
        return Integer.MAX_VALUE; // probably
    }
}
