package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;
import static net.glease.tc4tweak.asm.LoadingPlugin.dev;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

public class TileVisRelayVisitor extends ClassVisitor {

    private String superName;

    public TileVisRelayVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, this.superName = superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals("drawEffect")) {
            log.debug("Visiting {}{}", name, desc);
            return new DrawEffectVisitor(api, mv);
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        log.debug("Adding sendUpdate() via overriding setParent()");
        MethodVisitor mv = visitMethod(ACC_PUBLIC, "setParent", "(Ljava/lang/ref/WeakReference;)V", "(Ljava/lang/ref/WeakReference<Lthaumcraft/api/visnet/TileVisNode;>;)V", null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, superName, "setParent", "(Ljava/lang/ref/WeakReference;)V", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, ASMCALLHOOKSERVER_INTERNAL_NAME, "sendUpdate", "(Lnet/minecraft/tileentity/TileEntity;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        super.visitEnd();
    }

    private static class DrawEffectVisitor extends MethodVisitor {
        public DrawEffectVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            if (opcode == INVOKEVIRTUAL && name.equals(dev ? "getWorldObj" : "func_145831_w")) {
                log.trace("Injecting blockExists() check");
                Label l = new Label();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, "thaumcraft/common/tiles/TileVisRelay", dev ? "xCoord" : "field_145851_c", "I");
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, "thaumcraft/common/tiles/TileVisRelay", "px", "I");
                mv.visitInsn(ISUB);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, "thaumcraft/common/tiles/TileVisRelay", dev ? "yCoord" : "field_145848_d", "I");
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, "thaumcraft/common/tiles/TileVisRelay", "py", "I");
                mv.visitInsn(ISUB);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, "thaumcraft/common/tiles/TileVisRelay", dev ? "zCoord" : "field_145849_e", "I");
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, "thaumcraft/common/tiles/TileVisRelay", "pz", "I");
                mv.visitInsn(ISUB);
                mv.visitMethodInsn(INVOKESTATIC, ASMCALLHOOKSERVER_INTERNAL_NAME, "checkVisRelayParentLoaded", "(Lnet/minecraft/world/World;III)Z", false);
                mv.visitJumpInsn(IFNE, l);
                mv.visitInsn(RETURN);
                mv.visitLabel(l);
                mv.visitFrame(F_SAME, 0, null, 0, null);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
    }
}
