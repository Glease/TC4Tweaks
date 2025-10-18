package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOK_INTERNAL_NAME;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

class TileRunicMatrixRendererVisitor extends ClassVisitor {

    public TileRunicMatrixRendererVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals("renderInfusionMatrix") && desc.equals("(Lthaumcraft/common/tiles/TileInfusionMatrix;DDDF)V")) {
            log.debug("Visiting {}{}", name, desc);
            return new LimitTranslatefVisitor(api, mv);
        } else {
            return mv;
        }
    }

    private static class LimitTranslatefVisitor extends MethodVisitor {
        public LimitTranslatefVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            if (opcode == FSTORE && var == 10) {
                log.trace("Adding infusionMatrixLimitInstability() call");
                mv.visitMethodInsn(INVOKESTATIC, ASMCALLHOOK_INTERNAL_NAME, "infusionMatrixLimitInstability", "(F)F", false);
            }
            super.visitVarInsn(opcode, var);
        }
    }
}
