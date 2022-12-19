package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.LoadingPlugin.dev;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

public class BlockJarVisitor extends ClassVisitor {
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
        }
        return mv;
    }
}
