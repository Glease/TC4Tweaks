package net.glease.tc4tweak.asm;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import net.minecraft.launchwrapper.Launch;
import org.apache.commons.io.FileUtils;

import static net.glease.tc4tweak.asm.TC4Transformer.log;

@IFMLLoadingPlugin.TransformerExclusions("net.glease.tc4tweak.asm")
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.Name("TC4TweakCoreMod")
@IFMLLoadingPlugin.SortingIndex(2000)
public class LoadingPlugin implements IFMLLoadingPlugin {
    private static final boolean DEBUG = Boolean.getBoolean("glease.debugasm");
    static boolean dev;
    static boolean gt6;
    static boolean hodgepodge;
    private static File debugOutputDir;

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{"net.glease.tc4tweak.asm.TC4Transformer"};
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        if (!TC4Transformer.initialized) {
            throw new IllegalStateException("TC4Tweaks class transformer failed to be initialized");
        }
        dev = !(boolean) data.get("runtimeDeobfuscationEnabled");
        gt6 = ((List<?>) data.get("coremodList")).stream().anyMatch(o -> o.toString().contains("Greg-ASM"));
        hodgepodge = ((List<?>) data.get("coremodList")).stream().anyMatch(o -> o.toString().toLowerCase(Locale.ROOT).contains("hodgepodge"));
        if (((List<?>) data.get("coremodList")).stream().anyMatch(o -> o.toString().contains("BTPlugin"))) {
            String errorMessage = "Remove NotEnoughThaumcraftTabs. TC4Tweaks now comes with the same functionality and is incompatible with it.";
            if (!GraphicsEnvironment.isHeadless())
                JOptionPane.showMessageDialog(null, errorMessage);
            log.error("#################################################################################");
            log.error("#################################################################################");
            log.error("#################################################################################");
            log.error(errorMessage);
            log.error(errorMessage);
            log.error(errorMessage);
            log.error("#################################################################################");
            log.error("#################################################################################");
            log.error("#################################################################################");
            throw new RuntimeException(errorMessage);
        }
        if (isDebug()) getDebugOutputDir();
        // mixingasm (or mods that include it) compat
        markTransformersSafe(data);
    }

    private void markTransformersSafe(Map<String, Object> data) {
        // my transformers are idempotent, as it should be
        @SuppressWarnings("unchecked")
        List<String> list = (List<String>) data.computeIfAbsent("mixingasm.transformerInclusionList", k -> new ArrayList<>());
        list.addAll(Arrays.asList(getASMTransformerClass()));
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    public static boolean isDev() {
        return dev;
    }

    public static boolean isDebug() {
        return DEBUG;
    }

    public static File getDebugOutputDir() {
        if (debugOutputDir == null) {
            debugOutputDir = new File(Launch.minecraftHome, ".asm");
            try {
                FileUtils.deleteDirectory(debugOutputDir);
            } catch (IOException ignored) {}
            if (!debugOutputDir.exists()) {
                // noinspection ResultOfMethodCallIgnored
                debugOutputDir.mkdirs();
            }
        }
        return debugOutputDir;
    }
}
