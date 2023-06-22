package net.glease.tc4tweak.modules.generateItemHash;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;

public class GenerateItemHash {
    private static final String DEFAULT_NAMESPACE = "oops:";
    private static final int DEFAULT_NAMESPACE_HASH_BASE = DEFAULT_NAMESPACE.hashCode() * 31;
    private static final ConcurrentHashMap<List<?>, int[]> groupedObjectTags;
    private static final CustomItemStacks customItemStacksCache = new CustomItemStacks();
    private static final RangedObjectTags rangedObjectTags = new RangedObjectTags();
    private static final ThreadLocal<StringBuilder> buffer = ThreadLocal.withInitial(StringBuilder::new);

    static {
        ConcurrentHashMap<List<?>, int[]> tmp;
        try {
            tmp = ReflectionHelper.getPrivateValue(ThaumcraftApi.class, null, "groupedObjectTags");
        } catch (Exception e) {
            tmp = new ConcurrentHashMap<>();
        }
        groupedObjectTags = tmp;
    }

    /**
     * give hash as if the whole thing was a string: {@code (earlierString+":").hashCode()}
     *
     * @param hash   {@code earlierString.hashCode()}
     */
    private static int updateHashColon(int hash) {
        return hash * 31 + ':';
    }

    /**
     * give hash as if the whole thing was a string: {@code (earlierString+Integer.toString(number)).hashCode()}
     *
     * @param hash   {@code earlierString.hashCode()}
     * @param number the number part
     */
    private static int updateHash(int hash, int number) {
        // AbstractStringBuilder.append(int) is the only way to call into JDK's optimized int toString implementation
        // using a mutable buffer we control.
        // Integer.getChars() requires allocation and I hate it.
        // nasty, but it improves performance by up to 33% per JMH tests
        // not to mention the GC savings
        StringBuilder buffer = GenerateItemHash.buffer.get();
        buffer.delete(0, buffer.length());
        buffer.append(number);
        return updateHash(hash, buffer);
    }

    /**
     * give hash as if the whole thing was a string: {@code (earlierString+later).hashCode()}
     *
     * @param hash  {@code earlierString.hashCode()}
     * @param later the later part
     */
    private static int updateHash(int hash, CharSequence later) {
        // this has to be done this way to preserve integer overflow behavior
        int length = later.length();
        for (int i = 0; i < length; i++) {
            hash = hash * 31 + later.charAt(i);
        }
        return hash;
    }

    /**
     * Replacement of {@link thaumcraft.common.lib.research.ScanManager#generateItemHash(Item, int)}.
     * Basically remove all string creation. Blocks of logic is rearranged to minimize unnecessary hash generation
     * The returned hash code should be the same.
     * <p>
     * ALERT!!!
     * This hashCode is persisted across server restarts. Changing it means all scanned data will be lost!
     */
    public static int generateItemHash(Item item, int meta) {
        ItemStack t = new ItemStack(item, 1, meta);

        try {
            if (t.isItemStackDamageable() || !t.getHasSubtypes()) {
                meta = -1;
            }
        } catch (Exception ignored) {
        }

        List<Object> key = Arrays.asList(item, meta);
        final int[] value = groupedObjectTags.get(key);
        if (value != null) {
            meta = value[0];
        }

        key.set(1, meta);
        if (ThaumcraftApi.objectTags.containsKey(key)) {
            return hash(item, meta, t);
        }

        // NOTE!! This block does not function 100% the same as vanilla TC4, but this SHOULD be the correct way to handle
        // it.
        // nobody uses ranged object tag specification anyway
        if (rangedObjectTags.isEnabled()) {
            List<int[]> ints = rangedObjectTags.getCache().get(item);
            if (ints != null) {
                for (int[] range : ints) {
                    Arrays.sort(range);
                    if (Arrays.binarySearch(range, meta) >= 0) {
                        return hash(item, t, range);
                    }
                }
            }
        } else {
            for (List<?> l : ThaumcraftApi.objectTags.keySet()) {
                String name = ((Item) l.get(0)).getUnlocalizedName();
                // this is probably not correct, but vanilla TC4 does it.
                if (l.get(1) instanceof int[] && (Item.itemRegistry.getObject(name) == item || Block.blockRegistry.getObject(name) == Block.getBlockFromItem(item))) {
                    int[] range = (int[]) l.get(1);
                    Arrays.sort(range);
                    if (Arrays.binarySearch(range, meta) >= 0) {
                        return hash(item, t, range);
                    }
                }
            }
        }

        if (meta == -1) {
            for (int i = 0; i < 16; i++) {
                key.set(1, i);
                if (ThaumcraftApi.objectTags.containsKey(key))
                    return hash(item, i, t);
            }
        }
        return hash(item, meta, t);
    }

    private static int hash(Item item, ItemStack t, int[] range) {
        int hash = getUniqueIdentifierHash(item, t);

        for (int r : range) {
            hash = updateHash(updateHashColon(hash), r);
        }

        return hash;
    }

    private static int hash(Item item, int meta, ItemStack t) {
        return updateHash(updateHashColon(getUniqueIdentifierHash(item, t)), meta);
    }

    private static int getUniqueIdentifierHash(Item item, ItemStack t) {
        if (customItemStacksCache.isEnabled()) {
            String name = Item.itemRegistry.getNameForObject(item);
            if (name == null)
                return DEFAULT_NAMESPACE_HASH_BASE;
            if (customItemStacksCache.getCache().contains(name))
                return t.getUnlocalizedName().hashCode();
            return name.hashCode();
        } else {
            try {
                GameRegistry.UniqueIdentifier ui = GameRegistry.findUniqueIdentifierFor(item);
                if (ui == null)
                    return t.getUnlocalizedName().hashCode();
                return updateHash(updateHashColon(ui.modId.hashCode()), ui.name);
            } catch (Exception e) {
                return DEFAULT_NAMESPACE_HASH_BASE;
            }
        }
    }

    public static void onNewObjectTag(List<?> key) {
        if (key.get(1) instanceof int[] && rangedObjectTags.isEnabled()) {
            rangedObjectTags.getCache().merge((Item) key.get(0), Collections.singletonList((int[]) key.get(1)), (a, b) -> Stream.concat(a.stream(), b.stream()).collect(Collectors.toList()));
        }
    }

    public static void onRemoveObjectTag(List<?> key) {
        if (key.get(1) instanceof int[] && rangedObjectTags.isEnabled()) {
            rangedObjectTags.getCache()
                    .computeIfPresent((Item) key.get(0), (k, a) -> toNullIfEmpty(a.stream().filter(arr -> !Arrays.equals(arr, (int[]) key.get(1))).collect(Collectors.toList())));
        }
    }

    private static <C extends Collection<?>> C toNullIfEmpty(C c) {
        if (c == null || c.isEmpty())
            return null;
        return c;
    }
}
