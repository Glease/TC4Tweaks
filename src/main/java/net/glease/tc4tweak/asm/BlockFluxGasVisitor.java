package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

public class BlockFluxGasVisitor extends ClassVisitor {
    public BlockFluxGasVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public void visitEnd() {
        MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "canDrain", "(Lnet/minecraft/world/World;III)Z", null, null);
        mv.visitCode();
        mv.visitInsn(ICONST_0);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(1, 5);
        mv.visitEnd();
        mv = super.visitMethod(ACC_PUBLIC, "drain", "(Lnet/minecraft/world/World;IIIZ)Lnet/minecraftforge/fluids/FluidStack;", null, null);
        mv.visitCode();
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 6);
        mv.visitEnd();
        super.visitEnd();
    }
}
