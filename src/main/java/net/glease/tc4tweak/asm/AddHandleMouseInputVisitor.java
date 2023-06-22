package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOK_INTERNAL_NAME;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

class AddHandleMouseInputVisitor extends ClassVisitor {

    private String className;

    public AddHandleMouseInputVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitEnd() {
        String name;
        if (LoadingPlugin.dev)
            name = "handleMouseInput";
        else
            name = "func_146274_d";
        log.debug("Adding {} to {}", name, className);
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, name, "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, ASMCALLHOOK_INTERNAL_NAME, "handleMouseInput", "(L" + className + ";)V", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "net/minecraft/client/gui/GuiScreen", name, "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        super.visitEnd();
    }
}
