package net.glease.tc4tweak;

import net.minecraft.client.renderer.Tessellator;

public class ClientUtils {

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
}
