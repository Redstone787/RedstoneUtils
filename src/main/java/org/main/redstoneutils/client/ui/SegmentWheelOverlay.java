package org.main.redstoneutils.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.main.redstoneutils.client.util.ClientThreads;

import java.util.ArrayList;
import java.util.List;

final class SegmentWheelOverlay {

    private static final Object LOCK = new Object();

    private static final int OUTER_RADIUS = 160;
    private static final int INNER_RADIUS = 32;
    private static final int ICON_SIZE = 48;
    private static final int SELECTED_COLOR = 0xE66A707A;
    private static final int ACTIVE_COLOR = 0xE68A909A;
    private static final int SEGMENT_COLOR = 0xC02B2D31;
    private static final int ALTERNATE_SEGMENT_COLOR = 0xC033363C;
    private static final int SEPARATOR_COLOR = 0xE616171A;
    private static final int CENTER_COLOR = 0xE6212226;
    private static final int CENTER_BORDER_COLOR = 0xF05B6068;
    private static final int SHADOW_COLOR = 0x8016171A;

    private static boolean visible;
    private static int segmentCount = CircleSegment.segmentCount();
    private static List<Identifier> textures = List.of();
    private static CircleSegment hoveredSegment = CircleSegment.NONE;
    private static CircleSegment highlightedSegment = CircleSegment.NONE;
    private static CircleSegment confirmedSegment = CircleSegment.NONE;
    private static boolean wasLeftMouseDown;

    private SegmentWheelOverlay() {
    }

    static boolean isVisible() {
        synchronized (LOCK) {
            return visible;
        }
    }

    static void set(boolean visible, int segmentCount, List<Identifier> textures, CircleSegment highlightedSegment) {
        ClientThreads.run(() -> {
            synchronized (LOCK) {
                List<Identifier> resolvedTextures = textures == null ? List.of() : new ArrayList<>(textures);
                SegmentWheelOverlay.visible = visible;
                SegmentWheelOverlay.segmentCount = resolveSegmentCount(segmentCount, resolvedTextures);
                SegmentWheelOverlay.textures = resolvedTextures;
                SegmentWheelOverlay.highlightedSegment = visible ? resolveHighlightedSegment(highlightedSegment, SegmentWheelOverlay.segmentCount) : CircleSegment.NONE;
                if (visible) {
                    confirmedSegment = CircleSegment.NONE;
                } else {
                    resetSelection();
                }
            }
        });
    }

    static void setVisible(boolean visible) {
        ClientThreads.run(() -> {
            synchronized (LOCK) {
                SegmentWheelOverlay.visible = visible;
                if (visible) {
                    confirmedSegment = CircleSegment.NONE;
                } else {
                    resetSelection();
                }
            }
        });
    }

    static void setCount(int segmentCount) {
        ClientThreads.run(() -> {
            synchronized (LOCK) {
                SegmentWheelOverlay.segmentCount = resolveSegmentCount(segmentCount, textures);
                highlightedSegment = resolveHighlightedSegment(highlightedSegment, SegmentWheelOverlay.segmentCount);
            }
        });
    }

    static void setTextures(List<Identifier> textures) {
        ClientThreads.run(() -> {
            synchronized (LOCK) {
                SegmentWheelOverlay.textures = textures == null ? List.of() : new ArrayList<>(textures);
                segmentCount = resolveSegmentCount(0, SegmentWheelOverlay.textures);
                highlightedSegment = resolveHighlightedSegment(highlightedSegment, segmentCount);
            }
        });
    }

    static CircleSegment getSelectedSegment() {
        synchronized (LOCK) {
            return hoveredSegment;
        }
    }

    static CircleSegment consumeConfirmedSegment() {
        synchronized (LOCK) {
            CircleSegment segment = confirmedSegment;
            confirmedSegment = CircleSegment.NONE;
            return segment;
        }
    }

    static CircleSegment finishSelection() {
        synchronized (LOCK) {
            CircleSegment selectedSegment = hoveredSegment;
            visible = false;
            resetSelection();
            confirmedSegment = selectedSegment;
            return selectedSegment;
        }
    }

