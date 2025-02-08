package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

public class TileThaumatoriumRendererVisitor extends ClassVisitor {
    public TileThaumatoriumRendererVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (name.equals("entityItem")) return null;
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if ("renderTileEntityAt".equals(name)) {
            return new RenderTileEntityAtVisitor(api, mv);
        }
        return mv;
    }

    private static class RenderTileEntityAtVisitor extends MethodVisitor {
        public RenderTileEntityAtVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitVarInsn(int opcode, int var) {

            super.visitVarInsn(opcode, var);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            if (opcode == PUTFIELD && name.equals("entityItem")) {
                log.trace("Converted PUTFIELD to ASTORE");
                visitVarInsn(ASTORE, 14);
                visitInsn(POP);
            } else if (opcode == GETFIELD && name.equals("entityItem")) {
                log.trace("Converted GETFIELD to ALOAD");
                visitInsn(POP);
                visitVarInsn(ALOAD, 14);
            } else {
                super.visitFieldInsn(opcode, owner, name, desc);
            }
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(maxStack, maxLocals + 1);
        }
    }
}
