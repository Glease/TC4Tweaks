package net.glease.tc4tweak.asm;

import java.io.File;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.ImmutableMap;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.crash.CrashReport;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.util.ReportedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.TraceClassVisitor;

import static net.glease.tc4tweak.asm.LoadingPlugin.dev;
import static net.glease.tc4tweak.asm.LoadingPlugin.getDebugOutputDir;
import static net.glease.tc4tweak.asm.LoadingPlugin.isDebug;
import static org.objectweb.asm.Opcodes.ASM5;

/*
 * Logging convention:
 * 1. INFO
 *   * Start transforming a class
 *   * Not transforming a class due to wrong side
 * 2. WARNING
 *   * Error saving debug output
 *   * Not transforming a class due to mod compat
 * 3. ERROR
 *   * Unrecoverable transform error
 *   * Input class bytes does not match expectation
 * 4. Trace
 *   * Instruction level transforming detail.
 * 5. DEBUG
 *   * Everything else
 */
public class TC4Transformer implements IClassTransformer {
    static final Logger log = LogManager.getLogger("TC4TweakTransformer");
    private static final ConcurrentMap<String, Integer> transformCounts = new ConcurrentHashMap<>();
    private final Map<String, TransformerFactory> transformers = ImmutableMap.<String, TransformerFactory>builder()
            .put("net.minecraft.util.ChunkCoordinates", ChunkCoordinatesVisitor.createFactory())
            .put("com.kentington.thaumichorizons.client.renderer.tile.TileEtherealShardRender", NodeLikeRendererVisitor.createFactory(dev ? "func_147500_a" : "renderTileEntityAt"))
            .put("makeo.gadomancy.client.renderers.tile.RenderTileNodeBasic", NodeLikeRendererVisitor.createFactory("renderNode"))
            .put("thaumcraft.api.aspects.AspectList", new TransformerFactory(AspectListVisitor::new))
            .put("thaumcraft.api.WorldCoordinates", new TransformerFactory(HashCodeVisitor::new))
            .put("thaumcraft.api.BlockCoordinates", new TransformerFactory(HashCodeVisitor::new))
            .put("thaumcraft.api.visnet.VisNetHandler", new TransformerFactory(VisNetHandlerVisitor::new))
            .put("thaumcraft.api.crafting.InfusionRecipe", new TransformerFactory(InfusionRecipeVisitor::new))
            .put("thaumcraft.client.fx.beams.FXBeamPower", new TransformerFactory(FXBeamPowerVisitor::new, Side.CLIENT))
            .put("thaumcraft.client.gui.GuiResearchBrowser", new TransformerFactory(GuiResearchBrowserVisitor::new, Side.CLIENT))
            .put("thaumcraft.client.gui.GuiResearchRecipe", new TransformerFactory(GuiResearchRecipeVisitor::new, Side.CLIENT))
            .put("thaumcraft.client.gui.GuiResearchTable", new TransformerFactory(AddHandleMouseInputVisitor::new, Side.CLIENT))
            .put("thaumcraft.client.gui.MappingThread", new TransformerFactory(MappingThreadVisitor::new, Side.CLIENT))
            .put("thaumcraft.client.lib.UtilsFX", new TransformerFactory(UtilsFXVisitor::new, Side.CLIENT))
            .put("thaumcraft.client.renderers.tile.TileAlchemyFurnaceAdvancedRenderer", TileAlchemyFurnaceAdvancedRendererVisitor.FACTORY)
            .put("thaumcraft.client.renderers.tile.TileChestHungryRenderer", TESRGetBlockTypeNullSafetyVisitor.FACTORY)
            .put("thaumcraft.client.renderers.tile.TileEldritchCapRenderer", TESRGetBlockTypeNullSafetyVisitor.FACTORY)
            .put("thaumcraft.client.renderers.tile.TileEldritchObeliskRenderer", TESRGetBlockTypeNullSafetyVisitor.FACTORY)
            .put("thaumcraft.client.renderers.tile.TileManaPodRenderer", TESRGetBlockTypeNullSafetyVisitor.FACTORY)
            .put("thaumcraft.client.renderers.tile.TileNodeConverterRenderer", TESRGetBlockTypeNullSafetyVisitor.FACTORY)
            .put("thaumcraft.client.renderers.tile.TileNodeStabilizerRenderer", TESRGetBlockTypeNullSafetyVisitor.FACTORY)
            .put("thaumcraft.client.renderers.tile.TileNodeRenderer", NodeLikeRendererVisitor.createFactory("renderNode"))
            .put("thaumcraft.client.renderers.tile.ItemNodeRenderer", new TransformerFactory(ItemNodeRendererVisitor::new, Side.CLIENT))
            .put("thaumcraft.common.tiles.TileMagicWorkbench", new TransformerFactory(TileMagicWorkbenchVisitor::new, Side.CLIENT))
            .put("thaumcraft.client.fx.other.FXSonic", new TransformerFactory(MakeModelStaticVisitor::new, Side.CLIENT))
            .put("thaumcraft.api.research.ResearchCategories", new TransformerFactory(ResearchCategoriesVisitor::new))
            .put("thaumcraft.api.ThaumcraftApi", new TransformerFactory(ThaumcraftApiVisitor::new))
            .put("thaumcraft.common.blocks.BlockAiry", new TransformerFactory(BlockAiryVisitor::new))
            .put("thaumcraft.common.blocks.BlockFluxGas", new TransformerFactory(BlockFluxGasVisitor::new))
            .put("thaumcraft.common.blocks.BlockJar", new TransformerFactory(BlockJarVisitor::new))
            .put("thaumcraft.common.blocks.BlockMagicalLeaves", BlockMagicalLeavesVisitor.createFactory())
            .put("thaumcraft.common.blocks.BlockMagicalLog", BlockMagicalLogVisitor.createFactory())
            .put("thaumcraft.common.blocks.BlockMetalDevice", new TransformerFactory(BlockMetalDeviceVisitor::new))
            .put("thaumcraft.common.container.ContainerArcaneWorkbench", new TransformerFactory(ContainerArcaneWorkbenchVisitor::new))
            .put("thaumcraft.common.entities.ai.fluid.AILiquidGather", new TransformerFactory(AILiquidGatherVisitor::new))
            .put("thaumcraft.common.entities.ai.inventory.AIItemPickup", new TransformerFactory(AIItemPickupVisitor::new))
            .put("thaumcraft.common.entities.golems.EntityGolemBase", new TransformerFactory(EntityGolemBaseVisitor::new))
            .put("thaumcraft.common.entities.golems.ItemGolemBell", ReadMarkerNoCastVisitor.createFactory("getMarkers", "(Lnet/minecraft/item/ItemStack;)Ljava/util/ArrayList;"))
            .put("thaumcraft.common.entities.projectile.EntityShockOrb", new TransformerFactory(EntityShockOrdVisitor::new))
            .put("thaumcraft.common.items.equipment.ItemElementalShovel", new TransformerFactory(ItemElementalShovelVisitor::new, true))
            .put("thaumcraft.common.items.wands.ItemWandCasting", new TransformerFactory(ItemWandCastingVisitor::new, true))
            .put("thaumcraft.common.lib.crafting.ThaumcraftCraftingManager", new TransformerFactory(ThaumcraftCraftingManagerVisitor::new))
            .put("thaumcraft.common.lib.network.playerdata.PacketAspectCombinationToServer", new TransformerFactory(PacketAspectCombinationToServerVisitor::new))
            .put("thaumcraft.common.lib.network.playerdata.PacketPlayerCompleteToServer", new TransformerFactory(PacketPlayerCompleteToServerVisitor::new))
            .put("thaumcraft.common.lib.research.ScanManager", new TransformerFactory(ScanManagerVisitor::new) {
                @Override
                public boolean isInactive() {
                    if (super.isInactive()) return true;
                    if (LoadingPlugin.gt6) {
                        log.warn("generateItemHash patch disabled for GT6 compat.");
                        return true;
                    }
                    return false;
                }
            })
            .put("thaumcraft.common.lib.utils.EntityUtils", new TransformerFactory(EntityUtilsVisitor::new))
            .put("thaumcraft.common.lib.utils.Utils", new TransformerFactory(UtilsVisitor::new))
            .put("thaumcraft.common.lib.world.dim.CellLoc", new TransformerFactory(HashCodeVisitor::new))
            .put("thaumcraft.common.lib.world.dim.MazeHandler", new TransformerFactory(MazeHandlerVisitor::new))
            .put("thaumcraft.common.tiles.TileChestHungry", new TransformerFactory(TileChestHungryVisitor::new))
            .put("thaumcraft.common.tiles.TileInfusionMatrix", new TransformerFactory(TileInfusionMatrixVisitor::new))
            .put("thaumcraft.common.tiles.TileHole", new TransformerFactory(TileHoleVisitor::new))
            .put("thaumcraft.common.tiles.TileTube", new TransformerFactory(AddOnDataPacketMarkBlockForRenderUpdateVisitor::new))
            .put("thaumcraft.common.tiles.TileJarFillable", new TransformerFactory(AddOnDataPacketMarkBlockForRenderUpdateVisitor::new))
//            .put("", new TransformerFactory(AddOnDataPacketMarkBlockForRenderUpdateVisitor::new))
            .put("thaumcraft.common.Thaumcraft", new TransformerFactory(ThaumcraftVisitor::new))
            .build();

