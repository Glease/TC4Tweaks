package net.glease.tc4tweak.asm;

import org.lwjgl.opengl.GL11;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.SIPUSH;

// Bug initially discovered by @UnderscoreKilburn on github.
// inspiration: https://github.com/GTNewHorizons/Hodgepodge/pull/304
class TileAlchemyFurnaceAdvancedRendererVisitor extends ClassVisitor {
    static final TransformerFactory FACTORY = TESRGetBlockTypeNullSafetyVisitor.FACTORY
            .chain(MakeModelStaticVisitor::new)
            .chain(TileAlchemyFurnaceAdvancedRendererVisitor::new);
    
    public TileAlchemyFurnaceAdvancedRendererVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals("renderQuadCenteredFromIcon")) {
            log.debug("Visiting {}{}", name, desc);
            return new RenderQuadCenteredFromIconVisitor(api, mv);
        }
        return mv;
    }

    private static class RenderQuadCenteredFromIconVisitor extends MethodVisitor {
        public RenderQuadCenteredFromIconVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (opcode == INVOKESTATIC && owner.equals("net/minecraft/client/renderer/RenderHelper") && desc.equals("()V")) {
                if (name.equals("disableStandardItemLighting")) {
                    log.trace("Replacing INVOKESTATIC {}#{}{} with GL11.glDisable(GL11.GL_LIGHTING)", owner, name, desc);
                    mv.visitIntInsn(SIPUSH, GL11.GL_LIGHTING);
                    mv.visitMethodInsn(INVOKESTATIC, "org/lwjgl/opengl/GL11", "glDisable", "(I)V", false);
                } else if (name.equals("enableStandardItemLighting")) {
                    log.trace("Replacing INVOKESTATIC {}#{}{} with GL11.glEnable(GL11.GL_LIGHTING)", owner, name, desc);
                    mv.visitIntInsn(SIPUSH, GL11.GL_LIGHTING);
                    mv.visitMethodInsn(INVOKESTATIC, "org/lwjgl/opengl/GL11", "glEnable", "(I)V", false);
                }
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
    }
}
