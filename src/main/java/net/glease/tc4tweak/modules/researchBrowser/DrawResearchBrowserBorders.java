package net.glease.tc4tweak.modules.researchBrowser;

import net.glease.tc4tweak.ClientUtils;
import net.glease.tc4tweak.ConfigurationHandler;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.gui.GuiResearchBrowser;

public class DrawResearchBrowserBorders {
    // I'm actually amazed to see it's not the same...
    public static final int BORDER_HEIGHT = 17, BORDER_WIDTH = 16, TEXTURE_WIDTH = 256, TEXTURE_HEIGHT = 230;
    private static final double BACKGROUND_ZLEVEL = -100.0;
    // each int[] denotes two vector. the dot product with (coord, border size) produce the right coord for a side on the 3x3 grid
    private static final int[][] PARAMS = {{0, 0, 0, 1}, {0, 1, 1, -1}, {1, -1, 1, 0}};

    public static void drawResearchBrowserBorders(GuiResearchBrowser gui, int x, int y, int u, int v, int width, int height) {
        // enable depth test to write into depth buffer to update depth value
        int oldDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_ALWAYS);
        for (int[] paramX : PARAMS) {
            for (int[] paramY : PARAMS) {
                ClientUtils.drawRectTextured(
                        x + paramX[0] * width + paramX[1] * BORDER_WIDTH, x + paramX[2] * width + paramX[3] * BORDER_WIDTH,
                        y + paramY[0] * height + paramY[1] * BORDER_HEIGHT, y + paramY[2] * height + paramY[3] * BORDER_HEIGHT,
                        paramX[0] * TEXTURE_WIDTH + paramX[1] * BORDER_WIDTH, paramX[2] * TEXTURE_WIDTH + paramX[3] * BORDER_WIDTH,
                        paramY[0] * TEXTURE_HEIGHT + paramY[1] * BORDER_HEIGHT, paramY[2] * TEXTURE_HEIGHT + paramY[3] * BORDER_HEIGHT,
                        BACKGROUND_ZLEVEL);
            }
        }
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(oldDepthFunc);
    }

    public static void drawResearchBrowserBackground(GuiResearchBrowser gui, int x, int y, int u, int v, int width, int height) {
        float scale = ConfigurationHandler.INSTANCE.getBrowserScale();
        double x1 = x / scale, y1 = y / scale,
                u1 = u / scale, v1 = v / scale,
                w1 = ConfigurationHandler.INSTANCE.getBrowserWidth() / scale / 2 - BORDER_WIDTH / scale, h1 = ConfigurationHandler.INSTANCE.getBrowserHeight() / scale / 2 - BORDER_HEIGHT / scale;
        GL11.glScalef(scale, scale, 1.0f);
        ClientUtils.drawRectTextured(x1, x1 + w1, y1, y1 + h1, u1, u1 + w1, v1, v1 + h1, BACKGROUND_ZLEVEL);
    }
}
