package net.glease.tc4tweak;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.versioning.DefaultArtifactVersion;
import cpw.mods.fml.common.versioning.VersionParser;
import cpw.mods.fml.common.versioning.VersionRange;
import cpw.mods.fml.relauncher.Side;
import net.glease.tc4tweak.network.MessageSendConfiguration;

import java.util.Map;

@Mod(modid = TC4Tweak.MOD_ID, name = "TC4 Tweak", version = "${version}", dependencies = "required-after:Thaumcraft", guiFactory = "net.glease.tc4tweak.GuiFactory", canBeDeactivated = false)
public class TC4Tweak {
	public static final String MOD_ID = "tc4tweak";
	private static final VersionRange ACCEPTED_CLIENT_VERSION = VersionParser.parseRange("[1.2.0,)");

	@SidedProxy(serverSide = "net.glease.tc4tweak.CommonProxy", clientSide = "net.glease.tc4tweak.ClientProxy")
	static CommonProxy proxy;
	@Mod.Instance
	public static TC4Tweak INSTANCE;

	private boolean allowAll = true;
	public final SimpleNetworkWrapper CHANNEL = new SimpleNetworkWrapper(MOD_ID);

	void detectAndSendConfigChanges() {
		if (FMLCommonHandler.instance().getMinecraftServerInstance() != null)
			INSTANCE.CHANNEL.sendToAll(new MessageSendConfiguration());
	}

	void setAllowAll(boolean allowAll) {
		this.allowAll = allowAll;
	}

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent e) {
		proxy.preInit(e);
	}

	@Mod.EventHandler
	public void serverStarted(FMLServerStartedEvent e) {
		proxy.serverStarted(e);
	}

	@NetworkCheckHandler
	public boolean checkConnection(Map<String, String> remoteVersions, Side side) {
		if (side == Side.CLIENT) {
			String remoteVersionString = remoteVersions.getOrDefault(MOD_ID, null);
			return allowAll || remoteVersionString != null && ACCEPTED_CLIENT_VERSION.containsVersion(new DefaultArtifactVersion(MOD_ID, remoteVersionString));
		}
		return true;
	}
}
