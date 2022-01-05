package net.glease.tc4tweak;

import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;

import java.util.stream.Collectors;

public class CommonUtils {
    public static String toString(AspectList al) {
        return al.aspects.entrySet().stream().filter(e -> e.getKey() != null && e.getValue() != null).map(e -> String.format("%dx%s", e.getValue(), e.getKey().getName())).collect(Collectors.joining(";"));
    }
    public static String toString(CrucibleRecipe r) {
        return "CrucibleRecipe{key="+r.key+",catalyst="+r.catalyst+",output="+r.getRecipeOutput()+",aspects="+toString(r.aspects)+"}";
    }
}
