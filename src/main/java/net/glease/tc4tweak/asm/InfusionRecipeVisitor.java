package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;

class InfusionRecipeVisitor extends ClassVisitor {
    public InfusionRecipeVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if ("areItemStacksEqual".equals(name)) {
            TC4Transformer.log.debug("Replacing {}{}", name, desc);
            ASMUtils.writeMethodDeflected(ASMCALLHOOKSERVER_INTERNAL_NAME, "infusionItemMatches", mv, null, desc);
            return null;
        }
        return mv;
    }
}
