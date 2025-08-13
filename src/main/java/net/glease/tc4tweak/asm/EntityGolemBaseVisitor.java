package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.LoadingPlugin.dev;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

class EntityGolemBaseVisitor extends ClassVisitor {
    public EntityGolemBaseVisitor(int api, ClassVisitor cv) {
        super(api, maybePatchMarker(api, cv));
    }

    private static ClassVisitor maybePatchMarker(int api, ClassVisitor cv) {
        if (ASMUtils.isHodgepodgeFixActive("fixes", "fixThaumcraftGolemMarkerLoading", true)) {
            log.warn("Disabling TC4Tweaks's golem marker patch to prevent conflict with hodgepodge.");
            return cv;
        }
        return new ReadMarkerNoCastVisitor(api, cv, dev ? "readEntityFromNBT" : "func_70037_a", "(Lnet/minecraft/nbt/NBTTagCompound;)V");
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals("updateCarried")) {
            log.debug("Visiting {}{}", name, desc);
            return new UpdateCarriedVisitor(api, mv);
        }
        return mv;
    }

    private static class UpdateCarriedVisitor extends MethodVisitor {

        public UpdateCarriedVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (name.equals("addObjectByDataType") || name.equals("func_82709_a")) {
                String newName = dev ? "updateObject" : "func_75692_b";
                Label l = new Label();
                mv.visitLabel(l);
                mv.visitLineNumber(114514, l);
                log.trace("Replacing {}#{}(?1, ?2) with {}#{}(?1, null)", owner, name, owner, newName);
                mv.visitInsn(POP);
                mv.visitInsn(ACONST_NULL);
                mv.visitMethodInsn(opcode, owner, newName, "(ILjava/lang/Object;)V", false);
            } else if (name.equals("setObjectWatched") || name.equals("func_82708_h")) {
                log.trace("Dropping unnecessary {}#{}{}", owner, name, owner);
                mv.visitInsn(POP2);
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
    }
}
