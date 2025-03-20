package net.glease.tc4tweak.asm;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.glease.tc4tweak.CommonUtils;
import net.glease.tc4tweak.ConfigurationHandler;
import net.glease.tc4tweak.TC4Tweak;
import net.glease.tc4tweak.asm.PacketAspectCombinationToServerVisitor.PacketAspectCombinationToServerAccess;
import net.glease.tc4tweak.asm.PacketPlayerCompleteToServerVisitor.PacketPlayerCompleteToServerAccess;
import net.glease.tc4tweak.modules.blockJar.EntityCollisionBox;
import net.glease.tc4tweak.modules.findCrucibleRecipe.FindCrucibleRecipe;
import net.glease.tc4tweak.modules.findRecipes.FindRecipes;
import net.glease.tc4tweak.modules.generateItemHash.GenerateItemHash;
import net.glease.tc4tweak.modules.getResearch.GetResearch;
import net.glease.tc4tweak.modules.objectTag.GetObjectTags;
import net.glease.tc4tweak.modules.visrelay.SavedLinkHandler;
import net.glease.tc4tweak.network.NetworkedConfiguration;
import net.glease.tc4tweak.network.TileHoleSyncPacket;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
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
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import thaumcraft.api.BlockCoordinates;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.WorldCoordinates;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.visnet.TileVisNode;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.Config;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.entities.ai.fluid.AILiquidGather;
import thaumcraft.common.items.baubles.ItemAmuletVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.world.dim.CellLoc;
import thaumcraft.common.lib.world.dim.MazeHandler;
import thaumcraft.common.tiles.TileResearchTable;

import static net.glease.tc4tweak.TC4Tweak.log;

public class ASMCallhookServer {
    private static final Marker securityMarker = MarkerManager.getMarker("SuspiciousPackets");
    private static Method getRenderDistanceChunks;
    private ASMCallhookServer() {
    }

    /**
     * Called from both {@link ItemWandCasting#getFocusItem(ItemStack)} and {@link ItemWandCasting#getFocus(ItemStack)}
     *
     * @param stack reconstructed focus stack, not wand stack
     * @return true if the stack is valid
     */
    @Callhook(adder = ItemWandCastingVisitor.class, module = ASMConstants.Modules.Bugfix)
    public static boolean isValidFocusItemStack(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemFocusBasic;
    }

    @Callhook(adder = ResearchCategoriesVisitor.class, module = ASMConstants.Modules.Optimization)
    public static ResearchItem getResearch(String key) {
        return GetResearch.getResearch(key);
    }

    /**
     * Called from {@link thaumcraft.common.lib.crafting.ThaumcraftCraftingManager#findMatchingArcaneRecipe(IInventory, EntityPlayer)}
     */
    @Callhook(adder = ThaumcraftCraftingManagerVisitor.class, module = ASMConstants.Modules.Optimization)
    public static ItemStack findMatchingArcaneRecipe(IInventory awb, EntityPlayer player) {
        IArcaneRecipe recipe = FindRecipes.findArcaneRecipe(awb, player);
        return recipe == null ? null : recipe.getCraftingResult(awb);
    }

    /**
     * Called from {@link thaumcraft.common.lib.crafting.ThaumcraftCraftingManager#findMatchingArcaneRecipeAspects(IInventory, EntityPlayer)}
     */
    @Callhook(adder = ThaumcraftCraftingManagerVisitor.class, module = ASMConstants.Modules.Optimization)
    public static AspectList findMatchingArcaneRecipeAspects(IInventory awb, EntityPlayer player) {
        IArcaneRecipe recipe = FindRecipes.findArcaneRecipe(awb, player);
        return recipe == null ? new AspectList() : recipe.getAspects() == null ? recipe.getAspects(awb) : recipe.getAspects();
    }

    @Callhook(adder = ScanManagerVisitor.class, module = ASMConstants.Modules.Optimization)
    public static int generateItemHash(Item item, int meta) {
        return GenerateItemHash.generateItemHash(item, meta);
    }

