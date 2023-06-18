package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;
import static net.glease.tc4tweak.asm.LoadingPlugin.dev;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

class BlockJarVisitor extends ClassVisitor {
    public BlockJarVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals(dev ? "onBlockActivated" : "func_149727_a")) {
            log.debug("Modifying {}", name);
            return new MethodVisitor(api, mv) {
                @Override
                public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                    super.visitFieldInsn(opcode, owner, name, desc);
                    if (opcode == PUTFIELD && name.equals("aspectFilter")) {
                        log.debug("Adding markDirty & markBlockForUpdate");
                        mv.visitVarInsn(ALOAD, 1);
                        mv.visitVarInsn(ILOAD, 2);
                        mv.visitVarInsn(ILOAD, 3);
                        mv.visitVarInsn(ILOAD, 4);
                        mv.visitMethodInsn(INVOKEVIRTUAL, "net/minecraft/world/World", dev ? "markBlockForUpdate" : "func_147471_g", "(III)V", false);
                        mv.visitVarInsn(ALOAD, 10);
                        mv.visitMethodInsn(INVOKEVIRTUAL, "net/minecraft/tileentity/TileEntity", dev ? "markDirty" : "func_70296_d", "()V", false);
                    }
                }
            };
        } else if (name.equals(dev ? "addCollisionBoxesToList" : "func_149743_a")) {
            return new MethodVisitor(api, mv) {
                private int visited = 0;

                @Override
                public void visitInsn(int opcode) {
                    if (opcode == FCONST_1 || opcode == FCONST_0) {
                        if (visited >= 6)
                            throw new IllegalStateException();
                        mv.visitInsn(ICONST_0 + visited);
                        mv.visitMethodInsn(INVOKESTATIC, ASMCALLHOOKSERVER_INTERNAL_NAME, "getBlockJarEntityCollisionBoxParameter", "(I)F", false);
                        visited++;
                    } else
                        super.visitInsn(opcode);
                }
            };
        }
        return mv;
    }
}
