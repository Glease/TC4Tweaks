package net.glease.tc4tweak.modules.generateItemHash;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import net.glease.tc4tweak.modules.FlushableCache;
import net.minecraft.item.Item;
import thaumcraft.api.ThaumcraftApi;

import static java.util.stream.Collectors.*;

class RangedObjectTags extends FlushableCache<ConcurrentMap<Item, List<int[]>>> {
    @Override
    protected ConcurrentMap<Item, List<int[]>> createCache() {
        @SuppressWarnings({"unchecked", "rawtypes"})
        Set<List<?>> keys = (Set) ThaumcraftApi.objectTags.keySet();
        return keys.parallelStream()
                .filter(l -> l.get(1) instanceof int[])
                .collect(groupingByConcurrent(l -> (Item) l.get(0), mapping(l -> (int[]) l.get(1), toList())));
    }
}
