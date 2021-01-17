package net.glease.tc4tweak;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import net.glease.tc4tweak.asm.ASMCallhookServer;

public class CommonProxy {
	public void preInit(FMLPreInitializationEvent e) {
		ConfigurationHandler.INSTANCE.init(e.getSuggestedConfigurationFile());

		if (Loader.isModLoaded("MineTweaker3"))
			MTCompat.preInit();
	}

	public void serverStarted(FMLServerStartedEvent e) {
		ASMCallhookServer.flushAllCache();
	}
}
