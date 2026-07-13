package org.main.redstoneutils.client.macro;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.main.redstoneutils.client.ui.RedstoneUi;

import java.util.List;

public final class MacrosScreen extends Screen {

    private static final int PANEL_MIN_WIDTH = 500;
    private static final int PANEL_MAX_WIDTH = 720;
    private static final int PANEL_MIN_HEIGHT = 300;
    private static final int PANEL_MAX_HEIGHT = 460;
    private static final int HEADER_HEIGHT = 64;
    private static final int FOOTER_HEIGHT = 38;
    private static final int ROW_HEIGHT = 58;
    private static final int BUTTON_HEIGHT = 24;
    private static final int NEW_BUTTON_WIDTH = 122;
    private static final int DONE_BUTTON_WIDTH = 92;
    private static final int DELETE_BUTTON_WIDTH = 58;

    private double scrollOffset;
    private boolean draggingScrollbar;

    public MacrosScreen() {
        super(Component.literal("RedstoneUtils Macros"));
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.gui.setScreen(new MacrosScreen()));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        Layout layout = layout(graphics.guiWidth(), graphics.guiHeight());
        scrollOffset = clampScroll(layout, scrollOffset);

        graphics.nextStratum();
        RedstoneUi.drawPanel(graphics, layout.x(), layout.y(), layout.width(), layout.height());
        drawHeader(graphics, layout);

        graphics.enableScissor(layout.contentLeft(), layout.contentY(), layout.contentRight(), layout.contentBottom());
        drawRows(graphics, layout, mouseX, mouseY);
        graphics.disableScissor();

        drawScrollbar(graphics, layout);
        drawFooter(graphics, layout, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Layout layout = layout(width, height);
        double mouseX = event.x();
        double mouseY = event.y();

        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return true;

        if (isScrollbarVisible(layout) && RedstoneUi.contains(mouseX, mouseY, layout.scrollbarX() - 2, layout.contentY(), RedstoneUi.SCROLLBAR_WIDTH + 4, layout.contentHeight())) {
            draggingScrollbar = true;
            scrollToMouse(layout, mouseY);
            playClick();
            return true;
        }

        if (RedstoneUi.contains(mouseX, mouseY, layout.newKeybindX(), layout.footerButtonY(), NEW_BUTTON_WIDTH, BUTTON_HEIGHT)) {
            Minecraft.getInstance().gui.setScreen(MacroEditScreen.createKeybind(this));
            playClick();
            return true;
        }

        if (RedstoneUi.contains(mouseX, mouseY, layout.newCommandX(), layout.footerButtonY(), NEW_BUTTON_WIDTH, BUTTON_HEIGHT)) {
            Minecraft.getInstance().gui.setScreen(MacroEditScreen.createCommandAlias(this));
            playClick();
            return true;
        }

        if (RedstoneUi.contains(mouseX, mouseY, layout.doneX(), layout.footerButtonY(), DONE_BUTTON_WIDTH, BUTTON_HEIGHT)) {
            onClose();
            playClick();
            return true;
        }

        List<Macro> macros = MacroStore.macros();
        int rowIndex = rowIndexAt(layout, mouseX, mouseY, macros.size());
        if (rowIndex >= 0) {
            Macro macro = macros.get(rowIndex);
            int rowY = rowY(layout, rowIndex);
            if (RedstoneUi.contains(mouseX, mouseY, layout.deleteX(), rowY + 17, DELETE_BUTTON_WIDTH, BUTTON_HEIGHT)) {
                MacroStore.delete(macro.id());
                scrollOffset = clampScroll(layout, scrollOffset);
                playClick();
                return true;
            }

            if (doubleClick) {
                Minecraft.getInstance().gui.setScreen(MacroEditScreen.edit(this, macro));
                playClick();
                return true;
            }
        }

        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingScrollbar = false;
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingScrollbar) {
            scrollToMouse(layout(width, height), event.y());
            return true;
        }

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Layout layout = layout(width, height);
        if (contentHeight() <= layout.contentHeight()) return true;

