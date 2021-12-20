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
		return list.stream().filter(r -> r instanceof CrucibleRecipe).map(r -> (CrucibleRecipe) r).collect(Collector.of(TIntObjectHashMap::new, (m, r)-> m.put(r.hash, r), FlushableCache::mergeTIntObjectMap, Characteristics.IDENTITY_FINISH, Characteristics.UNORDERED));
	}
}
