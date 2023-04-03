package net.glease.tc4tweak.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

class PacketSanityMethodVisitor extends MethodVisitor {
    private final Label a = new Label();
    private final String targetName;
    private final String targetDesc;
    private Label b;

    public PacketSanityMethodVisitor(int api, MethodVisitor mv, String targetName, String targetDesc) {
        super(api, mv);
        this.targetName = targetName;
        this.targetDesc = targetDesc;
    }

    @Override
    public void visitCode() {
        super.visitCode();
        log.trace("Injected sanity check call");
        // 0 is handler. 1 is real message.
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKESTATIC, ASMCALLHOOKSERVER_INTERNAL_NAME, targetName, targetDesc, false);
        mv.visitJumpInsn(IFEQ, a);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        if (b == null && opcode == IFNULL) {
            log.trace("Found world null check. Label {} recorded.", label);
            b = label;
            super.visitJumpInsn(opcode, a);
        } else {
            super.visitJumpInsn(opcode, label);
        }
    }

    @Override
    public void visitLabel(Label label) {
        if (label == b) {
            log.trace("Replaced {} with {}.", label, a);
            super.visitLabel(a);
        } else {
            super.visitLabel(label);
        }
    }
}