        scrollOffset = clampScroll(layout, scrollOffset - scrollY * ROW_HEIGHT);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (key == InputConstants.KEY_UP) {
            scrollOffset = clampScroll(layout(width, height), scrollOffset - ROW_HEIGHT);
            return true;
        }
        if (key == InputConstants.KEY_DOWN) {
            scrollOffset = clampScroll(layout(width, height), scrollOffset + ROW_HEIGHT);
            return true;
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

    private void drawHeader(GuiGraphicsExtractor graphics, Layout layout) {
        graphics.fill(layout.x() + 1, layout.y() + 1, layout.x() + layout.width() - 1, layout.contentY(), RedstoneUi.PANEL_COLOR);
        graphics.text(font, "RedstoneUtils Macros", layout.x() + RedstoneUi.PANEL_PADDING, layout.y() + 8, RedstoneUi.TEXT_COLOR, false);
        graphics.text(font, "Double-click a row to edit.", layout.x() + RedstoneUi.PANEL_PADDING, layout.y() + 24, RedstoneUi.MUTED_TEXT_COLOR, false);

        int labelY = layout.contentY() - 14;
        graphics.text(font, "Name", layout.contentLeft() + 10, labelY, RedstoneUi.DETAIL_TEXT_COLOR, false);
        graphics.text(font, "Command", layout.commandX(), labelY, RedstoneUi.DETAIL_TEXT_COLOR, false);
        graphics.text(font, "Binding", layout.bindingX(), labelY, RedstoneUi.DETAIL_TEXT_COLOR, false);
    }

    private void drawRows(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        List<Macro> macros = MacroStore.macros();
        if (macros.isEmpty()) {
            int textX = layout.contentLeft() + 12;
            int textY = layout.contentY() + 18;
            graphics.text(font, "No macros yet.", textX, textY, RedstoneUi.TEXT_COLOR, false);
            graphics.text(font, "Create a keybind or command alias below.", textX, textY + 16, RedstoneUi.MUTED_TEXT_COLOR, false);
            return;
        }

        for (int index = 0; index < macros.size(); index++) {
            int rowY = rowY(layout, index);
            if (rowY + ROW_HEIGHT < layout.contentY() || rowY > layout.contentBottom()) continue;

            Macro macro = macros.get(index);
            boolean hovered = RedstoneUi.contains(mouseX, mouseY, layout.contentLeft(), rowY + 2, layout.contentWidth(), ROW_HEIGHT - 5);
            boolean deleteHovered = RedstoneUi.contains(mouseX, mouseY, layout.deleteX(), rowY + 17, DELETE_BUTTON_WIDTH, BUTTON_HEIGHT);
            int rowColor = index % 2 == 0 ? RedstoneUi.ROW_COLOR : RedstoneUi.ROW_ALT_COLOR;

            graphics.fill(layout.contentLeft(), rowY + 2, layout.contentRight(), rowY + ROW_HEIGHT - 3, hovered ? RedstoneUi.ROW_HOVER_COLOR : rowColor);
            graphics.outline(layout.contentLeft(), rowY + 2, layout.contentWidth(), ROW_HEIGHT - 5, RedstoneUi.PANEL_BORDER_COLOR);

            int tagWidth = 62;
            RedstoneUi.drawTag(graphics, font, typeLabel(macro), layout.contentLeft() + 10, rowY + 8, tagWidth);
            RedstoneUi.drawFittedText(graphics, font, macro.name(), layout.contentLeft() + 10 + tagWidth + RedstoneUi.GAP, rowY + 11, layout.nameWidth() - tagWidth - RedstoneUi.GAP - 8, RedstoneUi.TEXT_COLOR);
            RedstoneUi.drawFittedText(graphics, font, MacroCommandText.formatCommand(macro.command()), layout.commandX(), rowY + 11, layout.commandWidth(), RedstoneUi.DETAIL_TEXT_COLOR);
            RedstoneUi.drawFittedText(graphics, font, bindingLabel(macro), layout.bindingX(), rowY + 11, layout.bindingWidth(), RedstoneUi.TEXT_COLOR);
            RedstoneUi.drawWrappedText(graphics, font, detailText(macro), layout.contentLeft() + 10, rowY + 32, layout.detailWidth(), 1, RedstoneUi.MUTED_TEXT_COLOR);

            RedstoneUi.drawButton(graphics, font, "Delete", layout.deleteX(), rowY + 17, DELETE_BUTTON_WIDTH, BUTTON_HEIGHT, deleteHovered, RedstoneUi.ButtonTone.DANGER);
        }
    }

    private void drawFooter(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        graphics.fill(layout.x() + 1, layout.contentBottom(), layout.x() + layout.width() - 1, layout.y() + layout.height() - 1, RedstoneUi.PANEL_COLOR);

        boolean keybindHovered = RedstoneUi.contains(mouseX, mouseY, layout.newKeybindX(), layout.footerButtonY(), NEW_BUTTON_WIDTH, BUTTON_HEIGHT);
        boolean commandHovered = RedstoneUi.contains(mouseX, mouseY, layout.newCommandX(), layout.footerButtonY(), NEW_BUTTON_WIDTH, BUTTON_HEIGHT);
        boolean doneHovered = RedstoneUi.contains(mouseX, mouseY, layout.doneX(), layout.footerButtonY(), DONE_BUTTON_WIDTH, BUTTON_HEIGHT);

        RedstoneUi.drawButton(graphics, font, "New Keybind", layout.newKeybindX(), layout.footerButtonY(), NEW_BUTTON_WIDTH, BUTTON_HEIGHT, keybindHovered, RedstoneUi.ButtonTone.NORMAL);
        RedstoneUi.drawButton(graphics, font, "New Command", layout.newCommandX(), layout.footerButtonY(), NEW_BUTTON_WIDTH, BUTTON_HEIGHT, commandHovered, RedstoneUi.ButtonTone.NORMAL);
        RedstoneUi.drawButton(graphics, font, "Done", layout.doneX(), layout.footerButtonY(), DONE_BUTTON_WIDTH, BUTTON_HEIGHT, doneHovered, RedstoneUi.ButtonTone.NORMAL);
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics, Layout layout) {
        if (!isScrollbarVisible(layout)) return;

        int trackX = layout.scrollbarX();
        int trackY = layout.contentY() + 4;
        int trackHeight = layout.contentHeight() - 8;
        int thumbHeight = scrollbarThumbHeight(layout, trackHeight);
        int thumbY = scrollbarThumbY(layout, trackY, trackHeight, thumbHeight);

        graphics.fill(trackX, trackY, trackX + RedstoneUi.SCROLLBAR_WIDTH, trackY + trackHeight, RedstoneUi.SCROLLBAR_TRACK_COLOR);
        graphics.fill(trackX, thumbY, trackX + RedstoneUi.SCROLLBAR_WIDTH, thumbY + thumbHeight, RedstoneUi.SCROLLBAR_THUMB_COLOR);
    }

    private String bindingLabel(Macro macro) {
        if (macro.isCommandAlias()) return MacroCommandText.formatCommand(macro.alias());
        return MacroKeys.displayName(macro.keyCode());
    }

    private static String detailText(Macro macro) {
        return macro.isCommandAlias()
                ? "Typing " + MacroCommandText.formatCommand(macro.alias()) + " runs " + MacroCommandText.formatCommand(macro.command()) + "."
                : "Pressing " + MacroKeys.displayName(macro.keyCode()) + " runs " + MacroCommandText.formatCommand(macro.command()) + ".";
    }

    private static String typeLabel(Macro macro) {
        return macro.isCommandAlias() ? "Command" : "Keybind";
    }

    private int rowIndexAt(Layout layout, double mouseX, double mouseY, int macroCount) {
        if (!RedstoneUi.contains(mouseX, mouseY, layout.contentLeft(), layout.contentY(), layout.contentWidth(), layout.contentHeight())) {
            return -1;
        }

        int index = (int) ((mouseY - layout.contentY() + scrollOffset) / ROW_HEIGHT);
        if (index < 0 || index >= macroCount) return -1;

        int rowY = rowY(layout, index);
        if (!RedstoneUi.contains(mouseX, mouseY, layout.contentLeft(), rowY + 2, layout.contentWidth(), ROW_HEIGHT - 5)) {
            return -1;
        }

        return index;
    }

    private int rowY(Layout layout, int index) {
        return layout.contentY() + index * ROW_HEIGHT - (int) Math.round(scrollOffset);
    }

    private double clampScroll(Layout layout, double value) {
        return Mth.clamp(value, 0.0D, Math.max(0, contentHeight() - layout.contentHeight()));
    }

    private int contentHeight() {
        return MacroStore.macros().size() * ROW_HEIGHT;
    }

    private boolean isScrollbarVisible(Layout layout) {
        return contentHeight() > layout.contentHeight();
    }

    private int scrollbarThumbHeight(Layout layout, int trackHeight) {
        return Math.max(RedstoneUi.SCROLLBAR_MIN_THUMB_HEIGHT, (int) Math.round(trackHeight * layout.contentHeight() / (double) contentHeight()));
    }

    private int scrollbarThumbY(Layout layout, int trackY, int trackHeight, int thumbHeight) {
        double maxScroll = Math.max(1.0D, contentHeight() - layout.contentHeight());
        double progress = scrollOffset / maxScroll;
        return trackY + (int) Math.round((trackHeight - thumbHeight) * progress);
    }

    private void scrollToMouse(Layout layout, double mouseY) {
        int trackY = layout.contentY() + 4;
        int trackHeight = layout.contentHeight() - 8;
        int thumbHeight = scrollbarThumbHeight(layout, trackHeight);
        double progress = (mouseY - trackY - thumbHeight / 2.0D) / Math.max(1.0D, trackHeight - thumbHeight);
        double maxScroll = Math.max(0.0D, contentHeight() - layout.contentHeight());
        scrollOffset = clampScroll(layout, progress * maxScroll);
    }

    private static Layout layout(int screenWidth, int screenHeight) {
        int width = Mth.clamp(screenWidth - 24, PANEL_MIN_WIDTH, PANEL_MAX_WIDTH);
        int height = Mth.clamp(screenHeight - 24, PANEL_MIN_HEIGHT, PANEL_MAX_HEIGHT);
        int x = (screenWidth - width) / 2;
        int y = (screenHeight - height) / 2;
        int contentY = y + HEADER_HEIGHT;
        int contentHeight = height - HEADER_HEIGHT - FOOTER_HEIGHT;

        return new Layout(x, y, width, height, contentY, contentHeight);
    }

    private static void playClick() {
        AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
    }

    private record Layout(int x, int y, int width, int height, int contentY, int contentHeight) {
        private int contentBottom() {
            return contentY + contentHeight;
        }

        private int contentLeft() {
            return x + RedstoneUi.PANEL_PADDING;
        }

        private int contentRight() {
            return scrollbarX() - RedstoneUi.SCROLLBAR_GAP;
        }

        private int contentWidth() {
            return contentRight() - contentLeft();
        }

        private int scrollbarX() {
            return x + width - RedstoneUi.PANEL_PADDING - RedstoneUi.SCROLLBAR_WIDTH;
        }

        private int nameWidth() {
            return Math.max(134, contentWidth() / 4);
        }

        private int commandX() {
            return contentLeft() + nameWidth() + RedstoneUi.GAP;
        }

        private int commandWidth() {
            return Math.max(120, bindingX() - commandX() - RedstoneUi.GAP);
        }

        private int bindingX() {
            return deleteX() - bindingWidth() - RedstoneUi.GAP;
        }

        private int bindingWidth() {
            return Math.max(96, Math.min(154, contentWidth() / 4));
        }

        private int detailWidth() {
            return deleteX() - contentLeft() - 20;
        }

        private int deleteX() {
            return contentRight() - DELETE_BUTTON_WIDTH - 8;
        }

        private int footerButtonY() {
            return y + height - FOOTER_HEIGHT + 7;
        }

        private int newKeybindX() {
            return x + RedstoneUi.PANEL_PADDING;
        }

        private int newCommandX() {
            return newKeybindX() + NEW_BUTTON_WIDTH + RedstoneUi.GAP;
        }

        private int doneX() {
            return x + width - RedstoneUi.PANEL_PADDING - DONE_BUTTON_WIDTH;
        }
    }
}
