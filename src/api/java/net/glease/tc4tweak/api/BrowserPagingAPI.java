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

import java.util.Map;

import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.client.gui.GuiResearchBrowser;

/**
 * Gives insight into current states related to {@link GuiResearchBrowser} tabs paging.
 * Can also move between pages.
 */
public interface BrowserPagingAPI {
    /**
     * Get all research tabs on current page
     * @param gui gui instance
     * @param player player name
     * @return an unmodifiable view on actual tabs visible. safe to be cached for a given instance of gui.
     */
    Map<String, ResearchCategoryList> getTabsOnCurrentPage(GuiResearchBrowser gui, String player);

    /**
     * Set the current page to given page. Page index start at 0.
     * @param gui gui instance
     * @param page new page index
     */
    void setPage(GuiResearchBrowser gui, int page);

    /**
     * Move to next page if possible.
     * @param gui gui instance
     */
    void moveNextPage(GuiResearchBrowser gui);

    /**
     * Move to previous page if possible.
     * @param gui gui instance
     */
    void movePreviousPage(GuiResearchBrowser gui);

    /**
     * Get the page index we are currently on.
     * @param gui gui instance
     * @return current page index
     */
    int getCurrentPage(GuiResearchBrowser gui);

    /**
     * Get total number of pages available.
     * This value might change based on player knowledge.
     * @param gui gui instance
     * @return total number of pages
     */
    int getTotalPages(GuiResearchBrowser gui);

    /**
     * Get max number of tabs on each page. This value does not change as player switches page, but might change as
     * player resize his game window.
     * @param gui gui instance
     * @return tabs per page
     */
    int getTabsPerPage(GuiResearchBrowser gui);
}
