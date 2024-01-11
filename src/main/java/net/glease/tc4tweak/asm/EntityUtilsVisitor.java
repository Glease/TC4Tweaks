package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.NEW;

public class EntityUtilsVisitor extends ClassVisitor {

    public static final String TARGET_CLASS_INTERNAL_NAME = "net/glease/tc4tweak/asm/ConfigurationAttributeModifier";
    public static final String ORIGINAL_CLASS_INTERNAL_NAME = "net/minecraft/entity/ai/attributes/AttributeModifier";

    public EntityUtilsVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals("<clinit>")) {
            return new CLInitVisitor(api, mv);
        }
        return mv;
    }

    private static class CLInitVisitor extends MethodVisitor {
        public CLInitVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            if (opcode == NEW && type.equals(ORIGINAL_CLASS_INTERNAL_NAME)) {
                log.trace("Changed NEW from {} to {}", ORIGINAL_CLASS_INTERNAL_NAME, TARGET_CLASS_INTERNAL_NAME);
                type = TARGET_CLASS_INTERNAL_NAME;
            }
            super.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (opcode == INVOKESPECIAL && owner.equals(ORIGINAL_CLASS_INTERNAL_NAME) && name.equals("<init>")) {
                log.trace("Changed <init> owner from {} to {}", ORIGINAL_CLASS_INTERNAL_NAME, TARGET_CLASS_INTERNAL_NAME);
                owner = TARGET_CLASS_INTERNAL_NAME;
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }
}