    static void catching(Exception e) {
        log.fatal("Something went very wrong with class transforming! Aborting!!!", e);
        RuntimeException exception;
        try {
            exception = new ReportedException(CrashReport.makeCrashReport(e, "Transforming class"));
        } catch (Throwable e2) {
            // presumably because this happened too early
            exception = new RuntimeException("Transforming class", e);
        }
        throw exception;
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        TransformerFactory factory = transformers.get(name);
        if (factory == null || factory.isInactive()) {
            return basicClass;
        }
        log.info("Transforming class {}", name);
        ClassReader cr = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(factory.isExpandFrames() ? ClassWriter.COMPUTE_FRAMES : 0);
        // we are very probably the last one to run.
        byte[] transformedBytes = null;
        if (isDebug()) {
            int curCount = transformCounts.compute(transformedName, (k, v) -> v == null ? 0 : v + 1);
            String infix = curCount == 0 ? "" : "_" + curCount;
            try (PrintWriter origOut = new PrintWriter(new File(getDebugOutputDir(), name + infix + "_orig.txt"), "UTF-8");
                 PrintWriter tranOut = new PrintWriter(new File(getDebugOutputDir(), name + infix + "_tran.txt"), "UTF-8")) {
                cr.accept(new TraceClassVisitor(factory.apply(ASM5, new TraceClassVisitor(cw, tranOut)), origOut), factory.isExpandFrames() ? ClassReader.SKIP_FRAMES : 0);
                transformedBytes = cw.toByteArray();
            } catch (Exception e) {
                log.warn("Unable to transform with debug output on. Now retrying without debug output.", e);
            }
        }
        if (transformedBytes == null || transformedBytes.length == 0) {
            try {
                cr.accept(factory.apply(ASM5, cw), factory.isExpandFrames() ? ClassReader.SKIP_FRAMES : 0);
                transformedBytes = cw.toByteArray();
            } catch (Exception e) {
                catching(e);
            }
        }
        if (transformedBytes == null || transformedBytes.length == 0) {
            if (isDebug()) {
                catching(new RuntimeException("Null or empty byte array created. This will not work well!"));
            } else {
                log.error("Null or empty byte array created. Transforming will rollback as a last effort attempt to make things work! However features will not function!");
                return basicClass;
            }
        }
        return transformedBytes;
    }
}
