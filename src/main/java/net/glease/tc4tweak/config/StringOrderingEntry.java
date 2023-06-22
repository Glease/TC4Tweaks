package net.glease.tc4tweak.config;

import com.google.common.collect.ImmutableList;
import cpw.mods.fml.client.config.*;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;

import java.lang.reflect.Field;
import java.util.List;

import static cpw.mods.fml.client.config.GuiUtils.INVALID;
import static cpw.mods.fml.client.config.GuiUtils.VALID;
import static net.glease.tc4tweak.CommonUtils.reflectGet;

public class StringOrderingEntry extends GuiEditArrayEntries.StringEntry {
    private static final String[] upLabels = {"↑", "↑5", "↟"};
    private static final String[] downLabels = {"↓", "↓5", "↡"};
    private static final Field field_GuiEditArray_enabled = ReflectionHelper.findField(GuiEditArray.class, "enabled");
    private final GuiButtonExt btnMoveUp, btnMoveDown;
    private final HoverChecker moveUpHoverChecker, moveDownHoverChecker;
    private final List<List<String>> moveUpToolTip, moveDownToolTip;

    public StringOrderingEntry(GuiEditArray owningScreen, GuiEditArrayEntries owningEntryList, IConfigElement configElement, Object value) {
        super(owningScreen, owningEntryList, configElement, value);
        boolean enabled = reflectGet(field_GuiEditArray_enabled, owningScreen);
        btnMoveUp = new GuiButtonExt(0, 0, 0, 18, 18, "↑");
        btnMoveUp.packedFGColour = GuiUtils.getColorCode('7', true);
        btnMoveUp.enabled = enabled;
        btnMoveDown = new GuiButtonExt(0, 0, 0, 18, 18, "↓");
        btnMoveDown.packedFGColour = GuiUtils.getColorCode('7', true);
        btnMoveDown.enabled = enabled;
        moveUpHoverChecker = new HoverChecker(btnMoveUp, 800);
        moveDownHoverChecker = new HoverChecker(btnMoveDown, 800);
        moveUpToolTip = ImmutableList.of(
                ImmutableList.of(I18n.format("tc4tweaks.configgui.tooltip.moveUp")),
                ImmutableList.of(I18n.format("tc4tweaks.configgui.tooltip.moveUp.shift")),
                ImmutableList.of(I18n.format("tc4tweaks.configgui.tooltip.moveUp.ctrl"))
        );
        moveDownToolTip = ImmutableList.of(
                ImmutableList.of(I18n.format("tc4tweaks.configgui.tooltip.moveDown")),
                ImmutableList.of(I18n.format("tc4tweaks.configgui.tooltip.moveDown.shift")),
                ImmutableList.of(I18n.format("tc4tweaks.configgui.tooltip.moveDown.ctrl"))
        );
        textFieldValue.width -= 44;
    }

    private static int getKeyboardState(int normal, int shift, int ctrl) {
        return GuiScreen.isCtrlKeyDown() ? ctrl : GuiScreen.isShiftKeyDown() ? shift : normal;
    }

