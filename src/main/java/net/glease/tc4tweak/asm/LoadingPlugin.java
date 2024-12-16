package net.glease.tc4tweak.asm;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

import static net.glease.tc4tweak.asm.TC4Transformer.log;

@IFMLLoadingPlugin.TransformerExclusions("net.glease.tc4tweak.asm")
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.Name("TC4TweakCoreMod")
@IFMLLoadingPlugin.SortingIndex(2000)
public class LoadingPlugin implements IFMLLoadingPlugin {
    static boolean dev;
    static boolean gt6;
    static boolean hodgepodge;
    static File debugOutputDir;

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
        debugOutputDir = new File((File) data.get("mcLocation"), ".asm");
        //noinspection ResultOfMethodCallIgnored
        debugOutputDir.mkdir();
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
}
