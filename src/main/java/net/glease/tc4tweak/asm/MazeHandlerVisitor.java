package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;
import static net.glease.tc4tweak.asm.TC4Transformer.log;

class MazeHandlerVisitor extends ClassVisitor {
    public MazeHandlerVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals("writeNBT") && desc.equals("()Lnet/minecraft/nbt/NBTTagCompound;")) {
            log.debug("Replacing writeNBT()Lnet/minecraft/nbt/NBTTagCompound;");
            ASMUtils.writeMethodDeflected(ASMCALLHOOKSERVER_INTERNAL_NAME, "writeMazeToNBT", mv, null, desc);
            return null;
        }
        return mv;
    }
}