    @Override
    public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, Tessellator tessellator, int mouseX, int mouseY, boolean isSelected) {
        if (getValue() != null && isValidated)
            owningEntryList.mc.fontRenderer.drawString(
                    isValidValue ? EnumChatFormatting.GREEN + VALID : EnumChatFormatting.RED + INVALID,
                    listWidth / 4 - owningEntryList.mc.fontRenderer.getStringWidth(VALID) - 2,
                    y + slotHeight / 2 - owningEntryList.mc.fontRenderer.FONT_HEIGHT / 2,
                    16777215);

        int labelIndex = getKeyboardState(0, 1, 2);
        int half = listWidth / 2;
        if (owningEntryList.canAddMoreEntries) {
            btnAddNewEntryAbove.visible = true;
            btnAddNewEntryAbove.xPosition = half + ((half / 2) - 44);
            btnAddNewEntryAbove.yPosition = y;
            btnAddNewEntryAbove.drawButton(owningEntryList.mc, mouseX, mouseY);
        } else
            btnAddNewEntryAbove.visible = false;
        if (!configElement.isListLengthFixed() && slotIndex != owningEntryList.listEntries.size() - 1) {
            btnRemoveEntry.visible = true;
            btnRemoveEntry.xPosition = half + ((half / 2) - 22);
            btnRemoveEntry.yPosition = y;
            btnRemoveEntry.drawButton(owningEntryList.mc, mouseX, mouseY);
        } else
            btnRemoveEntry.visible = false;
        if (configElement.isListLengthFixed() || slotIndex != owningEntryList.listEntries.size() - 1) {
            textFieldValue.setVisible(true);
            textFieldValue.yPosition = y + 1;
            textFieldValue.drawTextBox();
        } else
            textFieldValue.setVisible(false);
        if (slotIndex > 0) {
            btnMoveUp.visible = true;
            btnMoveUp.xPosition = half + half / 2 - 88;
            btnMoveUp.yPosition = y;
            btnMoveUp.displayString = upLabels[labelIndex];
            btnMoveUp.drawButton(owningEntryList.mc, mouseX, mouseY);
        } else
            btnMoveUp.visible = false;
        if (slotIndex < owningEntryList.listEntries.size() - 2) {
            btnMoveDown.visible = true;
            btnMoveDown.xPosition = half + half / 2 - 66;
            btnMoveDown.yPosition = y;
            btnMoveDown.displayString = downLabels[labelIndex];
            btnMoveDown.drawButton(owningEntryList.mc, mouseX, mouseY);
        } else
            btnMoveDown.visible = false;
    }

    @Override
    public void drawToolTip(int mouseX, int mouseY) {
        super.drawToolTip(mouseX, mouseY);
        int labelIndex = getKeyboardState(0, 1, 2);
        boolean canHover = mouseY < owningEntryList.bottom && mouseY > owningEntryList.top;
        if (btnMoveUp.visible && moveUpHoverChecker.checkHover(mouseX, mouseY, canHover))
            owningScreen.drawToolTip(moveUpToolTip.get(labelIndex), mouseX, mouseY);
        if (btnMoveDown.visible && moveDownHoverChecker.checkHover(mouseX, mouseY, canHover))
            owningScreen.drawToolTip(moveDownToolTip.get(labelIndex), mouseX, mouseY);
    }

    @Override
    public boolean mousePressed(int index, int x, int y, int mouseEvent, int relativeX, int relativeY) {
        int delta = getKeyboardState(1, 5, owningEntryList.listEntries.size());
        if (btnAddNewEntryAbove.mousePressed(owningEntryList.mc, x, y)) {
            btnAddNewEntryAbove.func_146113_a(owningEntryList.mc.getSoundHandler());
            // forge bug: you cannot just use addEntry as that one has no support for custom IArrayEntry
            owningEntryList.listEntries.add(index, new StringOrderingEntry(owningScreen, owningEntryList, owningEntryList.configElement, ""));
            owningEntryList.canAddMoreEntries = !owningEntryList.configElement.isListLengthFixed()
                    && (owningEntryList.configElement.getMaxListLength() == -1 || owningEntryList.listEntries.size() - 1 < owningEntryList.configElement.getMaxListLength());
            owningEntryList.recalculateState();
            return true;
        } else if (btnRemoveEntry.mousePressed(owningEntryList.mc, x, y)) {
            btnRemoveEntry.func_146113_a(owningEntryList.mc.getSoundHandler());
            owningEntryList.removeEntry(index);
            owningEntryList.recalculateState();
            return true;
        } else if (btnMoveUp.mousePressed(owningScreen.mc, x, y)) {
            btnMoveUp.func_146113_a(owningScreen.mc.getSoundHandler());
            GuiEditArrayEntries.IArrayEntry e = owningEntryList.listEntries.remove(index);
            owningEntryList.listEntries.add(Math.max(index - delta, 0), e);
            owningEntryList.recalculateState();
            return true;
        } else if (btnMoveDown.mousePressed(owningScreen.mc, x, y)) {
            btnMoveDown.func_146113_a(owningScreen.mc.getSoundHandler());
            GuiEditArrayEntries.IArrayEntry e = owningEntryList.listEntries.remove(index);
            owningEntryList.listEntries.add(Math.min(index + delta, owningEntryList.listEntries.size()), e);
            owningEntryList.recalculateState();
            return true;
        }
        return false;
    }
}
