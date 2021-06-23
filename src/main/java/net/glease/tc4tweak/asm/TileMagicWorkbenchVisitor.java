package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOK_INTERNAL_NAME;
import static org.objectweb.asm.Opcodes.*;

class TileMagicWorkbenchVisitor extends ClassVisitor {
    public TileMagicWorkbenchVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (name.equals("func_70299_a") || name.equals("setInventorySlotContents") && desc.equals("(ILnet/minecraft/item/ItemStack;)V")) {
            TC4Transformer.log.debug("Altering {}(ILnet/minecraft/item/ItemStack;)V", name);
            return new SetInventorySlotContentsVisitor(api, super.visitMethod(access, name, desc, signature, exceptions));
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    private static class SetInventorySlotContentsVisitor extends MethodVisitor {
        public SetInventorySlotContentsVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if ((name.equals("onCraftMatrixChanged") || name.equals("func_75130_a")) &&
                    desc.equals("(Lnet/minecraft/inventory/IInventory;)V") &&
                    owner.equals("net/minecraft/inventory/Container")) {
                TC4Transformer.log.debug("Altering {}", name);
                mv.visitInsn(SWAP);
                mv.visitInsn(POP);
                mv.visitMethodInsn(INVOKESTATIC, ASMCALLHOOK_INTERNAL_NAME, "updateCraftingMatrix", "(Lthaumcraft/common/tiles/TileMagicWorkbench;)V", false);
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
    }
}
