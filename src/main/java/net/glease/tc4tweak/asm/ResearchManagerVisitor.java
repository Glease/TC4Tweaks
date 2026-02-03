package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.ALOAD;

public class ResearchManagerVisitor extends ClassVisitor {
    public ResearchManagerVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals("getResearchForPlayer")) {
            log.debug("Visiting {}{}", name, desc);
            return new GetResearchForPlayerVisitor(api, mv);
        }
        return mv;
    }

    private static class GetResearchForPlayerVisitor extends MethodVisitor {
        public GetResearchForPlayerVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (name.equals("loadPlayerData")) {
                mv.visitVarInsn(ALOAD, 0);
                ASMUtils.writeRedirect(mv, ASMCALLHOOKSERVER_INTERNAL_NAME, "fixOfflinePlayerDataLoad", null, name, desc, Type.getType(String.class));
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
    }
}
