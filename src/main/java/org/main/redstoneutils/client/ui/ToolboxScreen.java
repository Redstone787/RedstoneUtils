package org.main.redstoneutils.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.main.redstoneutils.client.RedstoneUtilsClientActions;
import org.main.redstoneutils.client.autowire.AutoWireHandler;
import org.main.redstoneutils.client.autowire.WireType;
import org.main.redstoneutils.client.calculator.Calculator;
import org.main.redstoneutils.client.overlay.OverlayFreeze;

import java.util.List;

/** A command-free entry point to all major RedstoneUtils tools. */
public final class ToolboxScreen extends Screen {

    private static final int WIDTH = 360;
    private static final int HEIGHT = 250;
    private static final int BUTTON_WIDTH = 158;
    private static final int BUTTON_HEIGHT = 28;
    private static final int GAP = 10;

    private final List<Tool> tools = List.of(
            new Tool("toolbox.redstoneutils.autowire", this::nextAutoWire),
            new Tool("toolbox.redstoneutils.overlays", this::toggleOverlays),
            new Tool("toolbox.redstoneutils.clock", () -> openCommand("/clock ")),
            new Tool("toolbox.redstoneutils.signal", () -> openCommand("/signal ")),
            new Tool("toolbox.redstoneutils.calculator", () -> Calculator.openCalculator()),
            new Tool("toolbox.redstoneutils.macros", RedstoneUtilsClientActions::openMacros),
            new Tool("toolbox.redstoneutils.config", RedstoneUtilsClientActions::openConfig),
            new Tool("toolbox.redstoneutils.freeze", OverlayFreeze::toggleAll)
    );

    public ToolboxScreen() {
        super(Component.translatable("screen.redstoneutils.toolbox"));
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
        int x = (graphics.guiWidth() - WIDTH) / 2;
        int y = (graphics.guiHeight() - HEIGHT) / 2;
        RedstoneUi.drawPanel(graphics, x, y, WIDTH, HEIGHT);
        graphics.text(font, title, x + 14, y + 12, RedstoneUi.TEXT_COLOR, false);
        graphics.text(font, Component.translatable("screen.redstoneutils.toolbox.subtitle"), x + 14, y + 30, RedstoneUi.MUTED_TEXT_COLOR, false);

        for (int index = 0; index < tools.size(); index++) {
            int column = index % 2;
            int row = index / 2;
            int buttonX = x + 12 + column * (BUTTON_WIDTH + GAP);
            int buttonY = y + 54 + row * (BUTTON_HEIGHT + GAP);
            boolean hovered = RedstoneUi.contains(mouseX, mouseY, buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT);
            RedstoneUi.drawButton(graphics, font, label(tools.get(index)), buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, hovered, RedstoneUi.ButtonTone.NORMAL);
        }

        String profile = Component.translatable("screen.redstoneutils.toolbox.profile", org.main.redstoneutils.client.config.RedstoneUtilsConfig.activeProfile()).getString();
        RedstoneUi.drawFittedText(graphics, font, profile, x + 14, y + HEIGHT - 22, WIDTH - 28, RedstoneUi.DETAIL_TEXT_COLOR);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return true;
        int x = (width - WIDTH) / 2;
        int y = (height - HEIGHT) / 2;
        for (int index = 0; index < tools.size(); index++) {
            int buttonX = x + 12 + index % 2 * (BUTTON_WIDTH + GAP);
            int buttonY = y + 54 + index / 2 * (BUTTON_HEIGHT + GAP);
            if (RedstoneUi.contains(event.x(), event.y(), buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
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
        if ("toolbox.redstoneutils.autowire".equals(tool.translationKey())) {
            return Component.translatable(tool.translationKey(), AutoWireHandler.getActiveWireType().getDisplayName()).getString();
        }
        if ("toolbox.redstoneutils.freeze".equals(tool.translationKey())) {
            return Component.translatable(tool.translationKey(), Component.translatable(OverlayFreeze.anyFrozen()
                    ? "state.redstoneutils.on" : "state.redstoneutils.off")).getString();
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

    private record Tool(String translationKey, Runnable action) {
    }
}
