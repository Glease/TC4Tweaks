package net.glease.tc4tweak.network;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import net.glease.tc4tweak.ConfigurationHandler;
import net.glease.tc4tweak.TC4Tweak;
import net.minecraft.entity.player.EntityPlayerMP;

public class NetworkedConfiguration {
	static boolean checkWorkbenchRecipes = true;

	public static boolean isCheckWorkbenchRecipes() {
		return checkWorkbenchRecipes;
	}

	public static void resetCheckWorkbenchRecipes() {
		checkWorkbenchRecipes = true;
	}

	@SubscribeEvent
	public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent e) {
		if (e.player instanceof EntityPlayerMP && !ConfigurationHandler.INSTANCE.isCheckWorkbenchRecipes())
			TC4Tweak.INSTANCE.CHANNEL.sendTo(new MessageSendConfiguration(false), (EntityPlayerMP) e.player);
	}
}
