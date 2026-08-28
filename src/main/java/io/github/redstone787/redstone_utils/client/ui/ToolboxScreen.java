/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstone_utils.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import io.github.redstone787.redstone_utils.client.RedstoneUtilsClientActions;
import io.github.redstone787.redstone_utils.client.autowire.AutoWireHandler;
import io.github.redstone787.redstone_utils.client.autowire.WireType;
import io.github.redstone787.redstone_utils.client.calculator.Calculator;
import io.github.redstone787.redstone_utils.client.overlay.OverlayFreeze;

import java.util.List;

/** A command-free entry point to all major RedstoneUtils tools. */
public final class ToolboxScreen extends Screen {

    private static final int WIDTH = 360;
    private static final int HEIGHT = 236;
    private static final int BUTTON_HEIGHT = 24;
    private static final int GAP = 6;

    private final List<Tool> tools = List.of(
            new Tool("toolbox.redstone_utils.autowire", this::nextAutoWire),
            new Tool("toolbox.redstone_utils.overlays", this::toggleOverlays),
            new Tool("toolbox.redstone_utils.clock", () -> openCommand("/clock ")),
            new Tool("toolbox.redstone_utils.signal", () -> openCommand("/signal ")),
            new Tool("toolbox.redstone_utils.calculator", () -> Calculator.openCalculator()),
            new Tool("toolbox.redstone_utils.macros", RedstoneUtilsClientActions::openMacros),
            new Tool("toolbox.redstone_utils.config", RedstoneUtilsClientActions::openConfig),
            new Tool("toolbox.redstone_utils.freeze", OverlayFreeze::toggleAll)
    );

    public ToolboxScreen() {
        super(Component.translatable("screen.redstone_utils.toolbox"));
    }

    public static void open() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.gui.setScreen(new ToolboxScreen()));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        Layout layout = layout(graphics.guiWidth(), graphics.guiHeight());
        int x = layout.x();
        int y = layout.y();
        RedstoneUi.drawPanel(graphics, x, y, layout.width(), layout.height());
        graphics.text(font, title, x + 14, y + 12, RedstoneUi.TEXT_COLOR, false);
        graphics.text(font, Component.translatable("screen.redstone_utils.toolbox.subtitle"), x + 14, y + 30, RedstoneUi.MUTED_TEXT_COLOR, false);

        for (int index = 0; index < tools.size(); index++) {
            int column = index % 2;
            int row = index / 2;
            int buttonX = x + 12 + column * (layout.buttonWidth() + GAP);
            int buttonY = y + 54 + row * (BUTTON_HEIGHT + GAP);
            boolean hovered = RedstoneUi.contains(mouseX, mouseY, buttonX, buttonY, layout.buttonWidth(), BUTTON_HEIGHT);
            RedstoneUi.drawButton(graphics, font, label(tools.get(index)), buttonX, buttonY, layout.buttonWidth(), BUTTON_HEIGHT, hovered, RedstoneUi.ButtonTone.NORMAL);
        }

        String profile = Component.translatable("screen.redstone_utils.toolbox.profile", io.github.redstone787.redstone_utils.client.config.RedstoneUtilsConfig.activeProfile()).getString();
        RedstoneUi.drawFittedText(graphics, font, profile, x + 14, y + layout.height() - 22, layout.width() - 28, RedstoneUi.DETAIL_TEXT_COLOR);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return true;
        Layout layout = layout(width, height);
        int x = layout.x();
        int y = layout.y();
        for (int index = 0; index < tools.size(); index++) {
            int buttonX = x + 12 + index % 2 * (layout.buttonWidth() + GAP);
            int buttonY = y + 54 + index / 2 * (BUTTON_HEIGHT + GAP);
            if (RedstoneUi.contains(event.x(), event.y(), buttonX, buttonY, layout.buttonWidth(), BUTTON_HEIGHT)) {
                AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
                tools.get(index).action().run();
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }

    private String label(Tool tool) {
        if ("toolbox.redstone_utils.autowire".equals(tool.translationKey())) {
            return Component.translatable(tool.translationKey(), AutoWireHandler.getActiveWireType().getDisplayName()).getString();
        }
        if ("toolbox.redstone_utils.freeze".equals(tool.translationKey())) {
            return Component.translatable(tool.translationKey(), Component.translatable(OverlayFreeze.anyFrozen()
                    ? "state.redstone_utils.on" : "state.redstone_utils.off")).getString();
        }
        return Component.translatable(tool.translationKey()).getString();
    }

    private void nextAutoWire() {
        List<WireType> modes = AutoWireHandler.getSelectableWireTypes();
        int current = modes.indexOf(AutoWireHandler.getActiveWireType());
        AutoWireHandler.setActiveWireType(modes.get((current + 1) % modes.size()));
    }

    private void toggleOverlays() {
        RedstoneUtilsClientActions.setAllOverlays(!RedstoneUtilsClientActions.allOverlaysVisible());
    }

    private void openCommand(String command) {
        Minecraft.getInstance().gui.setScreen(new ChatScreen(command, false));
    }

    private static Layout layout(int screenWidth, int screenHeight) {
        int width = Math.min(WIDTH, Math.max(1, screenWidth - 16));
        int height = Math.min(HEIGHT, Math.max(1, screenHeight - 8));
        int buttonWidth = Math.max(1, (width - 24 - GAP) / 2);
        return new Layout((screenWidth - width) / 2, (screenHeight - height) / 2, width, height, buttonWidth);
    }

    private record Layout(int x, int y, int width, int height, int buttonWidth) {
    }

    private record Tool(String translationKey, Runnable action) {
    }
}
