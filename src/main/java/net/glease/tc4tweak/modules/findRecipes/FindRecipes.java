package net.glease.tc4tweak.modules.findRecipes;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.crafting.IArcaneRecipe;

public class FindRecipes {
    private static final ArcaneCraftingHistory cache = new ArcaneCraftingHistory();

    private FindRecipes() {
    }

    public static IArcaneRecipe findArcaneRecipe(IInventory inv, EntityPlayer player) {
        IArcaneRecipe r = cache.findInCache(inv, player);
        if (r != null)
            return r;
        r = ((List<?>) ThaumcraftApi.getCraftingRecipes()).parallelStream()
                .filter(o -> o instanceof IArcaneRecipe && ((IArcaneRecipe) o).matches(inv, player.worldObj, player))
                .map(o -> (IArcaneRecipe) o)
                .findFirst()
                .orElse(null);
        if (r != null)
            cache.addToCache(r);
        return r;
    }
}
