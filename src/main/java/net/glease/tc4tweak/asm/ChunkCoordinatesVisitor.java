package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

// this fix is included so in dev env without hodgepodge decanting core does not take multiple second to compute, which
// uses a hash set of ChunkCoordinates
class ChunkCoordinatesVisitor extends ClassVisitor {

    private String className;

    private ChunkCoordinatesVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    public static TransformerFactory createFactory() {
        return new TransformerFactory(ChunkCoordinatesVisitor::new) {
            @Override
            public boolean isInactive() {
                if (super.isInactive()) return true;
                if (ASMUtils.isHodgepodgeFixActive("speedups", "speedupChunkCoordinatesHashCode", true)) {
                    // technically not really stolen
                    // I was among the discussion when hodgepodge's patch was made
                    // the multiply factors in hashCode impl are stolen from ChunkPosition
                    log.warn("Disabling TC4Tweaks's stolen ChunkCoordinates hashCode fix to prevent conflict with hodgepodge.");
                    return true;
                }
                return false;
            }
        };
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        super.visit(version, access, this.className, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if ("hashCode".equals(name)) {
            log.debug("Overwriting {}{}", name, desc);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "posX", "I");
            mv.visitLdcInsn(8976890);
            mv.visitInsn(IMUL);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "posY", "I");
            mv.visitLdcInsn(981131);
            mv.visitInsn(IMUL);
            mv.visitInsn(IADD);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "posZ", "I");
            mv.visitInsn(IADD);
            mv.visitInsn(IRETURN);
            mv.visitMaxs(3, 1);
            mv.visitEnd();
            return null;
        }
        return mv;
    }
}

