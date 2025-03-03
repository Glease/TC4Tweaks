package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.TC4Transformer.log;

class ItemEssenceVisitor extends ClassVisitor {
    public ItemEssenceVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals("onItemUseFirst")) {
            log.debug("Visiting onItemUseFirst");
            return new BetterAddItemStackToInventoryVisitor(api, mv);
        }
        return mv;
    }
}
