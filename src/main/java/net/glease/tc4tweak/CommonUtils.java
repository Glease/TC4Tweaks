package net.glease.tc4tweak;

import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;

public class CommonUtils {
    // only keep the strings, so tab objects doesn't leak if they are ever removed
    private static final LinkedHashSet<String> originalTabOrders = new LinkedHashSet<>();

    public static String toString(AspectList al) {
        return al.aspects.entrySet().stream().filter(e -> e.getKey() != null && e.getValue() != null).map(e -> String.format("%dx%s", e.getValue(), e.getKey().getName())).collect(Collectors.joining(";"));
    }

    public static String toString(CrucibleRecipe r) {
        return "CrucibleRecipe{key="+r.key+",catalyst="+r.catalyst+",output="+r.getRecipeOutput()+",aspects="+toString(r.aspects)+"}";
    }

    static void sortResearchCategories(boolean force) {
        if (force || !ConfigurationHandler.INSTANCE.getCategoryOrder().isEmpty()) {
            // no need to synchronize
            // we fetch data from a practically immutable collection
            // then create a local copy
            // then replace the reference to that immutable collection with our local copy,
            // which is a simple PUTFIELD, which is atomic.
            LinkedHashMap<String, ResearchCategoryList> categories = ResearchCategories.researchCategories;
            originalTabOrders.addAll(categories.keySet());
            Set<String> realOrder = new LinkedHashSet<>(ConfigurationHandler.INSTANCE.getCategoryOrder());
            realOrder.addAll(originalTabOrders);
            LinkedHashMap<String, ResearchCategoryList> newCategories = new LinkedHashMap<>();
            for (String tab : realOrder) {
                if (categories.containsKey(tab))
                    newCategories.put(tab, categories.get(tab));
            }
            ResearchCategories.researchCategories = newCategories;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T reflectGet(Field f, Object instance) {
        try {
            return (T) f.get(instance);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    public static Field getField(Class<?> clazz, String fieldName, int index) {
        try {
            Field f = null;
            Field[] fields = clazz.getDeclaredFields();
            if (index >= 0 && fields.length > index)
                f = fields[index];
            if (f == null || !f.getName().equalsIgnoreCase(fieldName))
                f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            return f;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    public static <T> T deref(Reference<T> ref) {
        return ref == null ? null : ref.get();
    }

    public static int clamp(int val, int min, int max) {
        return Math.min(Math.max(min, val), max);
    }

    public static boolean isChunkLoaded(World world, int x, int y, int z) {
        if (world.isRemote) {
            // client world lies about chunk existence
            return !world.getChunkFromBlockCoords(x, z).isEmpty();
        }
        return world.blockExists(x, y, z);
    }

    /**
     * Send a S35 update packet if given tile is very close to the end of player's view distance
     * When player is moving very fast this might lead to some missed update somehow and this should fix it.
     */
    public static void sendSupplementaryS35(TileEntity te) {
        if (!ConfigurationHandler.INSTANCE.isSendSupplementaryS35()) return;
        World w = te.getWorldObj();
        if (w.isRemote) return;
        Packet packet = te.getDescriptionPacket();
        if (packet == null) return;
        // here we send a little further out in case player is moving very fast
        int viewDistance = MinecraftServer.getServer().getConfigurationManager().getViewDistance();
        int sendDistanceLo = viewDistance - 2;
        int sendDistanceHi = viewDistance + 4;
        for (Object o : w.playerEntities) {
            if (!(o instanceof EntityPlayerMP)) continue;
            EntityPlayerMP player = (EntityPlayerMP) o;
            int dx = (((int) player.posX) >> 4) - (te.xCoord >> 4);
            int dz = (((int) player.posZ) >> 4) - (te.zCoord >> 4);
            int dist = Math.max(Math.abs(dx), Math.abs(dz));
            if (dist >= sendDistanceLo && dist <= sendDistanceHi) {
                player.playerNetServerHandler.sendPacket(packet);
            }
        }
    }
}
