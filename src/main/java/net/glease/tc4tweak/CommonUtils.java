package net.glease.tc4tweak;

import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;

import java.util.*;
import java.util.stream.Collectors;

public class CommonUtils {
    // only keep the strings, so tab objects doesn't leak if they are ever removed
    private static final LinkedHashSet<String> originalTabOrders = new LinkedHashSet<>();

    public static String toString(AspectList al) {
        return al.aspects.entrySet().stream().filter(e -> e.getKey() != null && e.getValue() != null).map(e -> String.format("%dx%s", e.getValue(), e.getKey().getName())).collect(Collectors.joining(";"));
    }
    public static String toString(CrucibleRecipe r) {
        return "CrucibleRecipe{key="+r.key+",catalyst="+r.catalyst+",output="+r.getRecipeOutput()+",aspects="+toString(r.aspects)+"}";
    }

    static void sortResearchCategories(boolean force) {
        if (force || !ConfigurationHandler.INSTANCE.getCategoryOrder().isEmpty()) {
            // no need to synchronize
            // we fetch data from a practically immutable collection
            // then create a local copy
            // then replace the reference to that immutable collection with our local copy,
            // which is a simple PUTFIELD, which is atomic.
            LinkedHashMap<String, ResearchCategoryList> categories = ResearchCategories.researchCategories;
            originalTabOrders.addAll(categories.keySet());
            Set<String> realOrder = new LinkedHashSet<>(ConfigurationHandler.INSTANCE.getCategoryOrder());
            realOrder.addAll(originalTabOrders);
            LinkedHashMap<String, ResearchCategoryList> newCategories = new LinkedHashMap<>();
            for (String tab : realOrder) {
                if (categories.containsKey(tab))
                    newCategories.put(tab, categories.get(tab));
            }
            ResearchCategories.researchCategories = newCategories;
        }
    }
}