    static void render(GuiGraphicsExtractor graphics) {
        int currentSegmentCount;
        List<Identifier> currentTextures;
        CircleSegment currentHighlightedSegment;

        synchronized (LOCK) {
            if (!visible) return;

            currentSegmentCount = segmentCount;
            currentTextures = textures;
            currentHighlightedSegment = highlightedSegment;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;

        updateSelection(minecraft, centerX, centerY, currentSegmentCount);

        CircleSegment selectedSegment;
        synchronized (LOCK) {
            selectedSegment = hoveredSegment;
        }

        graphics.nextStratum();
        fillRing(graphics, centerX + 2, centerY + 2, INNER_RADIUS, OUTER_RADIUS, SHADOW_COLOR);
        fillSegments(graphics, centerX, centerY, currentSegmentCount, selectedSegment, currentHighlightedSegment);
        fillCircle(graphics, centerX, centerY, INNER_RADIUS, CENTER_COLOR);
        drawSeparators(graphics, centerX, centerY, currentSegmentCount);
        drawCircleOutline(graphics, centerX, centerY, INNER_RADIUS, 2, CENTER_BORDER_COLOR);
        drawCircleOutline(graphics, centerX, centerY, OUTER_RADIUS, 2, CENTER_BORDER_COLOR);
        renderTextures(graphics, centerX, centerY, currentSegmentCount, currentTextures);
    }

    private static void resetSelection() {
        hoveredSegment = CircleSegment.NONE;
        highlightedSegment = CircleSegment.NONE;
        wasLeftMouseDown = false;
    }

    private static void updateSelection(Minecraft minecraft, int centerX, int centerY, int segmentCount) {
        double mouseX = minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
        double mouseY = minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        CircleSegment hovered = getSegmentAt(dx, dy, segmentCount);
        boolean leftMouseDown = minecraft.mouseHandler.isLeftPressed();

        synchronized (LOCK) {
            hoveredSegment = hovered;

            if (leftMouseDown && !wasLeftMouseDown && hovered != CircleSegment.NONE) {
                confirmedSegment = hovered;
            }

            wasLeftMouseDown = leftMouseDown;
        }
    }

    private static CircleSegment getSegmentAt(double dx, double dy, int segmentCount) {
        double distanceSquared = dx * dx + dy * dy;
        if (distanceSquared < INNER_RADIUS * INNER_RADIUS) return CircleSegment.NONE;
        if (distanceSquared > OUTER_RADIUS * OUTER_RADIUS) return CircleSegment.NONE;

        return CircleSegment.fromIndex(segmentIndexForVector(dx, dy, segmentCount));
    }

    private static void fillSegments(GuiGraphicsExtractor graphics, int centerX, int centerY, int segmentCount, CircleSegment selectedSegment, CircleSegment highlightedSegment) {
        int innerSquared = INNER_RADIUS * INNER_RADIUS;
        int outerSquared = OUTER_RADIUS * OUTER_RADIUS;

        for (int y = -OUTER_RADIUS; y <= OUTER_RADIUS; y++) {
            int runStart = Integer.MIN_VALUE;
            int runColor = 0;

            for (int x = -OUTER_RADIUS; x <= OUTER_RADIUS; x++) {
                int distanceSquared = x * x + y * y;
                int color = 0;

                if (distanceSquared >= innerSquared && distanceSquared <= outerSquared) {
                    int segmentIndex = segmentIndexForVector(x, y, segmentCount);
                    color = segmentColor(segmentIndex, selectedSegment, highlightedSegment);
                }

                if (color != runColor) {
                    if (runColor != 0) {
                        graphics.fill(centerX + runStart, centerY + y, centerX + x, centerY + y + 1, runColor);
                    }

                    runStart = x;
                    runColor = color;
                }
            }

            if (runColor != 0) {
                graphics.fill(centerX + runStart, centerY + y, centerX + OUTER_RADIUS + 1, centerY + y + 1, runColor);
            }
        }
    }

    private static int segmentColor(int segmentIndex, CircleSegment selectedSegment, CircleSegment highlightedSegment) {
        if (selectedSegment.index() == segmentIndex) return SELECTED_COLOR;
        if (highlightedSegment.index() == segmentIndex) return ACTIVE_COLOR;
        return segmentIndex % 2 == 0 ? SEGMENT_COLOR : ALTERNATE_SEGMENT_COLOR;
    }

    private static void drawSeparators(GuiGraphicsExtractor graphics, int centerX, int centerY, int segmentCount) {
        if (segmentCount <= 1) return;

        double segmentAngle = Math.PI * 2.0D / segmentCount;

        for (int segment = 0; segment < segmentCount; segment++) {
            double angle = segment * segmentAngle;
            double sin = Math.sin(angle);
            double cos = Math.cos(angle);

            for (int radius = INNER_RADIUS + 1; radius < OUTER_RADIUS; radius++) {
                int x = centerX + (int) Math.round(sin * radius);
                int y = centerY - (int) Math.round(cos * radius);
                graphics.fill(x, y, x + 1, y + 1, SEPARATOR_COLOR);
            }
        }
    }

    private static void renderTextures(GuiGraphicsExtractor graphics, int centerX, int centerY, int segmentCount, List<Identifier> textures) {
        if (textures.isEmpty()) return;

        double segmentAngle = Math.PI * 2.0D / segmentCount;
        int iconRadius = (INNER_RADIUS + OUTER_RADIUS) / 2;

        for (int segment = 0; segment < segmentCount; segment++) {
            if (segment >= textures.size()) return;

            Identifier texture = textures.get(segment);
            if (texture == null) continue;

            double angle = (segment + 0.5D) * segmentAngle;
            int iconX = centerX + (int) Math.round(Math.sin(angle) * iconRadius) - ICON_SIZE / 2;
            int iconY = centerY - (int) Math.round(Math.cos(angle) * iconRadius) - ICON_SIZE / 2;
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, iconX, iconY, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        }
    }

    private static void fillCircle(GuiGraphicsExtractor graphics, int centerX, int centerY, int radius, int color) {
        int radiusSquared = radius * radius;

        for (int y = -radius; y <= radius; y++) {
            int x = (int) Math.floor(Math.sqrt(radiusSquared - y * y));
            graphics.fill(centerX - x, centerY + y, centerX + x + 1, centerY + y + 1, color);
        }
    }

    private static void fillRing(GuiGraphicsExtractor graphics, int centerX, int centerY, int innerRadius, int outerRadius, int color) {
        int innerSquared = innerRadius * innerRadius;
        int outerSquared = outerRadius * outerRadius;

        for (int y = -outerRadius; y <= outerRadius; y++) {
            int runStart = Integer.MIN_VALUE;
            boolean inRun = false;

            for (int x = -outerRadius; x <= outerRadius; x++) {
                int distanceSquared = x * x + y * y;
                boolean inRing = distanceSquared >= innerSquared && distanceSquared <= outerSquared;

                if (inRing && !inRun) {
                    runStart = x;
                    inRun = true;
                } else if (!inRing && inRun) {
                    graphics.fill(centerX + runStart, centerY + y, centerX + x, centerY + y + 1, color);
                    inRun = false;
                }
            }

            if (inRun) {
                graphics.fill(centerX + runStart, centerY + y, centerX + outerRadius + 1, centerY + y + 1, color);
            }
        }
    }

    private static void drawCircleOutline(GuiGraphicsExtractor graphics, int centerX, int centerY, int radius, int thickness, int color) {
        int halfThickness = Math.max(1, thickness) / 2;
        int innerRadius = Math.max(0, radius - halfThickness);
        int outerRadius = radius + halfThickness;
        fillRing(graphics, centerX, centerY, innerRadius, outerRadius, color);
    }

    private static int segmentIndexForVector(double dx, double dy, int segmentCount) {
        double angle = Math.atan2(dx, -dy);
        if (angle < 0.0D) angle += Math.PI * 2.0D;

        int index = (int) (angle / (Math.PI * 2.0D / segmentCount));
        return Mth.clamp(index, 0, segmentCount - 1);
    }

    private static int resolveSegmentCount(int segmentCount, List<Identifier> textures) {
        if (segmentCount > 0) return clampSegmentCount(segmentCount);
        if (textures != null && !textures.isEmpty()) return clampSegmentCount(textures.size());
        return CircleSegment.segmentCount();
    }

    private static int clampSegmentCount(int segmentCount) {
        return Mth.clamp(segmentCount, 1, CircleSegment.maxSegments());
    }

    private static CircleSegment resolveHighlightedSegment(CircleSegment segment, int segmentCount) {
        if (segment == null || segment == CircleSegment.NONE) return CircleSegment.NONE;
        if (segment.index() < 0 || segment.index() >= segmentCount) return CircleSegment.NONE;
        return segment;
    }
}
