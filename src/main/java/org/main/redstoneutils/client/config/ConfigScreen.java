package org.main.redstoneutils.client.config;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.main.redstoneutils.client.autowire.AutoWireHandler;
import org.main.redstoneutils.client.autowire.AutoWirePreviewOverlay;
import org.main.redstoneutils.client.autowire.WireType;
import org.main.redstoneutils.client.bud.BudSwitchOverlay;
import org.main.redstoneutils.client.overlay.OverlayFreeze;
import org.main.redstoneutils.client.sculk.SculkSensorOverlay;
import org.main.redstoneutils.client.ui.RedstoneMessages;
import org.main.redstoneutils.client.ui.RedstoneOverlay;
import org.main.redstoneutils.client.ui.RedstoneUi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ConfigScreen extends Screen {

    private static final int PANEL_WIDTH = 600;
    private static final int PANEL_HEIGHT = 430;
    private static final int HEADER = 84;
    private static final int COMPACT_HEADER = 112;
    private static final int FOOTER = 46;
    private static final int ROW_HEIGHT = 72;
    private static final int COMPACT_ROW_HEIGHT = 78;
    private static final int VALUE_WIDTH = 126;
    private static final int RESET_WIDTH = 24;
    private static final int ROW_ACTION_INSET = 8;
    private static final int BUTTON_HEIGHT = 24;

    private final List<Option> options = createOptions();
    private Category category = Category.ALL;
    private EditBox searchBox;
    private double scroll;

    public ConfigScreen() {
        super(Component.translatable("screen.redstoneutils.config"));
    }

    public static void open() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.gui.setScreen(new ConfigScreen()));
    }

    @Override
    protected void init() {
        super.init();
        Layout layout = layout();
        searchBox = addWidget(new EditBox(
                font,
                layout.searchX(),
                layout.searchY(),
                layout.searchWidth(),
                22,
                Component.translatable("config.redstoneutils.search")
        ));
        searchBox.setHint(Component.translatable("config.redstoneutils.search"));
        searchBox.setMaxLength(80);
        searchBox.setResponder(ignored -> scroll = 0);
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) { }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {

        Layout layout = layout();
        List<Option> visible = visibleOptions();
        scroll = Mth.clamp(scroll, 0, maxScroll(layout, visible));

        RedstoneUi.drawPanel(graphics, layout.x, layout.y, layout.width, layout.height);
        graphics.text(font, title, layout.x + 14, layout.y + 10, RedstoneUi.TEXT_COLOR, false);
        graphics.text(font, Component.translatable("screen.redstoneutils.config.profile", RedstoneUtilsConfig.activeProfile()), layout.x + 14, layout.y + 27, RedstoneUi.MUTED_TEXT_COLOR, false);
        drawButton(graphics, category.label(), layout.categoryX(), layout.categoryY(), layout.categoryWidth(), 22,
                RedstoneUi.contains(mouseX, mouseY, layout.categoryX(), layout.categoryY(), layout.categoryWidth(), 22));
        searchBox.setX(layout.searchX());
        searchBox.setY(layout.searchY());
        searchBox.setSize(layout.searchWidth(), 22);
        searchBox.extractWidgetRenderState(graphics, mouseX, mouseY, deltaTicks);

        graphics.enableScissor(layout.contentLeft(), layout.contentTop(), layout.contentRight(), layout.contentBottom());
        for (int index = 0; index < visible.size(); index++) {

            Option option = visible.get(index);
            int rowY = layout.contentTop() + index * layout.rowHeight() - (int) scroll;
            if (rowY + layout.rowHeight() < layout.contentTop() || rowY > layout.contentBottom()) continue;

            int color = index % 2 == 0 ? RedstoneUi.ROW_COLOR : RedstoneUi.ROW_ALT_COLOR;

            graphics.fill(layout.contentLeft(), rowY + 3, layout.contentRight(), rowY + layout.rowHeight() - 5, color);
            graphics.outline(layout.contentLeft(), rowY + 3, layout.contentWidth(), layout.rowHeight() - 8, RedstoneUi.PANEL_BORDER_COLOR);

            int textX = layout.contentLeft() + 9;
            int textWidth = layout.valueX() - textX - 8;

            graphics.text(font, Component.translatable(option.titleKey), textX, rowY + 9, RedstoneUi.TEXT_COLOR, false);
            RedstoneUi.drawWrappedText(graphics, font, Component.translatable(option.descriptionKey).getString(), textX, rowY + 25, textWidth, 2, RedstoneUi.MUTED_TEXT_COLOR);

            boolean valueHovered = RedstoneUi.contains(mouseX, mouseY, layout.valueX(), rowY + 8, VALUE_WIDTH, BUTTON_HEIGHT);

            drawButton(graphics, option.value.get(), layout.valueX(), rowY + 8, VALUE_WIDTH, BUTTON_HEIGHT, valueHovered);

            boolean resetHovered = RedstoneUi.contains(mouseX, mouseY, layout.resetX(), rowY + 8, RESET_WIDTH, BUTTON_HEIGHT);

            drawButton(graphics, "↺", layout.resetX(), rowY + 8, RESET_WIDTH, BUTTON_HEIGHT, resetHovered);

            if (valueHovered) {
                graphics.setTooltipForNextFrame(Component.translatable(option.tooltipKey), mouseX, mouseY);
            } else if (resetHovered) {
                graphics.setTooltipForNextFrame(Component.translatable("config.redstoneutils.reset_one"), mouseX, mouseY);
            }
        }
        graphics.disableScissor();
        drawScrollbar(graphics, layout, visible.size());

        int footerY = layout.y + layout.height - 33;

        drawButton(graphics, Component.translatable("config.redstoneutils.reset_profile").getString(), layout.x + 14, footerY, 132, 24,
                RedstoneUi.contains(mouseX, mouseY, layout.x + 14, footerY, 132, 24));

        drawButton(graphics, Component.translatable("gui.done").getString(), layout.x + layout.width - 106, footerY, 92, 24,
                RedstoneUi.contains(mouseX, mouseY, layout.x + layout.width - 106, footerY, 92, 24));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Layout layout = layout();
        if (super.mouseClicked(event, doubleClick)) return true;
        clearFocus();
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return true;
        if (RedstoneUi.contains(event.x(), event.y(), layout.categoryX(), layout.categoryY(), layout.categoryWidth(), 22)) {
            category = Category.values()[(category.ordinal() + 1) % Category.values().length];
            scroll = 0;
            click();
            return true;
        }
        List<Option> visible = visibleOptions();
        for (int index = 0; index < visible.size(); index++) {
            int rowY = layout.contentTop() + index * layout.rowHeight() - (int) scroll;
            if (RedstoneUi.contains(event.x(), event.y(), layout.valueX(), rowY + 8, VALUE_WIDTH, BUTTON_HEIGHT)) {
                visible.get(index).edit.run();
                click();
                return true;
            }
            if (RedstoneUi.contains(event.x(), event.y(), layout.resetX(), rowY + 8, RESET_WIDTH, BUTTON_HEIGHT)) {
                visible.get(index).reset.run();
                click();
                return true;
            }
        }
        int footerY = layout.y + layout.height - 33;
        if (RedstoneUi.contains(event.x(), event.y(), layout.x + 14, footerY, 132, 24)) {
            RedstoneUtilsConfig.resetActiveProfile();
            applyProfileSettings();
            click();
            return true;
        }
        if (RedstoneUi.contains(event.x(), event.y(), layout.x + layout.width - 106, footerY, 92, 24)) {
            onClose();
            click();
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Layout layout = layout();
        scroll = Mth.clamp(scroll - scrollY * layout.rowHeight(), 0, maxScroll(layout, visibleOptions()));
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

    @Override
    public boolean charTyped(CharacterEvent event) {
        return super.charTyped(event);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }

    private List<Option> visibleOptions() {
        String query = searchBox == null ? "" : searchBox.getValue().strip().toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> category == Category.ALL || option.category == category)
                .filter(option -> query.isEmpty()
                        || Component.translatable(option.titleKey).getString().toLowerCase(Locale.ROOT).contains(query)
                        || Component.translatable(option.descriptionKey).getString().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics, Layout layout, int rows) {
        int total = rows * layout.rowHeight();
        if (total <= layout.contentHeight()) return;
        int x = layout.contentRight() + 4;
        graphics.fill(x, layout.contentTop(), x + 5, layout.contentBottom(), RedstoneUi.SCROLLBAR_TRACK_COLOR);
        int thumb = Math.max(24, layout.contentHeight() * layout.contentHeight() / total);
        int y = layout.contentTop() + (int) ((layout.contentHeight() - thumb) * (scroll / Math.max(1, total - layout.contentHeight())));
        graphics.fill(x, y, x + 5, y + thumb, RedstoneUi.SCROLLBAR_THUMB_COLOR);
    }

    private double maxScroll(Layout layout, List<Option> visible) {
        return Math.max(0, visible.size() * layout.rowHeight() - layout.contentHeight());
    }

    private void drawButton(GuiGraphicsExtractor graphics, String label, int x, int y, int width, int height, boolean hovered) {
        RedstoneUi.drawButton(graphics, font, label, x, y, width, height, hovered, RedstoneUi.ButtonTone.NORMAL);
    }

    private void click() {
        AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
    }

    private Layout layout() {
        int w = Math.min(PANEL_WIDTH, width - 20);
        int h = Math.min(PANEL_HEIGHT, height - 20);
        return new Layout((width - w) / 2, (height - h) / 2, w, h);
    }

    private static void applyProfileSettings() {
        RedstoneOverlay.setVisible(RedstoneUtilsConfig.isHudOverlayVisible());
        AutoWirePreviewOverlay.setVisible(RedstoneUtilsConfig.isWirePreviewOverlayVisible());
        BudSwitchOverlay.setVisible(RedstoneUtilsConfig.isBudOverlayVisible());
        SculkSensorOverlay.setVisible(RedstoneUtilsConfig.isSculkOverlayVisible());
        AutoWireHandler.reloadFromProfile();
    }

    private List<Option> createOptions() {
        List<Option> result = new ArrayList<>();
        result.add(toggle(Category.GENERAL, "config.redstoneutils.hud", RedstoneUtilsConfig::isHudOverlayVisible, RedstoneOverlay::setVisible, true));
        result.add(toggle(Category.GENERAL, "config.redstoneutils.status_hud", RedstoneUtilsConfig::isStatusHudVisible, RedstoneUtilsConfig::setStatusHudVisible, true));
        result.add(choice(Category.GENERAL, "config.redstoneutils.status_anchor", RedstoneUtilsConfig.HudAnchor.values(), RedstoneUtilsConfig::getStatusHudAnchor, RedstoneUtilsConfig::setStatusHudAnchor, RedstoneUtilsConfig.HudAnchor.TOP_RIGHT));
        result.add(choice(Category.GENERAL, "config.redstoneutils.feedback", RedstoneMessages.MessageTarget.values(), RedstoneMessages::getDefaultTarget, RedstoneMessages::setDefaultTarget, RedstoneMessages.MessageTarget.POPUP));
        result.add(number(Category.GENERAL, "config.redstoneutils.teleport", 10, 1000, 0, RedstoneUtilsConfig::getTeleportMaxRange, RedstoneUtilsConfig::setTeleportMaxRange, 100));

        result.add(choice(Category.AUTOWIRE, "config.redstoneutils.autowire", WireType.values(), AutoWireHandler::getActiveWireType, AutoWireHandler::setActiveWireType, WireType.NONE));
        result.add(toggle(Category.AUTOWIRE, "config.redstoneutils.wire_preview", AutoWirePreviewOverlay::isVisible, AutoWirePreviewOverlay::setVisible, true));

        result.add(toggle(Category.OVERLAYS, "config.redstoneutils.bud", BudSwitchOverlay::isVisible, BudSwitchOverlay::setVisible, true));
        result.add(number(Category.OVERLAYS, "config.redstoneutils.bud_range", 4, 64, 0, () -> (double) RedstoneUtilsConfig.getBudTestRange(), value -> { RedstoneUtilsConfig.setBudTestRange(value.intValue()); BudSwitchOverlay.clear(); }, 8));
        result.add(toggle(Category.OVERLAYS, "config.redstoneutils.sculk", SculkSensorOverlay::isVisible, SculkSensorOverlay::setVisible, false));
        result.add(number(Category.OVERLAYS, "config.redstoneutils.sculk_distance", 16, 256, 0, () -> (double) RedstoneUtilsConfig.getSculkSensorSearchDistance(), value -> { RedstoneUtilsConfig.setSculkSensorSearchDistance(value.intValue()); SculkSensorOverlay.requestRefresh(); }, 96));
        result.add(toggle(Category.OVERLAYS, "config.redstoneutils.freeze", OverlayFreeze::anyFrozen, OverlayFreeze::setAllFrozen, false));

        result.add(choice(Category.ACCESSIBILITY, "config.redstoneutils.palette", RedstoneUtilsConfig.ColorPalette.values(), RedstoneUtilsConfig::getColorPalette, RedstoneUtilsConfig::setColorPalette, RedstoneUtilsConfig.ColorPalette.DEFAULT));
        result.add(color(Category.ACCESSIBILITY, "config.redstoneutils.wire_color", RedstoneUtilsConfig::wirePreviewColor, RedstoneUtilsConfig::setWirePreviewColor, RedstoneUtilsConfig::resetWirePreviewColor));
        result.add(color(Category.ACCESSIBILITY, "config.redstoneutils.risk_color", RedstoneUtilsConfig::budRiskColor, RedstoneUtilsConfig::setBudRiskColor, RedstoneUtilsConfig::resetBudRiskColor));
        result.add(color(Category.ACCESSIBILITY, "config.redstoneutils.source_color", RedstoneUtilsConfig::budSourceColor, RedstoneUtilsConfig::setBudSourceColor, RedstoneUtilsConfig::resetBudSourceColor));
        result.add(color(Category.ACCESSIBILITY, "config.redstoneutils.sculk_color", RedstoneUtilsConfig::sculkColor, RedstoneUtilsConfig::setSculkColor, RedstoneUtilsConfig::resetSculkColor));
        result.add(number(Category.ACCESSIBILITY, "config.redstoneutils.opacity", 0.1, 1.0, 2, () -> (double) RedstoneUtilsConfig.getOverlayOpacity(), value -> RedstoneUtilsConfig.setOverlayOpacity(value.floatValue()), 0.85));
        result.add(number(Category.ACCESSIBILITY, "config.redstoneutils.line_width", 1, 8, 1, () -> (double) RedstoneUtilsConfig.getOverlayLineWidth(), value -> RedstoneUtilsConfig.setOverlayLineWidth(value.floatValue()), 2));
        result.add(toggle(Category.ACCESSIBILITY, "config.redstoneutils.through_walls", RedstoneUtilsConfig::renderOverlaysThroughWalls, RedstoneUtilsConfig::setOverlayThroughWalls, false));
        result.add(choice(Category.ACCESSIBILITY, "config.redstoneutils.popup_anchor", RedstoneUtilsConfig.PopupAnchor.values(), RedstoneUtilsConfig::getPopupAnchor, RedstoneUtilsConfig::setPopupAnchor, RedstoneUtilsConfig.PopupAnchor.TOP_LEFT));
        result.add(number(Category.ACCESSIBILITY, "config.redstoneutils.popup_duration", 1000, 15000, 0, () -> (double) RedstoneUtilsConfig.getPopupDurationMillis(), value -> RedstoneUtilsConfig.setPopupDurationMillis(value.intValue()), 3000));

        result.add(number(Category.PERFORMANCE, "config.redstoneutils.overlay_distance", 8, 256, 0, () -> (double) RedstoneUtilsConfig.getOverlayMaxDistance(), value -> RedstoneUtilsConfig.setOverlayMaxDistance(value.intValue()), 128));
        result.add(number(Category.PERFORMANCE, "config.redstoneutils.sculk_interval", 5, 100, 0, () -> (double) RedstoneUtilsConfig.getSculkRebuildIntervalTicks(), value -> { RedstoneUtilsConfig.setSculkRebuildIntervalTicks(value.intValue()); SculkSensorOverlay.requestRefresh(); }, 20));
        return result;
    }

    private static Option toggle(Category category, String key, Supplier<Boolean> getter, Consumer<Boolean> setter, boolean defaultValue) {
        return new Option(category, key + ".title", key + ".description", key + ".tooltip",
                () -> Component.translatable(getter.get() ? "state.redstoneutils.on" : "state.redstoneutils.off").getString(),
                () -> setter.accept(!getter.get()), () -> setter.accept(defaultValue));
    }

    private static <T> Option choice(Category category, String key, T[] values, Supplier<T> getter, Consumer<T> setter, T defaultValue) {
        return new Option(category, key + ".title", key + ".description", key + ".tooltip",
                () -> enumLabel(getter.get()), () -> {
                    int index = Arrays.asList(values).indexOf(getter.get());
                    setter.accept(values[(index + 1) % values.length]);
                }, () -> setter.accept(defaultValue));
    }

    private Option number(Category category, String key, double min, double max, int decimals,
                                 Supplier<Double> getter, Consumer<Double> setter, double defaultValue) {
        return new Option(category, key + ".title", key + ".description", key + ".tooltip",
                () -> decimals == 0 ? Integer.toString((int) Math.round(getter.get())) : String.format(Locale.ROOT, "% ." + decimals + "f", getter.get()).strip(),
                () -> Minecraft.getInstance().gui.setScreen(new NumericConfigScreen(
                        Component.translatable(key + ".title"), min, max, decimals, getter.get(), setter, this
                )), () -> setter.accept(defaultValue));
    }

    private Option color(Category category, String key, Supplier<Integer> getter, java.util.function.IntConsumer setter, Runnable reset) {
        return new Option(category, key + ".title", key + ".description", key + ".tooltip",
                () -> String.format(Locale.ROOT, "#%06X", getter.get() & 0xFFFFFF),
                () -> Minecraft.getInstance().gui.setScreen(new ColorConfigScreen(
                        Component.translatable(key + ".title"), getter.get(), setter, this
                )), reset);
    }

    private static String enumLabel(Object value) {
        if (value instanceof WireType wireType) return wireType.getDisplayName();
        String id = value == null ? "" : value.toString().toLowerCase(Locale.ROOT);
        return Component.translatable("enum.redstoneutils." + id).getString();
    }

    private enum Category {
        ALL, GENERAL, AUTOWIRE, OVERLAYS, ACCESSIBILITY, PERFORMANCE;
        private String label() { return Component.translatable("config.redstoneutils.category." + name().toLowerCase(Locale.ROOT)).getString(); }
    }

    private record Option(Category category, String titleKey, String descriptionKey, String tooltipKey,
                          Supplier<String> value, Runnable edit, Runnable reset) { }

    private record Layout(int x, int y, int width, int height) {
        boolean compact() { return width < 540; }
        int headerHeight() { return compact() ? COMPACT_HEADER : HEADER; }
        int rowHeight() { return compact() ? COMPACT_ROW_HEIGHT : ROW_HEIGHT; }
        int categoryX() { return x + 14; }
        int categoryY() { return y + 45; }
        int categoryWidth() { return compact() ? width - 28 : 216; }
        int searchX() { return compact() ? x + 14 : x + 242; }
        int searchY() { return compact() ? y + 77 : y + 45; }
        int searchWidth() { return compact() ? width - 28 : 210; }
        int contentLeft() { return x + 14; }
        int contentRight() { return x + width - 21; }
        int contentTop() { return y + headerHeight(); }
        int contentBottom() { return y + height - FOOTER; }
        int contentWidth() { return contentRight() - contentLeft(); }
        int contentHeight() { return contentBottom() - contentTop(); }
        int valueX() { return resetX() - 6 - VALUE_WIDTH; }
        int resetX() { return contentRight() - ROW_ACTION_INSET - RESET_WIDTH; }
    }
}