    @Callhook(adder = ThaumcraftCraftingManagerVisitor.class, module = ASMConstants.Modules.ObjectTagsLagFix)
    public static AspectList getObjectTags(ItemStack itemstack) {
        return GetObjectTags.getObjectTags(itemstack);
    }

    /**
     * Called from {@link thaumcraft.common.container.ContainerArcaneWorkbench#onCraftMatrixChanged(IInventory)}
     */
    @Callhook(adder = ContainerArcaneWorkbenchVisitor.class, module = ASMConstants.Modules.WorkbenchLagFix)
    public static ItemStack getNormalCraftingRecipeOutput(CraftingManager inst, InventoryCrafting ic, World world) {
        // only check synced config if in remote world
        if (ConfigurationHandler.INSTANCE.isCheckWorkbenchRecipes() && (!world.isRemote || NetworkedConfiguration.isCheckWorkbenchRecipes())) {
            return inst.findMatchingRecipe(ic, world);
        } else {
            return null;
        }
    }

    @Callhook(adder = HashCodeVisitor.class, module = ASMConstants.Modules.Optimization)
    public static int hash(CellLoc thiz) {
        return ((1664525 * thiz.x) + 1013904223) ^ ((1664525 * (thiz.z ^ -559038737)) + 1013904223);
    }

    @Callhook(adder = HashCodeVisitor.class, module = ASMConstants.Modules.Optimization)
    public static int hash(BlockCoordinates thiz) {
        return thiz.y * 31 + thiz.x * 91 + thiz.z * 29303;
    }

    @Callhook(adder = HashCodeVisitor.class, module = ASMConstants.Modules.Optimization)
    public static int hash(WorldCoordinates thiz) {
        return thiz.y * 31 + thiz.x * 91 + thiz.z * 29303 + thiz.dim * 39916801;
    }

    @Callhook(adder = ThaumcraftApiVisitor.class, module = ASMConstants.Modules.ObjectTagsLagFix)
    public static void postThaumcraftApiClinit() {
        ThaumcraftApi.objectTags = GetObjectTags.newReplacementObjectTagsMap();
    }

    /**
     * called from {@link MazeHandler#writeNBT()} as an overwrite.
     */
    @Callhook(adder = MazeHandlerVisitor.class, module = ASMConstants.Modules.Optimization)
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

    @Callhook(adder = ThaumcraftApiVisitor.class, module = ASMConstants.Modules.Optimization)
    public static CrucibleRecipe getCrucibleRecipeFromHash(int hash) {
        return FindCrucibleRecipe.getCrucibleRecipeFromHash(hash);
    }

    @Callhook(adder = AIItemPickupVisitor.class, module = ASMConstants.Modules.ExploitFix)
    public static Entity onlyIfAlive(Entity sus) {
        return sus != null && sus.isEntityAlive() ? sus : null;
    }

