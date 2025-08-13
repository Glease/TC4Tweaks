package net.glease.tc4tweak.asm;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

class MazeHandlerVisitor extends ClassVisitor {
    private static final Set<String> mutators = ImmutableSet.of("putToHashMap", "putToHashMapRaw", "removeFromHashMap", "clearHashMap");

    private String className;

    public MazeHandlerVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name;
        super.visit(version, access, className, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals("writeNBT") && desc.equals("()Lnet/minecraft/nbt/NBTTagCompound;")) {
            log.debug("Replacing writeNBT()Lnet/minecraft/nbt/NBTTagCompound;");
            ASMUtils.writeOverwrite(ASMCALLHOOKSERVER_INTERNAL_NAME, "writeMazeToNBT", mv, null, desc);
            return null;
        } else if (mutators.contains(name)) {
            log.debug("Visiting {}{}", name, desc);
            return new MarkDirtyMethodVisitor(mv);
        } else if ("loadMaze".equals(name)) {
            log.debug("Visiting {}{}", name, desc);
            return new ResetDirtyMethodVisitor(mv, Type.getReturnType(desc).getOpcode(Opcodes.IRETURN));
        } else if ("saveMaze".equals(name)) {
            log.debug("Visiting {}{}", name, desc);
            return new SaveMazeMethodVisitor(mv);
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        cv.visitField(ACC_PRIVATE | ACC_STATIC, "tc4tweaks$dirty", "Z", null, false).visitEnd();
        super.visitEnd();
    }

    private class MarkDirtyMethodVisitor extends MethodVisitor {
        public MarkDirtyMethodVisitor(MethodVisitor mv) {
            super(MazeHandlerVisitor.this.api, mv);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            log.trace("Add set dirty");
            mv.visitInsn(ICONST_1);
            mv.visitFieldInsn(PUTSTATIC, className, "tc4tweaks$dirty", "Z");
        }
    }

    private class ResetDirtyMethodVisitor extends MethodVisitor {
        private final int returnOpcode;

        public ResetDirtyMethodVisitor(MethodVisitor mv, int returnOpcode) {
            super(MazeHandlerVisitor.this.api, mv);
            this.returnOpcode = returnOpcode;
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == returnOpcode) {
                log.trace("Add clear dirty");
                mv.visitInsn(ICONST_0);
                mv.visitFieldInsn(PUTSTATIC, className, "tc4tweaks$dirty", "Z");
            }
            super.visitInsn(opcode);
        }
    }

    private class SaveMazeMethodVisitor extends MethodVisitor {
        public SaveMazeMethodVisitor(MethodVisitor mv) {
            super(MazeHandlerVisitor.this.api, mv);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            log.trace("Add dirty check");
            Label l0 = new Label();
            mv.visitFieldInsn(GETSTATIC, className, "tc4tweaks$dirty", "Z");
            mv.visitJumpInsn(IFNE, l0);
            mv.visitInsn(RETURN);
            mv.visitLabel(l0);
            mv.visitInsn(ICONST_0);
            mv.visitFieldInsn(PUTSTATIC, className, "tc4tweaks$dirty", "Z");
            mv.visitFrame(F_SAME, 0, null, 0, null);
        }
    }
}
