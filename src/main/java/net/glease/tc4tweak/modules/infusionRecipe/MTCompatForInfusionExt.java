package net.glease.tc4tweak.modules.infusionRecipe;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.api.data.IData;
import minetweaker.api.item.IIngredient;
import minetweaker.mc1710.data.NBTConverter;
import minetweaker.mc1710.item.MCItemStack;
import net.glease.tc4tweak.api.InfusionExtAPI;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.crafting.InfusionRecipe;

@ZenClass("mods.tc4tweaks.InfusionExt")
public class MTCompatForInfusionExt {
    @ZenMethod
    public static void registerCustomBehavior(IIngredient output, RecipeNBTBehaviorFunction behavior) {
        MineTweakerAPI.apply(new CustomBehavior<IIngredient>(output, behavior) {
            @Override
            Stream<InfusionRecipe> findMatches() {
                List<?> craftingRecipes = ThaumcraftApi.getCraftingRecipes();
                return craftingRecipes.stream()
                        .filter(r -> r instanceof InfusionRecipe)
                        .map(r -> (InfusionRecipe) r)
                        .filter(r -> r.getRecipeOutput() instanceof ItemStack && needle.matches(new MCItemStack((ItemStack) r.getRecipeOutput())));
            }
        });
    }

    // maybe late. this type of recipe is not really modified
    // @ZenMethod
    public static void registerCustomBehavior(IData output, RecipeNBTBehaviorFunction behavior) {
        NBTTagCompound tag = (NBTTagCompound) NBTConverter.from(output);
        MineTweakerAPI.apply(new CustomBehavior<NBTTagCompound>(tag, behavior) {
            @Override
            Stream<InfusionRecipe> findMatches() {
                List<?> craftingRecipes = ThaumcraftApi.getCraftingRecipes();
                return craftingRecipes.stream()
                        .filter(r -> r instanceof InfusionRecipe)
                        .map(r -> (InfusionRecipe) r)
                        .filter(r -> r.getRecipeOutput() instanceof NBTTagCompound && needle.equals(r.getRecipeOutput()));
            }
        });
    }

    @ZenClass("mods.tc4tweaks.RecipeNBTBehaviorFunction")
    public interface RecipeNBTBehaviorFunction extends InfusionExtAPI.RecipeNBTBehavior {}

    abstract static class CustomBehavior<T> implements IUndoableAction {
        protected final T needle;
        private final InfusionExtAPI.RecipeNBTBehavior behavior;
        private final List<WeakReference<InfusionRecipe>> matchedRecipes = new ArrayList<>();

        public CustomBehavior(T output, RecipeNBTBehaviorFunction behavior) {
            this.needle = output;
            this.behavior = behavior;
        }

        @Override
        public void apply() {
            findMatches().peek(r -> InfusionRecipeGetOutput.overrides.put(r, behavior)).map(WeakReference::new).forEach(matchedRecipes::add);
        }

        abstract Stream<InfusionRecipe> findMatches();

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void undo() {
            matchedRecipes.stream().map(WeakReference::get).filter(Objects::nonNull).forEach(InfusionRecipeGetOutput.overrides::remove);
            matchedRecipes.clear();
        }

        @Override
        public String describe() {
            return "Add custom recipe NBT behavior for " + needle;
        }

        @Override
        public String describeUndo() {
            return "Unset custom recipe NBT behavior for " + needle;
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }
}
