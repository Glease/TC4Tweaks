package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.LoadingPlugin.dev;
import static net.glease.tc4tweak.asm.MyConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;
import static org.objectweb.asm.Opcodes.*;

public class ContainerArcaneWorkbenchVisitor extends ClassVisitor {
	public ContainerArcaneWorkbenchVisitor(int api, ClassVisitor cv) {
		super(api, cv);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (desc.equals("(Lnet/minecraft/inventory/IInventory;)V") && dev ? name.equals("onCraftMatrixChanged") : name.equals("func_75130_a")) {
			TC4Transformer.log.debug("Replacing {} with ", name);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, "thaumcraft/common/container/ContainerArcaneWorkbench", "tileEntity", "Lthaumcraft/common/tiles/TileArcaneWorkbench;");
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, "thaumcraft/common/container/ContainerArcaneWorkbench", "ip", "Lnet/minecraft/entity/player/InventoryPlayer;");
			mv.visitMethodInsn(INVOKESTATIC, ASMCALLHOOKSERVER_INTERNAL_NAME, "onArcaneWorkbenchChanged", "(Lthaumcraft/common/tiles/TileArcaneWorkbench;Lnet/minecraft/entity/player/InventoryPlayer;)V", false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
			return null;
		}
		return mv;
	}
}
