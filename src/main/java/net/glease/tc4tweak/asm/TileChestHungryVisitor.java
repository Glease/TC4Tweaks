package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.LoadingPlugin.dev;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

class TileChestHungryVisitor extends ClassVisitor {
    public TileChestHungryVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        //func_70298_a(II)Lnet/minecraft/item/ItemStack;
        if (name.equals(dev ? "decrStackSize" : "func_70298_a")) {
            log.debug("Visiting {}{}", name, desc);
            return new DecrStackSizeVisitor(api, mv);
        }
        return mv;
    }

    private static class DecrStackSizeVisitor extends MethodVisitor {
        private boolean added = false;
        public DecrStackSizeVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            super.visitJumpInsn(opcode, label);
            if (opcode == IFNULL) {
                if (added) {
                    log.error("Unrecognized Thaumcraft class files. Things might not work! Is someone else patching it?");
                    return;
                } else {
                    added = true;
                    log.trace("Adding missing if branch after IFNULL");
                }
                Label lblDynamicContentMarker = new Label();
                mv.visitLabel(lblDynamicContentMarker);
                mv.visitLineNumber(114514, lblDynamicContentMarker);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, "thaumcraft/common/tiles/TileChestHungry", "chestContents", "[Lnet/minecraft/item/ItemStack;");
                mv.visitVarInsn(ILOAD, 1);
                mv.visitInsn(AALOAD);
                mv.visitFieldInsn(GETFIELD, "net/minecraft/item/ItemStack", dev ? "stackSize" : "field_77994_a", "I");
                mv.visitVarInsn(ILOAD, 2);
                Label lblNextIfBranch = new Label();
                mv.visitJumpInsn(IF_ICMPGT, lblNextIfBranch);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, "thaumcraft/common/tiles/TileChestHungry", "chestContents", "[Lnet/minecraft/item/ItemStack;");
                mv.visitVarInsn(ILOAD, 1);
                mv.visitInsn(AALOAD);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, "thaumcraft/common/tiles/TileChestHungry", "chestContents", "[Lnet/minecraft/item/ItemStack;");
                mv.visitVarInsn(ILOAD, 1);
                mv.visitInsn(ACONST_NULL);
                mv.visitInsn(AASTORE);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKEVIRTUAL, "thaumcraft/common/tiles/TileChestHungry", dev ? "markDirty" : "func_70296_d", "()V", false);
                mv.visitInsn(ARETURN);
                mv.visitLabel(lblNextIfBranch);
                mv.visitFrame(F_SAME, 0, null, 0, null);
            }
        }
    }
}
