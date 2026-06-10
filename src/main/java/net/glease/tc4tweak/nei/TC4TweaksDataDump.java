package net.glease.tc4tweak.nei;

import java.io.File;

import codechicken.nei.config.DataDumper;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.*;

abstract class TC4TweaksDataDump extends DataDumper {
    protected static final int DEFAULT_MODE = 0;
    public TC4TweaksDataDump(String name) {
        super(name);
    }

    @Override
    public IChatComponent dumpMessage(File file) {
        IChatComponent filemsg = new ChatComponentText("dumps/" + file.getName());
        ChatComponentTranslation msg = new ChatComponentTranslation(namespaced(name) + ".dumped", filemsg);
        try {
            filemsg.setChatStyle(new ChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getCanonicalPath())).setUnderlined(true).setColor(EnumChatFormatting.BLUE));
            return msg;
        } catch (Exception ex) {
            return msg.appendSibling(new ChatComponentText("Error preparing chat message: " + ex.getLocalizedMessage()));
        }
    }

    @Override
    public String modeButtonText() {
        return translateN(name + ".mode." + getTag().getIntValue(DEFAULT_MODE));
    }
}
