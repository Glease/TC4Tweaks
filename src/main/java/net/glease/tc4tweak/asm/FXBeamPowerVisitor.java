package net.glease.tc4tweak.asm;

import org.lwjgl.opengl.GL11;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.SIPUSH;

public class FXBeamPowerVisitor extends ClassVisitor {
    public FXBeamPowerVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals("func_70539_a") || name.equals("renderParticle") || name.equals("renderFlare")) {
            log.debug("Visiting {}#{}", name, desc);
            return new AddEnableBlendVisitor(api, mv);
        }
        return mv;
    }

    private static class AddEnableBlendVisitor extends MethodVisitor {
        private int order;

        public AddEnableBlendVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            boolean isBlendFunc = opcode == INVOKESTATIC && name.equals("glBlendFunc");
            int order = this.order;
            if (isBlendFunc && order >= 2) {
                log.warn("Unexpected glBlendFunc call. Occurred {} times so far.", order);
            }
            if (isBlendFunc && order == 0) {
                log.trace("Injecting GL11.glEnable(GL11.GL_BLEND)");
                mv.visitIntInsn(SIPUSH, GL11.GL_BLEND);
                mv.visitMethodInsn(INVOKESTATIC, owner, "glEnable", "(I)V", false);
                this.order = 1;
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            if (isBlendFunc && order == 1) {
                log.trace("Injecting GL11.glDisable(GL11.GL_BLEND)");
                mv.visitIntInsn(SIPUSH, GL11.GL_BLEND);
                mv.visitMethodInsn(INVOKESTATIC, owner, "glDisable", "(I)V", false);
                this.order = 2;
            }
        }
    }
}
