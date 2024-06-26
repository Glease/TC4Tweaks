package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;
import static net.glease.tc4tweak.asm.LoadingPlugin.dev;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

class UtilsVisitor extends ClassVisitor {
    public UtilsVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if ("generateLoot".equals(name)) {
            log.debug("Visiting {}{}", name, desc);
            return new GenerateLootVisitor(api, mv);
        } else if ("setBiomeAt".equals(name)) {
            log.debug("Visiting {}{}", name, desc);
            return new SetBiomeAtVisitor(api, mv);
        }
        return mv;
    }

    private static class GenerateLootVisitor extends MethodVisitor {
        public GenerateLootVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            if (opcode == ASTORE && var == 2) {
                log.trace("Injected copyIfNotNull");
                mv.visitMethodInsn(INVOKESTATIC, ASMCALLHOOKSERVER_INTERNAL_NAME, "copyIfNotNull", "(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;", false);
            }
            super.visitVarInsn(opcode, var);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (opcode == INVOKEVIRTUAL &&
                    "net/minecraft/item/ItemStack".equals(owner) &&
                    (dev ? "copy" : "func_77946_l").equals(name) &&
                    "()Lnet/minecraft/item/ItemStack;".equals(desc) && !itf) {
                log.trace("Replaced {}#{}{} with mutateGeneratedLoot", owner, name, desc);
                super.visitMethodInsn(INVOKESTATIC, ASMCALLHOOKSERVER_INTERNAL_NAME, "mutateGeneratedLoot", "(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;", false);
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
    }

    private static class SetBiomeAtVisitor extends MethodVisitor {
        public SetBiomeAtVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitLdcInsn(Object cst) {
            if (cst instanceof Double && Math.abs((double) cst) - 32D < 0.001D) {
                log.trace("Replaced LDC {} with ALOAD_0 + getViewDistance", cst);
                super.visitVarInsn(ALOAD, 0);
                super.visitMethodInsn(INVOKESTATIC, ASMCALLHOOKSERVER_INTERNAL_NAME, "getViewDistance", "(Lnet/minecraft/world/World;)D", false);
            } else {
                super.visitLdcInsn(cst);
            }
        }
    }
}
