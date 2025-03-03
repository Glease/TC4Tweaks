package net.glease.tc4tweak.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;
import static net.glease.tc4tweak.asm.LoadingPlugin.dev;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

class BetterAddItemStackToInventoryVisitor extends MethodVisitor {
    public BetterAddItemStackToInventoryVisitor(int api, MethodVisitor mv) {
        super(api, mv);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (opcode == INVOKEVIRTUAL && name.equals(dev ? "addItemStackToInventory" : "func_70441_a") && owner.equals("net/minecraft/entity/player/InventoryPlayer")) {
            log.trace("Redirecting {}#{}{}", owner, name, desc);
            Type[] argumentTypes = Type.getArgumentTypes(desc);
            Type returnType = Type.getReturnType(desc);
            String newDesc = Type.getMethodDescriptor(returnType, ASMUtils.arrayPrepend(argumentTypes, Type.getObjectType(owner)));
            mv.visitMethodInsn(INVOKESTATIC, ASMCALLHOOKSERVER_INTERNAL_NAME, "addItemStackToInventory", newDesc, false);
        } else {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }
}
