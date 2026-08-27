package org.main.redstoneutils.client.sculk;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import org.main.redstoneutils.client.ui.RedstoneUi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class SculkInfoScreen extends Screen {

    private static final int PANEL_WIDTH = 620;
    private static final int PANEL_HEIGHT = 450;
    private static final int HEADER_HEIGHT = 54;
    private static final int FOOTER_HEIGHT = 38;
    private static final int ROW_GAP = 8;
    private static final int CLOSE_WIDTH = 84;
    private static final int BUTTON_HEIGHT = 22;

    private final List<SignalEntry> entries;
    private double scroll;

    public SculkInfoScreen() {
        super(Component.translatable("screen.redstoneutils.sculk_info"));
        entries = collectEntries();
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.gui.setScreen(new SculkInfoScreen()));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        Layout layout = layout();
        List<Row> rows = rows(layout.contentWidth() - 20);
        scroll = Mth.clamp(scroll, 0.0D, maxScroll(layout, rows));

        RedstoneUi.drawPanel(graphics, layout.x(), layout.y(), layout.width(), layout.height());
        graphics.text(font, title, layout.x() + 14, layout.y() + 10, RedstoneUi.TEXT_COLOR, false);
        graphics.text(font, Component.translatable("screen.redstoneutils.sculk_info.subtitle"),
                layout.x() + 14, layout.y() + 28, RedstoneUi.MUTED_TEXT_COLOR, false);

        graphics.enableScissor(layout.left(), layout.top(), layout.right(), layout.bottom());
        int y = layout.top() - (int) scroll;
        for (int index = 0; index < rows.size(); index++) {
            Row row = rows.get(index);
            if (y + row.height() >= layout.top() && y <= layout.bottom()) {
                drawRow(graphics, layout, row, index, y);
            }
            y += row.height() + ROW_GAP;
        }
        graphics.disableScissor();

        drawScrollbar(graphics, layout, rows);
        int closeX = layout.x() + layout.width() - CLOSE_WIDTH - 14;
        int closeY = layout.y() + layout.height() - 30;
        RedstoneUi.drawButton(graphics, font, Component.translatable("gui.done").getString(),
                closeX, closeY, CLOSE_WIDTH, BUTTON_HEIGHT,
                RedstoneUi.contains(mouseX, mouseY, closeX, closeY, CLOSE_WIDTH, BUTTON_HEIGHT),
                RedstoneUi.ButtonTone.NORMAL);
    }

    private void drawRow(GuiGraphicsExtractor graphics, Layout layout, Row row, int index, int y) {
        int color = index % 2 == 0 ? RedstoneUi.ROW_COLOR : RedstoneUi.ROW_ALT_COLOR;
        graphics.fill(layout.left(), y, layout.right(), y + row.height(), color);
        graphics.outline(layout.left(), y, layout.contentWidth(), row.height(), RedstoneUi.PANEL_BORDER_COLOR);

        String strength = Integer.toString(row.entry().strength());
        graphics.centeredText(font, strength, layout.left() + 22, y + (row.height() - font.lineHeight) / 2,
                RedstoneUi.TEXT_COLOR);
        graphics.text(font, Component.translatable("screen.redstoneutils.sculk_info.strength", strength),
                layout.left() + 44, y + 8, RedstoneUi.TEXT_COLOR, false);

        int textY = y + 22;
        for (String line : row.lines()) {
            graphics.text(font, line, layout.left() + 44, textY, RedstoneUi.DETAIL_TEXT_COLOR, false);
            textY += font.lineHeight + 2;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return super.mouseClicked(event, doubleClick);

        Layout layout = layout();
        int closeX = layout.x() + layout.width() - CLOSE_WIDTH - 14;
        int closeY = layout.y() + layout.height() - 30;
        if (RedstoneUi.contains(event.x(), event.y(), closeX, closeY, CLOSE_WIDTH, BUTTON_HEIGHT)) {
            AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
            onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Layout layout = layout();
        List<Row> rows = rows(layout.contentWidth() - 20);
        scroll = Mth.clamp(scroll - scrollY * 42.0D, 0.0D, maxScroll(layout, rows));
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

    private List<Row> rows(int textWidth) {
        List<Row> rows = new ArrayList<>(entries.size());
        for (SignalEntry entry : entries) {
            List<String> lines = RedstoneUi.wrap(font, entry.events(), Math.max(40, textWidth - 44), Integer.MAX_VALUE);
            if (lines.isEmpty()) lines = List.of(Component.translatable("screen.redstoneutils.sculk_info.none").getString());
            rows.add(new Row(entry, lines, Math.max(40, 30 + lines.size() * (font.lineHeight + 2))));
        }
        return rows;
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics, Layout layout, List<Row> rows) {
        int total = totalHeight(rows);
        if (total <= layout.contentHeight()) return;

        int x = layout.right() + 4;
        graphics.fill(x, layout.top(), x + RedstoneUi.SCROLLBAR_WIDTH, layout.bottom(), RedstoneUi.SCROLLBAR_TRACK_COLOR);
        int thumb = Math.max(RedstoneUi.SCROLLBAR_MIN_THUMB_HEIGHT,
                layout.contentHeight() * layout.contentHeight() / total);
        int y = layout.top() + (int) ((layout.contentHeight() - thumb) * scroll
                / Math.max(1, total - layout.contentHeight()));
        graphics.fill(x, y, x + RedstoneUi.SCROLLBAR_WIDTH, y + thumb, RedstoneUi.SCROLLBAR_THUMB_COLOR);
    }

    private double maxScroll(Layout layout, List<Row> rows) {
        return Math.max(0, totalHeight(rows) - layout.contentHeight());
    }

    private int totalHeight(List<Row> rows) {
        if (rows.isEmpty()) return 0;
        return rows.stream().mapToInt(Row::height).sum() + (rows.size() - 1) * ROW_GAP;
    }

    private Layout layout() {
        int panelWidth = Math.min(PANEL_WIDTH, width - 16);
        int panelHeight = Math.min(PANEL_HEIGHT, height - 16);
        return new Layout((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
    }

    private static List<SignalEntry> collectEntries() {
        List<ResourceKey<GameEvent>> events = BuiltInRegistries.GAME_EVENT.registryKeySet().stream()
                .filter(key -> VibrationSystem.getGameEventFrequency(key) > 0)
                .sorted(Comparator.comparing(key -> key.identifier().toString()))
                .toList();

        List<SignalEntry> entries = new ArrayList<>(15);
        for (int strength = 1; strength <= 15; strength++) {
            int currentStrength = strength;
            String eventNames = events.stream()
                    .filter(key -> VibrationSystem.getGameEventFrequency(key) == currentStrength)
                    .map(SculkInfoScreen::displayName)
                    .sorted()
                    .reduce((left, right) -> left + " · " + right)
                    .orElse("");
            entries.add(new SignalEntry(strength, eventNames));
        }
        return List.copyOf(entries);
    }

    private static String displayName(ResourceKey<GameEvent> key) {
        Identifier identifier = key.identifier();
        String[] words = identifier.getPath().split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(word.substring(0, 1).toUpperCase(Locale.ROOT));
            result.append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        if (!Identifier.DEFAULT_NAMESPACE.equals(identifier.getNamespace())) {
            result.append(" (").append(identifier.getNamespace()).append(')');
        }
        return result.toString();
    }

    private record SignalEntry(int strength, String events) {
    }

    private record Row(SignalEntry entry, List<String> lines, int height) {
    }

    private record Layout(int x, int y, int width, int height) {
        int left() {
            return x + 14;
        }

        int right() {
            return x + width - 24;
        }

        int top() {
            return y + HEADER_HEIGHT;
        }

        int bottom() {
            return y + height - FOOTER_HEIGHT;
        }

        int contentWidth() {
            return right() - left();
        }

        int contentHeight() {
            return bottom() - top();
        }
    }
}
