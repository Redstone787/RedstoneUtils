package org.main.redstoneutils.client.calculator;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

final class CalculatorScreen extends Screen {

    private static final int PANEL_MIN_WIDTH = 248;
    private static final int PANEL_MAX_WIDTH = 306;
    private static final int PANEL_HEIGHT = 292;
    private static final int PADDING = 12;
    private static final int GAP = 6;
    private static final int COLUMN_COUNT = 5;
    private static final int BUTTON_ROW_COUNT = 5;
    private static final int DISPLAY_HEIGHT = 62;
    private static final int BUTTON_HEIGHT = 29;
    private static final int TITLE_HEIGHT = 13;

    private static final int SHADOW_COLOR = 0xD116171A;
    private static final int PANEL_COLOR = 0xE62B2D31;
    private static final int PANEL_BORDER_COLOR = 0xF05B6068;
    private static final int PANEL_HIGHLIGHT_COLOR = 0xFF7B8088;
    private static final int DISPLAY_COLOR = 0xE6212226;
    private static final int DISPLAY_BORDER_COLOR = 0xF05B6068;
    private static final int BUTTON_COLOR = 0xE633363C;
    private static final int BUTTON_HOVER_COLOR = 0xE66A707A;
    private static final int BUTTON_OPERATOR_COLOR = 0xE642454C;
    private static final int BUTTON_ACTION_COLOR = 0xE64B5058;
    private static final int BUTTON_EQUALS_COLOR = 0xE68A909A;
    private static final int BUTTON_DANGER_COLOR = 0xE65A3535;
    private static final int BUTTON_BORDER_COLOR = 0xF05B6068;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int MUTED_TEXT_COLOR = 0xFFB8BEC8;
    private static final int ERROR_TEXT_COLOR = 0xFFFFB4A9;

    private static final List<CalculatorButton> BUTTONS = List.of(
            button("C", "", ButtonAction.CLEAR, 0, 0),
            button("DEL", "", ButtonAction.BACKSPACE, 0, 1),
            button("(", "(", ButtonAction.APPEND, 0, 2),
            button(")", ")", ButtonAction.APPEND, 0, 3),
            button("/", "/", ButtonAction.APPEND, 0, 4),

            button("7", "7", ButtonAction.APPEND, 1, 0),
            button("8", "8", ButtonAction.APPEND, 1, 1),
            button("9", "9", ButtonAction.APPEND, 1, 2),
            button("%", "%", ButtonAction.APPEND, 1, 3),
            button("*", "*", ButtonAction.APPEND, 1, 4),

            button("4", "4", ButtonAction.APPEND, 2, 0),
            button("5", "5", ButtonAction.APPEND, 2, 1),
            button("6", "6", ButtonAction.APPEND, 2, 2),
            button("^", "^", ButtonAction.APPEND, 2, 3),
            button("-", "-", ButtonAction.APPEND, 2, 4),

            button("1", "1", ButtonAction.APPEND, 3, 0),
            button("2", "2", ButtonAction.APPEND, 3, 1),
            button("3", "3", ButtonAction.APPEND, 3, 2),
            button("sqrt", "sqrt(", ButtonAction.APPEND, 3, 3),
            button("+", "+", ButtonAction.APPEND, 3, 4),

            button("+/-", "", ButtonAction.TOGGLE_SIGN, 4, 0),
            button("0", "0", ButtonAction.APPEND, 4, 1),
            button(".", ".", ButtonAction.APPEND, 4, 2),
            button("ans", "ans", ButtonAction.APPEND, 4, 3),
            button("=", "", ButtonAction.EVALUATE, 4, 4)
    );

    private String expression = "";
    private String preview = "0";
    private String error = "";
    private double lastAnswer = 0.0D;
    private boolean justEvaluated;

    CalculatorScreen() {
        super(Component.translatable("screen.redstoneutils.calculator"));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        CalculatorLayout layout = layout(graphics.guiWidth(), graphics.guiHeight());

        graphics.nextStratum();
        drawPanel(graphics, layout);
        drawDisplay(graphics, layout);
        drawButtons(graphics, layout, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            CalculatorButton button = buttonAt(event.x(), event.y());
            if (button != null) {
                press(button);
                AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
                return true;
            }
        }

        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();

        if (key == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }

        if (key == InputConstants.KEY_BACKSPACE) {
            backspace();
            return true;
        }

        if (key == InputConstants.KEY_DELETE) {
            clear();
            return true;
        }

        if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
            evaluate();
            return true;
        }

        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        String typed = event.codepointAsString();
        if (typed.isEmpty()) return true;

