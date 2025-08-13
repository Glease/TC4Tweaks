package net.glease.tc4tweak.asm;

import cpw.mods.fml.relauncher.Side;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOK_INTERNAL_NAME;
import static net.glease.tc4tweak.asm.TC4Transformer.log;

class NodeLikeRendererVisitor extends ClassVisitor {
    private final String target;

    public NodeLikeRendererVisitor(int api, ClassVisitor cv, String target) {
        super(api, cv);
        this.target = target;
    }

    public static TransformerFactory createFactory(String target) {
        return new TransformerFactory((api, cv) -> new NodeLikeRendererVisitor(api, cv, target), Side.CLIENT);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals(target)) {
            log.debug("Visiting {}{}", name, desc);
            return new RenderNodeVisitor(api, mv);
        }
        return mv;
    }

    private static class RenderNodeVisitor extends MethodVisitor {
        public RenderNodeVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (owner.equals("thaumcraft/client/lib/UtilsFX") && name.equals("renderFacingStrip")) {
                log.trace("Replacing renderFacingStrip");
                super.visitMethodInsn(opcode, ASMCALLHOOK_INTERNAL_NAME, name, desc, false);
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
    }
}
