package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;
import static org.objectweb.asm.Opcodes.*;

class ResearchCategoriesVisitor extends ClassVisitor {
    public ResearchCategoriesVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals("getResearch") && desc.equals("(Ljava/lang/String;)Lthaumcraft/api/research/ResearchItem;")) {
            TC4Transformer.log.debug("Replacing getResearch(Ljava/lang/String;)Lthaumcraft/api/research/ResearchItem;");
            mv.visitParameter("key", 0);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESTATIC, ASMCALLHOOKSERVER_INTERNAL_NAME, name, desc, false);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
            return null;
        } else {
            return mv;
        }
    }
}
