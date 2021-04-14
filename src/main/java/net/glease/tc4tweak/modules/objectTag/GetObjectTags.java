package net.glease.tc4tweak.modules.objectTag;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GetObjectTags {
	static final Logger log = LogManager.getLogger("GetObjectTags");
	private static final ObjectTagsCache cache = new ObjectTagsCache();

	public static ConcurrentHashMap<List, AspectList> newReplacementObjectTagsMap() {
		return new InterceptingConcurrentHashMap();
	}

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

		if (item == null)
			return null;

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

	private static AspectList getBaseObjectTags(Item item, int meta) {
		ConcurrentMap<Item, TIntObjectMap<AspectList>> cache = GetObjectTags.cache.getCache();
		if (cache == null)
			return null;
		TIntObjectMap<AspectList> submap = cache.get(item);
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

	static void mutateObjectTagsSubmap(List<?> key, ObjectTagsMutation action) {
		ConcurrentMap<Item, TIntObjectMap<AspectList>> cache = GetObjectTags.cache.getCache();
		if (cache != null) {
			Item item = (Item) key.get(0);
			if (key.get(1) instanceof int[]) {
				int[] metas = (int[]) key.get(1);
				TIntObjectMap<AspectList> submap = cache.computeIfAbsent(item, k -> new TIntObjectHashMap<>(metas.length));
				for (int meta : metas) action.accept(submap, meta);
			} else if (key.get(1) instanceof Integer) {
				action.accept(cache.computeIfAbsent(item, k -> new TIntObjectHashMap<>()), ((Integer) key.get(1)));
			}
		}
	}
}
