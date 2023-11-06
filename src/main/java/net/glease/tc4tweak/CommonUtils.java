package net.glease.tc4tweak;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;

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

    @SuppressWarnings("unchecked")
    public static <T> T reflectGet(Field f, Object instance) {
        try {
            return (T) f.get(instance);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    public static Field getField(Class<?> clazz, String fieldName, int index) {
        try {
            Field f = null;
            Field[] fields = clazz.getDeclaredFields();
            if (index >= 0 && fields.length > index)
                f = fields[index];
            if (f == null || !f.getName().equalsIgnoreCase(fieldName))
                f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            return f;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
