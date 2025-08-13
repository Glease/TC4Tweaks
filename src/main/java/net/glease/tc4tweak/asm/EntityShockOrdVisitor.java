package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

public class EntityShockOrdVisitor extends ClassVisitor {
    public EntityShockOrdVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if ("onImpact".equals(name) || "func_70184_a".equals(name)) {
            log.debug("Visiting {}{}", name, desc);
            return new OnImpactVisitor(api, mv);
        }
        return mv;
    }

    private static class OnImpactVisitor extends MethodVisitor {
        private int state;

        public OnImpactVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            if (state == 0 && opcode == INVOKESTATIC && "canEntityBeSeen".equals(name)) {
                log.trace("Found first canEntityBeSeen");
                state = 1;
            }
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            super.visitJumpInsn(opcode, label);
            if (state == 1 && opcode == IFEQ) {
                log.trace("Found IFEQ. Injecting canEarthShockHurt()");
                state = 2;
                mv.visitVarInsn(ALOAD, 4);
                mv.visitMethodInsn(INVOKESTATIC, ASMCALLHOOKSERVER_INTERNAL_NAME, "canEarthShockHurt", "(Lnet/minecraft/entity/Entity;)Z", false);
                mv.visitJumpInsn(opcode, label);
            }
        }

        @Override
        public void visitEnd() {
            if (state != 2) {
                log.warn("Unexpected end of bytecode. Current state: {}", state);
            }
            super.visitEnd();
        }
    }
}
