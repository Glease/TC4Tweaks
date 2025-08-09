package net.glease.tc4tweak.network;

import net.glease.tc4tweak.ConfigurationHandler;
import net.glease.tc4tweak.modules.infusionRecipe.InfusionOreDictMode;

public class NetworkedConfiguration {
    static boolean checkWorkbenchRecipes = true;
    static boolean smallerJar = false;
    static InfusionOreDictMode infusionOreDictMode;

    public static boolean isCheckWorkbenchRecipes() {
        return checkWorkbenchRecipes;
    }

    public static void resetServer() {
        checkWorkbenchRecipes = ConfigurationHandler.INSTANCE.isCheckWorkbenchRecipes();
        smallerJar = ConfigurationHandler.INSTANCE.isSmallerJars();
        infusionOreDictMode = ConfigurationHandler.INSTANCE.getInfusionOreDictMode();
    }

    public static void resetClient() {
        checkWorkbenchRecipes = ConfigurationHandler.INSTANCE.isCheckWorkbenchRecipes();
        smallerJar = false;
        infusionOreDictMode = InfusionOreDictMode.Default;
    }

    public static boolean isSmallerJar() {
        return smallerJar;
    }

    public static InfusionOreDictMode getInfusionOreDictMode() {
        return infusionOreDictMode;
    }
}
