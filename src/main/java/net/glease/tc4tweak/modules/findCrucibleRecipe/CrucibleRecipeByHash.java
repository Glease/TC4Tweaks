package net.glease.tc4tweak.modules.findCrucibleRecipe;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.glease.tc4tweak.CommonUtils;
import net.glease.tc4tweak.modules.FlushableCache;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.crafting.CrucibleRecipe;

import java.util.List;

import static net.glease.tc4tweak.modules.findCrucibleRecipe.FindCrucibleRecipe.log;

class CrucibleRecipeByHash extends FlushableCache<TIntObjectMap<CrucibleRecipe>> {
	@Override
	protected TIntObjectMap<CrucibleRecipe> createCache() {
		List<?> list = ThaumcraftApi.getCraftingRecipes();
		TIntObjectMap<CrucibleRecipe> result = new TIntObjectHashMap<>();
		for (Object o : list) {
			if (o instanceof CrucibleRecipe) {
				CrucibleRecipe recipe = (CrucibleRecipe) o;
				CrucibleRecipe existing = result.putIfAbsent(recipe.hash, recipe);
				if (existing != null && log.isWarnEnabled())
					log.warn("Recipe {} ignored due to collision with {} for hash {}", CommonUtils.toString(recipe), CommonUtils.toString(existing), recipe.hash);
			}
		}
		return result;
	}
}
