package net.glease.tc4tweak;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.relauncher.Side;
import net.glease.tc4tweak.modules.FlushableCache;
import net.glease.tc4tweak.network.MessageSendConfiguration;
import net.glease.tc4tweak.network.MessageSendConfigurationV2;
import net.glease.tc4tweak.network.NetworkedConfiguration;
import net.glease.tc4tweak.network.TileHoleSyncPacket;
import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorProjectileDispense;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.dispenser.IPosition;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.entities.projectile.EntityPrimalArrow;

public class CommonProxy {
    public void preInit(FMLPreInitializationEvent e) {
        ConfigurationHandler.INSTANCE.init(e.getSuggestedConfigurationFile());

        if (Loader.isModLoaded("MineTweaker3"))
            MTCompat.preInit();

        TC4Tweak.INSTANCE.CHANNEL.registerMessage(MessageSendConfiguration.class, MessageSendConfiguration.class, 0, Side.CLIENT);
        TC4Tweak.INSTANCE.CHANNEL.registerMessage(MessageSendConfigurationV2.class, MessageSendConfigurationV2.class, 1, Side.CLIENT);
        TC4Tweak.INSTANCE.CHANNEL.registerMessage(TileHoleSyncPacket.class, TileHoleSyncPacket.class, 2, Side.CLIENT);
        int debugadd = Integer.getInteger("glease.debug.addtc4tabs.pre", 0);
        addDummyCategories(debugadd, "DUMMYPRE");
    }

    public void serverStarted(FMLServerStartedEvent e) {
        FlushableCache.enableAll(true);
        NetworkedConfiguration.reset();
        FMLCommonHandler.instance().bus().register(this);
    }

    public void init(FMLInitializationEvent e) {
        BlockDispenser.dispenseBehaviorRegistry.putObject(ConfigItems.itemPrimalArrow, new BehaviorProjectileDispense() {
            @Override
            public ItemStack dispenseStack(IBlockSource dispenser, ItemStack stack) {
                EnumFacing facing = BlockDispenser.func_149937_b(dispenser.getBlockMetadata());
                IPosition pos = BlockDispenser.func_149939_a(dispenser);
                ItemStack toDrop = stack.splitStack(1);
                if (ConfigurationHandler.INSTANCE.isDispenserShootPrimalArrow()) {
                    World w = dispenser.getWorld();
                    EntityPrimalArrow e = (EntityPrimalArrow) getProjectileEntity(w, pos);
                    e.type = toDrop.getItemDamage();
                    if (e.type == 3)
                        // inherent power of earth arrow
                        // this is unfortunately not done on hit, but at bow draw time, so we must emulate this as well
                        e.setKnockbackStrength(1);
                    e.setThrowableHeading(facing.getFrontOffsetX(), facing.getFrontOffsetY() + 0.1F, facing.getFrontOffsetZ(), this.func_82500_b(), this.func_82498_a());
                    w.spawnEntityInWorld(e);
                } else {
                    doDispense(dispenser.getWorld(), toDrop, 6, facing, pos);
                }
                return stack;
            }

            @Override
            protected IProjectile getProjectileEntity(World w, IPosition iposition) {
                return new EntityPrimalArrow(w, iposition.getX(), iposition.getY(), iposition.getZ());
            }
        });
    }

    public void postInit(FMLPostInitializationEvent e) {
        int debugadd = Integer.getInteger("glease.debug.addtc4tabs.post", 0);
        for (int i = 0; i < debugadd; i++) {
            addDummyCategories(debugadd, "DUMMYPOST");
        }
    }

    private void addDummyCategories(int amount, String categoryPrefix) {
        for (int i = 0; i < amount; i++) {
            ResearchCategories.registerCategory(categoryPrefix + i, new ResourceLocation("thaumcraft", "textures/items/thaumonomiconcheat.png"), new ResourceLocation("thaumcraft", "textures/gui/gui_researchback.png"));
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.player instanceof EntityPlayerMP && !TC4Tweak.INSTANCE.isAllowAll()) {
            TC4Tweak.INSTANCE.CHANNEL.sendTo(new MessageSendConfiguration(), (EntityPlayerMP) e.player);
            TC4Tweak.INSTANCE.CHANNEL.sendTo(new MessageSendConfigurationV2(), (EntityPlayerMP) e.player);
        }
    }
}
