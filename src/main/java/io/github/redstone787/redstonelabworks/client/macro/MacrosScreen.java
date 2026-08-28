/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.client.macro;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import io.github.redstone787.redstonelabworks.client.ui.RedstoneMessages;
import io.github.redstone787.redstonelabworks.client.ui.RedstoneUi;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class MacrosScreen extends Screen {

    private static final int PANEL_WIDTH = 760;
    private static final int PANEL_HEIGHT = 470;
    private static final int HEADER = 80;
    private static final int COMPACT_HEADER = 106;
    private static final int FOOTER = 46;
    private static final int ROW_HEIGHT = 72;
    private static final int COMPACT_ROW_HEIGHT = 96;
    private static final int SMALL_BUTTON = 54;
    private static final int BUTTON_HEIGHT = 22;
    private static final int ROW_ACTION_INSET = 8;
    private static final int ROW_TEXT_INSET = 9;
    private static final int ACTION_GAP = 6;

    private EditBox searchBox;
    private SortOrder sortOrder = SortOrder.CATEGORY;
    private double scroll;

    public MacrosScreen() {
        super(Component.translatable("screen.redstonelabworks.macros"));
    }

    public static void open() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.gui.setScreen(new MacrosScreen()));
    }

    @Override
    protected void init() {
        super.init();
        Layout layout = layout();
        searchBox = addWidget(new EditBox(font, layout.searchX(), layout.searchY(), layout.searchWidth(), 22,
                Component.translatable("macros.redstonelabworks.search")));
        searchBox.setHint(Component.translatable("macros.redstonelabworks.search"));
        searchBox.setMaxLength(80);
        searchBox.setResponder(ignored -> scroll = 0);
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) { }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        Layout layout = layout();
        List<Macro> macros = visibleMacros();
        scroll = Mth.clamp(scroll, 0, maxScroll(layout, macros.size()));
        RedstoneUi.drawPanel(graphics, layout.x, layout.y, layout.width, layout.height);
        graphics.text(font, title, layout.x + 14, layout.y + 10, RedstoneUi.TEXT_COLOR, false);
        graphics.text(font, Component.translatable("screen.redstonelabworks.macros.profile", MacroStore.activeProfile()), layout.x + 14, layout.y + 26, RedstoneUi.MUTED_TEXT_COLOR, false);
        searchBox.setX(layout.searchX());
        searchBox.setY(layout.searchY());
        searchBox.setSize(layout.searchWidth(), 22);
        searchBox.extractWidgetRenderState(graphics, mouseX, mouseY, deltaTicks);
        drawButton(graphics, Component.translatable("macros.redstonelabworks.sort", sortOrder.label()).getString(), layout.sortX(), layout.controlsY(), layout.sortWidth(), 22,
                RedstoneUi.contains(mouseX, mouseY, layout.sortX(), layout.controlsY(), layout.sortWidth(), 22), RedstoneUi.ButtonTone.NORMAL);
        drawButton(graphics, Component.translatable("macros.redstonelabworks.import").getString(), layout.importX(), layout.controlsY(), 74, 22,
                RedstoneUi.contains(mouseX, mouseY, layout.importX(), layout.controlsY(), 74, 22), RedstoneUi.ButtonTone.NORMAL);
        drawButton(graphics, Component.translatable("macros.redstonelabworks.export").getString(), layout.exportX(), layout.controlsY(), 78, 22,
                RedstoneUi.contains(mouseX, mouseY, layout.exportX(), layout.controlsY(), 78, 22), RedstoneUi.ButtonTone.NORMAL);

        graphics.enableScissor(layout.left(), layout.top(), layout.right(), layout.bottom());
        if (macros.isEmpty()) {
            graphics.text(font, Component.translatable("macros.redstonelabworks.empty"), layout.left() + 10, layout.top() + 16, RedstoneUi.MUTED_TEXT_COLOR, false);
        }
        for (int index = 0; index < macros.size(); index++) drawRow(graphics, layout, macros.get(index), index, mouseX, mouseY);
        graphics.disableScissor();
        drawScrollbar(graphics, layout, macros.size());
        drawFooter(graphics, layout, mouseX, mouseY);
    }

    private void drawRow(GuiGraphicsExtractor graphics, Layout layout, Macro macro, int index, int mouseX, int mouseY) {
        int y = layout.top() + index * layout.rowHeight() - (int) scroll;
        if (y + layout.rowHeight() < layout.top() || y > layout.bottom()) return;
        graphics.fill(layout.left(), y + 3, layout.right(), y + layout.rowHeight() - 5, index % 2 == 0 ? RedstoneUi.ROW_COLOR : RedstoneUi.ROW_ALT_COLOR);
        graphics.outline(layout.left(), y + 3, layout.contentWidth(), layout.rowHeight() - 8, RedstoneUi.PANEL_BORDER_COLOR);
        int textX = layout.left() + ROW_TEXT_INSET;
        int actionsX = layout.actionsX();
        int nameWidth = layout.compact() ? 160 : 170;
        RedstoneUi.drawFittedText(graphics, font, macro.name(), textX, y + 11, nameWidth, macro.enabled() ? RedstoneUi.TEXT_COLOR : RedstoneUi.MUTED_TEXT_COLOR);
        RedstoneUi.drawTag(graphics, font, macro.category(), textX, y + 34, 100);
        int detailX = layout.compact() ? textX + 112 : textX + 180;
        int detailWidth = layout.compact() ? layout.right() - detailX - 9 : actionsX - detailX - 10;
        RedstoneUi.drawFittedText(graphics, font, MacroCommandText.formatCommand(macro.command()), detailX, y + 11, detailWidth, RedstoneUi.DETAIL_TEXT_COLOR);
        String binding = macro.isCommandAlias()
                ? MacroCommandText.formatCommand(macro.alias())
                : MacroKeys.displayName(macro.keyCode(), macro.mouseButton(), macro.modifiers()) + " · " + macro.trigger();
        RedstoneUi.drawFittedText(graphics, font, binding, textX + 112, y + 38,
                (layout.compact() ? layout.right() : actionsX) - textX - 122, RedstoneUi.MUTED_TEXT_COLOR);

        String[] labels = {
                Component.translatable(macro.enabled() ? "macros.redstonelabworks.disable" : "macros.redstonelabworks.enable").getString(),
                Component.translatable("macros.redstonelabworks.edit").getString(),
                Component.translatable("macros.redstonelabworks.copy").getString(),
                Component.translatable("macros.redstonelabworks.delete").getString()
        };
        for (int button = 0; button < labels.length; button++) {
            int x = layout.actionX(button);
            int buttonY = layout.actionY(y);
            int buttonWidth = layout.actionWidth();
            boolean hovered = RedstoneUi.contains(mouseX, mouseY, x, buttonY, buttonWidth, BUTTON_HEIGHT);
            drawButton(graphics, labels[button], x, buttonY, buttonWidth, BUTTON_HEIGHT, hovered,
                    button == 3 ? RedstoneUi.ButtonTone.DANGER : RedstoneUi.ButtonTone.NORMAL);
        }
    }

    private void drawFooter(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        int y = layout.y + layout.height - 32;
        footerButton(graphics, "macros.redstonelabworks.new_keybind", layout.footerKeyX(), y, layout.footerKeyWidth(), mouseX, mouseY);
        footerButton(graphics, "macros.redstonelabworks.new_command", layout.footerCommandX(), y, layout.footerCommandWidth(), mouseX, mouseY);
        footerButton(graphics, "gui.done", layout.footerDoneX(), y, layout.footerDoneWidth(), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Layout layout = layout();
        if (super.mouseClicked(event, doubleClick)) return true;
        clearFocus();
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return true;
        if (hit(event, layout.sortX(), layout.controlsY(), layout.sortWidth(), 22)) {
            sortOrder = SortOrder.values()[(sortOrder.ordinal() + 1) % SortOrder.values().length]; click(); return true;
        }
        if (hit(event, layout.importX(), layout.controlsY(), 74, 22)) { importMacros(); click(); return true; }
        if (hit(event, layout.exportX(), layout.controlsY(), 78, 22)) { exportMacros(); click(); return true; }
        int footerY = layout.y + layout.height - 32;
        if (hit(event, layout.footerKeyX(), footerY, layout.footerKeyWidth(), 22)) { minecraft.gui.setScreen(MacroEditScreen.createKeybind(this)); click(); return true; }
        if (hit(event, layout.footerCommandX(), footerY, layout.footerCommandWidth(), 22)) { minecraft.gui.setScreen(MacroEditScreen.createCommandAlias(this)); click(); return true; }
        if (hit(event, layout.footerDoneX(), footerY, layout.footerDoneWidth(), 22)) { onClose(); click(); return true; }

        List<Macro> macros = visibleMacros();
        if (!hit(event, layout.left(), layout.top(), layout.contentWidth(), layout.contentHeight())) return true;
        int index = (int) ((event.y() - layout.top() + scroll) / layout.rowHeight());
        if (index < 0 || index >= macros.size()) return true;
        Macro macro = macros.get(index);
        int y = layout.top() + index * layout.rowHeight() - (int) scroll;
        for (int button = 0; button < 4; button++) {
            int x = layout.actionX(button);
            if (!hit(event, x, layout.actionY(y), layout.actionWidth(), BUTTON_HEIGHT)) continue;
            switch (button) {
                case 0 -> MacroStore.setEnabled(macro.id(), !macro.enabled());
                case 1 -> minecraft.gui.setScreen(MacroEditScreen.edit(this, macro));
                case 2 -> MacroStore.duplicate(macro.id());
                case 3 -> confirmDelete(macro);
            }
            click();
            return true;
        }
        if (doubleClick) minecraft.gui.setScreen(MacroEditScreen.edit(this, macro));
        return true;
    }

    private void confirmDelete(Macro macro) {
        minecraft.gui.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) MacroStore.delete(macro.id());
            minecraft.gui.setScreen(this);
        }, Component.translatable("macros.redstonelabworks.delete_confirm.title"),
                Component.translatable("macros.redstonelabworks.delete_confirm.message", macro.name())));
    }

    private void importMacros() {
        try {
            int count = MacroStore.importIntoActiveProfile(null);
            RedstoneMessages.popup(Component.translatable("macros.redstonelabworks.imported", count, MacroStore.defaultExportPath().toString()));
        } catch (IOException exception) {
            RedstoneMessages.popup(Component.translatable("macros.redstonelabworks.import_failed", MacroStore.defaultExportPath().toString()));
        }
    }

    private void exportMacros() {
        try {
            int count = MacroStore.exportActiveProfile(null);
            RedstoneMessages.popup(Component.translatable("macros.redstonelabworks.exported", count, MacroStore.defaultExportPath().toString()));
        } catch (IOException exception) {
            RedstoneMessages.popup(Component.translatable("macros.redstonelabworks.export_failed", MacroStore.defaultExportPath().toString()));
        }
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) { Layout layout = layout(); scroll = Mth.clamp(scroll - scrollY * layout.rowHeight(), 0, maxScroll(layout, visibleMacros().size())); return true; }
    @Override public boolean charTyped(CharacterEvent event) { return super.charTyped(event); }
    @Override public boolean keyPressed(KeyEvent event) { if (event.key() == InputConstants.KEY_ESCAPE) { onClose(); return true; } return super.keyPressed(event); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }

    private List<Macro> visibleMacros() {
        String query = searchBox == null ? "" : searchBox.getValue().strip().toLowerCase(Locale.ROOT);
        Comparator<Macro> comparator = sortOrder.comparator();
        return MacroStore.macros().stream()
                .filter(macro -> query.isEmpty() || (macro.name() + " " + macro.command() + " " + macro.alias() + " " + macro.category()).toLowerCase(Locale.ROOT).contains(query))
                .sorted(comparator).toList();
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics, Layout layout, int count) {
        int total = count * layout.rowHeight();
        if (total <= layout.contentHeight()) return;
        int x = layout.right() + 4;
        graphics.fill(x, layout.top(), x + 5, layout.bottom(), RedstoneUi.SCROLLBAR_TRACK_COLOR);
        int thumb = Math.max(22, layout.contentHeight() * layout.contentHeight() / total);
        int y = layout.top() + (int) ((layout.contentHeight() - thumb) * scroll / Math.max(1, total - layout.contentHeight()));
        graphics.fill(x, y, x + 5, y + thumb, RedstoneUi.SCROLLBAR_THUMB_COLOR);
    }

    private double maxScroll(Layout layout, int count) { return Math.max(0, count * layout.rowHeight() - layout.contentHeight()); }
    private void footerButton(GuiGraphicsExtractor g, String key, int x, int y, int w, int mx, int my) { drawButton(g, Component.translatable(key).getString(), x, y, w, 22, RedstoneUi.contains(mx, my, x, y, w, 22), RedstoneUi.ButtonTone.NORMAL); }
    private void drawButton(GuiGraphicsExtractor g, String label, int x, int y, int w, int h, boolean hover, RedstoneUi.ButtonTone tone) { RedstoneUi.drawButton(g, font, label, x, y, w, h, hover, tone); }
    private boolean hit(MouseButtonEvent e, int x, int y, int w, int h) { return RedstoneUi.contains(e.x(), e.y(), x, y, w, h); }
    private void click() { AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager()); }

    private Layout layout() {
        int w = Math.min(PANEL_WIDTH, width - 16);
        int h = Math.min(PANEL_HEIGHT, height - 16);
        return new Layout((width - w) / 2, (height - h) / 2, w, h);
    }

    private enum SortOrder {
        CATEGORY, NAME, TYPE;
        Component label() { return Component.translatable("macros.redstonelabworks.sort." + name().toLowerCase(Locale.ROOT)); }
        Comparator<Macro> comparator() {
            Comparator<Macro> byName = Comparator.comparing(macro -> macro.name().toLowerCase(Locale.ROOT));
            return switch (this) {
                case CATEGORY -> Comparator.comparing((Macro m) -> m.category().toLowerCase(Locale.ROOT)).thenComparing(byName);
                case NAME -> byName;
                case TYPE -> Comparator.comparing((Macro m) -> m.type().ordinal()).thenComparing(byName);
            };
        }
    }

    private record Layout(int x, int y, int width, int height) {
        boolean compact() { return width < 680; }
        int headerHeight() { return compact() ? COMPACT_HEADER : HEADER; }
        int rowHeight() { return compact() ? COMPACT_ROW_HEIGHT : ROW_HEIGHT; }
        int searchX() { return x + 14; }
        int searchY() { return y + 42; }
        int searchWidth() { return compact() ? width - 28 : 230; }
        int controlsY() { return compact() ? y + 72 : y + 42; }
        int sortX() { return compact() ? x + 14 : x + 254; }
        int sortWidth() { return compact() ? Math.max(86, width - 28 - 74 - 78 - 16) : 150; }
        int importX() { return compact() ? sortX() + sortWidth() + 8 : x + width - 174; }
        int exportX() { return compact() ? importX() + 82 : x + width - 92; }
        int left() { return x + 14; }
        int right() { return x + width - 21; }
        int top() { return y + headerHeight(); }
        int bottom() { return y + height - FOOTER; }
        int contentWidth() { return right() - left(); }
        int contentHeight() { return bottom() - top(); }
        int actionsX() { return right() - ROW_ACTION_INSET - SMALL_BUTTON * 4 - ACTION_GAP * 3; }
        int actionWidth() { return compact() ? Math.max(34, (contentWidth() - ROW_TEXT_INSET - ROW_ACTION_INSET - ACTION_GAP * 3) / 4) : SMALL_BUTTON; }
        int actionX(int index) { return compact() ? left() + ROW_TEXT_INSET + index * (actionWidth() + ACTION_GAP) : actionsX() + index * (SMALL_BUTTON + ACTION_GAP); }
        int actionY(int rowY) { return compact() ? rowY + 65 : rowY + 24; }
        int footerKeyWidth() { return compact() ? Math.max(54, (width - 40) / 3) : 118; }
        int footerCommandWidth() { return compact() ? footerKeyWidth() : 126; }
        int footerDoneWidth() { return compact() ? footerKeyWidth() : 84; }
        int footerKeyX() { return x + 14; }
        int footerCommandX() { return compact() ? footerKeyX() + footerKeyWidth() + 6 : x + 140; }
        int footerDoneX() { return compact() ? footerCommandX() + footerCommandWidth() + 6 : x + width - 98; }
    }
}
