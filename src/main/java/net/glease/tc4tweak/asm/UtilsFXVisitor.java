package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOK_INTERNAL_NAME;

class UtilsFXVisitor extends ClassVisitor {
    public UtilsFXVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals("getParticleTexture")) {
            ASMUtils.writeOverwrite(ASMCALLHOOK_INTERNAL_NAME, name, mv, null, desc);
            return null;
        }
        return mv;
    }
}
