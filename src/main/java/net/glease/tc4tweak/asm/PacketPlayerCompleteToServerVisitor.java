package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import thaumcraft.api.research.ResearchItem;

import static net.glease.tc4tweak.asm.ASMUtils.arrayAppend;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

public class PacketPlayerCompleteToServerVisitor extends ClassVisitor {

    private String className;

    public PacketPlayerCompleteToServerVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        super.visit(version, access, this.className, signature, superName, arrayAppend(interfaces, "net/glease/tc4tweak/asm/PacketPlayerCompleteToServerVisitor$PacketPlayerCompleteToServerAccess"));
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if ("onMessage".equals(name) && "(Lthaumcraft/common/lib/network/playerdata/PacketAspectCombinationToServer;Lcpw/mods/fml/common/network/simpleimpl/MessageContext;)Lcpw/mods/fml/common/network/simpleimpl/IMessage;".equals(desc)) {
            log.debug("Visiting {}{}", name, desc);
            return new PacketSanityMethodVisitor(api, mv, "sanityPlayerComplete", "(Lnet/glease/tc4tweak/asm/PacketPlayerCompleteToServerVisitor$PacketPlayerCompleteToServerAccess;Lcpw/mods/fml/common/network/simpleimpl/MessageContext;)Z");
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        // add implementation to accessor
        log.debug("Adding Accessor methods");
        MethodVisitor mv;
        {
            mv = cv.visitMethod(ACC_PUBLIC, "research", "()Lthaumcraft/api/research/ResearchItem;", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "key", "Ljava/lang/String;");
            mv.visitMethodInsn(INVOKESTATIC, "thaumcraft/api/research/ResearchCategories", "getResearch", "(Ljava/lang/String;)Lthaumcraft/api/research/ResearchItem;", false);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
            log.trace("research() added");
        }
        {
            mv = cv.visitMethod(ACC_PUBLIC, "type", "()B", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "type", "B");
            mv.visitInsn(IRETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
            log.trace("type() added");
        }
        super.visitEnd();
    }

    public interface PacketPlayerCompleteToServerAccess {
        ResearchItem research();
        byte type();
    }
}
