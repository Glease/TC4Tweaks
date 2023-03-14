package net.glease.tc4tweak.modules.generateItemHash

import cpw.mods.fml.common.registry.GameRegistry
import cpw.mods.fml.relauncher.ReflectionHelper
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import thaumcraft.api.ThaumcraftApi
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors
import java.util.stream.Stream

object GenerateItemHash {
    private const val DEFAULT_NAMESPACE = "oops:"
    private val DEFAULT_NAMESPACE_HASH_BASE = DEFAULT_NAMESPACE.hashCode() * 31
    private val groupedObjectTags: ConcurrentHashMap<List<*>, IntArray> = try {
        ReflectionHelper.getPrivateValue<ConcurrentHashMap<List<*>, IntArray>, ThaumcraftApi>(
            ThaumcraftApi::class.java, null, "groupedObjectTags"
        )
    } catch (e: Exception) {
        ConcurrentHashMap()
    }
    private val customItemStacksCache = CustomItemStacks()
    private val rangedObjectTags = RangedObjectTags()
    private val buffer = ThreadLocal.withInitial { StringBuilder() }

    /**
     * give hash as if the whole thing was a string: `(earlierString+":").hashCode()`
     *
     * @param hash   `earlierString.hashCode()`
     */
    private fun updateHashColon(hash: Int): Int {
        return hash * 31 + ':'.toInt()
    }

    /**
     * give hash as if the whole thing was a string: `(earlierString+Integer.toString(number)).hashCode()`
     *
     * @param hash   `earlierString.hashCode()`
     * @param number the number part
     */
    private fun updateHash(hash: Int, number: Int): Int {
        // AbstractStringBuilder.append(int) is the only way to call into JDK's optimized int toString implementation
        // using a mutable buffer we control.
        // Integer.getChars() requires allocation and I hate it.
        // nasty, but it improves performance by up to 33% per JMH tests
        // not to mention the GC savings
        val buffer = buffer.get()
        buffer.delete(0, buffer.length)
        buffer.append(number)
        return updateHash(hash, buffer)
    }

    /**
     * give hash as if the whole thing was a string: `(earlierString+later).hashCode()`
     *
     * @param hash  `earlierString.hashCode()`
     * @param later the later part
     */
    private fun updateHash(hash: Int, later: CharSequence): Int {
        // this has to be done this way to preserve integer overflow behavior
        var hash = hash
        val length = later.length
        for (i in 0 until length) {
            hash = hash * 31 + later[i].toInt()
        }
        return hash
    }

    /**
     * Replacement of [thaumcraft.common.lib.research.ScanManager.generateItemHash].
     * Basically remove all string creation. Blocks of logic is rearranged to minimize unnecessary hash generation
     * The returned hash code should be the same.
     *
     *
     * ALERT!!!
     * This hashCode is persisted across server restarts. Changing it means all scanned data will be lost!
     */
    @JvmStatic
    fun generateItemHash(item: Item, meta: Int): Int {
        var meta = meta
        val t = ItemStack(item, 1, meta)
        try {
            if (t.isItemStackDamageable || !t.hasSubtypes) {
                meta = -1
            }
        } catch (ignored: Exception) {
        }
        val key = Arrays.asList(item, meta)
        val value = groupedObjectTags[key]
        if (value != null) {
            meta = value[0]
        }
        key[1] = meta
        if (ThaumcraftApi.objectTags.containsKey(key)) {
            return hash(item, meta, t)
        }

        // NOTE!! This block does not function 100% the same as vanilla TC4, but this SHOULD be the correct way to handle
        // it.
        // nobody uses ranged object tag specification anyway
        if (rangedObjectTags.isEnabled) {
            val ints = rangedObjectTags.cache[item]
            if (ints != null) {
                for (range in ints) {
                    Arrays.sort(range)
                    if (Arrays.binarySearch(range, meta) >= 0) {
                        return hash(item, t, range)
                    }
                }
            }
        } else {
            for (l in ThaumcraftApi.objectTags.keys) {
                val name = (l[0] as Item).unlocalizedName
                // this is probably not correct, but vanilla TC4 does it.
                if (l[1] is IntArray && (Item.itemRegistry.getObject(name) === item || Block.blockRegistry.getObject(
                        name
                    ) === Block.getBlockFromItem(item))
                ) {
                    val range = l[1] as IntArray
                    Arrays.sort(range)
                    if (Arrays.binarySearch(range, meta) >= 0) {
                        return hash(item, t, range)
                    }
                }
            }
        }
        if (meta == -1) {
            for (i in 0..15) {
                key[1] = i
                if (ThaumcraftApi.objectTags.containsKey(key)) return hash(item, i, t)
            }
        }
        return hash(item, meta, t)
    }

    private fun hash(item: Item, t: ItemStack, range: IntArray): Int {
        var hash = getUniqueIdentifierHash(item, t)
        for (r in range) {
            hash = updateHash(updateHashColon(hash), r)
        }
        return hash
    }

    private fun hash(item: Item, meta: Int, t: ItemStack): Int {
        return updateHash(updateHashColon(getUniqueIdentifierHash(item, t)), meta)
    }

    private fun getUniqueIdentifierHash(item: Item, t: ItemStack): Int {
        return if (customItemStacksCache.isEnabled) {
            val name = Item.itemRegistry.getNameForObject(item)
                ?: return DEFAULT_NAMESPACE_HASH_BASE
            if (customItemStacksCache.cache.contains(name)) t.unlocalizedName.hashCode() else name.hashCode()
        } else {
            try {
                val ui = GameRegistry.findUniqueIdentifierFor(item) ?: return t.unlocalizedName.hashCode()
                updateHash(updateHashColon(ui.modId.hashCode()), ui.name)
            } catch (e: Exception) {
                DEFAULT_NAMESPACE_HASH_BASE
            }
        }
    }

    @JvmStatic
    fun onNewObjectTag(key: List<*>) {
        if (key[1] is IntArray && rangedObjectTags.isEnabled) {
            rangedObjectTags.cache.merge(key[0] as Item?, listOf(key[1] as IntArray)) { a, b ->
                Stream.concat(a.stream(), b.stream()).collect(Collectors.toList())
            }
        }
    }

    @JvmStatic
    fun onRemoveObjectTag(key: List<*>) {
        if (key[1] is IntArray && rangedObjectTags.isEnabled) {
            rangedObjectTags.cache
                .computeIfPresent(key[0] as Item?) { _, a -> toNullIfEmpty(a.stream().filter { arr -> !Arrays.equals(arr, key[1] as IntArray?) }.collect(Collectors.toList()))
                }
        }
    }

    private fun <C : Collection<*>?> toNullIfEmpty(c: C): C? {
        return if (c.isNullOrEmpty()) null else c
    }
}