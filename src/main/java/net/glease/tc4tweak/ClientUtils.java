package net.glease.tc4tweak;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;

import java.lang.reflect.Field;
import java.util.List;

public class ClientUtils {
    private static final Field fieldButtonList = ReflectionHelper.findField(GuiScreen.class, "buttonList", "field_146292_n");

    public static void drawRectTextured(double xmin, double xmax, double ymin, double ymax, double umin, double umax, double vmin, double vmax, double zLevel) {
        // can't just call gui.drawTexturedModalRect, it can't do width scales
        // assume texture is 256x256
        float f = 1f / 256f;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(xmin, ymax, zLevel, umin * f, vmax * f);
        tessellator.addVertexWithUV(xmax, ymax, zLevel, umax * f, vmax * f);
        tessellator.addVertexWithUV(xmax, ymin, zLevel, umax * f, vmin * f);
        tessellator.addVertexWithUV(xmin, ymin, zLevel, umin * f, vmin * f);
        tessellator.draw();
    }

    @SuppressWarnings("unchecked")
    public static List<GuiButton> getButtonList(GuiScreen gui) {
        try {
            return (List<GuiButton>) fieldButtonList.get(gui);
        } catch (IllegalAccessException e1) {
            throw new RuntimeException(e1);
        }
    }
}