        char value = typed.charAt(0);
        if (Character.isDigit(value)) {
            append(Character.toString(value));
            return true;
        }

        switch (value) {
            case '+', '-', '*', '/', '%', '^', '(', ')' -> append(Character.toString(value));
            case '.', ',' -> append(".");
            case '=', '\n', '\r' -> evaluate();
            case 'c', 'C' -> clear();
            case 'r', 'R' -> append("sqrt(");
            case 'a', 'A' -> append("ans");
            case 'x', 'X' -> append("*");
            default -> {
            }
        }

        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    private void drawPanel(GuiGraphicsExtractor graphics, CalculatorLayout layout) {
        graphics.fill(layout.x() + 3, layout.y() + 3, layout.x() + layout.width() + 3, layout.y() + layout.height() + 3, SHADOW_COLOR);
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + layout.height(), PANEL_COLOR);
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + 1, PANEL_HIGHLIGHT_COLOR);
        graphics.outline(layout.x(), layout.y(), layout.width(), layout.height(), PANEL_BORDER_COLOR);
        graphics.text(font, Component.translatable("screen.redstoneutils.calculator"), layout.x() + layout.padding(), layout.y() + 7, TEXT_COLOR, false);
    }

    private void drawDisplay(GuiGraphicsExtractor graphics, CalculatorLayout layout) {
        graphics.fill(
                layout.displayX(),
                layout.displayY(),
                layout.displayX() + layout.displayWidth(),
                layout.displayY() + layout.displayHeight(),
                DISPLAY_COLOR
        );
        graphics.outline(layout.displayX(), layout.displayY(), layout.displayWidth(), layout.displayHeight(), DISPLAY_BORDER_COLOR);

        int textRight = layout.displayX() + layout.displayWidth() - 8;
        int maxTextWidth = layout.displayWidth() - 16;
        String expressionText = expression.isBlank() ? "0" : expression;
        drawRightAligned(graphics, expressionText, textRight, layout.displayY() + 10, maxTextWidth, MUTED_TEXT_COLOR);

        String resultText = error.isBlank() ? preview : error;
        int resultColor = error.isBlank() ? TEXT_COLOR : ERROR_TEXT_COLOR;
        drawRightAligned(graphics, resultText, textRight, layout.displayY() + 37, maxTextWidth, resultColor);
    }

    private void drawButtons(GuiGraphicsExtractor graphics, CalculatorLayout layout, int mouseX, int mouseY) {
        for (CalculatorButton button : BUTTONS) {
            int x = buttonX(layout, button);
            int y = buttonY(layout, button);
            boolean hovered = contains(mouseX, mouseY, x, y, layout.buttonWidth(), layout.buttonHeight());
            int color = hovered ? BUTTON_HOVER_COLOR : buttonColor(button);

            graphics.fill(x, y, x + layout.buttonWidth(), y + layout.buttonHeight(), color);
            graphics.outline(x, y, layout.buttonWidth(), layout.buttonHeight(), BUTTON_BORDER_COLOR);
            graphics.centeredText(font, button.label(), x + layout.buttonWidth() / 2, y + (layout.buttonHeight() - font.lineHeight) / 2, TEXT_COLOR);
        }
    }

    private void drawRightAligned(GuiGraphicsExtractor graphics, String text, int right, int y, int maxWidth, int color) {
        String visibleText = fitFromEnd(font, text, maxWidth);
        graphics.text(font, visibleText, right - font.width(visibleText), y, color, false);
    }

    private CalculatorButton buttonAt(double mouseX, double mouseY) {
        CalculatorLayout layout = layout(width, height);

        for (CalculatorButton button : BUTTONS) {
            int x = buttonX(layout, button);
            int y = buttonY(layout, button);
            if (contains(mouseX, mouseY, x, y, layout.buttonWidth(), layout.buttonHeight())) return button;
        }

        return null;
    }

    private void press(CalculatorButton button) {
        switch (button.action()) {
            case APPEND -> append(button.value());
            case CLEAR -> clear();
            case BACKSPACE -> backspace();
            case EVALUATE -> evaluate();
            case TOGGLE_SIGN -> toggleSign();
        }
    }

    private void append(String value) {
        if (value == null || value.isEmpty()) return;

        if (justEvaluated) {
            expression = isBinaryOperator(value) ? expression + value : value;
            justEvaluated = false;
        } else {
            expression += value;
        }

        error = "";
        updatePreview();
    }

    private void clear() {
        expression = "";
        preview = "0";
        error = "";
        justEvaluated = false;
    }

    private void backspace() {
        if (justEvaluated) {
            clear();
            return;
        }

        if (!expression.isEmpty()) {
            expression = expression.substring(0, expression.length() - 1);
        }

        error = "";
        updatePreview();
    }

    private void toggleSign() {
        if (expression.isBlank()) {
            expression = "-";
        } else if (expression.startsWith("-(") && expression.endsWith(")")) {
            expression = expression.substring(2, expression.length() - 1);
        } else {
            expression = "-(" + expression + ")";
        }

        justEvaluated = false;
        error = "";
        updatePreview();
    }

    private void evaluate() {
        if (expression.isBlank()) return;

        try {
            double value = CalculatorExpression.evaluate(expression, lastAnswer);
            preview = CalculatorExpression.formatNumber(value);
            expression = preview;
            lastAnswer = value;
            error = "";
            justEvaluated = true;
        } catch (IllegalArgumentException exception) {
            error = Component.translatable("calculator.redstoneutils.error.invalid").getString();
            justEvaluated = false;
        }
    }

    private void updatePreview() {
        if (expression.isBlank()) {
            preview = "0";
            return;
        }

        try {
            preview = CalculatorExpression.formatNumber(CalculatorExpression.evaluate(expression, lastAnswer));
        } catch (IllegalArgumentException exception) {
            preview = "";
        }
    }

    private static CalculatorButton button(String label, String value, ButtonAction action, int row, int column) {
        return new CalculatorButton(label, value, action, row, column);
    }

    private static CalculatorLayout layout(int screenWidth, int screenHeight) {
        int panelWidth = Mth.clamp(screenWidth - 24, PANEL_MIN_WIDTH, PANEL_MAX_WIDTH);
        int panelHeight = Math.min(PANEL_HEIGHT, Math.max(252, screenHeight - 24));
        int buttonHeight = Math.max(24, Math.min(BUTTON_HEIGHT, (panelHeight - PADDING * 2 - TITLE_HEIGHT - GAP - DISPLAY_HEIGHT - GAP - (BUTTON_ROW_COUNT - 1) * GAP) / BUTTON_ROW_COUNT));
        int buttonWidth = (panelWidth - PADDING * 2 - (COLUMN_COUNT - 1) * GAP) / COLUMN_COUNT;
        int x = (screenWidth - panelWidth) / 2;
        int y = (screenHeight - panelHeight) / 2;
        int displayX = x + PADDING;
        int displayY = y + PADDING + TITLE_HEIGHT + GAP;
        int displayWidth = panelWidth - PADDING * 2;
        int buttonsY = displayY + DISPLAY_HEIGHT + GAP;

        return new CalculatorLayout(
                x,
                y,
                panelWidth,
                panelHeight,
                PADDING,
                GAP,
                displayX,
                displayY,
                displayWidth,
                DISPLAY_HEIGHT,
                buttonsY,
                buttonWidth,
                buttonHeight
        );
    }

    private static int buttonX(CalculatorLayout layout, CalculatorButton button) {
        return layout.x() + layout.padding() + button.column() * (layout.buttonWidth() + layout.gap());
    }

    private static int buttonY(CalculatorLayout layout, CalculatorButton button) {
        return layout.buttonsY() + button.row() * (layout.buttonHeight() + layout.gap());
    }

    private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static int buttonColor(CalculatorButton button) {
        if (button.action() == ButtonAction.CLEAR) return BUTTON_DANGER_COLOR;
        if (button.action() == ButtonAction.BACKSPACE || button.action() == ButtonAction.TOGGLE_SIGN) return BUTTON_ACTION_COLOR;
        if (button.action() == ButtonAction.EVALUATE) return BUTTON_EQUALS_COLOR;
        if (isBinaryOperator(button.value()) || "sqrt(".equals(button.value())) return BUTTON_OPERATOR_COLOR;
        return BUTTON_COLOR;
    }

    private static boolean isBinaryOperator(String value) {
        return "+".equals(value)
                || "-".equals(value)
                || "*".equals(value)
                || "/".equals(value)
                || "%".equals(value)
                || "^".equals(value);
    }

    private static String fitFromEnd(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;

        String prefix = "...";
        for (int index = 0; index < text.length(); index++) {
            String candidate = prefix + text.substring(index);
            if (font.width(candidate) <= maxWidth) return candidate;
        }

        return "";
    }

    private enum ButtonAction {
        APPEND,
        CLEAR,
        BACKSPACE,
        EVALUATE,
        TOGGLE_SIGN
    }

    private record CalculatorButton(String label, String value, ButtonAction action, int row, int column) {
    }

    private record CalculatorLayout(int x, int y, int width, int height, int padding, int gap, int displayX, int displayY,
                                    int displayWidth, int displayHeight, int buttonsY, int buttonWidth, int buttonHeight) {
    }
}
