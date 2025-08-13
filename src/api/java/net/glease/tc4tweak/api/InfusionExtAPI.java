package net.glease.tc4tweak.api;

import java.util.Set;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import thaumcraft.api.crafting.InfusionRecipe;

import static net.glease.tc4tweak.api.Utility.mergeNBTTags;

/**
 * Contains TC4Tweaks's infusion recipe enhancement.
 */
public interface InfusionExtAPI {
    /**
     * Overrides NBT merging behavior of TC4Tweaks.
     * NOTE: does absolutely nothing if TC4Tweaks is not loaded.
     * NOTE: if your recipe class overrides getRecipeOutput() method (or if any of its parents not including
     * InfusionRecipe do so), then TC4Tweaks will not do anything to it even if you set some behavior here.
     * @param recipe recipe to modify
     * @param behavior type of behavior
     */
    void setRecipeNBTBehavior(InfusionRecipe recipe, RecipeNBTBehavior behavior);

    /**
     * Decides how the NBT tag should transfer from input item (thing on center pedestal) to output item.
     * Contains two useful defaults: {@link #mergeNBT(Set)} and {@link #vanilla()}
     */
    @FunctionalInterface
    interface RecipeNBTBehavior {
        /**
         * Do post-output modification of recipe.
         * NOTE: it's not specified as when this will be run. This is an implementation subject to change without notice.
         * @param recipe the original recipe instance
         * @param input the current input
         * @param output the original output returned from {@link InfusionRecipe#getRecipeOutput(ItemStack)}.
         *               NOTE: for most implementations this is the ItemStack instance stored in InfusionRecipe.
         *               It is strongly advised to <b>NOT</b> modify this stack under any circumstance.
         *               if you do need to modify it, make a copy first.
         * @return the modified output. must not be null.
         */
        ItemStack getRecipeOutput(InfusionRecipe recipe, ItemStack input, ItemStack output);

        /**
         * Use vanilla behavior, i.e. pass through original output. Useful if you do not want TC4Tweaks to do anything
         * @return instance
         */
        static RecipeNBTBehavior vanilla() {
            return (r, i, o) -> o;
        }

        /**
         * Recursively merge all NBT tags from center input item.
         * In event of a conflict, the patterned recipe output takes precedence.
         * In other words, if a tag with a different value or type is
         * already present on recipe output, then the recipe output tag will be used.
         * If whitelist is a non-null set, only keys matching filter will be merged. Otherwise, all tags will be merged.
         * @param whitelist keys to whitelist or null to have no whitelist
         * @return instance
         */
        static RecipeNBTBehavior mergeNBT(Set<String> whitelist) {
            return (r, i, o) -> {
                if (i.hasTagCompound() && o.hasTagCompound()) {
                    o = o.copy();
                    mergeNBTTags(i.stackTagCompound, o.stackTagCompound, whitelist);
                } else if (i.hasTagCompound()) {
                    o = new ItemStack(o.getItem(), i.stackSize, Items.feather.getDamage(i));
                    if (whitelist == null) {
                        o.setTagCompound((NBTTagCompound) i.getTagCompound().copy());
                    } else {
                        NBTTagCompound newTag = new NBTTagCompound(), oldTag = o.getTagCompound();
                        for (String key : whitelist) {
                            if (oldTag.hasKey(key)) {
                                newTag.setTag(key, oldTag.getTag(key));
                            }
                        }
                        o.setTagCompound(newTag);
                    }
                }
                return o;
            };
        }

        /**
         * Shorthand for {@link #mergeNBT(Set) mergeNBT(null)}
         * @return instance
         */
        static RecipeNBTBehavior mergeNBT() {
            return mergeNBT(null);
        }
    }
}
