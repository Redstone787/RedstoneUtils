/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

public final class RedstoneUi {

    public static final int PANEL_PADDING = 12;
    public static final int GAP = 8;
    public static final int SCROLLBAR_WIDTH = 5;
    public static final int SCROLLBAR_GAP = 8;
    public static final int SCROLLBAR_MIN_THUMB_HEIGHT = 22;

    public static final int SHADOW_COLOR = 0xD116171A;
    public static final int PANEL_COLOR = 0xE62B2D31;
    public static final int PANEL_BORDER_COLOR = 0xF05B6068;
    public static final int PANEL_HIGHLIGHT_COLOR = 0xFF7B8088;
    public static final int ROW_COLOR = 0x8033363C;
    public static final int ROW_ALT_COLOR = 0x662B2D31;
    public static final int ROW_HOVER_COLOR = 0xA0454A52;
    public static final int FIELD_COLOR = 0xE6212226;
    public static final int BUTTON_COLOR = 0xE642454C;
    public static final int BUTTON_HOVER_COLOR = 0xE66A707A;
    public static final int BUTTON_ACTIVE_COLOR = 0xE68A909A;
    public static final int BUTTON_DANGER_COLOR = 0xE65A3535;
    public static final int BUTTON_DISABLED_COLOR = 0x8842454C;
    public static final int BUTTON_BORDER_COLOR = 0xF05B6068;
    public static final int SCROLLBAR_TRACK_COLOR = 0x6633363C;
    public static final int SCROLLBAR_THUMB_COLOR = 0xD07B8088;
    public static final int TEXT_COLOR = 0xFFFFFFFF;
    public static final int MUTED_TEXT_COLOR = 0xFFB8BEC8;
    public static final int DETAIL_TEXT_COLOR = 0xFFD5DAE2;
    public static final int ERROR_TEXT_COLOR = 0xFFFFB4A9;

    private RedstoneUi() {
    }

    public static void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x + 3, y + 3, x + width + 3, y + height + 3, SHADOW_COLOR);
        graphics.fill(x, y, x + width, y + height, PANEL_COLOR);
        graphics.fill(x, y, x + width, y + 1, PANEL_HIGHLIGHT_COLOR);
        graphics.outline(x, y, width, height, PANEL_BORDER_COLOR);
    }

    public static void drawButton(GuiGraphicsExtractor graphics, Font font, String label, int x, int y,
                                  int width, int height, boolean hovered, ButtonTone tone) {
        int color = switch (tone == null ? ButtonTone.NORMAL : tone) {
            case NORMAL -> hovered ? BUTTON_HOVER_COLOR : BUTTON_COLOR;
            case ACTIVE -> hovered ? BUTTON_HOVER_COLOR : BUTTON_ACTIVE_COLOR;
            case DANGER -> hovered ? BUTTON_HOVER_COLOR : BUTTON_DANGER_COLOR;
            case DISABLED -> BUTTON_DISABLED_COLOR;
        };

        graphics.fill(x, y, x + width, y + height, color);
        graphics.outline(x, y, width, height, BUTTON_BORDER_COLOR);
        graphics.centeredText(font, fitCentered(font, label, width - 8), x + width / 2, y + (height - font.lineHeight) / 2, TEXT_COLOR);
    }

    public static void drawTag(GuiGraphicsExtractor graphics, Font font, String label, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 16, BUTTON_COLOR);
        graphics.outline(x, y, width, 16, BUTTON_BORDER_COLOR);
        graphics.centeredText(font, fitCentered(font, label, width - 6), x + width / 2, y + (16 - font.lineHeight) / 2, TEXT_COLOR);
    }

    public static int drawWrappedText(GuiGraphicsExtractor graphics, Font font, String text, int x, int y,
                                      int maxWidth, int maxLines, int color) {
        List<String> lines = wrap(font, text, maxWidth, maxLines);
        for (int index = 0; index < lines.size(); index++) {
            graphics.text(font, lines.get(index), x, y + index * (font.lineHeight + 1), color, false);
        }

        return y + lines.size() * (font.lineHeight + 1);
    }

    public static void drawFittedText(GuiGraphicsExtractor graphics, Font font, String text, int x, int y,
                                      int maxWidth, int color) {
        graphics.text(font, fitEnd(font, text, maxWidth), x, y, color, false);
    }

    public static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public static List<String> wrap(Font font, String text, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank() || maxLines <= 0 || maxWidth <= 0) return lines;

        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.width(candidate) <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
                continue;
            }

            if (!line.isEmpty()) {
                lines.add(line.toString());
                if (lines.size() == maxLines) return finishWrappedLines(font, lines, maxWidth);
            }

            line.setLength(0);
            line.append(fitEnd(font, word, maxWidth));
        }

        if (!line.isEmpty() && lines.size() < maxLines) {
            lines.add(line.toString());
        }

        return lines;
    }

    public static String fitEnd(Font font, String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) return "";
        if (font.width(text) <= maxWidth) return text;

        String suffix = "...";
        int suffixWidth = font.width(suffix);
        if (suffixWidth >= maxWidth) return "";

        return font.plainSubstrByWidth(text, maxWidth - suffixWidth) + suffix;
    }

    public static String fitFromStart(Font font, String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) return "";
        if (font.width(text) <= maxWidth) return text;

        String prefix = "...";
        for (int index = 0; index < text.length(); index++) {
            String candidate = prefix + text.substring(index);
            if (font.width(candidate) <= maxWidth) return candidate;
        }

        return "";
    }

    private static String fitCentered(Font font, String text, int maxWidth) {
        return fitEnd(font, text, maxWidth);
    }

    private static List<String> finishWrappedLines(Font font, List<String> lines, int maxWidth) {
        int lastIndex = lines.size() - 1;
        String suffix = "...";
        String last = lines.get(lastIndex);
        int available = maxWidth - font.width(suffix);
        lines.set(lastIndex, available <= 0 ? "" : font.plainSubstrByWidth(last, available) + suffix);
        return lines;
    }

    public enum ButtonTone {
        NORMAL,
        ACTIVE,
        DANGER,
        DISABLED
    }
}
