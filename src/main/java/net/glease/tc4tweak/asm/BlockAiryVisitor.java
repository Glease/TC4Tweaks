package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

class BlockAiryVisitor extends ClassVisitor {
    public BlockAiryVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if ("func_149670_a".equals(name) || "onEntityCollidedWithBlock".equals(name)) {
            log.debug("Visiting {}{}", name, desc);
            return new OnEntityCollidedWithBlockVisitor(BlockAiryVisitor.this.api, mv);
        }
        return mv;
    }

    private static class OnEntityCollidedWithBlockVisitor extends MethodVisitor {
        private int state;
        public OnEntityCollidedWithBlockVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            super.visitIntInsn(opcode, operand);
            if ((opcode == BIPUSH || opcode == SIPUSH) && operand == 10) {
                String mnemonic = opcode == BIPUSH ? "BIPUSH" : "SIPUSH";
                log.trace("Found needle {} {}", mnemonic, operand);
                state = 1;
            }
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            super.visitJumpInsn(opcode, label);
            if (state == 1&& opcode == IF_ICMPNE) {
                state = 2;
                log.trace("Insert instanceof EntityLivingBase after IF_ICMPNE");
                mv.visitVarInsn(ALOAD, 5);
                mv.visitMethodInsn(INVOKESTATIC, ASMCALLHOOKSERVER_INTERNAL_NAME, "canEarthShockHurt", "(Lnet/minecraft/entity/Entity;)Z", false);
                mv.visitJumpInsn(IFEQ, label);
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
