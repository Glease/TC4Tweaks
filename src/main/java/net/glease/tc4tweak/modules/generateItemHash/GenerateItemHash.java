package net.glease.tc4tweak.modules.generateItemHash;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class GenerateItemHash {
    private static final int DEFAULT_NAMESPACE_HASH_BASE = "oops:".hashCode() * 31;
    private static final ConcurrentHashMap<List<?>, int[]> groupedObjectTags;

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
     * give hash as if the whole thing was a string: {@code (earlierString+Integer.toString(number)).hashCode()}
     *
     * @param hash   {@code earlierString.hashCode()}
     * @param number the number part
     */
    private static int updateHash(int hash, int number) {
        return updateHash(hash, Integer.toString(number));
    }

    /**
     * give hash as if the whole thing was a string: {@code (earlierString+later).hashCode()}
     *
     * @param hash  {@code earlierString.hashCode()}
     * @param later the later part
     */
    private static int updateHash(int hash, String later) {
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

        for (List<?> l : ThaumcraftApi.objectTags.keySet()) {
            String name = ((Item) l.get(0)).getUnlocalizedName();
            if ((Item.itemRegistry.getObject(name) == item || Block.blockRegistry.getObject(name) == Block.getBlockFromItem(item)) && l.get(1) instanceof int[]) {
                int[] range = (int[]) l.get(1);
                Arrays.sort(range);
                if (Arrays.binarySearch(range, meta) >= 0) {
                    GameRegistry.UniqueIdentifier ui = GameRegistry.findUniqueIdentifierFor(item);
                    int hash = ui != null ? ui.hashCode() : t.getUnlocalizedName().hashCode();
                    hash = hash * 31 + ':';

                    for (int r : range) {
                        hash = updateHash(hash, r);
                    }

                    return hash;
                }
            }
        }

        key.set(1, -1);
        if (meta == -1 && !ThaumcraftApi.objectTags.containsKey(key)) {
            for (int i = 0; i < 16; i++) {
                key.set(1, i);
                if (ThaumcraftApi.objectTags.containsKey(key))
                    return hash(item, i, t);
            }
        }
        return hash(item, meta, t);
    }

    private static int hash(Item item, int meta, ItemStack t) {
        try {
            GameRegistry.UniqueIdentifier ui = GameRegistry.findUniqueIdentifierFor(item);
            // this has be done this way to preserve integer overflow behavior
            return updateHash(ui != null ? updateHash(ui.modId.hashCode() * 31 + ':', ui.name) * 31 + ':' : t.getUnlocalizedName().hashCode() * 31 + ':', meta);
        } catch (Exception e) {
            return DEFAULT_NAMESPACE_HASH_BASE + meta;
        }
    }
}
