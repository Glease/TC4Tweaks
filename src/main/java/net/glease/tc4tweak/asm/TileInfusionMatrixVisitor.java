package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

class TileInfusionMatrixVisitor extends ClassVisitor {

    public TileInfusionMatrixVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals("craftCycle") && desc.equals("()V")) {
            log.debug("Visiting {}{}", name, desc);
            return new LoadMathAbsVisitor(api, mv);
        } else if (name.equals("craftingStart") && desc.equals("(Lnet/minecraft/entity/player/EntityPlayer;)V")) {
            log.debug("Visiting {}{}", name, desc);
            return new CraftingStartVisitor(api, mv);
        } else {
            return mv;
        }
    }

    private static class LoadMathAbsVisitor extends MethodVisitor {

        public LoadMathAbsVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if(opcode == Opcodes.INVOKEVIRTUAL && owner.equals("java/util/Random") && name.equals("nextInt") && desc.equals("(I)I") && !itf) {
                log.trace("Injecting Math#max before Random#nextInt calls in CraftCycle()");
                mv.visitInsn(Opcodes.ICONST_1);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "max", "(II)I", false);
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }

    private static class CraftingStartVisitor extends MethodVisitor {

        public CraftingStartVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            if(opcode == Opcodes.INVOKEVIRTUAL && owner.equals("thaumcraft/api/crafting/InfusionRecipe") && name.equals("getRecipeOutput") && desc.equals("(Lnet/minecraft/item/ItemStack;)Ljava/lang/Object;") && !itf) {
                log.trace("Injecting callhook after InfusionRecipe#getRecipeOutput calls in craftingStart()");
                mv.visitVarInsn(ALOAD, 4);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, "thaumcraft/common/tiles/TileInfusionMatrix", "recipeInput", "Lnet/minecraft/item/ItemStack;");
                mv.visitMethodInsn(INVOKESTATIC, ASMCALLHOOKSERVER_INTERNAL_NAME, "modifyInfusionOutput", "(Ljava/lang/Object;Lthaumcraft/api/crafting/InfusionRecipe;Lnet/minecraft/item/ItemStack;)Ljava/lang/Object;", false);
            }
        }
    }
}
