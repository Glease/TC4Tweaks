package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOK_INTERNAL_NAME;
import static org.objectweb.asm.Opcodes.*;

class GuiResearchTableVisitor extends ClassVisitor {

    public GuiResearchTableVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public void visitEnd() {
        String name;
        if (LoadingPlugin.dev)
            name = "handleMouseInput";
        else
            name = "func_146274_d";
        TC4Transformer.log.debug("Adding {} to GuiResearchTable", name);
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, name, "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, ASMCALLHOOK_INTERNAL_NAME, "handleMouseInput", "(Lthaumcraft/client/gui/GuiResearchTable;)V", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "net/minecraft/client/gui/GuiScreen", name, "()V", false);
        mv.visitInsn(RETURN);
        super.visitEnd();
    }
}
