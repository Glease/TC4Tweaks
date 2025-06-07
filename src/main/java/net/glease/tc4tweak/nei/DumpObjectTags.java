package net.glease.tc4tweak.nei;

import net.glease.tc4tweak.CommonUtils;
import net.glease.tc4tweak.modules.objectTag.GetObjectTags;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class DumpObjectTags extends TC4TweaksDataDump {
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
                        safeGetDisplayName(e.getKey()),
                        e.getKey().getUnlocalizedName(),
                        String.valueOf(Item.getIdFromItem(e.getKey().getItem())),
                        String.valueOf(Items.feather.getDamage(e.getKey())),
                        CommonUtils.toString(e.getValue())
                }).iterator();
    }

    private static String safeGetDisplayName(ItemStack is) {
        try {
            return is.getDisplayName();
        } catch (Exception e) {
            return "~~ERROR~~";
        }
    }

    @Override
    public int modeCount() {
        return 1;
    }
}
