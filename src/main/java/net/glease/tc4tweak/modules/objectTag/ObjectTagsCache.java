package net.glease.tc4tweak.modules.objectTag;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.glease.tc4tweak.modules.FlushableCache;
import net.minecraft.item.Item;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.AspectList;

class ObjectTagsCache extends FlushableCache<ConcurrentMap<Item, TIntObjectMap<AspectList>>> {

    private static TIntObjectMap<AspectList> bakeSubmap(@SuppressWarnings("rawtypes") Map.Entry<List, AspectList> e) {
        TIntObjectMap<AspectList> submap = new TIntObjectHashMap<>();
        Object o = e.getKey().get(1);
        if (o instanceof Integer) {
            submap.put((Integer) o, e.getValue());
        } else if (o instanceof int[]) {
            int[] metas = (int[]) o;
            for (int meta : metas) {
                submap.put(meta, e.getValue());
            }
        } else {
            GetObjectTags.log.error("Unrecognized key in objectTags map! {}", e.getKey());
        }
        return submap;
    }

    @Override
    public ConcurrentMap<Item, TIntObjectMap<AspectList>> createCache() {
        return ThaumcraftApi.objectTags.entrySet().parallelStream()
                .collect(Collectors.toConcurrentMap(
                        e -> (Item) e.getKey().get(0),
                        ObjectTagsCache::bakeSubmap,
                        FlushableCache::mergeTIntObjectMap
                ));
    }
}
