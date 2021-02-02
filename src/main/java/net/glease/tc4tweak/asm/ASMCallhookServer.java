package net.glease.tc4tweak.asm;

import cpw.mods.fml.common.registry.GameRegistry;
import net.glease.tc4tweak.ConfigurationHandler;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.common.container.ContainerDummy;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.tiles.TileArcaneWorkbench;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ASMCallhookServer {
	private static final int DEFAULT_NAMESPACE_HASH_BASE = "oops:".hashCode() * 31;
	// getResearch speed up
	private static Map<String, ResearchItem> researchItemMap = null;
	// arcane crafting recipe speed up
	// thread local to make integrated server happy
	private static ThreadLocal<LinkedList<IArcaneRecipe>> arcaneCraftingHistory = null;
	private static final int ARCANE_CRAFTING_HISTORY_SIZE = 256;

	private static IArcaneRecipe findArcaneRecipe(IInventory inv, EntityPlayer player) {
		if (arcaneCraftingHistory != null) {
			LinkedList<IArcaneRecipe> history = arcaneCraftingHistory.get();
			for (Iterator<IArcaneRecipe> iterator = history.iterator(); iterator.hasNext(); ) {
				IArcaneRecipe recipe = iterator.next();
				if (recipe.matches(inv, player.worldObj, player)) {
					iterator.remove();
					history.addFirst(recipe);
					return recipe;
				}
			}
		}
		IArcaneRecipe recipe = ((List<?>) ThaumcraftApi.getCraftingRecipes()).parallelStream()
				.filter(o -> o instanceof IArcaneRecipe && ((IArcaneRecipe) o).matches(inv, player.worldObj, player))
				.map(o -> (IArcaneRecipe) o)
				.findFirst()
				.orElse(null);
		if (recipe != null && arcaneCraftingHistory != null) {
			LinkedList<IArcaneRecipe> history = arcaneCraftingHistory.get();
			history.addFirst(recipe);
			if (history.size() > ARCANE_CRAFTING_HISTORY_SIZE)
				history.removeLast();
		}
		return recipe;
	}

	/**
	 * Called from {@link thaumcraft.common.lib.crafting.ThaumcraftCraftingManager#findMatchingArcaneRecipe(IInventory, EntityPlayer)}
	 */
	@Callhook
	public static ItemStack findMatchingArcaneRecipe(IInventory awb, EntityPlayer player) {
		IArcaneRecipe recipe = findArcaneRecipe(awb, player);
		return recipe == null ? null : recipe.getCraftingResult(awb);
	}

	/**
	 * Called from {@link thaumcraft.common.lib.crafting.ThaumcraftCraftingManager#findMatchingArcaneRecipeAspects(IInventory, EntityPlayer)}
	 */
	@Callhook
	public static AspectList findMatchingArcaneRecipeAspects(IInventory awb, EntityPlayer player) {
		IArcaneRecipe recipe = findArcaneRecipe(awb, player);
		return recipe == null ? new AspectList() : recipe.getAspects() == null ? recipe.getAspects(awb) : recipe.getAspects();
	}

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
		arcaneCraftingHistory = null;
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
		arcaneCraftingHistory = ThreadLocal.withInitial(LinkedList::new);
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

	/**
	 * Called from {@link thaumcraft.common.container.ContainerArcaneWorkbench#onCraftMatrixChanged(IInventory)}
	 */
	@Callhook
	public static void onArcaneWorkbenchChanged(TileArcaneWorkbench tileEntity, InventoryPlayer ip) {
		if (ConfigurationHandler.INSTANCE.isCheckWorkbenchRecipes()) {
			InventoryCrafting ic = new InventoryCrafting(new ContainerDummy(), 3, 3);
			for (int a = 0; a < 9; ++a) {
				ic.setInventorySlotContents(a, tileEntity.getStackInSlot(a));
			}
			tileEntity.setInventorySlotContentsSoftly(9, CraftingManager.getInstance().findMatchingRecipe(ic, tileEntity.getWorldObj()));
		} else {
			tileEntity.setInventorySlotContentsSoftly(9, null);
		}
		if (tileEntity.getStackInSlot(9) == null && tileEntity.getStackInSlot(10) != null && tileEntity.getStackInSlot(10).getItem() instanceof ItemWandCasting) {
			ItemWandCasting wand = (ItemWandCasting) tileEntity.getStackInSlot(10).getItem();
			if (wand.consumeAllVisCrafting(tileEntity.getStackInSlot(10), ip.player, ThaumcraftCraftingManager.findMatchingArcaneRecipeAspects(tileEntity, ip.player), false)) {
				tileEntity.setInventorySlotContentsSoftly(9, ThaumcraftCraftingManager.findMatchingArcaneRecipe(tileEntity, ip.player));
			}
		}
	}
}
