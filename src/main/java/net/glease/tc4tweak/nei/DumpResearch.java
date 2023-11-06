package net.glease.tc4tweak.nei;

import java.util.Arrays;
import java.util.stream.Collectors;

import net.glease.tc4tweak.CommonUtils;
import net.glease.tc4tweak.modules.getResearch.GetResearch;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ResearchItem;

public class DumpResearch extends TC4TweaksDataDump {
    public DumpResearch() {
        super("tools.dump.tc4tweaks.tc4research");
    }

    @Override
    public String[] header() {
        return new String[]{"Category", "Name", "Key", "Parents", "ParentsHidden", "Siblings", "PermWarp", "StickyWarp", "Tag", "ItemTrigger", "EntityTrigger", "AspectTrigger"};
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
                        String.valueOf(getPermWarp(i)),
                        String.valueOf(getStickyWarp(i)),
                        CommonUtils.toString(i.tags),
                        toString(i.getItemTriggers()),
                        toString(i.getEntityTriggers()),
                        toString(i.getAspectTriggers()),
                }).iterator();
    }

    private static int getPermWarp(ResearchItem i) {
        int warp = ThaumcraftApi.getWarp(i.key);
        if (warp <= 1)
            return warp;
        return Math.max(warp - warp / 2, 0);
    }

    private static int getStickyWarp(ResearchItem i) {
        int warp = ThaumcraftApi.getWarp(i.key);
        if (warp <= 1)
            return warp;
        return Math.max(warp / 2, 0);
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
