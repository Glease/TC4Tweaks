package net.glease.tc4tweak.modules.infusionRecipe;

import java.util.WeakHashMap;

import net.glease.tc4tweak.api.InfusionExtAPI;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import thaumcraft.api.crafting.InfusionRecipe;

public class InfusionRecipeGetOutput {
    private static final ClassValue<Boolean> CLASS_OVERRIDES_GETRECIPEOUTPUT = new ClassValue<Boolean>() {
        @Override
        protected Boolean computeValue(Class<?> c) {
            while (c != InfusionRecipe.class) {
                try {
                    c.getDeclaredMethod("getRecipeOutput", ItemStack.class);
                } catch (NoSuchMethodException e) {
                    c = c.getSuperclass();
                    continue;
                }
                return true;
            }
            return false;
        }
    };
    private static final WeakHashMap<InfusionRecipe, InfusionExtAPI.RecipeNBTBehavior> overrides = new WeakHashMap<>();
    private static final InfusionExtAPI.RecipeNBTBehavior MERGER = InfusionExtAPI.RecipeNBTBehavior.mergeNBT();

    public static boolean shouldModify(InfusionRecipe recipe, ItemStack input) {
        // if it ever overrides getRecipeOutput(), we'd assume it's doing something special
        // and we will allow its logic to take precedence
        if (CLASS_OVERRIDES_GETRECIPEOUTPUT.get(recipe.getClass())) {
            return false;
        }
        if (armorToolOnly()) { // config
            Item item = input.getItem();
            return item instanceof ItemArmor || !item.getToolClasses(input).isEmpty();
        }
        return input.hasTagCompound() && !input.getTagCompound().hasNoTags();
    }

    private static boolean armorToolOnly() {
        return true;
    }

    public static ItemStack getOutput(InfusionRecipe thiz, ItemStack input, ItemStack output) {
        InfusionExtAPI.RecipeNBTBehavior overridingBehavior = overrides.get(thiz);
        if (overridingBehavior != null) {
            return overridingBehavior.getRecipeOutput(thiz, input, output);
        }
        if (!shouldModify(thiz, input)) {
            return output;
        }
        // or maybe use a whitelist config
        return MERGER.getRecipeOutput(thiz, input, output);
    }

    public static class APIImpl implements InfusionExtAPI {
        @Override
        public void setRecipeNBTBehavior(InfusionRecipe recipe, RecipeNBTBehavior behavior) {
            overrides.put(recipe, behavior);
        }
    }
}