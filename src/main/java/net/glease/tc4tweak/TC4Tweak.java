package net.glease.tc4tweak;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

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
import net.glease.tc4tweak.asm.LoadingPlugin;
import net.glease.tc4tweak.network.MessageSendConfiguration;
import net.glease.tc4tweak.network.MessageSendConfigurationV2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = TC4Tweak.MOD_ID, name = "TC4 Tweak", version = TC4Tweak.VERSION, dependencies = "required-after:Thaumcraft", guiFactory = "net.glease.tc4tweak.GuiFactory")
public class TC4Tweak {
    public static final String MOD_ID = "tc4tweak";
    public static final String VERSION = "${version}";
    public static final Logger log = LogManager.getLogger("TC4Tweaks");
    private static final VersionRange ACCEPTED_CLIENT_VERSION = VersionParser.parseRange("[1.4.27,2)");
    private static final ImmutableMap<String, String> KNOWN_SIGNATURE = ImmutableMap.<String, String>builder()
            .put("473C3A397676978FF4877ABA2D57860DDA20E2FC", "glease")
            .put("004227A857B097EDE4E36FACB9B5491BC9808464", "glease")
            .build();
    @Mod.Instance
    public static TC4Tweak INSTANCE;
    @SidedProxy(serverSide = "net.glease.tc4tweak.CommonProxy", clientSide = "net.glease.tc4tweak.ClientProxy")
    static CommonProxy proxy;
    public final SimpleNetworkWrapper CHANNEL = new SimpleNetworkWrapper(MOD_ID);
    private boolean allowAll = true;

    public TC4Tweak() {
        FMLCommonHandler.instance().registerCrashCallable(new ICrashCallable() {
            @Override
            public String getLabel() {
                return "TC4Tweak signing signature";
            }

            @Override
            public String call() {
                return getFingerprintDescriptions();
            }
        });
    }

    private String getFingerprintDescriptions() {
        try {
            Certificate[] certificates = LoadingPlugin.class.getProtectionDomain().getCodeSource().getCertificates();
            if (certificates == null || certificates.length == 0)
                return "None. Do not bother glease for this crash!";
            // everyone can tamper the manifest and add a Built-By or sign a jar,
            // not so for signatures
            return Arrays.stream(certificates).map(c -> CertificateHelper.getFingerprint(c).toUpperCase(Locale.ROOT))
                    .map(f -> f.replace(".", "") + ", Built by: " + KNOWN_SIGNATURE.getOrDefault(f, "Not known"))
                    .collect(Collectors.joining("; "));
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            sw.append("Cannot determine due to error: ");
            e.printStackTrace(new PrintWriter(sw));
            return sw.toString();
        }
    }

    void detectAndSendConfigChanges() {
        if (FMLCommonHandler.instance().getMinecraftServerInstance() != null) {
            INSTANCE.CHANNEL.sendToAll(new MessageSendConfiguration());
            INSTANCE.CHANNEL.sendToAll(new MessageSendConfigurationV2());
        }
    }

    void setAllowAll(boolean allowAll) {
        this.allowAll = allowAll;
    }

    public boolean isAllowAll() {
        return allowAll;
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
