package net.glease.tc4tweak.modules.findCrucibleRecipe;

import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.crafting.CrucibleRecipe;

import java.util.List;

public class FindCrucibleRecipe {
	private static final CrucibleRecipeByHash cache = new CrucibleRecipeByHash();

	public static CrucibleRecipe getCrucibleRecipeFromHash(int hash) {
		if (cache.isEnabled())
			return cache.getCache().get(hash);
		List<?> list = ThaumcraftApi.getCraftingRecipes();
		return (CrucibleRecipe) list.stream().filter(r -> r instanceof CrucibleRecipe && ((CrucibleRecipe) r).hash == hash).findAny().orElse(null);
	}
}
