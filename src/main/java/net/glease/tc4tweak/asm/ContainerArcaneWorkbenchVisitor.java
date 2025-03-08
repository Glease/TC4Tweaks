package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;
import static net.glease.tc4tweak.asm.ASMUtils.writeRedirect;
import static net.glease.tc4tweak.asm.LoadingPlugin.dev;
import static net.glease.tc4tweak.asm.TC4Transformer.log;

class ContainerArcaneWorkbenchVisitor extends ClassVisitor {
    public ContainerArcaneWorkbenchVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (desc.equals("(Lnet/minecraft/inventory/IInventory;)V") && dev ? name.equals("onCraftMatrixChanged") : name.equals("func_75130_a")) {
            log.debug("Visiting {}{}", name, desc);
            return new OnCraftMatrixChangedVisitor(api, mv);
        }
        return mv;
    }

    private static class OnCraftMatrixChangedVisitor extends MethodVisitor {

        public OnCraftMatrixChangedVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (owner.equals("net/minecraft/item/crafting/CraftingManager") && name.equals(dev ? "findMatchingRecipe" : "func_82787_a")) {
                writeRedirect(mv, ASMCALLHOOKSERVER_INTERNAL_NAME, "getNormalCraftingRecipeOutput", owner, name, desc);
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
    }
}
