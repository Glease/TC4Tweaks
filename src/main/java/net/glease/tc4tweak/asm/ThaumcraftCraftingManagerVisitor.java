package net.glease.tc4tweak.asm;

import com.google.common.collect.ImmutableMap;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.Map;

import static net.glease.tc4tweak.asm.MyConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;
import static org.objectweb.asm.Opcodes.*;

public class ThaumcraftCraftingManagerVisitor extends ClassVisitor {
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
		if ("getObjectTags2".equals(name) && "(Lnet/minecraft/item/ItemStack;)Lthaumcraft/api/aspects/AspectList;".equals(desc)) {
			return new GetObjectTagsVisitor(api, mv);
		} else if (METHODS_TO_DEFLECT.getOrDefault(name, "").equals(desc)) {
			TC4Transformer.log.debug("Replacing {}", name);
			ASMUtils.writeMethodDeflected(ASMCALLHOOKSERVER_INTERNAL_NAME, name, mv, null, desc);
			return null;
		} else {
			return mv;
		}
	}

	private static class GetObjectTagsVisitor extends MethodVisitor {
		private Label jumpTarget = new Label();
		private Label oldJumpTarget;
		private int state = 0;

		public GetObjectTagsVisitor(int api, MethodVisitor mv) {
			super(api, mv);
		}


		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String desc) {
			if (state == 0 && opcode == GETSTATIC && "thaumcraft/api/ThaumcraftApi".equals(owner) && "objectTags".equals(name)) {
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKESTATIC, ASMCALLHOOKSERVER_INTERNAL_NAME, "getBaseObjectTags", "(Lnet/minecraft/item/ItemStack;)Lthaumcraft/api/aspects/AspectList;", false);
				mv.visitInsn(DUP);
				mv.visitVarInsn(ASTORE, 3);
				mv.visitJumpInsn(IFNONNULL, jumpTarget);
				state = 1;
			}
			super.visitFieldInsn(opcode, owner, name, desc);
		}

		@Override
		public void visitJumpInsn(int opcode, Label label) {
			if (state == 1 && opcode == IFNONNULL) {
				super.visitJumpInsn(opcode, jumpTarget);
				oldJumpTarget = label;
				state = 2;
			} else {
				super.visitJumpInsn(opcode, label);
			}
		}

		@Override
		public void visitLabel(Label label) {
			if (state == 2 && label == oldJumpTarget) {
				super.visitLabel(jumpTarget);
				state = 3;
			} else {
				super.visitLabel(label);
			}
		}

		@Override
		public void visitMaxs(int maxStack, int maxLocals) {
			if (state != 3)
				throw new RuntimeException("state should be 3 but is " + state);
			super.visitMaxs(maxStack, maxLocals);
		}
	}
}
