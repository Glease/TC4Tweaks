package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.LoadingPlugin.dev;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

class ItemElementalShovelVisitor extends ClassVisitor {
    public ItemElementalShovelVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals(dev ? "onItemUse" : "func_77648_a")) {
            log.debug("Visiting {}{}", name, desc);
            return new OnItemUseVisitor(mv);
        }
        return mv;
    }

    private class OnItemUseVisitor extends MethodVisitor {
        public OnItemUseVisitor(MethodVisitor mv) {
            super(ItemElementalShovelVisitor.this.api, mv);
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == IRETURN) {
                log.trace("Modifying return value");
                mv.visitInsn(POP);
                mv.visitVarInsn(ALOAD, 3);
                mv.visitFieldInsn(GETFIELD, "net/minecraft/world/World", "capturedBlockSnapshots", "Ljava/util/ArrayList;");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/ArrayList", "size", "()I", false);
                Label l = new Label(), l2 = new Label();
                mv.visitJumpInsn(IFLE, l);
                mv.visitInsn(ICONST_1);
                mv.visitJumpInsn(GOTO, l2);
                mv.visitLabel(l);
                mv.visitFrame(F_SAME, 0, null, 0, null);
                mv.visitInsn(ICONST_0);
                mv.visitLabel(l2);
                mv.visitFrame(F_SAME1, 0, null, 1, new Object[] {INTEGER});
            }
            super.visitInsn(opcode);
        }
    }
}
