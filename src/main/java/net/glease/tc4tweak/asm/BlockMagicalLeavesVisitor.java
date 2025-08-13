package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.ALOAD;

// fix initially proposed as mixin by HoleFish at https://github.com/GTNewHorizons/Hodgepodge/pull/359
class BlockMagicalLeavesVisitor extends ClassVisitor {

    private BlockMagicalLeavesVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    public static TransformerFactory createFactory() {
        return new TransformerFactory(BlockMagicalLeavesVisitor::new) {
            @Override
            public boolean isInactive() {
                if (super.isInactive()) return true;
                if (ASMUtils.isHodgepodgeFixActive("fixes", "fixThaumcraftLeavesLag", true)) {
                    log.warn("Disabling TC4Tweaks's leaves lag fix to prevent conflict with hodgepodge.");
                    return true;
                }
                return false;
            }
        };
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (name.equals("func_149674_a") || name.equals("updateTick")) {
            log.debug("Visiting {}{}", name, desc);
            return new MethodVisitor(api, super.visitMethod(access, name, desc, signature, exceptions)) {
                private boolean foundNeedle;
                @Override
                public void visitLineNumber(int line, Label start) {
                    super.visitLineNumber(line, start);
                    if (line == 292) {
                        foundNeedle = true;
                        log.trace("Found needle: Line 292");
                    }
                }

                @Override
                public void visitVarInsn(int opcode, int var) {
                    if (foundNeedle && opcode == ALOAD && var == 0) {
                        log.trace("Removing ALOAD_0");
                        return;
                    }
                    super.visitVarInsn(opcode, var);
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                    String realName = name, realDesc = desc;
                    if (foundNeedle && name.equals("func_147465_d") || name.equals("setBlock")) {
                        realDesc = "(IIIII)Z";
                        if (!LoadingPlugin.isDev()) {
                            realName = "func_72921_c";
                        } else {
                            realName = "setBlockMetadataWithNotify";
                        }
                        log.debug("Replacing call {}#{}{} to {}#{}{}", owner, name, desc, owner, realName, realDesc);
                        foundNeedle = false;
                    }
                    super.visitMethodInsn(opcode, owner, realName, realDesc, itf);
                }
            };
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }
}
