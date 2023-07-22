package net.glease.tc4tweak.modules.hudNotif;

import java.util.List;

import cpw.mods.fml.client.config.GuiButtonExt;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.glease.tc4tweak.ConfigurationHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import thaumcraft.client.lib.PlayerNotifications;

public class HUDNotification {

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new EventHandler());
    }

    public static class EventHandler {
        @SubscribeEvent
        public void onGuiInitPost(GuiScreenEvent.InitGuiEvent.Post e) {
            if (e.gui instanceof GuiChat && ConfigurationHandler.INSTANCE.isAddClearButton()) {
                String caption = I18n.format("tc4tweaks.gui.clear_notification");
                int width = e.gui.mc.fontRenderer.getStringWidth(caption) + 8;
                @SuppressWarnings("unchecked")
                List<GuiButton> buttonList = e.buttonList;
                buttonList.add(new GuiButtonExt(114514, e.gui.width - width, e.gui.height - 18, width, 18, caption) {
                    @Override
                    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
                        this.visible = !PlayerNotifications.notificationList.isEmpty() || !PlayerNotifications.aspectList.isEmpty();
                        super.drawButton(mc, mouseX, mouseY);
                    }
                });
            }
        }

        @SubscribeEvent
        public void onGuiClick(GuiScreenEvent.ActionPerformedEvent.Pre e) {
            if (e.gui instanceof GuiChat && e.button.id == 114514 && ConfigurationHandler.INSTANCE.isAddClearButton()) {
                PlayerNotifications.notificationList.clear();
                PlayerNotifications.aspectList.clear();
            }
        }
    }
}
