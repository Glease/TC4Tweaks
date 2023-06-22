package net.glease.tc4tweak.nei;

import java.util.Arrays;
import java.util.stream.Collectors;

import net.glease.tc4tweak.CommonUtils;
import net.glease.tc4tweak.modules.getResearch.GetResearch;
import thaumcraft.api.aspects.Aspect;

public class DumpResearch extends TC4TweaksDataDump {
    public DumpResearch() {
        super("tools.dump.tc4tweaks.tc4research");
    }

    @Override
    public String[] header() {
        return new String[]{"Category", "Name", "Key", "Parents", "ParentsHidden", "Siblings", "Tag", "ItemTrigger", "EntityTrigger", "AspectTrigger"};
    }

    @Override
    public Iterable<String[]> dump(int mode) {
        return () -> GetResearch.stream()
                .map(i -> new String[]{
                        i.category,
                        i.getName(),
                        i.key,
                        toString(i.parents),
                        toString(i.parentsHidden),
                        toString(i.siblings),
                        CommonUtils.toString(i.tags),
                        toString(i.getItemTriggers()),
                        toString(i.getEntityTriggers()),
                        toString(i.getAspectTriggers()),
                }).iterator();
    }

    private static String toString(String[] arr) {
        if (arr == null)
            return "";
        return String.join(";", arr);
    }

    private static String toString(Aspect[] arr) {
        if (arr == null)
            return "";
        return Arrays.stream(arr).map(Aspect::getName).collect(Collectors.joining(";"));
    }

    private static String toString(Object[] arr) {
        if (arr == null)
            return "";
        return Arrays.stream(arr).map(Object::toString).collect(Collectors.joining(";"));
    }

    @Override
    public String renderName() {
        return translateN(name);
    }

    @Override
    public int modeCount() {
        return 1;
    }
}
