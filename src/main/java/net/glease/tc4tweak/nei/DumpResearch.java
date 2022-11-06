package net.glease.tc4tweak.nei;

import codechicken.nei.config.DataDumper;
import net.glease.tc4tweak.CommonUtils;
import net.glease.tc4tweak.modules.getResearch.GetResearch;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;
import thaumcraft.api.aspects.Aspect;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

public class DumpResearch extends DataDumper {
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
    public IChatComponent dumpMessage(File file) {
        ChatComponentTranslation msg = new ChatComponentTranslation("nei.options.tools.dump.tc4tweaks.tc4research.dumped", "dumps/" + file.getName());
        try {
            return msg.setChatStyle(new ChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getCanonicalPath())).setUnderlined(true));
        } catch (Exception ex) {
            return msg.appendSibling(new ChatComponentText("Error preparing chat message: " + ex.getLocalizedMessage()));
        }
    }

    @Override
    public int modeCount() {
        return 1;
    }
}
