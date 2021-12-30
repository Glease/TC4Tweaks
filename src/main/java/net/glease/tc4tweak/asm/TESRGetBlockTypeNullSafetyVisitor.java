package net.glease.tc4tweak.asm;

import cpw.mods.fml.relauncher.Side;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.LoadingPlugin.dev;
import static org.objectweb.asm.Opcodes.*;

class TESRGetBlockTypeNullSafetyVisitor extends ClassVisitor {
    static final TransformerFactory FACTORY = new TransformerFactory(TESRGetBlockTypeNullSafetyVisitor::new, Side.CLIENT);

    public TESRGetBlockTypeNullSafetyVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if ((name.equals("renderTileEntityAt") || name.equals("func_147500_a")) && desc.equals("(Lnet/minecraft/tileentity/TileEntity;DDDF)V")) {
            TC4Transformer.log.debug("Visiting {}", name);
            return new RenderTileEntityAtVisitor(api, mv);
        }
        return mv;
    }

    private static class RenderTileEntityAtVisitor extends MethodVisitor {
        public RenderTileEntityAtVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            TC4Transformer.log.debug("Injected if guard at HEAD");
            Label lblSkipReturn = new Label();
            // no world probably means the block is being rendered in inventory
            // it probably have guards against getBlockType() == null regardless, so don't do our checks.
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "net/minecraft/tileentity/TileEntity", dev ? "hasWorldObj" : "func_145830_o", "()Z", false);
            mv.visitJumpInsn(IFEQ, lblSkipReturn);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "net/minecraft/tileentity/TileEntity", dev ? "getBlockType" : "func_145838_q", "()Lnet/minecraft/block/Block;", false);
            mv.visitJumpInsn(IFNONNULL, lblSkipReturn);
            mv.visitInsn(RETURN);
            mv.visitLabel(lblSkipReturn);
        }
    }
}
