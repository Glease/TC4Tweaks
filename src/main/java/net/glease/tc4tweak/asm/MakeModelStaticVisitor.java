package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

class MakeModelStaticVisitor extends ClassVisitor {

    private static final String FIELD_MODEL_DESC = "Lnet/minecraftforge/client/model/IModelCustom;";
    private static final String FIELD_MODEL_NAME = "model";
    private String thisName;

    public MakeModelStaticVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        thisName = name;
        super.visit(version, access, thisName, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (name.equals(FIELD_MODEL_NAME) && desc.equals(FIELD_MODEL_DESC)) {
            log.debug("Making field model static in {}", thisName);
            return super.visitField(access | ACC_STATIC, name, desc, signature, value);
        } else {
            return super.visitField(access, name, desc, signature, value);
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        log.debug("Visiting {}{}", name, desc);
        return new FieldModelScrubberVisitor(api, name, desc, thisName, super.visitMethod(access, name, desc, signature, exceptions));
    }

    private static class FieldModelScrubberVisitor extends MethodVisitor {
        private final String name;
        private final String desc;
        private final String thisName;

        public FieldModelScrubberVisitor(int api, String name, String desc, String thisName, MethodVisitor mv) {
            super(api, mv);
            this.name = name;
            this.desc = desc;
            this.thisName = thisName;
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            if (owner.equals(thisName) && name.equals(FIELD_MODEL_NAME) && desc.equals(FIELD_MODEL_DESC)) {
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
