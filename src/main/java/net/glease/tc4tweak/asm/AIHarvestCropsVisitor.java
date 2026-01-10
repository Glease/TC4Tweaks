package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.Objects;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

public class AIHarvestCropsVisitor extends ClassVisitor {
    public AIHarvestCropsVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        if (name.equals("harvest")) {
            log.debug("Visiting {}{}", name, desc);
            return new HarvestMethodVisitor(api, mv);
        }
        return mv;
    }

    private static class HarvestMethodVisitor extends MethodVisitor {
        private Label lastLabel;
        private Label loopStartLabel;

        private boolean nextJump = false;
        private boolean addedFooter = false;

        public HarvestMethodVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitCode() {
            super.visitCode();

            log.trace("Injecting saveManaBeanType");

            Label lblDynamicContentMarker = new Label();
            mv.visitLabel(lblDynamicContentMarker);
            mv.visitLineNumber(114514, lblDynamicContentMarker);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "thaumcraft/common/entities/ai/interact/AIHarvestCrops", "theWorld", "Lnet/minecraft/world/World;");

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "thaumcraft/common/entities/ai/interact/AIHarvestCrops", "xx", "I");

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "thaumcraft/common/entities/ai/interact/AIHarvestCrops", "yy", "I");

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "thaumcraft/common/entities/ai/interact/AIHarvestCrops", "zz", "I");

            mv.visitMethodInsn(INVOKESTATIC, ASMCALLHOOKSERVER_INTERNAL_NAME, "saveManaBeanType", "(Lnet/minecraft/world/World;III)V", false);
        }

        @Override
        public void visitLabel(Label label) {
            super.visitLabel(label);

            lastLabel = label;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);

            if (loopStartLabel != null) return;

            if (opcode == INVOKEINTERFACE && Objects.equals(owner, "java/util/Iterator") && Objects.equals(name, "next")) {
                if (lastLabel == null)
                    throw new RuntimeException("Failed to patch method: Last label was still null when the loop started!");

                log.trace("Found loop start");
                loopStartLabel = lastLabel;
            }
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            super.visitTypeInsn(opcode, type);

            if (addedFooter || opcode != INSTANCEOF || !Objects.equals(type, "net/minecraft/entity/item/EntityItem"))
                return;

            nextJump = true;
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            super.visitJumpInsn(opcode, label);

            if (addedFooter || !nextJump) return;


            addedFooter = true;
            nextJump = false;

            mv.visitVarInsn(ALOAD, 6);
            mv.visitMethodInsn(INVOKESTATIC, ASMCALLHOOKSERVER_INTERNAL_NAME, "isBadManaBean", "(Lnet/minecraft/entity/Entity;)Z", false);
            mv.visitJumpInsn(IFNE, loopStartLabel);
            log.trace("Injected isBadManaBean check.");
        }
    }
}