package net.glease.tc4tweak.asm;

import net.minecraft.entity.player.EntityPlayerMP;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.tiles.TileResearchTable;

import static net.glease.tc4tweak.asm.ASMUtils.arrayAppend;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

public class PacketAspectCombinationToServerVisitor extends ClassVisitor {
    public PacketAspectCombinationToServerVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, arrayAppend(interfaces, "net/glease/tc4tweak/asm/PacketAspectCombinationToServerVisitor$PacketAspectCombinationToServerAccess"));
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if ("onMessage".equals(name) && "(Lthaumcraft/common/lib/network/playerdata/PacketAspectCombinationToServer;Lcpw/mods/fml/common/network/simpleimpl/MessageContext;)Lcpw/mods/fml/common/network/simpleimpl/IMessage;".equals(desc)) {
            log.debug("Visiting {}{}", name, desc);
            return new PacketSanityMethodVisitor(api, mv, "sanityCheckAspectCombination", "(Lnet/glease/tc4tweak/asm/PacketAspectCombinationToServerVisitor$PacketAspectCombinationToServerAccess;Lcpw/mods/fml/common/network/simpleimpl/MessageContext;)Z");
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        // add implementation to accessor
        log.debug("Adding Accessor methods");
        MethodVisitor mv;
        {
            mv = cv.visitMethod(ACC_PUBLIC, "table", "()Lthaumcraft/common/tiles/TileResearchTable;", null, null);
            mv.visitCode();
            writeGetWorld(mv);
            mv.visitFieldInsn(GETFIELD, "thaumcraft/common/lib/network/playerdata/PacketAspectCombinationToServer", "x", "I");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "thaumcraft/common/lib/network/playerdata/PacketAspectCombinationToServer", "y", "I");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "thaumcraft/common/lib/network/playerdata/PacketAspectCombinationToServer", "z", "I");
            mv.visitMethodInsn(INVOKEVIRTUAL, "net/minecraft/world/WorldServer", "getTileEntity", "(III)Lnet/minecraft/tileentity/TileEntity;", false);
            mv.visitVarInsn(ASTORE, 2);
            mv.visitVarInsn(ALOAD, 2);
            writeNotInstanceOf(mv, "thaumcraft/common/tiles/TileResearchTable");
            mv.visitFrame(Opcodes.F_APPEND, 1, new Object[]{"net/minecraft/tileentity/TileEntity"}, 0, null);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitTypeInsn(CHECKCAST, "thaumcraft/common/tiles/TileResearchTable");
            mv.visitInsn(ARETURN);
            mv.visitMaxs(4, 3);
            mv.visitEnd();
            log.trace("table() added");
        }
        {
            mv = cv.visitMethod(ACC_PUBLIC, "player", "()Lnet/minecraft/entity/player/EntityPlayerMP;", null, null);
            mv.visitCode();
            Label label0 = new Label();
            mv.visitLabel(label0);
            writeGetWorld(mv);
            mv.visitFieldInsn(GETFIELD, "thaumcraft/common/lib/network/playerdata/PacketAspectCombinationToServer", "playerid", "I");
            mv.visitMethodInsn(INVOKEVIRTUAL, "net/minecraft/world/WorldServer", "getEntityByID", "(I)Lnet/minecraft/entity/Entity;", false);
            mv.visitVarInsn(ASTORE, 2);
            mv.visitVarInsn(ALOAD, 2);
            writeNotInstanceOf(mv, "net/minecraft/entity/player/EntityPlayerMP");
            mv.visitFrame(Opcodes.F_APPEND, 1, new Object[]{"net/minecraft/entity/Entity"}, 0, null);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitTypeInsn(CHECKCAST, "net/minecraft/entity/player/EntityPlayerMP");
            mv.visitInsn(ARETURN);
            mv.visitMaxs(2, 3);
            mv.visitEnd();
            log.trace("player() added");
        }
        {
            mv = cv.visitMethod(ACC_PUBLIC, "lhs", "()Lthaumcraft/api/aspects/Aspect;", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "thaumcraft/common/lib/network/playerdata/PacketAspectCombinationToServer", "aspect1", "Lthaumcraft/api/aspects/Aspect;");
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
            log.trace("lhs() added");
        }
        {
            mv = cv.visitMethod(ACC_PUBLIC, "rhs", "()Lthaumcraft/api/aspects/Aspect;", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "thaumcraft/common/lib/network/playerdata/PacketAspectCombinationToServer", "aspect2", "Lthaumcraft/api/aspects/Aspect;");
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
            log.trace("rhs() added");
        }
        super.visitEnd();
    }

    private void writeGetWorld(MethodVisitor mv) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "thaumcraft/common/lib/network/playerdata/PacketAspectCombinationToServer", "dim", "I");
        mv.visitMethodInsn(INVOKESTATIC, "net/minecraftforge/common/DimensionManager", "getWorld", "(I)Lnet/minecraft/world/WorldServer;", false);
        mv.visitVarInsn(ASTORE, 1);
        mv.visitVarInsn(ALOAD, 1);
        Label label2 = new Label();
        mv.visitJumpInsn(IFNONNULL, label2);
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ARETURN);
        mv.visitLabel(label2);
        mv.visitFrame(Opcodes.F_APPEND, 1, new Object[]{"net/minecraft/world/WorldServer"}, 0, null);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 0);
    }

    private static void writeNotInstanceOf(MethodVisitor mv, String type) {
        mv.visitTypeInsn(INSTANCEOF, type);
        Label label4 = new Label();
        mv.visitJumpInsn(IFNE, label4);
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ARETURN);
        mv.visitLabel(label4);
    }

    public interface PacketAspectCombinationToServerAccess {
        TileResearchTable table();
        EntityPlayerMP player();
        Aspect lhs();
        Aspect rhs();
    }
}
