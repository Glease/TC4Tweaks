package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.TC4Transformer.log;

// fix initially proposed as mixin by HoleFish at https://github.com/GTNewHorizons/Hodgepodge/pull/359
class BlockMagicalLogVisitor extends ClassVisitor {
    private static final boolean hodgepodge = ASMUtils.isHodgepodgeFixActive("fixThaumcraftLeavesLag");
    private BlockMagicalLogVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    public static TransformerFactory createFactory() {
        return new TransformerFactory(BlockMagicalLogVisitor::new) {
            @Override
            public boolean isInactive() {
                if (super.isInactive()) return true;
                if (hodgepodge)
                    log.warn("Disabling TC4Tweaks's leaves lag fix to prevent conflict with hodgepodge.");
                return hodgepodge;
            }
        };
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        String realName = name, realDesc = desc;
        if (name.equals("breakBlock") || name.equals("func_149749_a")) {
            realDesc = "(Lnet/minecraft/world/World;IIILnet/minecraft/block/Block;I)V";
            if (!LoadingPlugin.isDev()) {
                realName = "func_149749_a";
            }
        }
        TC4Transformer.log.debug("Renaming {}{} to {}{}", name, desc, realName, realDesc);
        return super.visitMethod(access, realName, realDesc, signature, exceptions);
    }
}
