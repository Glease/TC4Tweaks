package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOK_INTERNAL_NAME;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

class MappingThreadVisitor extends ClassVisitor {

    private String className;

    public MappingThreadVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name;
        super.visit(version, access, className, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals("run") && desc.equals("()V")) {
            log.debug("Visiting run()V");
            return new RunVisitor(api, mv);
        } else {
            return mv;
        }
    }

    private class RunVisitor extends MethodVisitor {
        public RunVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            log.trace("Injecting callhook at HEAD");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "idMappings", "Ljava/util/Map;");
            mv.visitMethodInsn(INVOKESTATIC, ASMCALLHOOK_INTERNAL_NAME, "onMappingStart", "(Ljava/util/Map;)V", false);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            if (opcode == INVOKEINTERFACE && "java/util/Iterator".equals(owner) && "next".equals(name)) {
                log.trace("Injecting callhook before Iterator#next()");
                mv.visitMethodInsn(INVOKESTATIC, ASMCALLHOOK_INTERNAL_NAME, "onMappingDidWork", "()V", false);
            }
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == RETURN) {
                log.trace("Injecting callhook before {}", opcode);
                mv.visitMethodInsn(INVOKESTATIC, ASMCALLHOOK_INTERNAL_NAME, "onMappingFinished", "()V", false);
            }
            super.visitInsn(opcode);
        }
    }
}
