package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;
import static net.glease.tc4tweak.asm.LoadingPlugin.dev;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

class TileHoleVisitor extends ClassVisitor {
    public TileHoleVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals(dev ? "getDescriptionPacket" : "func_145844_m")) {
            log.debug("Visiting {}{}", name, desc);
            return new GetDescriptionPacketVisitor(api, mv);
        }
        return mv;
    }

    private static class GetDescriptionPacketVisitor extends MethodVisitor {
        public GetDescriptionPacketVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == ARETURN) {
                log.trace("Modifying return value");
                mv.visitMethodInsn(INVOKESTATIC, ASMCALLHOOKSERVER_INTERNAL_NAME, "createTileHoleSyncPacket", "(Lnet/minecraft/network/play/server/S35PacketUpdateTileEntity;)Lnet/minecraft/network/Packet;", false);
            }
            super.visitInsn(opcode);
        }
    }
}
