package net.glease.tc4tweak;

import java.util.List;
import java.util.stream.Collectors;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;

public class GuiModConfig extends GuiConfig {
    public GuiModConfig(GuiScreen guiScreen) {
        super(guiScreen, getConfigElements(), TC4Tweak.MOD_ID, false, false, GuiConfig.getAbridgedConfigPath(ConfigurationHandler.INSTANCE.getConfig().toString()));
    }

    @SuppressWarnings("rawtypes")
    private static List<IConfigElement> getConfigElements() {
        final Configuration config = ConfigurationHandler.INSTANCE.getConfig();
        return config.getCategoryNames().stream()
                .filter(name -> name.indexOf('.') == -1)
                .map(name -> new ConfigElement(config.getCategory(name)))
                .collect(Collectors.toList());
    }
}
