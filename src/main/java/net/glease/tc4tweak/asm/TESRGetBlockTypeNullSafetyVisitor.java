package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static net.glease.tc4tweak.asm.LoadingPlugin.dev;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.IFNONNULL;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.RETURN;

public class TESRGetBlockTypeNullSafetyVisitor extends ClassVisitor {
    private final String transformedName;

    public TESRGetBlockTypeNullSafetyVisitor(int api, String transformedName, ClassVisitor cv) {
        super(api, cv);
        this.transformedName = transformedName;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if ((name.equals("renderTileEntityAt") || name.equals("func_147500_a")) && desc.equals("(Lnet/minecraft/tileentity/TileEntity;DDDF)V")) {
            mv.visitCode();
            Label lblSkipReturn = new Label();
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "net/minecraft/tileentity/TileEntity", dev ? "hasWorldObj" : "func_145830_o", "()Z", false);
            mv.visitJumpInsn(IFNE, lblSkipReturn);
            mv.visitInsn(RETURN);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "net/minecraft/tileentity/TileEntity", dev ? "getBlockType" : "func_145838_q", "()Lnet/minecraft/block/Block;", false);
            mv.visitJumpInsn(IFNONNULL, lblSkipReturn);
            mv.visitInsn(RETURN);
            mv.visitLabel(lblSkipReturn);

            mv.visitVarInsn(ALOAD, 0);
            Type[] argumentTypes = Type.getMethodType(desc).getArgumentTypes();
            int base = 1;
            for (Type argumentType : argumentTypes) {
                mv.visitVarInsn(argumentType.getOpcode(ILOAD), base);
                base += argumentType.getSize();
            }
            mv.visitMethodInsn(INVOKEVIRTUAL, transformedName.replace('.', '/'), name + ASMConstants.RENAMED_METHOD_SUFFIX, desc, false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(9, 9);
            mv = super.visitMethod(access, name + ASMConstants.RENAMED_METHOD_SUFFIX, desc, signature, exceptions);
        }
        return mv;
    }
}
