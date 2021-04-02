package net.glease.tc4tweak.asm;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.glease.tc4tweak.ConfigurationHandler;
import net.glease.tc4tweak.network.NetworkedConfiguration;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.container.ContainerDummy;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.tiles.TileArcaneWorkbench;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.glease.tc4tweak.asm.TC4Transformer.log;

public class ASMCallhookServer {
	private static final int DEFAULT_NAMESPACE_HASH_BASE = "oops:".hashCode() * 31;
	// getResearch speed up
	private static Map<String, ResearchItem> researchItemMap = null;
	// arcane crafting recipe speed up
	// thread local to make integrated server happy
	private static ThreadLocal<LinkedList<IArcaneRecipe>> arcaneCraftingHistory = null;
	private static ConcurrentHashMap<List<?>, int[]> groupedObjectTags = null;
	private static ConcurrentMap<Item, TIntObjectMap<AspectList>> objectTags = null;

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
			if (history.size() > ConfigurationHandler.INSTANCE.getArcaneCraftingHistorySize())
				history.removeLast();
		}
		return recipe;
	}

	/**
	 * Called from both {@link ItemWandCasting#getFocusItem(ItemStack)} and {@link ItemWandCasting#getFocus(ItemStack)}
	 *
	 * @param stack reconstructed focus stack, not wand stack
	 * @return true if the stack is valid
	 */
	@Callhook
	public static boolean isValidFocusItemStack(ItemStack stack) {
		return stack != null && stack.getItem() instanceof ItemFocusBasic;
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
		objectTags = null;
	}

	public static void flushAllCache(boolean doCreate) {
		if (doCreate || researchItemMap != null) {
			researchItemMap = ResearchCategories.researchCategories.values().stream()
					.flatMap(l -> l.research.values().stream())
					.collect(Collectors.toMap(
							i -> i.key,
							Function.identity(),
							(u, v) -> u,
							LinkedHashMap::new
					));
			arcaneCraftingHistory = ThreadLocal.withInitial(LinkedList::new);
			objectTags = ThaumcraftApi.objectTags.entrySet().parallelStream()
					.collect(Collectors.toConcurrentMap(
							e -> (Item) e.getKey().get(0),
							ASMCallhookServer::bakeSubmap,
							ASMCallhookServer::mergeTIntObjectMap
					));
		}
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
	@Callhook
	public static int generateItemHash(Item item, int meta) {
		ItemStack t = new ItemStack(item, 1, meta);

		try {
			if (t.isItemStackDamageable() || !t.getHasSubtypes()) {
				meta = -1;
			}
		} catch (Exception ignored) {
		}

		List<Object> key = Arrays.asList(item, meta);
		final int[] value = getGroupedObjectTags().get(key);
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

	/**
	 * Called from {@link thaumcraft.common.container.ContainerArcaneWorkbench#onCraftMatrixChanged(IInventory)}
	 */
	@Callhook
	public static void onArcaneWorkbenchChanged(TileArcaneWorkbench tileEntity, InventoryPlayer ip) {
		// only check synced config if in remote world
		if (ConfigurationHandler.INSTANCE.isCheckWorkbenchRecipes() && (!tileEntity.getWorldObj().isRemote || NetworkedConfiguration.isCheckWorkbenchRecipes())) {
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

	private static ConcurrentHashMap<List<?>, int[]> getGroupedObjectTags() {
		if (groupedObjectTags == null) {
			synchronized (ASMCallhookServer.class) {
				if (groupedObjectTags == null) {
					try {
						groupedObjectTags = ReflectionHelper.getPrivateValue(ThaumcraftApi.class, null, "groupedObjectTags");
					} catch (Exception e) {
						groupedObjectTags = new ConcurrentHashMap<>();
					}
				}
			}
		}
		return groupedObjectTags;
	}

	@Callhook
	public static void postThaumcraftApiClinit() {
		ThaumcraftApi.objectTags = new InterceptingConcurrentHashMap();
	}

	@Callhook
	@SuppressWarnings({"rawtypes"})
	public static AspectList getObjectTags(ItemStack itemstack) {
		Item item;
		int meta;
		try {
			item = itemstack.getItem();
			meta = itemstack.getItemDamage();
		} catch (Exception e) {
			return null;
		}

		AspectList tmp = getBaseObjectTags(item, meta);
		if (tmp == null)
			tmp = ThaumcraftApi.objectTags.get(Arrays.asList(item, meta));
		if (tmp == null) {
			for (List l : ThaumcraftApi.objectTags.keySet()) {
				if (l.get(0) == item && l.get(1) instanceof int[]) {
					int[] range = (int[]) l.get(1);
					Arrays.sort(range);
					if (Arrays.binarySearch(range, meta) >= 0) {
						tmp = ThaumcraftApi.objectTags.get(Arrays.asList(item, range));
						return tmp;
					}
				}
			}

			tmp = ThaumcraftApi.objectTags.get(Arrays.asList(item, 32767));
			if (tmp == null) {
				if (meta == 32767) {
					for (int index = 0; index < 16; ++index) {
						tmp = ThaumcraftApi.objectTags.get(Arrays.asList(item, index));
						if (tmp != null)
							break;
					}
				}

				if (tmp == null) {
					tmp = ThaumcraftCraftingManager.generateTags(item, meta);
				}
			}
		}

		if (itemstack.getItem() instanceof ItemWandCasting) {
			ItemWandCasting wand = (ItemWandCasting) itemstack.getItem();
			if (tmp == null) tmp = new AspectList();
			addWandTags(itemstack, tmp, wand);
		} else if (item != null && item == Items.potionitem) {
			if (tmp == null) tmp = new AspectList();
			addPotionTags(itemstack, (ItemPotion) item, tmp);
		}

		if (tmp == null) {
			return null;
		} else if (tmp.aspects.values().stream().allMatch(n -> n <= 64)) {
			return tmp;
		}
		AspectList out = tmp.copy();
		out.aspects.replaceAll((a, n) -> Math.min(64, n));
		return out;
	}

	private static void addWandTags(ItemStack itemstack, AspectList tmp, ItemWandCasting wand) {
		tmp.merge(Aspect.MAGIC, (wand.getRod(itemstack).getCraftCost() + wand.getCap(itemstack).getCraftCost()) / 2);
		tmp.merge(Aspect.TOOL, (wand.getRod(itemstack).getCraftCost() + wand.getCap(itemstack).getCraftCost()) / 3);
	}

	@SuppressWarnings("unchecked")
	private static void addPotionTags(ItemStack itemstack, ItemPotion item, AspectList tmp) {
		tmp.merge(Aspect.WATER, 1);
		List<PotionEffect> effects = item.getEffects(itemstack.getItemDamage());
		if (effects != null) {
			if (ItemPotion.isSplash(itemstack.getItemDamage())) {
				tmp.merge(Aspect.ENTROPY, 2);
			}

			for (PotionEffect effect : effects) {
				int amplifier = effect.getAmplifier();
				int potionID = effect.getPotionID();
				tmp.merge(Aspect.MAGIC, (amplifier + 1) * 2);
				if (potionID == Potion.blindness.id) {
					tmp.merge(Aspect.DARKNESS, (amplifier + 1) * 3);
				} else if (potionID == Potion.confusion.id) {
					tmp.merge(Aspect.ELDRITCH, (amplifier + 1) * 3);
				} else if (potionID == Potion.damageBoost.id) {
					tmp.merge(Aspect.WEAPON, (amplifier + 1) * 3);
				} else if (potionID == Potion.digSlowdown.id) {
					tmp.merge(Aspect.TRAP, (amplifier + 1) * 3);
				} else if (potionID == Potion.digSpeed.id) {
					tmp.merge(Aspect.TOOL, (amplifier + 1) * 3);
				} else if (potionID == Potion.fireResistance.id) {
					tmp.merge(Aspect.ARMOR, amplifier + 1);
					tmp.merge(Aspect.FIRE, (amplifier + 1) * 2);
				} else if (potionID == Potion.harm.id) {
					tmp.merge(Aspect.DEATH, (amplifier + 1) * 3);
				} else if (potionID == Potion.heal.id) {
					tmp.merge(Aspect.HEAL, (amplifier + 1) * 3);
				} else if (potionID == Potion.hunger.id) {
					tmp.merge(Aspect.DEATH, (amplifier + 1) * 3);
				} else if (potionID == Potion.invisibility.id) {
					tmp.merge(Aspect.SENSES, (amplifier + 1) * 3);
				} else if (potionID == Potion.jump.id) {
					tmp.merge(Aspect.FLIGHT, (amplifier + 1) * 3);
				} else if (potionID == Potion.moveSlowdown.id) {
					tmp.merge(Aspect.TRAP, (amplifier + 1) * 3);
				} else if (potionID == Potion.moveSpeed.id) {
					tmp.merge(Aspect.MOTION, (amplifier + 1) * 3);
				} else if (potionID == Potion.nightVision.id) {
					tmp.merge(Aspect.SENSES, (amplifier + 1) * 3);
				} else if (potionID == Potion.poison.id) {
					tmp.merge(Aspect.POISON, (amplifier + 1) * 3);
				} else if (potionID == Potion.regeneration.id) {
					tmp.merge(Aspect.HEAL, (amplifier + 1) * 3);
				} else if (potionID == Potion.resistance.id) {
					tmp.merge(Aspect.ARMOR, (amplifier + 1) * 3);
				} else if (potionID == Potion.waterBreathing.id) {
					tmp.merge(Aspect.AIR, (amplifier + 1) * 3);
				} else if (potionID == Potion.weakness.id) {
					tmp.merge(Aspect.DEATH, (amplifier + 1) * 3);
				}
			}
		}
	}

	@Callhook
	public static AspectList getBaseObjectTags(Item item, int meta) {
		if (objectTags == null)
			return null;
		TIntObjectMap<AspectList> submap = objectTags.get(item);
		if (submap != null) {
			AspectList aspectList;
			if ((aspectList = submap.get(meta)) != null) return aspectList;
			if ((aspectList = submap.get(32767)) != null) return aspectList;

			if (meta == 32767) {
				for (int i = 0; i < 16; i++) {
					if ((aspectList = submap.get(i)) != null) return aspectList;
				}
			}
		}
		return ThaumcraftCraftingManager.generateTags(item, meta);
	}

	@SuppressWarnings("rawtypes")
	private static class InterceptingConcurrentHashMap extends ConcurrentHashMap<List, AspectList> {
		@Override
		public AspectList put(List key, AspectList value) {
			if (objectTags != null) {
				Item item = (Item) key.get(0);
				int[] metas = (int[]) key.get(1);
				TIntObjectMap<AspectList> submap = objectTags.computeIfAbsent(item, k -> new TIntObjectHashMap<>(metas.length));
				for (int meta : metas) submap.put(meta, value);
			}
			return super.put(key, value);
		}
	}

	private static TIntObjectMap<AspectList> mergeTIntObjectMap(TIntObjectMap<AspectList> lhs, TIntObjectMap<AspectList> rhs) {
		lhs.putAll(rhs);
		return lhs;
	}

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
			log.error("Unrecognized key in objectTags map! {}", e.getKey());
		}
		return submap;
	}
}
