package net.glease.tc4tweak.asm;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ASMCallhookServer {
	private static final int DEFAULT_NAMESPACE_HASH_BASE = "oops:".hashCode()  * 31;
	// getResearch speed up
	private static Map<String, ResearchItem> researchItemMap = null;

	/**
	 * Called from {@link ResearchCategories#getResearch(String)}
	 */
	@Callhook
	public static ResearchItem getResearch(String key) {
		final Map<String, ResearchItem> map = ASMCallhookServer.researchItemMap;
		return map == null ? getResearchSlow(key) : map.get(key);
	}

	public static void preventAllCache() {
		researchItemMap = null;
	}

	public static void flushAllCache() {
		researchItemMap = ResearchCategories.researchCategories.values().stream()
				.flatMap(l -> l.research.values().stream())
				.collect(Collectors.toMap(
						i -> i.key,
						Function.identity(),
						(u, v) -> u,
						LinkedHashMap::new
				));
	}

	/**
	 * Fallback for when server starting
	 * even though labeled as slow, it's still faster due to being parallel
	 */
	private static ResearchItem getResearchSlow(String key) {
		return ResearchCategories.researchCategories.values().stream()
				.flatMap(l -> l.research.values().stream())
				.filter(i -> i.key.equals(key))
				.findFirst().orElse(null);
	}

	/**
	 * Replacement of {@link thaumcraft.common.lib.research.ScanManager#generateItemHash(Item, int)}.
	 * Basically remove all string creation. Blocks of logic is rearranged to minimize unnecessary hash generation
	 * The returned hash code should be the same.
	 */
	@Callhook
	public static int generateItemHash(Item item, int meta) {
		ItemStack t = new ItemStack(item, 1, meta);

		try {
			if (t.isItemStackDamageable() || !t.getHasSubtypes()) {
				meta = -1;
			}
		} catch (Exception ignored) {
		}

		final int[] value = ThaumcraftApi.groupedObjectTags.get(Arrays.asList(item, meta));
		if (value != null) {
			meta = value[0];
		}

		if (ThaumcraftApi.objectTags.containsKey(Arrays.asList(item, meta))) {
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

					for (int r : range) {
						hash = hash * 31 * 31 + ':' * 31 + r;
					}

					return hash;
				}
			}
		}

		if (meta != -1 || ThaumcraftApi.objectTags.containsKey(Arrays.asList(item, -1)))
			return hash(item, meta, t);


		for (int i = 0; i < 16; i++) {
			if (ThaumcraftApi.objectTags.containsKey(Arrays.asList(item, i)))
				return hash(item, i, t);
		}
		return hash(item, meta, t);
	}

	private static int hash(Item item, int meta, ItemStack t) {
		try {
			int hash;
			GameRegistry.UniqueIdentifier ui = GameRegistry.findUniqueIdentifierFor(item);
			if (ui != null) {
				hash = ui.modId.hashCode() * 31 * 31 * 31 * 31 + ':' * 31 * 31 * 31 + ui.name.hashCode() * 31 * 31 + ':' * 31 + meta;
			} else {
				hash = t.getUnlocalizedName().hashCode() * 31 * 31 + ':' * 31 + meta;
			}
			return hash;
		} catch (Exception e) {
			return DEFAULT_NAMESPACE_HASH_BASE + meta;
		}
	}
}
