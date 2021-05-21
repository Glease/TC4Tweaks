package net.glease.tc4tweak;

import com.google.common.collect.ImmutableMap;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.versioning.DefaultArtifactVersion;
import cpw.mods.fml.common.versioning.VersionParser;
import cpw.mods.fml.common.versioning.VersionRange;
import cpw.mods.fml.relauncher.Side;
import net.glease.tc4tweak.network.MessageSendConfiguration;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.cert.Certificate;
import java.util.Map;

@Mod(modid = TC4Tweak.MOD_ID, name = "TC4 Tweak", version = "${version}", dependencies = "required-after:Thaumcraft", guiFactory = "net.glease.tc4tweak.GuiFactory", canBeDeactivated = false)
public class TC4Tweak {
	public static final String MOD_ID = "tc4tweak";
	private static final VersionRange ACCEPTED_CLIENT_VERSION = VersionParser.parseRange("[1.2.0-beta1,)");
	private static final ImmutableMap<String, String> KNOWN_SIGNATURE =
			ImmutableMap.of("47:3C:3A:39:76:76:97:8F:F4:87:7A:BA:2D:57:86:0D:DA:20:E2:FC", "glease");

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

	public TC4Tweak() {
		FMLCommonHandler.instance().registerCrashCallable(new ICrashCallable() {
			@Override
			public String getLabel() {
				return "TC4Tweak signing signature";
			}

			@Override
			public String call() {
				try {
					Certificate certificate = Loader.instance().getIndexedModList().get(MOD_ID).getSigningCertificate();
					if (certificate == null)
						return "None. Do not bother glease for this crash!";
					String fingerprint = CertificateHelper.getFingerprint(certificate);
					// everyone can tamper the manifest and add a Built-By,
					// not so for signatures
					return fingerprint + ", Built by: " + KNOWN_SIGNATURE.getOrDefault(fingerprint, "Not known");
				} catch (Exception e) {
					StringWriter sw = new StringWriter();
					sw.append("Cannot determine due to error: ");
					e.printStackTrace(new PrintWriter(sw));
					return sw.toString();
				}
			}
		});
	}

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent e) {
		proxy.preInit(e);
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent e) {
		proxy.init(e);
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent e) {
		proxy.postInit(e);
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
