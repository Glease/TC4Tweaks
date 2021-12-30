package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.PUTFIELD;

class AIItemPickupVisitor extends ClassVisitor {
    public AIItemPickupVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if ("findItem".equals(name) && "()Z".equals(desc)) {
            return new FindItemVisitor(api, mv);
        }
        return mv;
    }

    private static class FindItemVisitor extends MethodVisitor {
        public FindItemVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            if (opcode == PUTFIELD && "thaumcraft/common/entities/ai/inventory/AIItemPickup".equals(owner) && "targetEntity".equals(name)) {
                super.visitMethodInsn(INVOKESTATIC, ASMCALLHOOKSERVER_INTERNAL_NAME, "onlyIfAlive", "(" + desc + ")" + desc, false);
            }
            super.visitFieldInsn(opcode, owner, name, desc);
        }
    }
}
