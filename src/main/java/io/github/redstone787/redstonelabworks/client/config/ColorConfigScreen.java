/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.client.config;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import io.github.redstone787.redstonelabworks.client.ui.RedstoneUi;

import java.util.Locale;
import java.util.function.IntConsumer;

final class ColorConfigScreen extends Screen {

    private final IntConsumer setter;
    private final Screen parent;
    private int color;
    private EditBox valueBox;
    private boolean valid = true;

    ColorConfigScreen(Component title, int color, IntConsumer setter, Screen parent) {
        super(title);
        this.color = color;
        this.setter = setter;
        this.parent = parent;
    }

    @Override
    protected void init() {
        valueBox = addWidget(new EditBox(font, width / 2 - 90, height / 2 - 24, 180, 24, title));
        valueBox.setMaxLength(7);
        valueBox.setValue(String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF));
        valueBox.setResponder(this::parse);
        valueBox.setFocused(true);
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) { }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        int x = width / 2 - 120;
        int y = height / 2 - 72;
        RedstoneUi.drawPanel(graphics, x, y, 240, 144);
        graphics.centeredText(font, title, width / 2, y + 13, RedstoneUi.TEXT_COLOR);
        valueBox.extractWidgetRenderState(graphics, mouseX, mouseY, deltaTicks);
        graphics.fill(x + 24, y + 82, x + 216, y + 99, valid ? 0xFF000000 | color & 0xFFFFFF : RedstoneUi.BUTTON_DANGER_COLOR);
        graphics.outline(x + 24, y + 82, 192, 17, RedstoneUi.BUTTON_BORDER_COLOR);
        RedstoneUi.drawButton(graphics, font, Component.translatable("gui.cancel").getString(), x + 24, y + 108, 88, 24,
                RedstoneUi.contains(mouseX, mouseY, x + 24, y + 108, 88, 24), RedstoneUi.ButtonTone.NORMAL);
        RedstoneUi.drawButton(graphics, font, Component.translatable("gui.done").getString(), x + 128, y + 108, 88, 24,
                RedstoneUi.contains(mouseX, mouseY, x + 128, y + 108, 88, 24), valid ? RedstoneUi.ButtonTone.ACTIVE : RedstoneUi.ButtonTone.DISABLED);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (valueBox.mouseClicked(event, doubleClick)) return true;
        int x = width / 2 - 120;
        int y = height / 2 - 72;
        if (RedstoneUi.contains(event.x(), event.y(), x + 24, y + 108, 88, 24)) { minecraft.gui.setScreen(parent); return true; }
        if (valid && RedstoneUi.contains(event.x(), event.y(), x + 128, y + 108, 88, 24)) { save(); return true; }
        return true;
    }

    @Override public boolean charTyped(CharacterEvent event) { return valueBox.charTyped(event); }
    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_RETURN && valid) { save(); return true; }
        if (event.key() == InputConstants.KEY_ESCAPE) { minecraft.gui.setScreen(parent); return true; }
        return valueBox.keyPressed(event);
    }
    @Override public boolean isPauseScreen() { return false; }

    private void parse(String input) {
        String value = input == null ? "" : input.strip();
        if (value.startsWith("#")) value = value.substring(1);
        try {
            if (value.length() != 6) throw new NumberFormatException();
            color = Integer.parseInt(value, 16);
            valid = true;
        } catch (NumberFormatException ignored) {
            valid = false;
        }
    }

    private void save() {
        setter.accept(color);
        minecraft.gui.setScreen(parent);
    }
}
