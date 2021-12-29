package net.glease.tc4tweak.nei;

import codechicken.nei.config.DataDumper;
import net.glease.tc4tweak.modules.objectTag.GetObjectTags;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import thaumcraft.api.aspects.AspectList;

import java.io.File;
import java.util.stream.Collectors;

public class DumpObjectTags extends DataDumper {
    public DumpObjectTags() {
        super("tools.dump.tc4tweaks.tc4tag");
    }

    @Override
    public String[] header() {
        return new String[]{"ItemStack Display Name", "Item Unlocalized Name", "Item Numerical ID", "Meta", "Aspect List"};
    }

    @Override
    public Iterable<String[]> dump(int mode) {
        return () -> GetObjectTags.stream()
                .filter(e -> e.getKey().getItem() != null && e.getValue() != null)
                .map(e -> new String[]{
                        e.getKey().getDisplayName(),
                        e.getKey().getItem() == null ? "BROKEN ITEM" : e.getKey().getItem().getUnlocalizedName(),
                        String.valueOf(Item.getIdFromItem(e.getKey().getItem())),
                        String.valueOf(Items.feather.getDamage(e.getKey())),
                        toString(e.getValue())
                }).iterator();
    }

    private static String toString(AspectList al) {
        return al.aspects.entrySet().stream().filter(e -> e.getKey() != null && e.getValue() != null).map(e -> String.format("%dx%s", e.getValue(), e.getKey().getName())).collect(Collectors.joining(";"));
    }

    @Override
    public IChatComponent dumpMessage(File file) {
        return new ChatComponentTranslation("nei.options.tools.dump.tc4tweaks.tc4tags.dumped", "dumps/" + file.getName());
    }

    @Override
    public int modeCount() {
        return 1;
    }
}
