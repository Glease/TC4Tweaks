package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

class FXSonicVisitor extends ClassVisitor {

    private static final String FIELD_MODEL_DESC = "Lnet/minecraftforge/client/model/IModelCustom;";
    private static final String FIELD_MODEL_NAME = "model";

    public FXSonicVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (name.equals(FIELD_MODEL_NAME) && desc.equals(FIELD_MODEL_DESC)) {
            log.debug("Making field model static");
            return super.visitField(access | ACC_STATIC, name, desc, signature, value);
        } else {
            return super.visitField(access, name, desc, signature, value);
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new FieldModelScrubberVisitor(api, name, desc, super.visitMethod(access, name, desc, signature, exceptions));
    }

    private static class FieldModelScrubberVisitor extends MethodVisitor {
        private final String name;
        private final String desc;

        public FieldModelScrubberVisitor(int api, String name, String desc, MethodVisitor mv) {
            super(api, mv);
            this.name = name;
            this.desc = desc;
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            if (owner.equals("thaumcraft/client/fx/other/FXSonic") && name.equals(FIELD_MODEL_NAME) && desc.equals(FIELD_MODEL_DESC)) {
                log.trace("Replacing opcode {} with {} in method {}{}", opcode, opcode - 2, this.name, this.desc);
                if (opcode == GETFIELD) {
                    // before
                    // ..., this, -> ..., model,
                    // after
                    // ..., this, -> ..., -> ..., model
                    mv.visitInsn(POP);
                    super.visitFieldInsn(GETSTATIC, owner, name, desc);
                } else if (opcode == PUTFIELD) {
                    // before
                    // ..., this, model -> ...,
                    // after
                    // ..., this, model -> ..., this, -> ...,
                    super.visitFieldInsn(PUTSTATIC, owner, name, desc);
                    mv.visitInsn(POP);
                }
            } else {
                super.visitFieldInsn(opcode, owner, name, desc);
            }
        }
    }
}
