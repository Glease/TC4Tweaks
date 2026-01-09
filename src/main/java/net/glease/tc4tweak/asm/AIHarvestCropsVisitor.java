package net.glease.tc4tweak.asm;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.ItemManaBean;
import thaumcraft.common.tiles.TileManaPod;

import java.util.Objects;

import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

public class AIHarvestCropsVisitor extends ClassVisitor {
    public AIHarvestCropsVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        if (name.equals("harvest")) {
            log.debug("Visiting {}{}", name, desc);
            return new HarvestMethodVisitor(api, mv, access, name, desc);
        }
        return mv;
    }

    private static Aspect manaBeanType;

    @SuppressWarnings("unused")
    public static void getManaBeanType(World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(x, y, z);

        manaBeanType = (te instanceof TileManaPod) ? ((TileManaPod) te).aspect : null;
    }

    @SuppressWarnings("unused")
    public static boolean isBadManaBean(Entity entity) {
        if (manaBeanType == null) return false;

        EntityItem entityItem = (EntityItem) entity;
        ItemStack itemStack = entityItem.getEntityItem();
        if (itemStack.getItem() != ConfigItems.itemManaBean) return false;

        AspectList aspects = ((ItemManaBean) ConfigItems.itemManaBean).getAspects(itemStack);
        return aspects.size() == 0 || manaBeanType != aspects.getAspects()[0];
    }


    private static class HarvestMethodVisitor extends MethodVisitor {
        private boolean addedHeader = false;

        public HarvestMethodVisitor(int api, MethodVisitor mv, int access, String name, String desc) {
            super(api, mv);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            super.visitFieldInsn(opcode, owner, name, desc);

            if (opcode != PUTFIELD || addedHeader) return;

            addedHeader = true;

            Label lblDynamicContentMarker = new Label();
            mv.visitLabel(lblDynamicContentMarker);
            mv.visitLineNumber(114514, lblDynamicContentMarker);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "thaumcraft/common/entities/ai/interact/AIHarvestCrops", "theWorld", "Lnet/minecraft/world/World;");

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "thaumcraft/common/entities/ai/interact/AIHarvestCrops", "xx", "I");

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "thaumcraft/common/entities/ai/interact/AIHarvestCrops", "yy", "I");

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "thaumcraft/common/entities/ai/interact/AIHarvestCrops", "zz", "I");

            mv.visitMethodInsn(INVOKESTATIC, "net/glease/tc4tweak/asm/AIHarvestCropsVisitor", "getManaBeanType", "(Lnet/minecraft/world/World;III)V", false);
        }

        private Label lastLabel;
        private Label loopStartLabel;

        @Override
        public void visitLabel(Label label) {
            super.visitLabel(label);

            lastLabel = label;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);

            if (loopStartLabel != null) return;

            if (opcode == INVOKEINTERFACE && Objects.equals(owner, "java/util/Iterator") && Objects.equals(name, "next")) {
                if (lastLabel == null)
                    throw new RuntimeException("Failed to patch method: Last label was still null when the loop started!");

                loopStartLabel = lastLabel;
            }
        }

        private boolean nextJump = false;
        private boolean addedFooter = false;

        @Override
        public void visitTypeInsn(int opcode, String type) {
            super.visitTypeInsn(opcode, type);

            if (addedFooter || opcode != INSTANCEOF || !Objects.equals(type, "net/minecraft/entity/item/EntityItem"))
                return;

            nextJump = true;
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            super.visitJumpInsn(opcode, label);

            if (addedFooter || !nextJump) return;

            if (!addedHeader) throw new RuntimeException("Cannot add footer before header!");

            addedFooter = true;
            nextJump = false;

            mv.visitVarInsn(ALOAD, 6);
            mv.visitMethodInsn(INVOKESTATIC, "net/glease/tc4tweak/asm/AIHarvestCropsVisitor", "isBadManaBean", "(Lnet/minecraft/entity/Entity;)Z", false);
            mv.visitJumpInsn(IFNE, loopStartLabel);
        }
    }
}