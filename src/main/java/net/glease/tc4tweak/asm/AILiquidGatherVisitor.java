package net.glease.tc4tweak.asm;

import java.lang.invoke.MethodHandle;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

class AILiquidGatherVisitor extends ClassVisitor {

    private static final String MH_CTOR = "tc4tweaks$a";

    public AILiquidGatherVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public void visitEnd() {
        visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, MH_CTOR, Type.getDescriptor(MethodHandle.class), null, null).visitEnd();
        MethodVisitor mv = visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
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
        mv.visitMaxs(7, 1);
        mv.visitEnd();

        super.visitEnd();
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals("rebuildQueue")) {
            log.debug("Visiting {}", name);
            return new RebuildQueueVisitor(api, mv);
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
                Type[] args = Type.getArgumentTypes(desc);
                Type[] newArgs = new Type[args.length + 3];
                System.arraycopy(args, 0, newArgs, 1, args.length);
                newArgs[0] = Type.getObjectType(owner);
                newArgs[newArgs.length - 2] = Type.FLOAT_TYPE;
                newArgs[newArgs.length - 1] = Type.getType(MethodHandle.class);
                String newDesc = Type.getMethodType(Type.VOID_TYPE, newArgs).getDescriptor();
                log.trace("Redirecting {}, new desc {}", name, newDesc);
                mv.visitMethodInsn(INVOKESTATIC, ASMCALLHOOKSERVER_INTERNAL_NAME, "getConnectedFluidBlocks", newDesc, false);
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
    }
}
