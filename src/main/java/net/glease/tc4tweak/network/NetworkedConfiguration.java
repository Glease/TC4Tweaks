package net.glease.tc4tweak.network;

import net.glease.tc4tweak.ConfigurationHandler;

public class NetworkedConfiguration {
    static boolean checkWorkbenchRecipes = true;
    static boolean smallerJar = true;

    public static boolean isCheckWorkbenchRecipes() {
        return checkWorkbenchRecipes;
    }

    public static void reset() {
        checkWorkbenchRecipes = ConfigurationHandler.INSTANCE.isCheckWorkbenchRecipes();
        smallerJar = ConfigurationHandler.INSTANCE.isSmallerJars();
    }

    public static boolean isSmallerJar() {
        return smallerJar;
    }
}
