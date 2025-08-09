package net.glease.tc4tweak.modules.infusionRecipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import gnu.trove.set.hash.TIntHashSet;
import net.glease.tc4tweak.api.infusionrecipe.RecipeIngredient;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import thaumcraft.api.ThaumcraftApiHelper;

public enum InfusionOreDictMode {
    @SuppressWarnings("deprecation")
    Default {
        @Override
        public boolean test(ItemStack playerInput, ItemStack recipeSpec) {
            int od = OreDictionary.getOreID(playerInput);
            if (od == -1) return false;
            ItemStack[] ores = OreDictionary.getOres(od).toArray(new ItemStack[0]);
            return ThaumcraftApiHelper.containsMatch(false, new ItemStack[]{recipeSpec}, ores);
        }

        @Override
        public RecipeIngredient get(ItemStack recipeSpec) {
            int oreID = OreDictionary.getOreID(recipeSpec);
            if (oreID == -1) return RecipeIngredient.item(false, recipeSpec);
            return RecipeIngredient.oredict(OreDictionary.getOreName(oreID));
        }
    },
    Strict {
        @Override
        public boolean test(ItemStack playerInput, ItemStack recipeSpec) {
            return new TIntHashSet(OreDictionary.getOreIDs(playerInput)).equals(new TIntHashSet(OreDictionary.getOreIDs(recipeSpec)));
        }

        private List<ItemStack> intersect(List<ItemStack> all, List<ItemStack> stacks) {
            if (all.isEmpty() || stacks.isEmpty()) {
                all.clear();
                return Collections.emptyList();
            }
            List<ItemStack> newStacks = new ArrayList<>();
            next: for (ItemStack stack : stacks) {
                for (ItemStack toMatch : all) {
                    if (stack.isItemEqual(toMatch)) {
                        newStacks.add(stack);
                        continue next;
                    }
                }
            }
            return newStacks;
        }

        @Override
        public RecipeIngredient get(ItemStack recipeSpec) {
            int[] oreIDs = OreDictionary.getOreIDs(recipeSpec);
            if (oreIDs.length == 0) return RecipeIngredient.item(true, recipeSpec);
            if (oreIDs.length == 1) return RecipeIngredient.oredictStrict(OreDictionary.getOreName(oreIDs[0]));

            List<ItemStack> ores = null;
            for (int oreID : oreIDs) {
                @SuppressWarnings({"deprecation"})
                ArrayList<ItemStack> oresThis = OreDictionary.getOres(oreID);
                if (oresThis.size() == 1) return RecipeIngredient.item(true, recipeSpec);
                if (ores == null) ores = new ArrayList<>(oresThis);
                else {
                    ores = intersect(ores, oresThis);
                    if (ores.size() == 1) return RecipeIngredient.item(true, recipeSpec);
                }
            }
            return RecipeIngredient.items(false, ores.toArray(new ItemStack[0]));
        }
    },
    Relaxed {
        @Override
        public boolean test(ItemStack playerInput, ItemStack recipeSpec) {
            TIntHashSet set = new TIntHashSet(OreDictionary.getOreIDs(playerInput));
            for (int i : OreDictionary.getOreIDs(recipeSpec)) {
                if (set.contains(i))
                    return true;
            }
            return false;
        }

        @Override
        public RecipeIngredient get(ItemStack recipeSpec) {
            return Arrays.stream(OreDictionary.getOreIDs(recipeSpec))
                    .mapToObj(OreDictionary::getOreName)
                    .map(RecipeIngredient::oredict)
                    .reduce(RecipeIngredient::or)
                    .orElseGet(() -> RecipeIngredient.item(false, recipeSpec));
        }
    },
    None {
        @Override
        public boolean test(ItemStack playerInput, ItemStack recipeSpec) {
            return recipeSpec.isItemEqual(playerInput) && ItemStack.areItemStackTagsEqual(recipeSpec, playerInput);
        }

        @Override
        public RecipeIngredient get(ItemStack recipeSpec) {
            return RecipeIngredient.item(false, recipeSpec);
        }
    };

    public abstract boolean test(ItemStack playerInput, ItemStack recipeSpec);

    public abstract RecipeIngredient get(ItemStack recipeSpec);

    public static InfusionOreDictMode get(String name) {
        for (InfusionOreDictMode value : values()) {
            if (value.name().equals(name))
                return value;
        }
        return Default;
    }
}