    @Callhook(adder = VisNetHandlerVisitor.class, module = ASMConstants.Modules.Optimization)
    public static boolean canNodeBeSeen(TileVisNode source, TileVisNode target) {
        World world = source.getWorldObj();
        Vec3 v1 = Vec3.createVectorHelper((double) source.xCoord + 0.5D, (double) source.yCoord + 0.5D, (double) source.zCoord + 0.5D);
        Vec3 v2 = Vec3.createVectorHelper((double) target.xCoord + 0.5D, (double) target.yCoord + 0.5D, (double) target.zCoord + 0.5D);
        if (Double.isNaN(v1.xCoord) || Double.isNaN(v1.yCoord) || Double.isNaN(v1.zCoord)) return true;
        if (Double.isNaN(v2.xCoord) || Double.isNaN(v2.yCoord) || Double.isNaN(v2.zCoord)) return true;
        int x2 = MathHelper.floor_double(v2.xCoord);
        int y2 = MathHelper.floor_double(v2.yCoord);
        int z2 = MathHelper.floor_double(v2.zCoord);
        int x1 = MathHelper.floor_double(v1.xCoord);
        int y1 = MathHelper.floor_double(v1.yCoord);
        int z1 = MathHelper.floor_double(v1.zCoord);
        int maxStep = source.getRange() * 5; // mathematician please help. likely not * 5...

        while (maxStep-- >= 0) {
            if (Double.isNaN(v1.xCoord) || Double.isNaN(v1.yCoord) || Double.isNaN(v1.zCoord)) {
                return true;
            }

            if (x1 != x2 || y1 != y2 || z1 != z2) {
                boolean xDiff = true;
                boolean yDIff = true;
                boolean zDiff = true;
                double x0 = 999.0D;
                double y0 = 999.0D;
                double z0 = 999.0D;
                if (x2 > x1) {
                    x0 = (double) x1 + 1.0D;
                } else if (x2 < x1) {
                    x0 = (double) x1 + 0.0D;
                } else {
                    xDiff = false;
                }

                if (y2 > y1) {
                    y0 = (double) y1 + 1.0D;
                } else if (y2 < y1) {
                    y0 = (double) y1 + 0.0D;
                } else {
                    yDIff = false;
                }

                if (z2 > z1) {
                    z0 = (double) z1 + 1.0D;
                } else if (z2 < z1) {
                    z0 = (double) z1 + 0.0D;
                } else {
                    zDiff = false;
                }

                double xpercent = 999.0D;
                double ypercent = 999.0D;
                double zpercent = 999.0D;
                double dx = v2.xCoord - v1.xCoord;
                double dy = v2.yCoord - v1.yCoord;
                double dz = v2.zCoord - v1.zCoord;
                if (xDiff) {
                    xpercent = (x0 - v1.xCoord) / dx;
                }

                if (yDIff) {
                    ypercent = (y0 - v1.yCoord) / dy;
                }

                if (zDiff) {
                    zpercent = (z0 - v1.zCoord) / dz;
                }

                byte checkType;
                if (xpercent < ypercent && xpercent < zpercent) {
                    // x changes next
                    if (x2 > x1) {
                        checkType = 4;
                    } else {
                        checkType = 5;
                    }

                    v1.xCoord = x0;
                    v1.yCoord += dy * xpercent;
                    v1.zCoord += dz * xpercent;
                } else if (ypercent < zpercent) {
                    // y changes next
                    if (y2 > y1) {
                        checkType = 0;
                    } else {
                        checkType = 1;
                    }

                    v1.xCoord += dx * ypercent;
                    v1.yCoord = y0;
                    v1.zCoord += dz * ypercent;
                } else {
                    // z changes next
                    if (z2 > z1) {
                        checkType = 2;
                    } else {
                        checkType = 3;
                    }

                    v1.xCoord += dx * zpercent;
                    v1.yCoord += dy * zpercent;
                    v1.zCoord = z0;
                }

                x1 = MathHelper.floor_double(v1.xCoord);
                if (checkType == 5) {
                    --x1;
                }

                y1 = MathHelper.floor_double(v1.yCoord);
                if (checkType == 1) {
                    --y1;
                }

                z1 = MathHelper.floor_double(v1.zCoord);
                if (checkType == 3) {
                    --z1;
                }

                if (x1 == target.xCoord && y1 == target.yCoord && z1 == target.zCoord)
                    return true;

                Block block = world.getBlock(x1, y1, z1);
                int meta = world.getBlockMetadata(x1, y1, z1);
                if (block.canCollideCheck(meta, false)) {
                    if (block.getCollisionBoundingBoxFromPool(world, x1, y1, z1) != null) {
                        MovingObjectPosition movingobjectposition1 = block.collisionRayTrace(world, x1, y1, z1, v1, v2);
                        if (movingobjectposition1 != null && movingobjectposition1.typeOfHit != MovingObjectPosition.MovingObjectType.MISS) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    @Callhook(adder = BlockJarVisitor.class, module = ASMConstants.Modules.Misc)
    public static float getBlockJarEntityCollisionBoxParameter(int index) {
        return EntityCollisionBox.getBlockJarEntityCollisionBoxParameter(index);
    }

    @Callhook(adder = BlockMetalDeviceVisitor.class, module = ASMConstants.Modules.Misc)
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

    @Callhook(adder = TileHoleVisitor.class, module = ASMConstants.Modules.Bugfix)
    public static Packet createTileHoleSyncPacket(S35PacketUpdateTileEntity origin) {
        try {
            return TC4Tweak.INSTANCE.CHANNEL.getPacketFrom(new TileHoleSyncPacket(origin));
        } catch (Exception ex) {
            // fallback to original packet if anything goes wrong
            return origin;
        }
    }

    @Callhook(adder = PacketPlayerCompleteToServerVisitor.class, module = ASMConstants.Modules.ExploitFix)
    public static boolean sanityPlayerComplete(PacketPlayerCompleteToServerAccess packet, MessageContext ctx) {
        if (packet.type() != 0) return true;
        EntityPlayerMP playerEntity = ctx.getServerHandler().playerEntity;
        ResearchItem research = packet.research();
        if (research == null) return false;
        boolean secondary = isSecondaryResearch(research);
        if (secondary) {
            if (hasAspect(playerEntity, research))
                return true;
        }
        log.info(securityMarker, "Player {} sent suspicious packet to complete research {}@{}", playerEntity.getGameProfile(), research.key, research.category);
        return false;
    }

    private static boolean hasAspect(EntityPlayerMP playerEntity, ResearchItem research) {
        return research.tags.aspects.entrySet().stream().noneMatch(e -> e.getValue() != null && !hasAspect(playerEntity, e.getKey(), e.getValue()));
    }

    private static boolean isSecondaryResearch(ResearchItem research) {
        return research.tags != null && research.tags.size() > 0 && (Config.researchDifficulty == -1 || Config.researchDifficulty == 0 && research.isSecondary());
    }

    @Callhook(adder = PacketAspectCombinationToServerVisitor.class, module = ASMConstants.Modules.ExploitFix)
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
        return hasAspect(player, aspect, 0) || table.bonusAspects.getAmount(aspect) > 0;
    }

    private static boolean hasAspect(EntityPlayerMP player, Aspect aspect, int threshold) {
        return Thaumcraft.proxy.playerKnowledge.getAspectPoolFor(player.getCommandSenderName(), aspect) > threshold;
    }

    @Callhook(adder = UtilsVisitor.class, module = ASMConstants.Modules.Bugfix)
    public static ItemStack copyIfNotNull(ItemStack stack) {
        if (!ConfigurationHandler.INSTANCE.isMoreRandomizedLoot()) return stack;
        return stack == null ? null : stack.copy();
    }

    @Callhook(adder = UtilsVisitor.class, module = ASMConstants.Modules.Bugfix)
    public static ItemStack mutateGeneratedLoot(ItemStack stack) {
        if (!ConfigurationHandler.INSTANCE.isMoreRandomizedLoot()) return stack.copy();
        if (stack.getItem() == ConfigItems.itemAmuletVis) {
            ItemAmuletVis ai = (ItemAmuletVis)stack.getItem();

            for(Aspect a : Aspect.getPrimalAspects()) {
                ai.storeVis(stack, a, ThreadLocalRandom.current().nextInt(5) * 100);
            }
        }
        return stack;
    }

    @Callhook(adder = InfusionRecipeVisitor.class, module = ASMConstants.Modules.Bugfix)
    public static boolean infusionItemMatches(ItemStack playerInput, ItemStack recipeSpec, boolean fuzzy) {
        if (playerInput == null) {
            return recipeSpec == null;
        }
        if (recipeSpec == null) return false;
        if (!ThaumcraftApiHelper.areItemStackTagsEqualForCrafting(playerInput, recipeSpec)) return false;
        if (fuzzy) {
            if (ConfigurationHandler.INSTANCE.getInfusionOreDictMode().test(playerInput, recipeSpec)) {
                return true;
            }
        }

        return playerInput.getItem() == recipeSpec.getItem() &&
                (playerInput.getItemDamage() == recipeSpec.getItemDamage() || recipeSpec.getItemDamage() == 32767) &&
                playerInput.stackSize <= playerInput.getMaxStackSize();
    }

    @Callhook(adder = UtilsVisitor.class, module = ASMConstants.Modules.Bugfix)
    public static double getViewDistance(World w) {
        int chunks;
        try {
            if (getRenderDistanceChunks == null) {
                // the latest mcp mapping calls it getRenderDistanceChunks,
                // but it remains as a srg name in the mapping comes with forge 1614
                getRenderDistanceChunks = ReflectionHelper.findMethod(World.class, null, new String[]{"getRenderDistanceChunks", "func_152379_p", "p"});
            }
            chunks = (Integer) getRenderDistanceChunks.invoke(w);
        } catch (ReflectiveOperationException | ReflectionHelper.UnableToFindMethodException ex) {
            log.error("error calling World#getRenderDistanceChunks", ex);
            chunks = 12;
        }
        return chunks * 16D;
    }

    @Callhook(adder = AILiquidGatherVisitor.class, module = ASMConstants.Modules.Bugfix)
    public static void getConnectedFluidBlocks(AILiquidGather thiz, World world, int x, int y, int z, Fluid fluid, ArrayList<Object> sources, float pumpDist, MethodHandle ctor) throws Throwable {
        if (fluid == null) return;
        Set<ChunkCoordinates> seen = new HashSet<>();  // ChunkCoordinates has quite terrible hash function, but there are multiple different optimization mod that optimize this. given the popularity of those I'd say it's fine to use it
        Queue<ChunkCoordinates> toVisit = new ArrayDeque<>();
        ChunkCoordinates origin = new ChunkCoordinates(x, y, z);
        toVisit.add(origin);
        while (!toVisit.isEmpty()) {
            ChunkCoordinates v = toVisit.poll();
            if (seen.contains(v)) continue;
            seen.add(v);
            float dist = v.getDistanceSquared(x, y, z);
            if (dist > pumpDist) continue;
            Block block = world.getBlock(v.posX, v.posY, v.posZ);
            if (block == Blocks.flowing_lava)
                block = Blocks.lava;
            else if (block == Blocks.flowing_water)
                block = Blocks.water;
            Fluid f = FluidRegistry.lookupFluidForBlock(block);
            if (f != fluid) continue;
            if (validFluidBlock(world, fluid, v.posX, v.posY, v.posZ)) {
                sources.add(ctor.invokeExact(thiz, v, dist));
                if (sources.size() >= ConfigurationHandler.INSTANCE.getDecantMaxBlocks())
                    return;
            }
            for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
                toVisit.add(new ChunkCoordinates(v.posX + direction.offsetX, v.posY + direction.offsetY, v.posZ + direction.offsetZ));
            }
        }
    }

    private static boolean validFluidBlock(World world, Fluid fluid, int i, int j, int k) {
        Block bi = world.getBlock(i, j, k);
        if(FluidRegistry.lookupFluidForBlock(bi) != fluid) {
            return false;
        } else {
            if(bi instanceof BlockFluidBase && ((IFluidBlock)bi).canDrain(world, i, j, k)) {
                FluidStack fs = ((IFluidBlock)bi).drain(world, i, j, k, false);
                if(fs != null) {
                    return true;
                }
            }

            return (FluidRegistry.lookupFluidForBlock(bi) == FluidRegistry.WATER && fluid == FluidRegistry.WATER || FluidRegistry.lookupFluidForBlock(bi) == FluidRegistry.LAVA && fluid == FluidRegistry.LAVA) && world.getBlockMetadata(i, j, k) == 0;
        }
    }

    @Callhook(adder = EntityShockOrdVisitor.class, module = ASMConstants.Modules.Bugfix)
    public static Class<? extends Entity> getEarthShockEntityFilter() {
        if (ConfigurationHandler.INSTANCE.getEarthShockHarmMode() == ConfigurationHandler.EarthShockHarmMode.OnlyLiving) {
            return EntityLivingBase.class;
        } else {
            return Entity.class;
        }
    }

    @Callhook(adder = EntityShockOrdVisitor.class, module = ASMConstants.Modules.Bugfix)
    @Callhook(adder = BlockAiryVisitor.class, module = ASMConstants.Modules.Bugfix)
    public static boolean canEarthShockHurt(Entity entity) {
        switch (ConfigurationHandler.INSTANCE.getEarthShockHarmMode()) {
            case OnlyLiving:
                return entity instanceof EntityLivingBase;
            case ExceptItemXp:
                return !(entity instanceof EntityItem) && !(entity instanceof EntityXPOrb);
            case AllEntity:
            default:
                return true;
        }
    }

    @Callhook(adder = TileVisNodeVisitor.class, module = ASMConstants.Modules.VisNetPersist)
    public static void writeLoadedLink(TileVisNode thiz, NBTTagCompound tag) {
        SavedLinkHandler.writeToNBT(thiz, tag);
    }

    @Callhook(adder = TileVisNodeVisitor.class, module = ASMConstants.Modules.VisNetPersist)
    public static List<ChunkCoordinates> readLoadedLink(TileVisNode thiz, NBTTagCompound tag) {
        return SavedLinkHandler.readFromNBT(thiz, tag);
    }

    @Callhook(adder = TileVisNodeVisitor.class, module = ASMConstants.Modules.VisNetPersist)
    public static boolean processSavedLink(ITileVisNode visNode) {
        return SavedLinkHandler.processSavedLink(visNode);
    }

    @Callhook(adder = ItemEssenceVisitor.class, module = ASMConstants.Modules.Misc)
    public static boolean addItemStackToInventory(InventoryPlayer inv, ItemStack itemStack) {
        if (!ConfigurationHandler.INSTANCE.isAlternativeAddStack()) return inv.addItemStackToInventory(itemStack);
        // first check if a partial stack exists
        for (ItemStack stack : inv.mainInventory) {
            // empty or same stack
            if (stack == null || !stack.isItemEqual(itemStack) || !ItemStack.areItemStackTagsEqual(itemStack, stack) ||
                    // space left
                    stack.stackSize <= 0 || stack.stackSize >= stack.getMaxStackSize()) continue;
            int toAdd = Math.min(stack.getMaxStackSize() - stack.stackSize, itemStack.stackSize);
            itemStack.stackSize -= toAdd;
            stack.stackSize += toAdd;
            if (itemStack.stackSize <= 0) return true;
        }
        // then try to add to current active slot if it's now empty
        ItemStack currentStack = inv.mainInventory[inv.currentItem];
        if (currentStack == null || currentStack.getItem() == null || currentStack.stackSize <= 0) {
            inv.mainInventory[inv.currentItem] = itemStack;
            return true;
        }
        // fallback to vanilla logic if both failed
        return inv.addItemStackToInventory(itemStack);
    }

    @Callhook(adder = TileVisRelayVisitor.class, module = ASMConstants.Modules.VisNetPersist)
    public static boolean checkVisRelayParentLoaded(World world, int x, int y, int z) {
        return CommonUtils.isChunkLoaded(world, x, y, z);
    }

    @Callhook(adder = TileVisRelayVisitor.class, module = ASMConstants.Modules.VisNetPersist)
    public static void sendUpdate(TileEntity te) {
        CommonUtils.sendSupplementaryS35(te);
    }
}
