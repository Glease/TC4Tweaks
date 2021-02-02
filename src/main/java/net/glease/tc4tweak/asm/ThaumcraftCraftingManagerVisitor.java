package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.HashMap;
import java.util.Map;

import static net.glease.tc4tweak.asm.MyConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;

public class ThaumcraftCraftingManagerVisitor extends ClassVisitor {
	private static final Map<String, String> METHODS_TO_DEFLECT = new HashMap<>();

	static {
		METHODS_TO_DEFLECT.put("findMatchingArcaneRecipe", "(Lnet/minecraft/inventory/IInventory;Lnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/item/ItemStack;");
		METHODS_TO_DEFLECT.put("findMatchingArcaneRecipeAspects", "(Lnet/minecraft/inventory/IInventory;Lnet/minecraft/entity/player/EntityPlayer;)Lthaumcraft/api/aspects/AspectList;");
	}

	public ThaumcraftCraftingManagerVisitor(int api, ClassVisitor cv) {
		super(api, cv);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (!METHODS_TO_DEFLECT.getOrDefault(name, "").equals(desc)) {
			return mv;
		}
		TC4Transformer.log.debug("Replacing {}", name);
		ASMUtils.writeMethodDeflected(ASMCALLHOOKSERVER_INTERNAL_NAME, name, mv, null, desc);
		return null;
	}
}
