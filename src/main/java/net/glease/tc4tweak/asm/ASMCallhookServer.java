package net.glease.tc4tweak.asm;

import net.glease.tc4tweak.ConfigurationHandler;
import net.glease.tc4tweak.modules.findCrucibleRecipe.FindCrucibleRecipe;
import net.glease.tc4tweak.modules.findRecipes.FindRecipes;
import net.glease.tc4tweak.modules.generateItemHash.GenerateItemHash;
import net.glease.tc4tweak.modules.getResearch.GetResearch;
import net.glease.tc4tweak.modules.objectTag.GetObjectTags;
import net.glease.tc4tweak.network.NetworkedConfiguration;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.visnet.TileVisNode;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.container.ContainerDummy;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.lib.world.dim.CellLoc;
import thaumcraft.common.lib.world.dim.MazeHandler;
import thaumcraft.common.tiles.TileArcaneWorkbench;

import java.util.Map.Entry;

public class ASMCallhookServer {
    private ASMCallhookServer() {
    }

    /**
     * Called from both {@link ItemWandCasting#getFocusItem(ItemStack)} and {@link ItemWandCasting#getFocus(ItemStack)}
     *
     * @param stack reconstructed focus stack, not wand stack
     * @return true if the stack is valid
     */
    @Callhook
    public static boolean isValidFocusItemStack(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemFocusBasic;
    }

    @Callhook
    public static ResearchItem getResearch(String key) {
        return GetResearch.getResearch(key);
    }

    /**
     * Called from {@link thaumcraft.common.lib.crafting.ThaumcraftCraftingManager#findMatchingArcaneRecipe(IInventory, EntityPlayer)}
     */
    @Callhook
    public static ItemStack findMatchingArcaneRecipe(IInventory awb, EntityPlayer player) {
        IArcaneRecipe recipe = FindRecipes.findArcaneRecipe(awb, player);
        return recipe == null ? null : recipe.getCraftingResult(awb);
    }

    /**
     * Called from {@link thaumcraft.common.lib.crafting.ThaumcraftCraftingManager#findMatchingArcaneRecipeAspects(IInventory, EntityPlayer)}
     */
    @Callhook
    public static AspectList findMatchingArcaneRecipeAspects(IInventory awb, EntityPlayer player) {
        IArcaneRecipe recipe = FindRecipes.findArcaneRecipe(awb, player);
        return recipe == null ? new AspectList() : recipe.getAspects() == null ? recipe.getAspects(awb) : recipe.getAspects();
    }

    @Callhook
    public static int generateItemHash(Item item, int meta) {
        return GenerateItemHash.generateItemHash(item, meta);
    }

    @Callhook
    public static AspectList getObjectTags(ItemStack itemstack) {
        return GetObjectTags.getObjectTags(itemstack);
    }

    /**
     * Called from {@link thaumcraft.common.container.ContainerArcaneWorkbench#onCraftMatrixChanged(IInventory)}
     */
    @Callhook
    public static void onArcaneWorkbenchChanged(TileArcaneWorkbench tileEntity, InventoryPlayer ip) {
        // only check synced config if in remote world
        if (ConfigurationHandler.INSTANCE.isCheckWorkbenchRecipes() && (!tileEntity.getWorldObj().isRemote || NetworkedConfiguration.isCheckWorkbenchRecipes())) {
            InventoryCrafting ic = new InventoryCrafting(new ContainerDummy(), 3, 3);
            for (int a = 0; a < 9; ++a) {
                ic.setInventorySlotContents(a, tileEntity.getStackInSlot(a));
            }
            tileEntity.setInventorySlotContentsSoftly(9, CraftingManager.getInstance().findMatchingRecipe(ic, tileEntity.getWorldObj()));
        } else {
            tileEntity.setInventorySlotContentsSoftly(9, null);
        }
        if (tileEntity.getStackInSlot(9) == null && tileEntity.getStackInSlot(10) != null && tileEntity.getStackInSlot(10).getItem() instanceof ItemWandCasting) {
            ItemWandCasting wand = (ItemWandCasting) tileEntity.getStackInSlot(10).getItem();
            if (wand.consumeAllVisCrafting(tileEntity.getStackInSlot(10), ip.player, ThaumcraftCraftingManager.findMatchingArcaneRecipeAspects(tileEntity, ip.player), false)) {
                tileEntity.setInventorySlotContentsSoftly(9, ThaumcraftCraftingManager.findMatchingArcaneRecipe(tileEntity, ip.player));
            }
        }
    }

    @Callhook
    public static int hashCellLoc(CellLoc thiz) {
        return ((1664525 * thiz.x) + 1013904223) ^ ((1664525 * (thiz.z ^ -559038737)) + 1013904223);
    }

    @Callhook
    public static void postThaumcraftApiClinit() {
        ThaumcraftApi.objectTags = GetObjectTags.newReplacementObjectTagsMap();
    }

    @Callhook
    public static NBTTagCompound writeMazeToNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        NBTTagList tagList = new NBTTagList();

        for (Entry<CellLoc, Short> entry : MazeHandler.labyrinth.entrySet()) {
            short v;
            if (entry.getValue() == null) continue;
            if ((v = entry.getValue()) <= 0) continue;
            CellLoc loc = entry.getKey();
            NBTTagCompound cell = new NBTTagCompound();
            cell.setInteger("x", loc.x);
            cell.setInteger("z", loc.z);
            cell.setShort("cell", v);
            tagList.appendTag(cell);
        }

