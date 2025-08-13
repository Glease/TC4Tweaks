package net.glease.tc4tweak.modules.infusionRecipe;

import java.util.HashSet;
import java.util.List;
import java.util.WeakHashMap;

import net.glease.tc4tweak.ConfigurationHandler;
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
    static final WeakHashMap<InfusionRecipe, InfusionExtAPI.RecipeNBTBehavior> overrides = new WeakHashMap<>();
    private static InfusionExtAPI.RecipeNBTBehavior MERGER;

    static {
        reload();
    }

    public static boolean shouldModify(InfusionRecipe recipe, ItemStack input, ItemStack output) {
        if (!ConfigurationHandler.INSTANCE.isInfusionRecipeNBTCarryOver()) {
            return false;
        }
        // if it ever overrides getRecipeOutput(), we'd assume it's doing something special
        // and we will allow its logic to take precedence
        if (CLASS_OVERRIDES_GETRECIPEOUTPUT.get(recipe.getClass())) {
            return false;
        }
        if (ConfigurationHandler.INSTANCE.isInfusionRecipeNBTModifyArmorToolOnly()) { // config
            if (!isArmorOrTool(input) || !isArmorOrTool(output)) {
                return false;
            }
        }
        return input.hasTagCompound() && !input.getTagCompound().hasNoTags();
    }

    private static boolean isArmorOrTool(ItemStack input) {
        Item item = input.getItem();
        return item instanceof ItemArmor || !item.getToolClasses(input).isEmpty();
    }

    public static ItemStack getOutput(InfusionRecipe thiz, ItemStack input, ItemStack output) {
        InfusionExtAPI.RecipeNBTBehavior overridingBehavior = overrides.get(thiz);
        if (overridingBehavior != null) {
            return overridingBehavior.getRecipeOutput(thiz, input, output);
        }
        if (!shouldModify(thiz, input, output)) {
            return output;
        }
        // or maybe use a whitelist config
        return MERGER.getRecipeOutput(thiz, input, output);
    }

    public static void reload() {
        // while this is not used in client mode, we have no idea whether we will run a SP server later on, so we just
        // unconditionally refresh the merger instance
        List<String> whitelist = ConfigurationHandler.INSTANCE.getInfusionRecipeNBTWhitelist();
        MERGER = InfusionExtAPI.RecipeNBTBehavior.mergeNBT(whitelist.isEmpty() ? null : new HashSet<>(whitelist));
    }

    public static class APIImpl implements InfusionExtAPI {
        @Override
        public void setRecipeNBTBehavior(InfusionRecipe recipe, RecipeNBTBehavior behavior) {
            overrides.put(recipe, behavior);
        }
    }
}