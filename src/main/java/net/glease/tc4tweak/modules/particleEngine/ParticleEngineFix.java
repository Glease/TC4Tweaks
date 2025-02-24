package net.glease.tc4tweak.modules.particleEngine;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import net.minecraft.client.particle.EntityFX;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import org.apache.commons.lang3.reflect.FieldUtils;
import thaumcraft.client.fx.ParticleEngine;

public enum ParticleEngineFix {
    INSTANCE;
    private static final Field particlesField = FieldUtils.getDeclaredField(ParticleEngine.class, "particles", true);

    public HashMap<Integer, ArrayList<EntityFX>>[] getParticles() {
        return getParticles(ParticleEngine.instance);
    }

    @SuppressWarnings("unchecked")
    private HashMap<Integer, ArrayList<EntityFX>>[] getParticles(ParticleEngine instance) {
        try {
            return (HashMap<Integer, ArrayList<EntityFX>>[]) particlesField.get(instance);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    // flush the particles that don't belong to this current game session
    @SubscribeEvent
    public void onServerConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        for (HashMap<Integer, ArrayList<EntityFX>> map : getParticles()) {
            map.clear();
        }
    }

    // reset worldObj to try to prevent weird crashes
    // still skeptical as whether this works at all
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (!event.world.isRemote) return;
        for (HashMap<Integer, ArrayList<EntityFX>> map : getParticles()) {
            ArrayList<EntityFX> list = map.get(event.world.provider.dimensionId);
            if (list == null) continue;
            for (EntityFX e : list) {
                e.setWorld(event.world);
            }
        }
    }

    public static void init() {
        FMLCommonHandler.instance().bus().register(INSTANCE);
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }
}
