package net.glease.tc4tweak.modules.manaBean;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.ItemManaBean;
import thaumcraft.common.tiles.TileManaPod;

public class GolemPlantManaBeanFix {
    private static final ThreadLocal<Aspect> manaBeanType = new ThreadLocal<>();

    public static void saveManaBeanType(World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(x, y, z);

        manaBeanType.set((te instanceof TileManaPod) ? ((TileManaPod) te).aspect : null);
    }

    public static boolean isBadManaBean(Entity entity) {
        if (manaBeanType.get() == null) return false;

        EntityItem entityItem = (EntityItem) entity;
        ItemStack itemStack = entityItem.getEntityItem();
        if (itemStack.getItem() != ConfigItems.itemManaBean) return false;

        AspectList aspects = ((ItemManaBean) ConfigItems.itemManaBean).getAspects(itemStack);
        return aspects.size() == 0 || manaBeanType.get() != aspects.getAspects()[0];
    }
}
