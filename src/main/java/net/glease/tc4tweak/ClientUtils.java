package net.glease.tc4tweak;

import java.awt.*;
import java.util.List;

import codechicken.lib.math.MathHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

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

    // region GuiDraw lifted code
    // code in this region is lifted from GuiDraw.java in CodeChickenLib to ensure this mod does not depend on
    // any library of any particular version, except the latest thaumcraft 4.
    // this region means the code between "region GuiDraw lifted code" and the nearest "endregion" or the end of file
    // Copyright (C) 2014 ChickenBones
    // This library is free software; you can redistribute it and/or
    // modify it under the terms of the GNU Lesser General Public
    // License as published by the Free Software Foundation; either
    // version 2.1 of the License, or (at your option) any later version.
    //
    // This library is distributed in the hope that it will be useful,
    // but WITHOUT ANY WARRANTY; without even the implied warranty of
    // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    // Lesser General Public License for more details.
    //
    // You should have received a copy of the GNU Lesser General Public
    // License along with this library; if not, write to the Free Software
    // Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
    // USA

    /**
     * Append a string in the tooltip list with TOOLTIP_LINESPACE to have a small gap between it and the next line
     */
    public static final String TOOLTIP_LINESPACE = "§h";

    public static Dimension displaySize() {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        return new Dimension(res.getScaledWidth(), res.getScaledHeight());
    }

    public static void drawMultilineTip(FontRenderer font, int x, int y, List<String> list) {
        if (list.isEmpty()) return;

        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        RenderHelper.disableStandardItemLighting();

        int w = 0;
        int h = -2;
        for (int i = 0; i < list.size(); i++) {
            String s = list.get(i);
            Dimension d = new Dimension(
                    font.getStringWidth(s),
                    list.get(i).endsWith(TOOLTIP_LINESPACE) && i + 1 < list.size() ? 12 : 10);
            w = Math.max(w, d.width);
            h += d.height;
        }

        if (x < 8) x = 8;
        else if (x > displaySize().width - w - 8) {
            x -= 24 + w; // flip side of cursor
            if (x < 8) x = 8;
        }
        y = (int) MathHelper.clip(y, 8, displaySize().height - 8 - h);

        gui.incZLevel(300);
        drawTooltipBox(x - 4, y - 4, w + 7, h + 7);
        for (String s : list) {
            font.drawStringWithShadow(s, x, y, -1);
            y += s.endsWith(TOOLTIP_LINESPACE) ? 12 : 10;
        }

        gui.incZLevel(-300);

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.enableGUIStandardItemLighting();
    }

    public static void drawTooltipBox(int x, int y, int w, int h) {
        int bg = 0xf0100010;
        drawGradientRect(x + 1, y, w - 1, 1, bg, bg);
        drawGradientRect(x + 1, y + h, w - 1, 1, bg, bg);
        drawGradientRect(x + 1, y + 1, w - 1, h - 1, bg, bg); // center
        drawGradientRect(x, y + 1, 1, h - 1, bg, bg);
        drawGradientRect(x + w, y + 1, 1, h - 1, bg, bg);
        int grad1 = 0x505000ff;
        int grad2 = 0x5028007F;
        drawGradientRect(x + 1, y + 2, 1, h - 3, grad1, grad2);
        drawGradientRect(x + w - 1, y + 2, 1, h - 3, grad1, grad2);

        drawGradientRect(x + 1, y + 1, w - 1, 1, grad1, grad1);
        drawGradientRect(x + 1, y + h - 1, w - 1, 1, grad2, grad2);
    }

    public static void drawGradientRect(int x, int y, int w, int h, int colour1, int colour2) {
        gui.drawGradientRect(x, y, x + w, y + h, colour1, colour2);
    }

    private static final GuiHook gui = new GuiHook();

    @SuppressWarnings("unused")
    public static class GuiHook extends Gui {

        public void setZLevel(float f) {
            zLevel = f;
        }

        public float getZLevel() {
            return zLevel;
        }

        public void incZLevel(float f) {
            zLevel += f;
        }

        @Override
        public void drawGradientRect(int par1, int par2, int par3, int par4, int par5, int par6) {
            super.drawGradientRect(par1, par2, par3, par4, par5, par6);
        }
    }
    // endregion
}
