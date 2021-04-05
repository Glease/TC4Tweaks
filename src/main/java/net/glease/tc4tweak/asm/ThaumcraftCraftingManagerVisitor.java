package net.glease.tc4tweak.asm;

import com.google.common.collect.ImmutableMap;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.Map;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;

class ThaumcraftCraftingManagerVisitor extends ClassVisitor {
	private static final Map<String, String> METHODS_TO_DEFLECT = ImmutableMap.of(
			"getObjectTags", "(Lnet/minecraft/item/ItemStack;)Lthaumcraft/api/aspects/AspectList;",
			"findMatchingArcaneRecipe", "(Lnet/minecraft/inventory/IInventory;Lnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/item/ItemStack;",
			"findMatchingArcaneRecipeAspects", "(Lnet/minecraft/inventory/IInventory;Lnet/minecraft/entity/player/EntityPlayer;)Lthaumcraft/api/aspects/AspectList;"
	);

	public ThaumcraftCraftingManagerVisitor(int api, ClassVisitor cv) {
		super(api, cv);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (METHODS_TO_DEFLECT.getOrDefault(name, "").equals(desc)) {
			TC4Transformer.log.debug("Replacing {}", name);
			ASMUtils.writeMethodDeflected(ASMCALLHOOKSERVER_INTERNAL_NAME, name, mv, null, desc);
			return null;
		} else {
			return mv;
		}
	}
}
