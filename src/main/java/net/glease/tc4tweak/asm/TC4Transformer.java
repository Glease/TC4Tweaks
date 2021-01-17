package net.glease.tc4tweak.asm;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static org.objectweb.asm.Opcodes.ASM5;

public class TC4Transformer implements IClassTransformer {
	static final Logger log = LogManager.getLogger("TC4TweakTransformer");
	private static final boolean DEBUG = Boolean.getBoolean("glease.debugasm");
	private final Map<String, BiFunction<Integer, ClassVisitor, ClassVisitor>> transformers = new HashMap<>();
	private final Map<String, BiFunction<Integer, ClassVisitor, ClassVisitor>> serverTransformers = new HashMap<>();

	{
		transformers.put("thaumcraft.client.gui.GuiResearchTable", GuiResearchTableVisitor::new);
		transformers.put("thaumcraft.common.tiles.TileMagicWorkbench", TileMagicWorkbenchVisitor::new);
		transformers.put("thaumcraft.client.fx.other.FXSonic", FXSonicVisitor::new);
		serverTransformers.put("thaumcraft.api.research.ResearchCategories", ResearchCategoriesVisitor::new);
		serverTransformers.put("thaumcraft.common.lib.research.ScanManager", ScanManagerVisitor::new);
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		BiFunction<Integer, ClassVisitor, ClassVisitor> factory = serverTransformers.get(name);
		if (factory == null) {
			/*
			 query transformers first as a hack to not load FMLCommonHandler too early
			 otherwise you need to do an expensive class lookup to determine if said class is initialized
			 and potentially screw up early class loading order
			*/
			factory = transformers.get(name);
			if (factory == null || isServerSide())
				return basicClass;
		}
		log.info("Transforming class {}", name);
		ClassReader cr = new ClassReader(basicClass);
		ClassWriter cw = new ClassWriter(0);
		if (DEBUG) {
			try (PrintWriter pw = new PrintWriter(name + ".txt", "UTF-8")) {
				cr.accept(factory.apply(ASM5, new TraceClassVisitor(cw, pw)), ClassReader.SKIP_DEBUG);
			} catch (FileNotFoundException | UnsupportedEncodingException e) {
				log.warn("Unable to dump debug output", e);
				cr.accept(factory.apply(ASM5, cw), ClassReader.SKIP_DEBUG);
			}
		} else {
			cr.accept(factory.apply(ASM5, cw), ClassReader.SKIP_DEBUG);
		}
		return cw.toByteArray();
	}

	private static boolean isServerSide() {
		return FMLCommonHandler.instance().getSide() == Side.SERVER;
	}
}
