package net.glease.tc4tweak.network;

import net.glease.tc4tweak.ConfigurationHandler;

public class NetworkedConfiguration {
    static boolean checkWorkbenchRecipes = true;
    static boolean smallerJar = false;

    public static boolean isCheckWorkbenchRecipes() {
        return checkWorkbenchRecipes;
    }

    public static void resetServer() {
        checkWorkbenchRecipes = ConfigurationHandler.INSTANCE.isCheckWorkbenchRecipes();
        smallerJar = ConfigurationHandler.INSTANCE.isSmallerJars();
    }

    public static void resetClient() {
        checkWorkbenchRecipes = ConfigurationHandler.INSTANCE.isCheckWorkbenchRecipes();
        smallerJar = false;
    }

    public static boolean isSmallerJar() {
        return smallerJar;
    }
}
