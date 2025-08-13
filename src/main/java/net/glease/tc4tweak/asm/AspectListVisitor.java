package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

class AspectListVisitor extends ClassVisitor {
    public AspectListVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals("add") && desc.equals("(Lthaumcraft/api/aspects/Aspect;I)Lthaumcraft/api/aspects/AspectList;")) {
            log.debug("Visiting {}{}", name, desc);
            return new AddVisitor(api, mv);
        }
        return mv;
    }

    private static class AddVisitor extends MethodVisitor {
        private final Label end = new Label();
        private int labels = 0;

        public AddVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            log.trace("Adding if guard to HEAD");
            mv.visitVarInsn(ALOAD, 1);
            mv.visitJumpInsn(IFNULL, end);
        }

        @Override
        public void visitLabel(Label label) {
            if (++labels == 5) {
                log.trace("Replacing L4 with our own");
                mv.visitLabel(end);
            } else {
                super.visitLabel(label);
            }
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            if (line == 197) {
                log.trace("Replacing label instance");
                super.visitLineNumber(line, end);
                log.trace("Adding F_SAME");
                mv.visitFrame(F_SAME, 0, null, 0, null);
            } else {
                super.visitLineNumber(line, start);
            }
        }
    }
}
