package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

class ThaumcraftVisitor extends ClassVisitor {
    public ThaumcraftVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if ("addWarpToPlayer".equals(name) || "addStickyWarpToPlayer".equals(name)) {
            log.debug("Visiting {}", name);
            return new AddFakePlayerGuardVisitor(api, mv);
        }
        return mv;
    }
    
    private static class AddFakePlayerGuardVisitor extends MethodVisitor {
        public AddFakePlayerGuardVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            log.debug("Adding fake player check");
            Label l = new Label(), l2 = new Label(), l3 = new Label();
            mv.visitLabel(l);
            mv.visitLineNumber(114514, l);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitTypeInsn(INSTANCEOF, "net/minecraftforge/common/util/FakePlayer");
            mv.visitJumpInsn(IFNE, l3);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitTypeInsn(INSTANCEOF, "thaumcraft/common/lib/FakeThaumcraftPlayer");
            mv.visitJumpInsn(IFEQ, l2);
            mv.visitLabel(l3);
            mv.visitFrame(F_SAME, 3, null, 0, null);
            mv.visitInsn(RETURN);
            mv.visitLabel(l2);
            mv.visitFrame(F_SAME, 3, null, 0, null);
        }
    }
}
