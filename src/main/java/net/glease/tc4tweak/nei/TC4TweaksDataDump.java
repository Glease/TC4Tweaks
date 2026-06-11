package net.glease.tc4tweak.nei;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import codechicken.lib.vec.Rectangle4i;
import codechicken.nei.config.DataDumper;
import codechicken.nei.config.GuiOptionList;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.*;

abstract class TC4TweaksDataDump extends DataDumper {
    protected static final int DEFAULT_MODE = 0;
    private static final MethodHandle SLOT_ACCESSOR = resolveSlotAccessor();

    public TC4TweaksDataDump(String name) {
        super(name);
    }

    /*
     * NEI since 2.8.96-GTNH (41554f4c5b5a0e40ad82d7100143aee464bc416e) wraps slot in a WeakReference and demoted to a
     * getter. this MethodHandle bridges the gap between new NEI and old NEI.
     *
     * I don't really want to bump NEI version required as it now carries so much more dependencies
     */
    private static MethodHandle resolveSlotAccessor() {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        // 1. Prefer the getSlot() getter (only present in newer NEI).
        try {
            // of course it doesn't exist. it's in a newer dependency
            //noinspection JavaReflectionMemberAccess
            Method getSlot = DataDumper.class.getMethod("getSlot");
            return lookup.unreflect(getSlot);
        } catch (NoSuchMethodException ignored) {
            // not this NEI version - fall through to the field
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access NEI Option#getSlot()", e);
        }
        // 2. Fall back to the public `slot` field (present in the NEI we compile against).
        try {
            Field slot = DataDumper.class.getField("slot");
            return lookup.unreflectGetter(slot);
        } catch (NoSuchFieldException e) {
            // 3. Neither available - unsupported NEI.
            throw new RuntimeException("NEI Option exposes neither getSlot() nor a public slot field", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access NEI Option#slot field", e);
        }
    }

    private GuiOptionList.OptionScrollSlot myGetSlot() {
        try {
            return (GuiOptionList.OptionScrollSlot) SLOT_ACCESSOR.invoke(this);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to read NEI Option slot", t);
        }
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
    public Rectangle4i dumpButtonSize() {
        int width = 50;
        return new Rectangle4i(myGetSlot().slotWidth() - width, 0, width, 20);
    }

    @Override
    public String modeButtonText() {
        return translateN(name + ".mode." + getTag().getIntValue(DEFAULT_MODE));
    }
}
