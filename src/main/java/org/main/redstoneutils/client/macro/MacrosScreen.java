package org.main.redstoneutils.client.macro;

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
import org.main.redstoneutils.client.ui.RedstoneMessages;
import org.main.redstoneutils.client.ui.RedstoneUi;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class MacrosScreen extends Screen {

    private static final int PANEL_WIDTH = 760;
    private static final int PANEL_HEIGHT = 470;
    private static final int HEADER = 74;
    private static final int FOOTER = 42;
    private static final int ROW_HEIGHT = 64;
    private static final int SMALL_BUTTON = 54;
    private static final int BUTTON_HEIGHT = 22;

    private EditBox searchBox;
    private SortOrder sortOrder = SortOrder.CATEGORY;
    private double scroll;

    public MacrosScreen() {
        super(Component.translatable("screen.redstoneutils.macros"));
    }

    public static void open() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.gui.setScreen(new MacrosScreen()));
    }

    @Override
    protected void init() {
        Layout layout = layout();
        searchBox = addWidget(new EditBox(font, layout.x + 14, layout.y + 42, 230, 22, Component.translatable("macros.redstoneutils.search")));
        searchBox.setHint(Component.translatable("macros.redstoneutils.search"));
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
        graphics.text(font, Component.translatable("screen.redstoneutils.macros.profile", MacroStore.activeProfile()), layout.x + 14, layout.y + 26, RedstoneUi.MUTED_TEXT_COLOR, false);
        searchBox.extractWidgetRenderState(graphics, mouseX, mouseY, deltaTicks);
        drawButton(graphics, Component.translatable("macros.redstoneutils.sort", sortOrder.label()).getString(), layout.x + 254, layout.y + 42, 150, 22,
                RedstoneUi.contains(mouseX, mouseY, layout.x + 254, layout.y + 42, 150, 22), RedstoneUi.ButtonTone.NORMAL);
        drawButton(graphics, Component.translatable("macros.redstoneutils.import").getString(), layout.x + layout.width - 174, layout.y + 42, 74, 22,
                RedstoneUi.contains(mouseX, mouseY, layout.x + layout.width - 174, layout.y + 42, 74, 22), RedstoneUi.ButtonTone.NORMAL);
        drawButton(graphics, Component.translatable("macros.redstoneutils.export").getString(), layout.x + layout.width - 92, layout.y + 42, 78, 22,
                RedstoneUi.contains(mouseX, mouseY, layout.x + layout.width - 92, layout.y + 42, 78, 22), RedstoneUi.ButtonTone.NORMAL);

        graphics.enableScissor(layout.left(), layout.top(), layout.right(), layout.bottom());
        if (macros.isEmpty()) {
            graphics.text(font, Component.translatable("macros.redstoneutils.empty"), layout.left() + 10, layout.top() + 16, RedstoneUi.MUTED_TEXT_COLOR, false);
        }
        for (int index = 0; index < macros.size(); index++) drawRow(graphics, layout, macros.get(index), index, mouseX, mouseY);
        graphics.disableScissor();
        drawScrollbar(graphics, layout, macros.size());
        drawFooter(graphics, layout, mouseX, mouseY);
    }

    private void drawRow(GuiGraphicsExtractor graphics, Layout layout, Macro macro, int index, int mouseX, int mouseY) {
        int y = layout.top() + index * ROW_HEIGHT - (int) scroll;
        if (y + ROW_HEIGHT < layout.top() || y > layout.bottom()) return;
        graphics.fill(layout.left(), y + 2, layout.right(), y + ROW_HEIGHT - 3, index % 2 == 0 ? RedstoneUi.ROW_COLOR : RedstoneUi.ROW_ALT_COLOR);
        graphics.outline(layout.left(), y + 2, layout.contentWidth(), ROW_HEIGHT - 5, RedstoneUi.PANEL_BORDER_COLOR);
        int textX = layout.left() + 9;
        int actionsX = layout.right() - SMALL_BUTTON * 4 - 18;
        RedstoneUi.drawFittedText(graphics, font, macro.name(), textX, y + 9, 170, macro.enabled() ? RedstoneUi.TEXT_COLOR : RedstoneUi.MUTED_TEXT_COLOR);
        RedstoneUi.drawTag(graphics, font, macro.category(), textX, y + 29, 100);
        RedstoneUi.drawFittedText(graphics, font, MacroCommandText.formatCommand(macro.command()), textX + 180, y + 9, actionsX - textX - 190, RedstoneUi.DETAIL_TEXT_COLOR);
        String binding = macro.isCommandAlias()
                ? MacroCommandText.formatCommand(macro.alias())
                : MacroKeys.displayName(macro.keyCode(), macro.mouseButton(), macro.modifiers()) + " · " + macro.trigger();
        RedstoneUi.drawFittedText(graphics, font, binding, textX + 112, y + 33, actionsX - textX - 122, RedstoneUi.MUTED_TEXT_COLOR);

        String[] labels = {
                Component.translatable(macro.enabled() ? "macros.redstoneutils.disable" : "macros.redstoneutils.enable").getString(),
                Component.translatable("macros.redstoneutils.edit").getString(),
                Component.translatable("macros.redstoneutils.copy").getString(),
                Component.translatable("macros.redstoneutils.delete").getString()
        };
        for (int button = 0; button < labels.length; button++) {
            int x = actionsX + button * (SMALL_BUTTON + 6);
            boolean hovered = RedstoneUi.contains(mouseX, mouseY, x, y + 20, SMALL_BUTTON, BUTTON_HEIGHT);
            drawButton(graphics, labels[button], x, y + 20, SMALL_BUTTON, BUTTON_HEIGHT, hovered,
                    button == 3 ? RedstoneUi.ButtonTone.DANGER : RedstoneUi.ButtonTone.NORMAL);
        }
    }

    private void drawFooter(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        int y = layout.y + layout.height - 32;
        footerButton(graphics, "macros.redstoneutils.new_keybind", layout.x + 14, y, 118, mouseX, mouseY);
        footerButton(graphics, "macros.redstoneutils.new_command", layout.x + 140, y, 126, mouseX, mouseY);
        footerButton(graphics, "gui.done", layout.x + layout.width - 98, y, 84, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Layout layout = layout();
        if (searchBox.mouseClicked(event, doubleClick)) return true;
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return true;
        if (hit(event, layout.x + 254, layout.y + 42, 150, 22)) {
            sortOrder = SortOrder.values()[(sortOrder.ordinal() + 1) % SortOrder.values().length]; click(); return true;
        }
        if (hit(event, layout.x + layout.width - 174, layout.y + 42, 74, 22)) { importMacros(); click(); return true; }
        if (hit(event, layout.x + layout.width - 92, layout.y + 42, 78, 22)) { exportMacros(); click(); return true; }
        int footerY = layout.y + layout.height - 32;
        if (hit(event, layout.x + 14, footerY, 118, 22)) { minecraft.gui.setScreen(MacroEditScreen.createKeybind(this)); click(); return true; }
        if (hit(event, layout.x + 140, footerY, 126, 22)) { minecraft.gui.setScreen(MacroEditScreen.createCommandAlias(this)); click(); return true; }
        if (hit(event, layout.x + layout.width - 98, footerY, 84, 22)) { onClose(); click(); return true; }

        List<Macro> macros = visibleMacros();
        int index = (int) ((event.y() - layout.top() + scroll) / ROW_HEIGHT);
        if (index < 0 || index >= macros.size()) return true;
        Macro macro = macros.get(index);
        int y = layout.top() + index * ROW_HEIGHT - (int) scroll;
        int actionsX = layout.right() - SMALL_BUTTON * 4 - 18;
        for (int button = 0; button < 4; button++) {
            int x = actionsX + button * (SMALL_BUTTON + 6);
            if (!hit(event, x, y + 20, SMALL_BUTTON, BUTTON_HEIGHT)) continue;
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
        }, Component.translatable("macros.redstoneutils.delete_confirm.title"),
                Component.translatable("macros.redstoneutils.delete_confirm.message", macro.name())));
    }

    private void importMacros() {
        try {
            int count = MacroStore.importIntoActiveProfile(null);
            RedstoneMessages.popup(Component.translatable("macros.redstoneutils.imported", count, MacroStore.defaultExportPath().toString()));
        } catch (IOException exception) {
            RedstoneMessages.popup(Component.translatable("macros.redstoneutils.import_failed", MacroStore.defaultExportPath().toString()));
        }
    }

    private void exportMacros() {
        try {
            int count = MacroStore.exportActiveProfile(null);
            RedstoneMessages.popup(Component.translatable("macros.redstoneutils.exported", count, MacroStore.defaultExportPath().toString()));
        } catch (IOException exception) {
            RedstoneMessages.popup(Component.translatable("macros.redstoneutils.export_failed", MacroStore.defaultExportPath().toString()));
        }
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) { scroll = Mth.clamp(scroll - scrollY * ROW_HEIGHT, 0, maxScroll(layout(), visibleMacros().size())); return true; }
    @Override public boolean charTyped(CharacterEvent event) { return searchBox.charTyped(event) || super.charTyped(event); }
    @Override public boolean keyPressed(KeyEvent event) { if (searchBox.keyPressed(event)) return true; if (event.key() == InputConstants.KEY_ESCAPE) { onClose(); return true; } return super.keyPressed(event); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }

    private List<Macro> visibleMacros() {
        String query = searchBox == null ? "" : searchBox.getValue().strip().toLowerCase(Locale.ROOT);
        Comparator<Macro> comparator = sortOrder.comparator();
        return MacroStore.macros().stream()
                .filter(macro -> query.isEmpty() || (macro.name() + " " + macro.command() + " " + macro.category()).toLowerCase(Locale.ROOT).contains(query))
                .sorted(comparator).toList();
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics, Layout layout, int count) {
        int total = count * ROW_HEIGHT;
        if (total <= layout.contentHeight()) return;
        int x = layout.right() + 4;
        graphics.fill(x, layout.top(), x + 5, layout.bottom(), RedstoneUi.SCROLLBAR_TRACK_COLOR);
        int thumb = Math.max(22, layout.contentHeight() * layout.contentHeight() / total);
        int y = layout.top() + (int) ((layout.contentHeight() - thumb) * scroll / Math.max(1, total - layout.contentHeight()));
        graphics.fill(x, y, x + 5, y + thumb, RedstoneUi.SCROLLBAR_THUMB_COLOR);
    }

    private double maxScroll(Layout layout, int count) { return Math.max(0, count * ROW_HEIGHT - layout.contentHeight()); }
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
        Component label() { return Component.translatable("macros.redstoneutils.sort." + name().toLowerCase(Locale.ROOT)); }
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
        int left() { return x + 14; }
        int right() { return x + width - 21; }
        int top() { return y + HEADER; }
        int bottom() { return y + height - FOOTER; }
        int contentWidth() { return right() - left(); }
        int contentHeight() { return bottom() - top(); }
    }
}
