package net.glease.tc4tweak.asm;

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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import static org.objectweb.asm.Opcodes.ASM5;

public class TC4Transformer implements IClassTransformer {
	static final Logger log = LogManager.getLogger("TC4TweakTransformer");
	private static final boolean DEBUG = Boolean.getBoolean("glease.debugasm");
	private final Map<String, TransformerFactory> transformers = ImmutableMap.<String, TransformerFactory>builder()
			.put("thaumcraft.client.gui.GuiResearchRecipe", new TransformerFactory(GuiResearchRecipeVisitor::new, Side.CLIENT))
			.put("thaumcraft.client.gui.GuiResearchTable", new TransformerFactory(GuiResearchTableVisitor::new, Side.CLIENT))
			.put("thaumcraft.client.gui.MappingThread", new TransformerFactory(MappingThreadVisitor::new, Side.CLIENT))
			.put("thaumcraft.common.tiles.TileMagicWorkbench", new TransformerFactory(TileMagicWorkbenchVisitor::new, Side.CLIENT))
			.put("thaumcraft.client.fx.other.FXSonic", new TransformerFactory(FXSonicVisitor::new, Side.CLIENT))
			.put("thaumcraft.api.research.ResearchCategories", new TransformerFactory(ResearchCategoriesVisitor::new))
			.put("thaumcraft.common.container.ContainerArcaneWorkbench", new TransformerFactory(ContainerArcaneWorkbenchVisitor::new))
			.put("thaumcraft.common.items.wands.ItemWandCasting", new TransformerFactory(ItemWandCastingVisitor::new, false))
			.put("thaumcraft.common.lib.crafting.ThaumcraftCraftingManager", new TransformerFactory(ThaumcraftCraftingManagerVisitor::new))
			.put("thaumcraft.common.lib.research.ScanManager", new TransformerFactory(ScanManagerVisitor::new) {
				@Override
				public boolean isInactive() {
					return super.isInactive() || LoadingPlugin.gt6;
				}
			})
			.build();

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		TransformerFactory factory = transformers.get(name);
		if (factory == null || factory.isInactive()) {
			return basicClass;
		}
		log.info("Transforming class {}", name);
		ClassReader cr = new ClassReader(basicClass);
		ClassWriter cw = new ClassWriter(factory.isExpandFrames() ? ClassWriter.COMPUTE_FRAMES : 0);
		boolean success = false;
		// we are very probably the last one to run.
		try {
			if (DEBUG) {
				try (PrintWriter pw = new PrintWriter(new File(debugOutputDir, name + ".txt"), "UTF-8")) {
					cr.accept(factory.apply(ASM5, new TraceClassVisitor(cw, pw)), factory.isExpandFrames() ? 0 : ClassReader.SKIP_FRAMES);
					success = true;
				} catch (IOException e) {
					log.warn("Unable to dump debug output. Redoing transform without debug!", e);
				}
			}
			if (!success) {
				cr.accept(factory.apply(ASM5, cw), (factory.isExpandFrames() ? ClassReader.SKIP_DEBUG : ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG));
			}
		} catch (Exception e) {
			Util.catching(e);
		}
		byte[] transformedBytes = cw.toByteArray();
		if (transformedBytes == null || transformedBytes.length == 0) {
			if (DEBUG) {
				Util.catching(new RuntimeException("Null or empty byte array created. This will not work well!"));
			} else {
				log.fatal("Null or empty byte array created. Transforming will rollback as a last effort attempt to make things work! However features will not function!");
				return basicClass;
			}
		}
		return transformedBytes;
	}

	private static class Util {
		static void catching(Exception e) {
			log.fatal("Something went very wrong with class transforming! Aborting!!!", e);
			throw new ReportedException(CrashReport.makeCrashReport(e, "Transforming class"));
		}
	}
}
