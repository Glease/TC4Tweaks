package net.glease.tc4tweak;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;

@Mod(modid = TC4Tweak.MOD_ID, name = "TC4 Tweak", version = "${version}", dependencies = "required-after:Thaumcraft", acceptableRemoteVersions = "*", guiFactory = "net.glease.tc4tweak.GuiFactory", canBeDeactivated = false)
public class TC4Tweak {
	public static final String MOD_ID = "tc4tweak";

	@SidedProxy(serverSide = "net.glease.tc4tweak.CommonProxy", clientSide = "net.glease.tc4tweak.ClientProxy")
	private static CommonProxy proxy;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent e) {
		proxy.preInit(e);
	}

	@Mod.EventHandler
	public void serverStarted(FMLServerStartedEvent e) {
		proxy.serverStarted(e);
	}
}
