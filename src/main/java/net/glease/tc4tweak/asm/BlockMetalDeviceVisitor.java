package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;
import static net.glease.tc4tweak.asm.LoadingPlugin.dev;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

class BlockMetalDeviceVisitor extends ClassVisitor {
    public BlockMetalDeviceVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if ((dev ? "onBlockActivated" : "func_149727_a").equals(name)) {
            log.debug("Modifying {}", name);
            return new MethodVisitor(api, mv) {
                private boolean visited = false;

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                    if (!visited && opcode == INVOKEVIRTUAL &&
                            owner.equals("net/minecraft/entity/player/InventoryPlayer") &&
                            name.equals(dev ? "addItemStackToInventory" : "func_70441_a") &&
                            desc.equals("(Lnet/minecraft/item/ItemStack;)Z")) {
                        log.debug("Redirecting {}", name);
                        super.visitMethodInsn(INVOKESTATIC, ASMCALLHOOKSERVER_INTERNAL_NAME, "addToPlayerInventoryBiased", "(Lnet/minecraft/entity/player/InventoryPlayer;Lnet/minecraft/item/ItemStack;)Z", false);
                        visited = true;
                    } else {
                        super.visitMethodInsn(opcode, owner, name, desc, itf);
                    }
                }
            };
        }
        return mv;
    }
}
