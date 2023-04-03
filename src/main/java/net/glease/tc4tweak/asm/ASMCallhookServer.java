package net.glease.tc4tweak.asm;

import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import net.glease.tc4tweak.ConfigurationHandler;
import net.glease.tc4tweak.TC4Tweak;
import net.glease.tc4tweak.asm.PacketAspectCombinationToServerVisitor.PacketAspectCombinationToServerAccess;
import net.glease.tc4tweak.modules.blockJar.EntityCollisionBox;
import net.glease.tc4tweak.modules.findCrucibleRecipe.FindCrucibleRecipe;
import net.glease.tc4tweak.modules.findRecipes.FindRecipes;
import net.glease.tc4tweak.modules.generateItemHash.GenerateItemHash;
import net.glease.tc4tweak.modules.getResearch.GetResearch;
import net.glease.tc4tweak.modules.objectTag.GetObjectTags;
import net.glease.tc4tweak.network.NetworkedConfiguration;
import net.glease.tc4tweak.network.TileHoleSyncPacket;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.visnet.TileVisNode;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.container.ContainerDummy;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.lib.network.playerdata.PacketAspectCombinationToServer;
import thaumcraft.common.lib.world.dim.CellLoc;
import thaumcraft.common.lib.world.dim.MazeHandler;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileResearchTable;

import java.util.Map.Entry;

import static net.glease.tc4tweak.TC4Tweak.log;

public class ASMCallhookServer {
    private static final Marker securityMarker = MarkerManager.getMarker("SuspiciousPackets");
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

    @Callhook
    public static float getBlockJarEntityCollisionBoxParameter(int index) {
        return EntityCollisionBox.getBlockJarEntityCollisionBoxParameter(index);
    }

    @Callhook
    public static boolean addToPlayerInventoryBiased(InventoryPlayer inv, ItemStack s) {
        if (s == null || s.stackSize == 0 || s.getItem() == null) return false;
        // logic: first try to stack, then try to place in original slot, last fallback to vanilla logic
        for (ItemStack stack : inv.mainInventory) {
            if (stack != null && stack.isStackable() && stack.getItem() == s.getItem() &&
                    stack.stackSize < Math.min(stack.getMaxStackSize(), inv.getInventoryStackLimit())
                    && (!stack.getHasSubtypes() || stack.getItemDamage() == s.getItemDamage()) &&
                    ItemStack.areItemStackTagsEqual(stack, s)) {
                int toAdd = Math.min(s.stackSize, Math.min(stack.getMaxStackSize(), inv.getInventoryStackLimit()) - stack.stackSize);
                s.stackSize -= toAdd;
                stack.stackSize += toAdd;
                stack.animationsToGo = 5;
                if (s.stackSize == 0) return true;
            }
        }
        if (inv.currentItem >= 0 && inv.currentItem < InventoryPlayer.getHotbarSize() && inv.getCurrentItem() == null) {
            inv.setInventorySlotContents(inv.currentItem, s);
            return true;
        } else {
            return inv.addItemStackToInventory(s);
        }
    }

    @Callhook
    public static Packet createTileHoleSyncPacket(S35PacketUpdateTileEntity origin) {
        try {
            return TC4Tweak.INSTANCE.CHANNEL.getPacketFrom(new TileHoleSyncPacket(origin));
        } catch (Exception ex) {
            // fallback to original packet if anything goes wrong
            return origin;
        }
    }

    @Callhook
    public static boolean sanityCheckAspectCombination(PacketAspectCombinationToServerAccess packet, MessageContext ctx) {
        if (sanityCheckAspectCombination0(packet))
            return true;
        EntityPlayerMP playerEntity = ctx.getServerHandler().playerEntity;
        log.info(securityMarker, "Player {} sent suspicious packet to get more aspects", playerEntity.getGameProfile());
        return false;
    }

    private static boolean sanityCheckAspectCombination0(PacketAspectCombinationToServerAccess packet) {
        TileResearchTable table = packet.table();
        if (table == null) return false;
        EntityPlayerMP player = packet.player();
        if (player == null) return false;
        return hasAspect(table, player, packet.lhs()) && hasAspect(table, player, packet.rhs());
    }

    private static boolean hasAspect(TileResearchTable table, EntityPlayerMP player, Aspect aspect) {
        return Thaumcraft.proxy.playerKnowledge.getAspectPoolFor(player.getCommandSenderName(), aspect) > 0 ||
                table.bonusAspects.getAmount(aspect) > 0;
    }
}
