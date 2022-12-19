package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.LoadingPlugin.dev;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

public class AddOnDataPacketMarkBlockForRenderUpdateVisitor extends ClassVisitor {

    private String className;
    private String superClass;

    public AddOnDataPacketMarkBlockForRenderUpdateVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name;
        superClass = superName;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitEnd() {
        log.debug("Adding onDataPacket with World#func_147479_m to {}", className);
        String name = "onDataPacket";
        String desc = "(Lnet/minecraft/network/NetworkManager;Lnet/minecraft/network/play/server/S35PacketUpdateTileEntity;)V";
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, name, desc, null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKESPECIAL, superClass, name, desc, false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "net/minecraft/tileentity/TileEntity", dev ? "worldObj" : "field_145850_b", "Lnet/minecraft/world/World;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "net/minecraft/tileentity/TileEntity", dev ? "xCoord" : "field_145851_c", "I");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "net/minecraft/tileentity/TileEntity", dev ? "yCoord" : "field_145848_d", "I");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "net/minecraft/tileentity/TileEntity", dev ? "zCoord" : "field_145849_e", "I");
        mv.visitMethodInsn(INVOKEVIRTUAL, "net/minecraft/world/World", "func_147479_m", "(III)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(4, 3);
        mv.visitEnd();
        super.visitEnd();
    }
}
