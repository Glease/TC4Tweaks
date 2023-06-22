package net.glease.tc4tweak.modules.generateItemHash;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Table;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.glease.tc4tweak.modules.FlushableCache;
import net.minecraft.item.ItemStack;

public class CustomItemStacks extends FlushableCache<Set<String>> {
    @Override
    protected Set<String> createCache() {
        try {
            Table<String, String, ItemStack> tmp = ReflectionHelper.getPrivateValue(GameData.class, null, "customItemStacks");
            return tmp.rowMap().entrySet().stream()
                    .flatMap(e -> e.getValue().keySet().stream().map(c -> e.getKey() + c))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }
}
