package net.glease.tc4tweak.nei;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import net.glease.tc4tweak.TC4Tweak;

public class NEIConfig implements IConfigureNEI {
    @Override
    public void loadConfig() {
        API.addOption(new DumpObjectTags());
        API.addOption(new DumpResearch());
        API.addOption(new DumpResearchCategories());
    }

    @Override
    public String getName() {
        return TC4Tweak.MOD_ID;
    }

    @Override
    public String getVersion() {
        return TC4Tweak.VERSION;
    }
}
