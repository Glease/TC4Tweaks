package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;

public class CellLocVisitor extends ClassVisitor {
    public CellLocVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals("hashCode") && desc.equals("()I")) {
            TC4Transformer.log.debug("Replacing hashCode()I");
            ASMUtils.writeMethodDeflected(ASMCALLHOOKSERVER_INTERNAL_NAME, "hashCellLoc", mv, "thaumcraft/common/lib/world/dim/CellLoc", desc);
            return null;
        }
        return mv;
    }
}
