package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;
import static net.glease.tc4tweak.asm.ASMUtils.arrayAppend;
import static net.glease.tc4tweak.asm.LoadingPlugin.dev;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

class TileVisNodeVisitor extends ClassVisitor {
    private static final String FIELD_NAME = "tc4tweaks$loadedLink";
    private static final String FIELD_DESC = "Ljava/util/List;";
    private String name;
    private String superName;

    public TileVisNodeVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.name = name;
        this.superName = superName;
        super.visit(version, access, name, signature, superName, arrayAppend(interfaces, "net/glease/tc4tweak/asm/ITileVisNode"));
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if ("func_145845_h".equals(name) || "updateEntity".equals(name)) {
            return new UpdateEntityVisitor(api, mv);
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        cv.visitField(ACC_PRIVATE, FIELD_NAME, FIELD_DESC, "Ljava/util/List<Lnet/minecraft/util/ChunkCoordinates;>;", null).visitEnd();
        {
            String methodName = dev ? "writeToNBT" : "func_145839_a";
            MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, methodName, "(Lnet/minecraft/nbt/NBTTagCompound;)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, superName, methodName, "(Lnet/minecraft/nbt/NBTTagCompound;)V", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESTATIC, "net/glease/tc4tweak/asm/ASMCallhookServer", "readLoadedLink", "(Lthaumcraft/api/visnet/TileVisNode;Lnet/minecraft/nbt/NBTTagCompound;)Ljava/util/List;", false);
            mv.visitFieldInsn(PUTFIELD, name, FIELD_NAME, FIELD_DESC);
            mv.visitInsn(RETURN);
            mv.visitMaxs(3, 2);
            mv.visitEnd();
            log.debug("Added {}", methodName);
        }
        {
            String methodName = dev ? "writeToNBT" : "func_145841_b";
            MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, methodName, "(Lnet/minecraft/nbt/NBTTagCompound;)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, superName, methodName, "(Lnet/minecraft/nbt/NBTTagCompound;)V", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESTATIC, "net/glease/tc4tweak/asm/ASMCallhookServer", "writeLoadedLink", "(Lthaumcraft/api/visnet/TileVisNode;Lnet/minecraft/nbt/NBTTagCompound;)V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
            log.debug("Added {}", methodName);
        }
        {
            String methodName = "getSavedLink";
            MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, methodName, "()Ljava/util/List;", "()Ljava/util/List<Lnet/minecraft/util/ChunkCoordinates;>;", null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, name, FIELD_NAME, FIELD_DESC);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 1);
            log.debug("Added {}", methodName);
        }
        {
            String methodName = "clearSavedLink";
            MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, methodName, "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ACONST_NULL);
            mv.visitFieldInsn(PUTFIELD, name, FIELD_NAME, FIELD_DESC);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 1);
            log.debug("Added {}", methodName);
        }
        super.visitEnd();
    }
    
    private static class UpdateEntityVisitor extends MethodVisitor {
        private int state;

        public UpdateEntityVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            super.visitFieldInsn(opcode, owner, name, desc);
            if (state == 0 && opcode == GETFIELD && name.equals("nodeRefresh")) {
                state = 1;
                log.trace("Found GETFIELD nodeRefresh");
            }
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            super.visitJumpInsn(opcode, label);
            if (state == 1 && opcode == IFEQ) {
                state = 2;
            }
        }

        @Override
        public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
            super.visitFrame(type, nLocal, local, nStack, stack);
            if (state == 2) {
                log.trace("Inserting loadedLink processing by frame insn {}", type);
                insert();
            }
        }

        private void insert() {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESTATIC, ASMCALLHOOKSERVER_INTERNAL_NAME, "processSavedLink", "(Lnet/glease/tc4tweak/asm/ITileVisNode;)Z", false);
            Label label = new Label();
            mv.visitJumpInsn(IFEQ, label);
            mv.visitInsn(RETURN);
            mv.visitLabel(label);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            log.trace("Inserted processSavedLink");
            state = 3;
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            super.visitVarInsn(opcode, var);
            if (state == 2) {
                log.trace("Inserting loadedLink processing by var insn {}", opcode);
                insert();
            }
        }

        @Override
        public void visitEnd() {
            if (state != 3) {
                throw new IllegalStateException("Unexpected state: " + state);
            }
            super.visitEnd();
        }
    }
}
