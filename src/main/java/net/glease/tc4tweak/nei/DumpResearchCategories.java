package net.glease.tc4tweak.nei;

import thaumcraft.api.research.ResearchCategories;

public class DumpResearchCategories extends TC4TweaksDataDump {
    public DumpResearchCategories() {
        super("tools.dump.tc4tweaks.tc4researchtab");
    }

    @Override
    public String[] header() {
        if (getTag().getIntValue(DEFAULT_MODE) == 0) {
            return new String[]{"Name", "Icon", "Background"};
        } else {
            return new String[] {"Name"};
        }
    }

    @Override
    public Iterable<String[]> dump(int mode) {
        if (getTag().getIntValue() == 0) {
            return () -> ResearchCategories.researchCategories.entrySet().stream()
                    .map(e -> new String[] {
                            e.getKey(),
                            e.getValue().icon.toString(),
                            e.getValue().background.toString()
                    }).iterator();
        } else {
            return () -> ResearchCategories.researchCategories.keySet().stream()
                    .map(s -> new String[] {
                            s
                    }).iterator();
        }
    }

    @Override
    public String renderName() {
        return translateN(name);
    }

    @Override
    public int modeCount() {
        return 2;
    }
}
