package net.glease.tc4tweak.asm;

import java.lang.invoke.MethodHandle;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;
import static net.glease.tc4tweak.asm.ASMUtils.writeRedirect;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

class AILiquidGatherVisitor extends ClassVisitor {

    private static final String MH_CTOR = "tc4tweaks$a";
    public static final int MH_INITIALIZER_MAX_STACK = 7;
    public static final int MH_INITIALIZER_MAX_LOCALS = 1;

    private boolean mhInitialized = false;

    public AILiquidGatherVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public void visitEnd() {
        log.debug("Adding MethodHandle field {}", MH_CTOR);
        visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, MH_CTOR, Type.getDescriptor(MethodHandle.class), null, null).visitEnd();
        if (mhInitialized) {
            log.debug("Not adding <clinit> since one is present already");
        } else {
            log.debug("Adding MethodHandle initializer by adding a <clinit>");
            MethodVisitor mv = visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            mv.visitCode();
            writeMHInitializer(mv);
            mv.visitMaxs(MH_INITIALIZER_MAX_STACK, MH_INITIALIZER_MAX_LOCALS);
            mv.visitEnd();
        }
        super.visitEnd();
    }

    private static void writeMHInitializer(MethodVisitor mv) {
        // this method handle is the handle to construct the inner class
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodHandles", "lookup", "()Ljava/lang/invoke/MethodHandles$Lookup;", false);
        mv.visitLdcInsn(Type.getObjectType("thaumcraft/common/entities/ai/fluid/AILiquidGather$SourceBlock"));
        mv.visitFieldInsn(GETSTATIC, "java/lang/Void", "TYPE", "Ljava/lang/Class;");
        mv.visitInsn(ICONST_3);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_0);
        mv.visitLdcInsn(Type.getObjectType("thaumcraft/common/entities/ai/fluid/AILiquidGather"));
        mv.visitInsn(AASTORE);
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_1);
        mv.visitLdcInsn(Type.getObjectType("net/minecraft/util/ChunkCoordinates"));
        mv.visitInsn(AASTORE);
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_2);
        mv.visitFieldInsn(GETSTATIC, "java/lang/Float", "TYPE", "Ljava/lang/Class;");
        mv.visitInsn(AASTORE);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodType", "methodType", "(Ljava/lang/Class;[Ljava/lang/Class;)Ljava/lang/invoke/MethodType;", false);
        mv.visitInsn(DUP_X2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findConstructor", "(Ljava/lang/Class;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);
        mv.visitInsn(SWAP);
        mv.visitLdcInsn(Type.getType(Object.class));
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodType", "changeReturnType", "(Ljava/lang/Class;)Ljava/lang/invoke/MethodType;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "asType", "(Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);
        mv.visitFieldInsn(PUTSTATIC, "thaumcraft/common/entities/ai/fluid/AILiquidGather", MH_CTOR, "Ljava/lang/invoke/MethodHandle;");
        mv.visitInsn(RETURN);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals("rebuildQueue")) {
            log.debug("Visiting {}{}", name, desc);
            return new RebuildQueueVisitor(api, mv);
        } else if (name.equals("<clinit>") && desc.equals("()V")) {
            log.debug("Visiting {}{}", name, desc);
            mhInitialized = true;
            return new ClinitVisitor(api, mv);
        }
        return mv;
    }

    private static class RebuildQueueVisitor extends MethodVisitor {
        public RebuildQueueVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if ("getConnectedFluidBlocks".equals(name)) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, owner, "pumpDist", "F");
                mv.visitFieldInsn(GETSTATIC, owner, MH_CTOR, Type.getDescriptor(MethodHandle.class));
                writeRedirect(mv, ASMCALLHOOKSERVER_INTERNAL_NAME, "getConnectedFluidBlocks", owner, name, desc, Type.FLOAT_TYPE, Type.getType(MethodHandle.class));
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
    }

    private static class ClinitVisitor extends MethodVisitor {
        public ClinitVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            log.trace("Adding method handler initializer");
            writeMHInitializer(mv);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(Math.max(maxStack, MH_INITIALIZER_MAX_STACK), Math.max(maxLocals, MH_INITIALIZER_MAX_LOCALS));
        }
    }
}
