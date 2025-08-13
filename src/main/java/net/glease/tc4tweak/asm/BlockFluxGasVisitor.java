package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

class BlockFluxGasVisitor extends ClassVisitor {
    private String className;

    public BlockFluxGasVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitEnd() {
        log.debug("Adding canDrain(Lnet/minecraft/world/World;III)Z to {}", className);
        MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "canDrain", "(Lnet/minecraft/world/World;III)Z", null, null);
        mv.visitCode();
        mv.visitInsn(ICONST_0);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(1, 5);
        mv.visitEnd();
        log.debug("Adding drain(Lnet/minecraft/world/World;IIIZ)Lnet/minecraftforge/fluids/FluidStack; to {}", className);
        mv = super.visitMethod(ACC_PUBLIC, "drain", "(Lnet/minecraft/world/World;IIIZ)Lnet/minecraftforge/fluids/FluidStack;", null, null);
        mv.visitCode();
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 6);
        mv.visitEnd();
        super.visitEnd();
    }
}
