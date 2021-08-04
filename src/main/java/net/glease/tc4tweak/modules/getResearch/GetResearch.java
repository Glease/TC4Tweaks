package net.glease.tc4tweak.modules.getResearch;

import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;

import java.util.Map;

/**
 * getResearch speed up
 */
public class GetResearch {
    private static final ResearchItemCache cache = new ResearchItemCache();

    private GetResearch() {
    }

    /**
     * Called from {@link ResearchCategories#getResearch(String)}
     */
    public static ResearchItem getResearch(String key) {
        final Map<String, ResearchItem> map = cache.getCache();
        return map == null ? getResearchSlow(key) : map.get(key);
    }

    /**
     * Fallback for when server starting
     */
    private static ResearchItem getResearchSlow(String key) {
        return ResearchCategories.researchCategories.values().stream()
                .flatMap(l -> l.research.values().stream())
                .filter(i -> i.key.equals(key))
                .findFirst().orElse(null);
    }
}
