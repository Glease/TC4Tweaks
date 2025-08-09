package net.glease.tc4tweak.api;

import thaumcraft.api.crafting.InfusionRecipe;

class DummyInfusionExtAPI implements InfusionExtAPI {
    @Override
    public void setRecipeNBTBehavior(InfusionRecipe recipe, RecipeNBTBehavior behavior) {
        // noop
    }
}