        nbt.setTag("cells", tagList);
        return nbt;
    }

    @Callhook
    public static CrucibleRecipe getCrucibleRecipeFromHash(int hash) {
        return FindCrucibleRecipe.getCrucibleRecipeFromHash(hash);
    }

    @Callhook
    public static Entity onlyIfAlive(Entity sus) {
        return sus != null && sus.isEntityAlive() ? sus : null;
    }

    @Callhook
    public static boolean canNodeBeSeen(TileVisNode source, TileVisNode target) {
        World world = source.getWorldObj();
        Vec3 v1 = Vec3.createVectorHelper((double) source.xCoord + 0.5D, (double) source.yCoord + 0.5D, (double) source.zCoord + 0.5D);
        Vec3 v2 = Vec3.createVectorHelper((double) target.xCoord + 0.5D, (double) target.yCoord + 0.5D, (double) target.zCoord + 0.5D);
        if (Double.isNaN(v1.xCoord) || Double.isNaN(v1.yCoord) || Double.isNaN(v1.zCoord)) return false;
        if (Double.isNaN(v2.xCoord) || Double.isNaN(v2.yCoord) || Double.isNaN(v2.zCoord)) return false;
        int i = MathHelper.floor_double(v2.xCoord);
        int j = MathHelper.floor_double(v2.yCoord);
        int k = MathHelper.floor_double(v2.zCoord);
        int l = MathHelper.floor_double(v1.xCoord);
        int i1 = MathHelper.floor_double(v1.yCoord);
        int j1 = MathHelper.floor_double(v1.zCoord);
        int k1 = source.getRange() * 5; // mathematician please help. likely not * 5...

        while (k1-- >= 0) {
            if (Double.isNaN(v1.xCoord) || Double.isNaN(v1.yCoord) || Double.isNaN(v1.zCoord)) {
                return false;
            }

            if (l != i || i1 != j || j1 != k) {
                boolean flag6 = true;
                boolean flag3 = true;
                boolean flag4 = true;
                double d0 = 999.0D;
                double d1 = 999.0D;
                double d2 = 999.0D;
                if (i > l) {
                    d0 = (double) l + 1.0D;
                } else if (i < l) {
                    d0 = (double) l + 0.0D;
                } else {
                    flag6 = false;
                }

                if (j > i1) {
                    d1 = (double) i1 + 1.0D;
                } else if (j < i1) {
                    d1 = (double) i1 + 0.0D;
                } else {
                    flag3 = false;
                }

                if (k > j1) {
                    d2 = (double) j1 + 1.0D;
                } else if (k < j1) {
                    d2 = (double) j1 + 0.0D;
                } else {
                    flag4 = false;
                }

                double d3 = 999.0D;
                double d4 = 999.0D;
                double d5 = 999.0D;
                double d6 = v2.xCoord - v1.xCoord;
                double d7 = v2.yCoord - v1.yCoord;
                double d8 = v2.zCoord - v1.zCoord;
                if (flag6) {
                    d3 = (d0 - v1.xCoord) / d6;
                }

                if (flag3) {
                    d4 = (d1 - v1.yCoord) / d7;
                }

                if (flag4) {
                    d5 = (d2 - v1.zCoord) / d8;
                }

                byte b0;
                if (d3 < d4 && d3 < d5) {
                    if (i > l) {
                        b0 = 4;
                    } else {
                        b0 = 5;
                    }

                    v1.xCoord = d0;
                    v1.yCoord += d7 * d3;
                    v1.zCoord += d8 * d3;
                } else if (d4 < d5) {
                    if (j > i1) {
                        b0 = 0;
                    } else {
                        b0 = 1;
                    }

                    v1.xCoord += d6 * d4;
                    v1.yCoord = d1;
                    v1.zCoord += d8 * d4;
                } else {
                    if (k > j1) {
                        b0 = 2;
                    } else {
                        b0 = 3;
                    }

                    v1.xCoord += d6 * d5;
                    v1.yCoord += d7 * d5;
                    v1.zCoord = d2;
                }

                l = MathHelper.floor_double(v1.xCoord);
                if (b0 == 5) {
                    --l;
                }

                i1 = MathHelper.floor_double(v1.yCoord);
                if (b0 == 1) {
                    --i1;
                }

                j1 = MathHelper.floor_double(v1.zCoord);
                if (b0 == 3) {
                    --j1;
                }

                if (l == target.xCoord && i1 == target.yCoord && j1 == target.zCoord)
                    return true;

                Block block1 = world.getBlock(l, i1, j1);
                int l1 = world.getBlockMetadata(l, i1, j1);
                if (block1.canCollideCheck(l1, false)) {
                    if (block1.getCollisionBoundingBoxFromPool(world, l, i1, j1) != null) {
                        MovingObjectPosition movingobjectposition1 = block1.collisionRayTrace(world, l, i1, j1, v1, v2);
                        if (movingobjectposition1 != null && movingobjectposition1.typeOfHit != MovingObjectPosition.MovingObjectType.MISS) {
                            return false;
                        }
                    }
                }
            }
        }
        return false;
    }
}
