package net.glease.tc4tweak.modules.researchBrowser;

import net.glease.tc4tweak.ConfigurationHandler;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.gui.GuiResearchBrowser;

public class DrawResearchBrowserBorders {
    // I'm actually amazed to see it's not the same...
    public static final int BORDER_HEIGHT = 17, BORDER_WIDTH = 16, TEXTURE_WIDTH = 256, TEXTURE_HEIGHT = 230;
    private static final double ZLEVEL = 0.0;
    // each int[] denotes two vector. the dot product with (coord, border size) produce the right coord for a side on the 3x3 grid
    private static final int[][] PARAMS = {{0, 0, 0, 1}, {0, 1, 1, -1}, {1, -1, 1, 0}};

    public static void drawResearchBrowserBorders(GuiResearchBrowser gui, int x, int y, int u, int v, int width, int height) {
//        if (false)
        for (int[] paramX : PARAMS) {
            for (int[] paramY : PARAMS) {
                drawRectTextured(
                        x + paramX[0] * width + paramX[1] * BORDER_WIDTH, x + paramX[2] * width + paramX[3] * BORDER_WIDTH,
                        y + paramY[0] * height + paramY[1] * BORDER_HEIGHT, y + paramY[2] * height + paramY[3] * BORDER_HEIGHT,
                        paramX[0] * TEXTURE_WIDTH + paramX[1] * BORDER_WIDTH, paramX[2] * TEXTURE_WIDTH + paramX[3] * BORDER_WIDTH,
                        paramY[0] * TEXTURE_HEIGHT + paramY[1] * BORDER_HEIGHT, paramY[2] * TEXTURE_HEIGHT + paramY[3] * BORDER_HEIGHT
                );
            }
        }
    }

    private static void drawRectTextured(int xmin, int xmax, int ymin, int ymax, int umin, int umax, int vmin, int vmax) {
        float f = 0.00390625F;
        float f1 = 0.00390625F;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(xmin, ymax, ZLEVEL, umin * f, vmax * f1);
        tessellator.addVertexWithUV(xmax, ymax, ZLEVEL, umax * f, vmax * f1);
        tessellator.addVertexWithUV(xmax, ymin, ZLEVEL, umax * f, vmin * f1);
        tessellator.addVertexWithUV(xmin, ymin, ZLEVEL, umin * f, vmin * f1);
        tessellator.draw();
    }

    public static void drawResearchBrowserBackground(GuiResearchBrowser gui, int x, int y, int u, int v, int width, int height) {
        float scale = ConfigurationHandler.INSTANCE.getBrowserScale();
        double x1 = x / scale, y1 = y / scale,
                u1 = u / scale, v1 = v / scale,
                w1 = ConfigurationHandler.INSTANCE.getBrowserWidth() / scale / 2 - BORDER_WIDTH / scale, h1 = ConfigurationHandler.INSTANCE.getBrowserHeight() / scale / 2 - BORDER_HEIGHT / scale;
        float f = 0.00390625F;
        float f1 = 0.00390625F;
        GL11.glScalef(scale, scale, 1.0f);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x1 + 0, y1 + h1, ZLEVEL, (u1 + 0) * f, (v1 + h1) * f1);
        tessellator.addVertexWithUV(x1 + w1, y1 + h1, ZLEVEL, (u1 + w1) * f, (v1 + h1) * f1);
        tessellator.addVertexWithUV(x1 + w1, y1 + 0, ZLEVEL, (u1 + w1) * f, (v1 + 0) * f1);
        tessellator.addVertexWithUV(x1 + 0, y1 + 0, ZLEVEL, (u1 + 0) * f, (v1 + 0) * f1);
        tessellator.draw();
    }
}
