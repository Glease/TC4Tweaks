package net.glease.tc4tweak;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.glease.tc4tweak.asm.ASMCallhook;
import net.glease.tc4tweak.modules.researchBrowser.BrowserPaging;
import net.glease.tc4tweak.modules.researchBrowser.ThaumonomiconIndexSearcher;
import net.glease.tc4tweak.network.NetworkedConfiguration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.input.Mouse;
import thaumcraft.client.fx.other.FXSonic;
import thaumcraft.client.gui.GuiResearchTable;
import thaumcraft.common.config.ConfigItems;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ClientProxy extends CommonProxy {
    static final FMLEventHandler instance = new FMLEventHandler();
    static long lastScroll = 0;
    static Field fieldPage = null;
    static Field fieldLastPage = null;
    static Method methodPlayScroll = null;
    static Field fieldModel = null;

    public static void handleMouseInput(GuiResearchTable screen) {
        final int dwheel = Mouse.getEventDWheel();
        if (dwheel == 0)
            return;
        final long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - lastScroll > 50) {
            lastScroll = currentTimeMillis;
            try {
                int page = fieldPage.getInt(screen);
                if ((dwheel < 0) != ConfigurationHandler.INSTANCE.isInverted()) {
                    int lastPage = fieldLastPage.getInt(screen);
                    if (page < lastPage) {
                        fieldPage.setInt(screen, page + 1);
                        methodPlayScroll.invoke(screen);
                    }
                } else {
                    if (page > 0) {
                        fieldPage.setInt(screen, page - 1);
                        methodPlayScroll.invoke(screen);
                    }
                }
            } catch (ReflectiveOperationException err) {
                System.err.println("Error scrolling through aspect list!");
                err.printStackTrace();
            }
        }
    }

    @Override
    public void preInit(FMLPreInitializationEvent e) {
        super.preInit(e);
        try {
            fieldPage = GuiResearchTable.class.getDeclaredField("page");
            fieldPage.setAccessible(true);
            fieldLastPage = GuiResearchTable.class.getDeclaredField("lastPage");
            fieldLastPage.setAccessible(true);
            methodPlayScroll = GuiResearchTable.class.getDeclaredMethod("playButtonScroll");
            methodPlayScroll.setAccessible(true);
            fieldModel = FXSonic.class.getDeclaredField("model");
            fieldModel.setAccessible(true);
        } catch (Exception err) {
            System.err.println("Cannot find thaumcraft fields. The mod will not properly function!");
            err.printStackTrace();
            return;
        }
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(instance);
        final IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
        if (resourceManager instanceof IReloadableResourceManager) {
            //noinspection Convert2Lambda
            ((IReloadableResourceManager) resourceManager).registerReloadListener(new IResourceManagerReloadListener() {
                @Override
                public void onResourceManagerReload(IResourceManager ignored) {
                    try {
                        fieldModel.set(null, null);
                    } catch (IllegalAccessException | NullPointerException err) {
                        // illegal access exception is impossible
                        // NPE means ASM has failed somehow.
                        err.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    public void init(FMLInitializationEvent e) {
        super.init(e);
        ThaumonomiconIndexSearcher.init();
        BrowserPaging.init();
    }

    @Override
    public void postInit(FMLPostInitializationEvent e) {
        super.postInit(e);
        try {
            Object wgSearcher = Class.forName("witchinggadgets.client.ThaumonomiconIndexSearcher").getField("instance").get(null);
            MinecraftForge.EVENT_BUS.unregister(wgSearcher);
            FMLCommonHandler.instance().bus().unregister(wgSearcher);
        } catch (ReflectiveOperationException ignored) {
            // WG is probably installed, ignoring
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTooltip(ItemTooltipEvent e) {
        if (ConfigurationHandler.INSTANCE.isAddTooltip() && e.itemStack != null) {
            if (e.itemStack.getItem() == ConfigItems.itemResearchNotes)
                e.toolTip.add(EnumChatFormatting.GOLD + StatCollector.translateToLocal("tc4tweaks.enabled_scrolling"));
            else if (e.itemStack.getItem() == ConfigItems.itemWandCasting) {
                if (!ConfigurationHandler.INSTANCE.isCheckWorkbenchRecipes() || !NetworkedConfiguration.isCheckWorkbenchRecipes()) {
                    e.toolTip.add(EnumChatFormatting.RED + StatCollector.translateToLocal("tc4tweaks.disable_vanilla"));
                }
            }
        }
    }

    public static class FMLEventHandler {
        private long updateCounter = 0;

        @SubscribeEvent
        public void onTickEnd(TickEvent.ClientTickEvent e) {
            if (e.phase == TickEvent.Phase.END) {
                if (++updateCounter > ConfigurationHandler.INSTANCE.getUpdateInterval()) {
                    updateCounter = 0;
                    ASMCallhook.updatePostponed();
                }
            }
        }
    }
}
