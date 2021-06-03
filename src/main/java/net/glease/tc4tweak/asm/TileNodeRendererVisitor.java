package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOK_INTERNAL_NAME;
import static thaumcraft.common.Thaumcraft.log;

public class TileNodeRendererVisitor extends ClassVisitor {
    private static class RenderNodeVisitor extends  MethodVisitor {
        public RenderNodeVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (owner.equals("thaumcraft/client/lib/UtilsFX") && name.equals("renderFacingStrip")) {
                log.debug("Replacing renderFacingStrip");
                super.visitMethodInsn(opcode, ASMCALLHOOK_INTERNAL_NAME, name, desc, false);
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
    }
    public TileNodeRendererVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals("renderNode")) {
            log.debug("Visiting renderNode");
            return new RenderNodeVisitor(api, mv);
        }
        return mv;
    }
}
