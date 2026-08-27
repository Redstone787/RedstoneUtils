package org.main.redstoneutils.client.config;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.main.redstoneutils.client.ui.RedstoneUi;

import java.util.Locale;
import java.util.function.Consumer;

final class NumericConfigScreen extends Screen {

    private final double minimum;
    private final double maximum;
    private final int decimals;
    private final Consumer<Double> setter;
    private final Screen parent;
    private double value;
    private EditBox valueBox;
    private boolean dragging;
    private boolean valid = true;

    NumericConfigScreen(Component title, double minimum, double maximum, int decimals, double value,
                        Consumer<Double> setter, Screen parent) {
        super(title);
        this.minimum = minimum;
        this.maximum = maximum;
        this.decimals = decimals;
        this.value = Mth.clamp(value, minimum, maximum);
        this.setter = setter;
        this.parent = parent;
    }

    @Override
    protected void init() {
        valueBox = addWidget(new EditBox(font, width / 2 - 90, height / 2 - 34, 180, 24, title));
        valueBox.setMaxLength(24);
        valueBox.setValue(format(value));
        valueBox.setResponder(this::parse);
        valueBox.setFocused(true);
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) { }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        int x = width / 2 - 120;
        int y = height / 2 - 78;
        RedstoneUi.drawPanel(graphics, x, y, 240, 156);
        graphics.centeredText(font, title, width / 2, y + 14, RedstoneUi.TEXT_COLOR);
        valueBox.extractWidgetRenderState(graphics, mouseX, mouseY, deltaTicks);
        int sliderX = x + 24;
        int sliderY = y + 84;
        int sliderWidth = 192;
        graphics.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + 6, RedstoneUi.FIELD_COLOR);
        int knob = sliderX + (int) Math.round((value - minimum) / (maximum - minimum) * sliderWidth);
        graphics.fill(knob - 3, sliderY - 4, knob + 3, sliderY + 10, RedstoneUi.BUTTON_ACTIVE_COLOR);
        RedstoneUi.drawButton(graphics, font, Component.translatable("gui.cancel").getString(), x + 24, y + 119, 88, 24,
                RedstoneUi.contains(mouseX, mouseY, x + 24, y + 119, 88, 24), RedstoneUi.ButtonTone.NORMAL);
        RedstoneUi.drawButton(graphics, font, Component.translatable("gui.done").getString(), x + 128, y + 119, 88, 24,
                RedstoneUi.contains(mouseX, mouseY, x + 128, y + 119, 88, 24), valid ? RedstoneUi.ButtonTone.ACTIVE : RedstoneUi.ButtonTone.DISABLED);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (valueBox.mouseClicked(event, doubleClick)) return true;
        int x = width / 2 - 120;
        int y = height / 2 - 78;
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && RedstoneUi.contains(event.x(), event.y(), x + 24, y + 78, 192, 18)) {
            dragging = true;
            setFromMouse(event.x(), x + 24, 192);
            return true;
        }
        if (RedstoneUi.contains(event.x(), event.y(), x + 24, y + 119, 88, 24)) {
            minecraft.gui.setScreen(parent);
            return true;
        }
        if (valid && RedstoneUi.contains(event.x(), event.y(), x + 128, y + 119, 88, 24)) {
            saveAndClose();
            return true;
        }
        return true;
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) { dragging = false; return true; }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) { if (dragging) setFromMouse(event.x(), width / 2 - 96, 192); return true; }
    @Override public boolean charTyped(CharacterEvent event) { return valueBox.charTyped(event); }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_RETURN && valid) { saveAndClose(); return true; }
        if (event.key() == InputConstants.KEY_ESCAPE) { minecraft.gui.setScreen(parent); return true; }
        return valueBox.keyPressed(event);
    }

    @Override public boolean isPauseScreen() { return false; }

    private void parse(String text) {
        try {
            double parsed = Double.parseDouble(text.replace(',', '.'));
            if (!Double.isFinite(parsed)) throw new NumberFormatException("Non-finite number");
            value = Mth.clamp(parsed, minimum, maximum);
            valid = true;
            if (valueBox != null) valueBox.setTextColor(RedstoneUi.TEXT_COLOR);
        } catch (NumberFormatException ignored) {
            valid = false;
            if (valueBox != null) valueBox.setTextColor(RedstoneUi.ERROR_TEXT_COLOR);
        }
    }

    private void setFromMouse(double mouseX, int x, int sliderWidth) {
        double progress = Mth.clamp((mouseX - x) / sliderWidth, 0, 1);
        double raw = minimum + (maximum - minimum) * progress;
        double factor = Math.pow(10, decimals);
        value = Math.round(raw * factor) / factor;
        valid = true;
        valueBox.setTextColor(RedstoneUi.TEXT_COLOR);
        valueBox.setValue(format(value));
    }

    private void saveAndClose() {
        parse(valueBox.getValue());
        if (!valid) return;
        setter.accept(value);
        minecraft.gui.setScreen(parent);
    }

    private String format(double number) {
        return decimals == 0 ? Long.toString(Math.round(number)) : String.format(Locale.ROOT, "%." + decimals + "f", number);
    }
}
