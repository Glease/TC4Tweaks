package net.glease.tc4tweak.modules.findCrucibleRecipe;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.glease.tc4tweak.modules.FlushableCache;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.crafting.CrucibleRecipe;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;

class CrucibleRecipeByHash extends FlushableCache<TIntObjectMap<CrucibleRecipe>> {
	@Override
	protected TIntObjectMap<CrucibleRecipe> createCache() {
		List<?> list = ThaumcraftApi.getCraftingRecipes();
		TIntObjectMap<CrucibleRecipe> result = new TIntObjectHashMap<>();
		for (Object o : list) {
			if (o instanceof CrucibleRecipe) {
				CrucibleRecipe recipe = (CrucibleRecipe) o;
				result.putIfAbsent(recipe.hash, recipe);
			}
		}
		return result;
	}
}
