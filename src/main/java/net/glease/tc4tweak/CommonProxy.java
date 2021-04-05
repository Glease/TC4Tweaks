package net.glease.tc4tweak;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.relauncher.Side;
import net.glease.tc4tweak.modules.FlushableCache;
import net.glease.tc4tweak.network.MessageSendConfiguration;
import net.glease.tc4tweak.network.NetworkedConfiguration;

public class CommonProxy {
	public void preInit(FMLPreInitializationEvent e) {
		ConfigurationHandler.INSTANCE.init(e.getSuggestedConfigurationFile());

		if (Loader.isModLoaded("MineTweaker3"))
			MTCompat.preInit();

		TC4Tweak.INSTANCE.CHANNEL.registerMessage(MessageSendConfiguration.class, MessageSendConfiguration.class, 0, Side.CLIENT);
	}

	public void serverStarted(FMLServerStartedEvent e) {
		FlushableCache.enableAll(true);
		NetworkedConfiguration.resetCheckWorkbenchRecipes();
	}
}
