package org.main.redstoneutils.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import org.main.redstoneutils.client.util.ClientThreads;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class PopupOverlay {

    private static final Object LOCK = new Object();
    private static final List<PopupMessage> EXITING_POPUPS = new ArrayList<>();

    private static PopupMessage currentPopup;

    private PopupOverlay() {
    }

    static void show(String message) {
        if (message == null || message.isBlank()) return;
        ClientThreads.run(() -> add(message));
    }

    static void render(GuiGraphicsExtractor graphics) {
        long now = System.currentTimeMillis();

        synchronized (LOCK) {
            if (currentPopup == null && EXITING_POPUPS.isEmpty()) return;

            graphics.nextStratum();

            Iterator<PopupMessage> iterator = EXITING_POPUPS.iterator();
            while (iterator.hasNext()) {
                PopupMessage popup = iterator.next();
                popup.render(graphics, now);

                if (popup.isFinished(now)) {
                    iterator.remove();
                }
            }

            if (currentPopup != null) {
                currentPopup.render(graphics, now);

                if (currentPopup.isFinished(now)) {
                    currentPopup = null;
                }
            }
        }
    }

    private static void add(String message) {
        long now = System.currentTimeMillis();

        synchronized (LOCK) {
            if (currentPopup != null) {
                currentPopup.startExit(now);
                EXITING_POPUPS.add(currentPopup);
            }

            currentPopup = new PopupMessage(message, now);
        }
    }

    private static final class PopupMessage {

        private static final long DISPLAY_TIME = 3000L;
        private static final long ENTER_TIME = 240L;
        private static final long EXIT_TIME = 180L;

        private static final int TARGET_X = 8;
        private static final int TARGET_Y = 8;
        private static final int HEIGHT = 30;
        private static final int MIN_WIDTH = 92;
        private static final int MAX_WIDTH = 240;
        private static final int ACCENT_WIDTH = 4;
        private static final int PADDING_X = 9;
        private static final int SCREEN_PADDING = 6;

        private static final int BACKGROUND_COLOR = 0xE62B2D31;
        private static final int BORDER_COLOR = 0xF05B6068;
        private static final int HIGHLIGHT_COLOR = 0xFF7B8088;
        private static final int SHADOW_COLOR = 0xD116171A;
        private static final int ACCENT_COLOR = 0xFF9CA3AF;
        private static final int TEXT_COLOR = 0xFFFFFFFF;

        private final String message;
        private final long createdAt;
        private final int naturalWidth;

        private long exitStartedAt = -1L;
        private float exitStartX = Float.NaN;
        private float exitStartOpacity = Float.NaN;
        private int width;

        private PopupMessage(String message, long createdAt) {
            this.message = message == null ? "" : message.strip();
            this.createdAt = createdAt;

            Font font = Minecraft.getInstance().font;
            int textWidth = font == null ? this.message.length() * 6 : font.width(this.message);
            this.naturalWidth = Math.max(MIN_WIDTH, textWidth + ACCENT_WIDTH + PADDING_X * 2);
            this.width = this.naturalWidth;
        }

        private void startExit(long now) {
            if (exitStartedAt != -1L) return;

            exitStartedAt = now;
            exitStartX = Float.NaN;
            exitStartOpacity = Float.NaN;
        }

        private void render(GuiGraphicsExtractor graphics, long now) {
            Font font = Minecraft.getInstance().font;
            updateWidth(graphics);

            if (shouldExit(now)) {
                startExit(now);
            }

            float opacity = opacityAt(now);
            if (opacity <= 0.01F) return;

            int x = Math.round(xAt(now));
            int y = TARGET_Y;
            int textX = x + ACCENT_WIDTH + PADDING_X;
            int textY = y + (HEIGHT - font.lineHeight) / 2;
            int textWidth = width - ACCENT_WIDTH - PADDING_X * 2;
            String visibleMessage = fitMessage(font, textWidth);

            graphics.fill(x + 2, y + 2, x + width + 2, y + HEIGHT + 2, UiRender.scaleAlpha(SHADOW_COLOR, opacity));
            graphics.fill(x, y, x + width, y + HEIGHT, UiRender.scaleAlpha(BACKGROUND_COLOR, opacity));
            graphics.fill(x, y, x + ACCENT_WIDTH, y + HEIGHT, UiRender.scaleAlpha(ACCENT_COLOR, opacity));
            graphics.fill(x + ACCENT_WIDTH, y, x + width, y + 1, UiRender.scaleAlpha(HIGHLIGHT_COLOR, opacity));
            graphics.outline(x, y, width, HEIGHT, UiRender.scaleAlpha(BORDER_COLOR, opacity));
            graphics.text(font, visibleMessage, textX, textY, UiRender.scaleAlpha(TEXT_COLOR, opacity), false);
        }

        private void updateWidth(GuiGraphicsExtractor graphics) {
            int availableWidth = Math.max(MIN_WIDTH, graphics.guiWidth() - TARGET_X - SCREEN_PADDING);
            width = Mth.clamp(naturalWidth, MIN_WIDTH, Math.min(MAX_WIDTH, availableWidth));
        }

        private boolean shouldExit(long now) {
            return exitStartedAt == -1L && now - createdAt >= ENTER_TIME + DISPLAY_TIME;
        }

        private boolean isFinished(long now) {
            return exitStartedAt != -1L && now - exitStartedAt >= EXIT_TIME;
        }

        private float xAt(long now) {
            if (exitStartedAt != -1L) {
                if (Float.isNaN(exitStartX)) {
                    exitStartX = enterXAt(exitStartedAt);
                }

                float progress = progress(now, exitStartedAt, EXIT_TIME);
                return Mth.lerp(easeInCubic(progress), exitStartX, hiddenX());
            }

            return enterXAt(now);
        }

        private float enterXAt(long now) {
            float progress = progress(now, createdAt, ENTER_TIME);
            return Mth.lerp(easeOutCubic(progress), hiddenX(), TARGET_X);
        }

        private float opacityAt(long now) {
            if (exitStartedAt != -1L) {
                if (Float.isNaN(exitStartOpacity)) {
                    exitStartOpacity = enterOpacityAt(exitStartedAt);
                }

                float progress = progress(now, exitStartedAt, EXIT_TIME);
                return Mth.lerp(easeInCubic(progress), exitStartOpacity, 0.0F);
            }

            return enterOpacityAt(now);
        }

        private float enterOpacityAt(long now) {
            return easeOutCubic(progress(now, createdAt, ENTER_TIME));
        }

        private float hiddenX() {
            return -width - SCREEN_PADDING;
        }

        private String fitMessage(Font font, int maxWidth) {
            if (font.width(message) <= maxWidth) return message;

            String suffix = "...";
            int suffixWidth = font.width(suffix);
            return font.plainSubstrByWidth(message, Math.max(0, maxWidth - suffixWidth)) + suffix;
        }

        private static float progress(long now, long start, long duration) {
            return Mth.clamp((float) (now - start) / (float) duration, 0.0F, 1.0F);
        }

        private static float easeOutCubic(float value) {
            float inverted = 1.0F - value;
            return 1.0F - inverted * inverted * inverted;
        }

        private static float easeInCubic(float value) {
            return value * value * value;
        }
    }
}
