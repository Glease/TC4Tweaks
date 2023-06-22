package net.glease.tc4tweak;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.glease.tc4tweak.asm.ASMCallhook;
import net.glease.tc4tweak.asm.LoadingPlugin;
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
import net.minecraftforge.event.world.WorldEvent;
import org.lwjgl.input.Mouse;
import thaumcraft.client.fx.other.FXSonic;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.client.gui.GuiResearchTable;
import thaumcraft.common.config.ConfigItems;

public class ClientProxy extends CommonProxy {
    static final FMLEventHandler instance = new FMLEventHandler();
    static long lastScroll = 0;
    static Field fieldPage = null;
    static Field fieldLastPage = null;
    static Method methodPlayScroll = null;
    static Field fieldModel = null;
    static Method GuiResearchRecipeMouseClicked = null;
    private static final int mPrevX = 261, mPrevY = 189, mNextX = -17, mNextY = 189;
    private static final int paneWidth = 256, paneHeight = 181;

    public static void handleMouseInput(GuiResearchTable screen) {
        if (fieldLastPage == null || fieldPage == null || methodPlayScroll == null) return;
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

    public static void handleMouseInput(GuiResearchRecipe screen) {
        if (GuiResearchRecipeMouseClicked == null) return;
        final int dwheel = Mouse.getEventDWheel();
        if (dwheel == 0)
            return;
        final long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - lastScroll > 50) {
            lastScroll = currentTimeMillis;
            // emulate a click into respective buttons
            int mX, mY;
            if ((dwheel < 0) != ConfigurationHandler.INSTANCE.isInverted()) {
                mX = mPrevX;
                mY= mPrevY;
            } else {
                mX = mNextX;
                mY = mNextY;
            }
            mX += (screen.width - paneWidth) / 2;
            mY += (screen.height - paneHeight) / 2;
            try {
                GuiResearchRecipeMouseClicked.invoke(screen, mX, mY, 1);
            } catch (ReflectiveOperationException err) {
                System.err.println("Error scrolling through research page!");
                err.printStackTrace();
            }
        }
    }

    @Override
    public void preInit(FMLPreInitializationEvent e) {
        super.preInit(e);
        try {
            Class<GuiResearchTable> guiResearchTableClass = GuiResearchTable.class;
            fieldPage = guiResearchTableClass.getDeclaredField("page");
            fieldPage.setAccessible(true);
            fieldLastPage = guiResearchTableClass.getDeclaredField("lastPage");
            fieldLastPage.setAccessible(true);
            methodPlayScroll = guiResearchTableClass.getDeclaredMethod("playButtonScroll");
            methodPlayScroll.setAccessible(true);
            fieldModel = FXSonic.class.getDeclaredField("model");
            fieldModel.setAccessible(true);
        } catch (Exception err) {
            System.err.println("Cannot find thaumcraft fields. Aspect list scrolling will not properly function!");
            err.printStackTrace();
        }
        String mouseClicked = LoadingPlugin.isDev() ? "mouseClicked" : "func_73864_a";
        try {
            GuiResearchRecipeMouseClicked = GuiResearchRecipe.class.getDeclaredMethod(mouseClicked, int.class, int.class, int.class);
            GuiResearchRecipeMouseClicked.setAccessible(true);
        } catch (Exception err) {
            System.err.println("Cannot find thaumcraft fields. Research page scrolling will not properly function!");
            err.printStackTrace();
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
            // WG is probably not installed, ignoring
        }
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load e) {
        if (e.world.isRemote)
            CommonUtils.sortResearchCategories(false);
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
