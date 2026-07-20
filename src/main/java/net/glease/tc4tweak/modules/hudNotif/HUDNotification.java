package net.glease.tc4tweak.modules.hudNotif;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import acs.tabbychat.api.IChatMouseExtension;
import acs.tabbychat.api.TCExtensionManager;
import cpw.mods.fml.client.config.GuiButtonExt;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.glease.tc4tweak.ClientProxy;
import net.glease.tc4tweak.ConfigurationHandler;
import net.glease.tc4tweak.TC4Tweak;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import thaumcraft.client.lib.PlayerNotifications;

public class HUDNotification {

    public static void init() {
        tabbyChatIntegration();
        MinecraftForge.EVENT_BUS.register(new EventHandler());
    }

    private static void tabbyChatIntegration() {
        try {
            Class.forName("acs.tabbychat.api.TCExtensionManager", false, ClientProxy.class.getClassLoader());
        } catch (ReflectiveOperationException e) {
            // probably not loaded
            return;
        }
        TC4Tweak.log.info("Tabby Chat Integration Added");
        TCExtensionManager.INSTANCE.registerExtension(TabbyChatIntegration.class);
    }

    static void checkClear() {
        if (ConfigurationHandler.INSTANCE.isAddClearButton()) {
            PlayerNotifications.notificationList.clear();
            PlayerNotifications.aspectList.clear();
        }
    }

    public static class TabbyChatIntegration implements IChatMouseExtension {
        @Override
        public boolean mouseClicked(int x, int y, int button) {
            if (button != 0) return false;
            // tabbychat refuse to wire clicks through to buttons not a subclass of ChatButton, so we just wire it up ourselves
            Minecraft mc = Minecraft.getMinecraft();
            List<GuiButton> btns = ourButtons.get(mc.currentScreen);
            if (btns == null) {
                // sanity check
                return false;
            }
            for (GuiButton btn : btns) {
                if (btn.mousePressed(mc, x, y) && actionPerformed(btn)) {
                    // vanilla does it before actionPerformed. we do it after. there shouldn't be a meaningful difference
                    btn.func_146113_a(mc.getSoundHandler());
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean actionPerformed(GuiButton button) {
            if (button.id == 114514) {
                checkClear();
                return true;
            }
            return false;
        }

        @Override
        public void handleMouseInput() {

        }

        @Override
        public void load() {
            // future note: api doc lies, or at least so on forge version.
            // load() is called once per chat gui open, not at game start
            // doesn't matter for us tho
        }
    }

    private static final Map<GuiScreen, List<GuiButton>> ourButtons = new WeakHashMap<>();

    public static class EventHandler {
        @SubscribeEvent
        public void onGuiInitPost(GuiScreenEvent.InitGuiEvent.Post e) {
            if (e.gui instanceof GuiChat && ConfigurationHandler.INSTANCE.isAddClearButton()) {
                String caption = I18n.format("tc4tweaks.gui.clear_notification");
                int width = e.gui.mc.fontRenderer.getStringWidth(caption) + 8;
                @SuppressWarnings("unchecked")
                List<GuiButton> buttonList = e.buttonList;
                GuiButtonExt btn = new GuiButtonExt(114514, e.gui.width - width, e.gui.height - 18, width, 18, caption) {
                    @Override
                    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
                        this.visible = !PlayerNotifications.notificationList.isEmpty() || !PlayerNotifications.aspectList.isEmpty();
                        super.drawButton(mc, mouseX, mouseY);
                    }
                };
                buttonList.add(btn);
                ourButtons.computeIfAbsent(e.gui, v -> new ArrayList<>()).add(btn);
            }
        }

        @SubscribeEvent
        public void onGuiClick(GuiScreenEvent.ActionPerformedEvent.Pre e) {
            if (e.gui instanceof GuiChat && e.button.id == 114514)
                checkClear();
        }
    }
}
