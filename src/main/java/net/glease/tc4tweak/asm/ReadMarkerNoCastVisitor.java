package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

public class ReadMarkerNoCastVisitor extends ClassVisitor {
    private static final boolean hodgepodge = ASMUtils.isHodgepodgeFixActive("fixThaumcraftGolemMarkerLoading");
    private final String targetMethod;
    private final String targetDesc;

    public ReadMarkerNoCastVisitor(int api, ClassVisitor cv, String targetMethod, String targetDesc) {
        super(api, cv);
        this.targetMethod = targetMethod;
        this.targetDesc = targetDesc;
    }

    public static TransformerFactory createFactory(String targetMethod, String targetDesc) {
        return new TransformerFactory((api, cv) -> new ReadMarkerNoCastVisitor(api, cv, targetMethod, targetDesc)) {
            @Override
            public boolean isInactive() {
                if (super.isInactive()) return true;
                if (hodgepodge)
                    log.info("Disabling TC4Tweaks's golem marker patch to prevent conflict with hodgepodge.");
                return hodgepodge;
            }
        };
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (targetMethod.equals(name) && targetDesc.equals(desc)) {
            log.debug("Visiting {}{}", name, desc);
            return new MarkerInstantiateFixVisitor(api, mv);
        }
        return mv;
    }

    private static class MarkerInstantiateFixVisitor extends MethodVisitor {
        private boolean active;

        public MarkerInstantiateFixVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitInsn(int opcode) {
            if (active && opcode == I2B) {
                log.debug("Dropped I2B");
                return;
            }
            super.visitInsn(opcode);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            if (opcode == NEW && "thaumcraft/common/entities/golems/Marker".equals(type)) {
                log.trace("Start looking for I2B");
                active = true;
            }
            super.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (active && opcode == INVOKESPECIAL && "thaumcraft/common/entities/golems/Marker".equals(owner) && "<init>".equals(name)) {
                log.trace("Stop looking for I2B");
                active = false;
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }
}
