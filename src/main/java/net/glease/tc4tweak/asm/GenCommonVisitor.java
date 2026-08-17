package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

public class GenCommonVisitor extends ClassVisitor {
    public GenCommonVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals("processDecorations")) {
            log.debug("Visiting {}{}", name, desc);
            return new ProcessDecorationsVisitor(api, mv);
        }
        return mv;
    }

    private static class ProcessDecorationsVisitor extends MethodVisitor {
        boolean visited;
        public ProcessDecorationsVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (!visited && opcode == INVOKEVIRTUAL && (name.equals("getTileEntity") || name.equals("func_147438_o"))) {
                visited = true;
                ASMUtils.writeRedirect(mv, ASMCALLHOOKSERVER_INTERNAL_NAME, "genCommonGetCrystalTileEntity", owner, name, desc);
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
    }
}
