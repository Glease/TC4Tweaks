package net.glease.tc4tweak.network;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import net.glease.tc4tweak.ConfigurationHandler;
import net.glease.tc4tweak.TC4Tweak;
import net.minecraft.entity.player.EntityPlayerMP;

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
