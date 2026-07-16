package org.main.redstoneutils.client.config;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.main.redstoneutils.client.autowire.AutoWireHandler;
import org.main.redstoneutils.client.autowire.WireType;
import org.main.redstoneutils.client.autowire.AutoWirePreviewOverlay;
import org.main.redstoneutils.client.sculk.SculkSensorOverlay;
import org.main.redstoneutils.client.ui.RedstoneMessages;
import org.main.redstoneutils.client.ui.RedstoneOverlay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ConfigScreen extends Screen {

    private static final int PANEL_MIN_WIDTH = 420;
    private static final int PANEL_MAX_WIDTH = 560;
    private static final int PANEL_MAX_HEIGHT = 420;
    private static final int PANEL_PADDING = 12;
    private static final int HEADER_HEIGHT = 68;
    private static final int FOOTER_HEIGHT = 38;
    private static final int ROW_HEIGHT = 82;
    private static final int HELP_ROW_HEIGHT = 76;
    private static final int TAB_WIDTH = 116;
    private static final int TAB_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 122;
    private static final int BUTTON_HEIGHT = 24;
    private static final int SCROLLBAR_WIDTH = 5;
    private static final int SCROLLBAR_GAP = 8;
    private static final int SCROLLBAR_MIN_THUMB_HEIGHT = 22;
    private static final int GAP = 8;
    private static final int CONTROL_RIGHT_INSET = 14;

    private static final int SHADOW_COLOR = 0xD116171A;
    private static final int PANEL_COLOR = 0xE62B2D31;
    private static final int PANEL_BORDER_COLOR = 0xF05B6068;
    private static final int PANEL_HIGHLIGHT_COLOR = 0xFF7B8088;
    private static final int ROW_COLOR = 0x8033363C;
    private static final int ROW_ALT_COLOR = 0x662B2D31;
    private static final int ROW_HOVER_COLOR = 0xA0454A52;
    private static final int BUTTON_COLOR = 0xE642454C;
    private static final int BUTTON_HOVER_COLOR = 0xE66A707A;
    private static final int BUTTON_DANGER_COLOR = 0xE65A3535;
    private static final int BUTTON_BORDER_COLOR = 0xF05B6068;
    private static final int SCROLLBAR_TRACK_COLOR = 0x6633363C;
    private static final int SCROLLBAR_THUMB_COLOR = 0xD07B8088;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int MUTED_TEXT_COLOR = 0xFFB8BEC8;
    private static final int DETAIL_TEXT_COLOR = 0xFFD5DAE2;

    private final List<ConfigOption> options = createOptions();
    private final List<HelpEntry> helpEntries = createHelpEntries();
    private Tab activeTab = Tab.SETTINGS;
    private double scrollOffset;
    private boolean draggingScrollbar;

    public ConfigScreen() {
        super(Component.literal("RedstoneUtils Config"));
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.gui.setScreen(new ConfigScreen()));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        Layout layout = layout(graphics.guiWidth(), graphics.guiHeight());
        scrollOffset = clampScroll(layout, scrollOffset);

        graphics.nextStratum();
        drawPanel(graphics, layout);
        graphics.enableScissor(layout.contentLeft(), layout.contentY(), layout.contentRight(), layout.contentY() + layout.contentHeight());
        if (activeTab == Tab.SETTINGS) {
            drawOptions(graphics, layout, mouseX, mouseY);
        } else {
            drawHelp(graphics, layout);
        }
        graphics.disableScissor();
        drawScrollbar(graphics, layout);
        drawHeader(graphics, layout);
        drawFooter(graphics, layout, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Layout layout = layout(width, height);
        double mouseX = event.x();
        double mouseY = event.y();

        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            if (isScrollbarVisible(layout) && contains(mouseX, mouseY, layout.scrollbarX() - 2, layout.contentY(), SCROLLBAR_WIDTH + 4, layout.contentHeight())) {
                draggingScrollbar = true;
                scrollToMouse(layout, mouseY);
                AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
                return true;
            }

            if (selectTab(layout, mouseX, mouseY)) {
                AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
                return true;
            }

            if (contains(mouseX, mouseY, layout.doneX(), layout.footerButtonY(), BUTTON_WIDTH, BUTTON_HEIGHT)) {
                onClose();
                AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
                return true;
            }
            if (activeTab == Tab.SETTINGS && contains(mouseX, mouseY, layout.resetX(), layout.footerButtonY(), BUTTON_WIDTH, BUTTON_HEIGHT)) {
                resetDefaults();
                AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
                return true;
            }

            int optionIndex = activeTab == Tab.SETTINGS ? optionIndexAt(layout, mouseX, mouseY) : -1;
            if (optionIndex >= 0) {
                options.get(optionIndex).next().run();
                AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
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

        scrollOffset = clampScroll(layout, scrollOffset - scrollY * rowHeight());
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
            scrollOffset = clampScroll(layout(width, height), scrollOffset - rowHeight());
            return true;
        }
        if (key == InputConstants.KEY_DOWN) {
            scrollOffset = clampScroll(layout(width, height), scrollOffset + rowHeight());
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

    private void drawPanel(GuiGraphicsExtractor graphics, Layout layout) {
        graphics.fill(layout.x() + 3, layout.y() + 3, layout.x() + layout.width() + 3, layout.y() + layout.height() + 3, SHADOW_COLOR);
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + layout.height(), PANEL_COLOR);
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + 1, PANEL_HIGHLIGHT_COLOR);
        graphics.outline(layout.x(), layout.y(), layout.width(), layout.height(), PANEL_BORDER_COLOR);
    }

    private void drawHeader(GuiGraphicsExtractor graphics, Layout layout) {
        graphics.fill(layout.x() + 1, layout.y() + 1, layout.x() + layout.width() - 1, layout.contentY(), PANEL_COLOR);
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + 1, PANEL_HIGHLIGHT_COLOR);
        graphics.text(font, "RedstoneUtils Config", layout.x() + PANEL_PADDING, layout.y() + 8, TEXT_COLOR, false);
        String subtitle = activeTab == Tab.SETTINGS
                ? "Changes are saved immediately."
                : "Commands, keybinds, and features at a glance.";
        graphics.text(font, subtitle, layout.x() + PANEL_PADDING, layout.y() + 24, MUTED_TEXT_COLOR, false);

        drawTab(graphics, "Settings", activeTab == Tab.SETTINGS, layout.settingsTabX(), layout.tabY());
        drawTab(graphics, "Help", activeTab == Tab.HELP, layout.helpTabX(), layout.tabY());
    }

    private void drawOptions(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        int contentTop = layout.contentY();
        int contentBottom = layout.contentY() + layout.contentHeight();
        int rowWidth = layout.contentWidth();

        for (int index = 0; index < options.size(); index++) {
            int rowY = contentTop + index * ROW_HEIGHT - (int) Math.round(scrollOffset);
            if (rowY + ROW_HEIGHT < contentTop || rowY > contentBottom) {
                continue;
            }

            ConfigOption option = options.get(index);
            int rowX = layout.contentLeft();
            int rowColor = index % 2 == 0 ? ROW_COLOR : ROW_ALT_COLOR;
            boolean hovered = contains(mouseX, mouseY, layout.controlX(), rowY + GAP, BUTTON_WIDTH, BUTTON_HEIGHT);
            graphics.fill(rowX, rowY + 2, rowX + rowWidth, rowY + ROW_HEIGHT - 4, hovered ? ROW_HOVER_COLOR : rowColor);
            graphics.outline(rowX, rowY + 2, rowWidth, ROW_HEIGHT - 6, PANEL_BORDER_COLOR);

            int textX = rowX + 10;
            int textWidth = layout.controlX() - textX - GAP;
            graphics.text(font, option.title(), textX, rowY + 10, TEXT_COLOR, false);
            int nextY = drawWrappedText(graphics, option.description(), textX, rowY + 25, textWidth, 2, MUTED_TEXT_COLOR);
            drawWrappedText(graphics, option.valueDescription().get(), textX, nextY + 2, textWidth, 2, DETAIL_TEXT_COLOR);

            drawButton(graphics, option.valueLabel().get(), layout.controlX(), rowY + GAP, BUTTON_WIDTH, BUTTON_HEIGHT, hovered, false);
        }
    }

    private void drawHelp(GuiGraphicsExtractor graphics, Layout layout) {
        int contentTop = layout.contentY();
        int contentBottom = layout.contentY() + layout.contentHeight();
        int rowWidth = layout.contentWidth();

        for (int index = 0; index < helpEntries.size(); index++) {
            int rowY = contentTop + index * HELP_ROW_HEIGHT - (int) Math.round(scrollOffset);
            if (rowY + HELP_ROW_HEIGHT < contentTop || rowY > contentBottom) {
                continue;
            }

            HelpEntry entry = helpEntries.get(index);
            int rowX = layout.contentLeft();
            int rowColor = index % 2 == 0 ? ROW_COLOR : ROW_ALT_COLOR;
            graphics.fill(rowX, rowY + 2, rowX + rowWidth, rowY + HELP_ROW_HEIGHT - 4, rowColor);
            graphics.outline(rowX, rowY + 2, rowWidth, HELP_ROW_HEIGHT - 6, PANEL_BORDER_COLOR);

            int textX = rowX + 10;
            int textWidth = rowWidth - 20;
            graphics.text(font, entry.title(), textX, rowY + 9, TEXT_COLOR, false);
            int nextY = drawWrappedText(graphics, entry.usage(), textX, rowY + 23, textWidth, 1, DETAIL_TEXT_COLOR);
            drawWrappedText(graphics, entry.description(), textX, nextY + 3, textWidth, 3, MUTED_TEXT_COLOR);
        }
    }

    private void drawFooter(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        int y = layout.footerButtonY();
        graphics.fill(layout.x() + 1, layout.contentY() + layout.contentHeight(), layout.x() + layout.width() - 1, layout.y() + layout.height() - 1, PANEL_COLOR);
        boolean resetHovered = activeTab == Tab.SETTINGS && contains(mouseX, mouseY, layout.resetX(), y, BUTTON_WIDTH, BUTTON_HEIGHT);
        boolean doneHovered = contains(mouseX, mouseY, layout.doneX(), y, BUTTON_WIDTH, BUTTON_HEIGHT);
        if (activeTab == Tab.SETTINGS) {
            drawButton(graphics, "Defaults", layout.resetX(), y, BUTTON_WIDTH, BUTTON_HEIGHT, resetHovered, true);
        }
        drawButton(graphics, "Done", layout.doneX(), y, BUTTON_WIDTH, BUTTON_HEIGHT, doneHovered, false);
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics, Layout layout) {
        if (!isScrollbarVisible(layout)) return;

        int trackX = layout.scrollbarX();
        int trackY = layout.contentY() + 4;
        int trackHeight = layout.contentHeight() - 8;
        int thumbHeight = scrollbarThumbHeight(layout, trackHeight);
        int thumbY = scrollbarThumbY(layout, trackY, trackHeight, thumbHeight);

        graphics.fill(trackX, trackY, trackX + SCROLLBAR_WIDTH, trackY + trackHeight, SCROLLBAR_TRACK_COLOR);
        graphics.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbHeight, SCROLLBAR_THUMB_COLOR);
    }

    private void drawTab(GuiGraphicsExtractor graphics, String label, boolean active, int x, int y) {
        int color = active ? BUTTON_HOVER_COLOR : BUTTON_COLOR;
        graphics.fill(x, y, x + TAB_WIDTH, y + TAB_HEIGHT, color);
        graphics.outline(x, y, TAB_WIDTH, TAB_HEIGHT, BUTTON_BORDER_COLOR);
        graphics.centeredText(font, label, x + TAB_WIDTH / 2, y + (TAB_HEIGHT - font.lineHeight) / 2, TEXT_COLOR);
    }

    private void drawButton(GuiGraphicsExtractor graphics, String label, int x, int y, int width, int height, boolean hovered, boolean danger) {
        int color = hovered ? BUTTON_HOVER_COLOR : (danger ? BUTTON_DANGER_COLOR : BUTTON_COLOR);
        graphics.fill(x, y, x + width, y + height, color);
        graphics.outline(x, y, width, height, BUTTON_BORDER_COLOR);
        graphics.centeredText(font, label, x + width / 2, y + (height - font.lineHeight) / 2, TEXT_COLOR);
    }

    private int drawWrappedText(GuiGraphicsExtractor graphics, String text, int x, int y, int maxWidth, int maxLines, int color) {
        List<String> lines = wrap(font, text, maxWidth, maxLines);
        for (int index = 0; index < lines.size(); index++) {
            graphics.text(font, lines.get(index), x, y + index * (font.lineHeight + 1), color, false);
        }

        return y + lines.size() * (font.lineHeight + 1);
    }

    private int optionIndexAt(Layout layout, double mouseX, double mouseY) {
        if (!contains(mouseX, mouseY, layout.controlX(), layout.contentY(), BUTTON_WIDTH, layout.contentHeight())) {
            return -1;
        }

        int index = (int) ((mouseY - layout.contentY() + scrollOffset) / ROW_HEIGHT);
        if (index < 0 || index >= options.size()) return -1;

        int rowY = layout.contentY() + index * ROW_HEIGHT - (int) Math.round(scrollOffset);
        if (!contains(mouseX, mouseY, layout.controlX(), rowY + GAP, BUTTON_WIDTH, BUTTON_HEIGHT)) {
            return -1;
        }

        return index;
    }

    private boolean selectTab(Layout layout, double mouseX, double mouseY) {
        if (contains(mouseX, mouseY, layout.settingsTabX(), layout.tabY(), TAB_WIDTH, TAB_HEIGHT)) {
            setActiveTab(Tab.SETTINGS);
            return true;
        }
        if (contains(mouseX, mouseY, layout.helpTabX(), layout.tabY(), TAB_WIDTH, TAB_HEIGHT)) {
            setActiveTab(Tab.HELP);
            return true;
        }

        return false;
    }

    private void setActiveTab(Tab tab) {
        if (tab == null || tab == activeTab) return;
        activeTab = tab;
        scrollOffset = 0.0D;
        draggingScrollbar = false;
    }

    private void resetDefaults() {
        RedstoneUtilsConfig.resetToDefaults();
        RedstoneOverlay.setVisible(RedstoneUtilsConfig.isHudOverlayVisible());
        AutoWirePreviewOverlay.setVisible(RedstoneUtilsConfig.isWirePreviewOverlayVisible());
        SculkSensorOverlay.setVisible(RedstoneUtilsConfig.isSculkOverlayVisible());
        AutoWireHandler.setActiveWireType(RedstoneUtilsConfig.getActiveWireType());
        RedstoneMessages.setDefaultTarget(RedstoneUtilsConfig.getMessageTarget());
        SculkSensorOverlay.requestRefresh();
        RedstoneMessages.popup("Config reset");
    }

    private static Layout layout(int screenWidth, int screenHeight) {
        int width = Mth.clamp(screenWidth - 24, PANEL_MIN_WIDTH, PANEL_MAX_WIDTH);
        int height = Mth.clamp(screenHeight - 24, 300, PANEL_MAX_HEIGHT);
        int x = (screenWidth - width) / 2;
        int y = (screenHeight - height) / 2;
        int contentY = y + HEADER_HEIGHT;
        int contentHeight = height - HEADER_HEIGHT - FOOTER_HEIGHT;
        int controlX = x + width - PANEL_PADDING - CONTROL_RIGHT_INSET - SCROLLBAR_WIDTH - SCROLLBAR_GAP - BUTTON_WIDTH;
        int footerButtonY = y + height - FOOTER_HEIGHT + 7;

        return new Layout(x, y, width, height, contentY, contentHeight, controlX, footerButtonY);
    }

    private double clampScroll(Layout layout, double value) {
        return Mth.clamp(value, 0.0D, Math.max(0, contentHeight() - layout.contentHeight()));
    }

    private double contentHeight() {
        return activeTab == Tab.SETTINGS ? options.size() * ROW_HEIGHT : helpEntries.size() * HELP_ROW_HEIGHT;
    }

    private int rowHeight() {
        return activeTab == Tab.SETTINGS ? ROW_HEIGHT : HELP_ROW_HEIGHT;
    }

    private boolean isScrollbarVisible(Layout layout) {
        return contentHeight() > layout.contentHeight();
    }

    private int scrollbarThumbHeight(Layout layout, int trackHeight) {
        return Math.max(SCROLLBAR_MIN_THUMB_HEIGHT, (int) Math.round(trackHeight * layout.contentHeight() / contentHeight()));
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

    private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static List<String> wrap(Font font, String text, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank() || maxLines <= 0) return lines;

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
            line.append(word);
        }

        if (!line.isEmpty() && lines.size() < maxLines) {
            lines.add(line.toString());
        }

        return lines;
    }

    private static List<String> finishWrappedLines(Font font, List<String> lines, int maxWidth) {
        int lastIndex = lines.size() - 1;
        String suffix = "...";
        String line = lines.get(lastIndex);
        if (font.width(line + suffix) <= maxWidth) {
            lines.set(lastIndex, line + suffix);
            return lines;
        }

        lines.set(lastIndex, font.plainSubstrByWidth(line, Math.max(0, maxWidth - font.width(suffix))) + suffix);
        return lines;
    }

    private static List<ConfigOption> createOptions() {
        List<ConfigOption> options = new ArrayList<>();

        options.add(toggle(
                "HUD Overlay",
                "Shows or hides the RedstoneUtils HUD layer.",
                "Controls popups and the wire selection wheel. The config screen remains usable either way.",
                RedstoneUtilsConfig::isHudOverlayVisible,
                RedstoneOverlay::setVisible
        ));
        options.add(choice(
                "Active Wire Mode",
                "Controls what is automatically placed above a support block after you place it.",
                Arrays.stream(WireType.values())
                        .map(wireType -> new OptionValue<>(wireType, wireType.getDisplayName(), wireTypeDescription(wireType)))
                        .toList(),
                AutoWireHandler::getActiveWireType,
                AutoWireHandler::setActiveWireType
        ));
        options.add(toggle(
                "Wire Preview",
                "Shows a translucent preview before placement.",
                "The preview includes the support block you are placing and the AutoWire result above it.",
                AutoWirePreviewOverlay::isVisible,
                AutoWirePreviewOverlay::setVisible
        ));
        options.add(toggle(
                "Sculk Overlay",
                "Shows sculk sensor ranges in the world.",
                "Handles normal and calibrated sensors, including blocks that occlude vibration signals.",
                SculkSensorOverlay::isVisible,
                SculkSensorOverlay::setVisible
        ));
        options.add(choice(
                "Sculk Search Distance",
                "Controls how far around you the overlay searches for sensors.",
                List.of(
                        new OptionValue<>(32, "32 Blocks", "Very light on performance, but only covers nearby sensors."),
                        new OptionValue<>(64, "64 Blocks", "Good compromise for regular use."),
                        new OptionValue<>(96, "96 Blocks", "Default: covers larger builds without scanning too much."),
                        new OptionValue<>(128, "128 Blocks", "More range with higher chunk scanning cost."),
                        new OptionValue<>(192, "192 Blocks", "For large tests only; this can become noticeably heavier.")
                ),
                RedstoneUtilsConfig::getSculkSensorSearchDistance,
                value -> {
                    RedstoneUtilsConfig.setSculkSensorSearchDistance(value);
                    SculkSensorOverlay.requestRefresh();
                }
        ));
        options.add(choice(
                "Sculk Update Interval",
                "Controls how often the sculk overlay geometry is rebuilt.",
                List.of(
                        new OptionValue<>(1, "1 Tick", "Very responsive, but the most expensive option."),
                        new OptionValue<>(5, "5 Ticks", "Default: responsive enough without rebuilding constantly."),
                        new OptionValue<>(10, "10 Ticks", "Lighter for large sensor fields."),
                        new OptionValue<>(20, "20 Ticks", "Once per second; useful for mostly static analysis.")
                ),
                RedstoneUtilsConfig::getSculkRebuildIntervalTicks,
                value -> {
                    RedstoneUtilsConfig.setSculkRebuildIntervalTicks(value);
                    SculkSensorOverlay.requestRefresh();
                }
        ));
        options.add(choice(
                "Teleport Range",
                "Maximum raycast distance for the teleport keybind.",
                List.of(
                        new OptionValue<>(25.0D, "25 Blocks", "Precise short jumps."),
                        new OptionValue<>(50.0D, "50 Blocks", "Short build and debug distances."),
                        new OptionValue<>(100.0D, "100 Blocks", "Default range."),
                        new OptionValue<>(200.0D, "200 Blocks", "Longer jumps for large test worlds."),
                        new OptionValue<>(500.0D, "500 Blocks", "Very long range; uses a /tp command on multiplayer servers.")
                ),
                RedstoneUtilsConfig::getTeleportMaxRange,
                RedstoneUtilsConfig::setTeleportMaxRange
        ));
        options.add(choice(
                "Feedback Output",
                "Controls where RedstoneUtils feedback messages are shown.",
                Arrays.stream(RedstoneMessages.MessageTarget.values())
                        .map(target -> new OptionValue<>(target, messageTargetName(target), messageTargetDescription(target)))
                        .toList(),
                RedstoneMessages::getDefaultTarget,
                RedstoneMessages::setDefaultTarget
        ));

        return options;
    }

    private static List<HelpEntry> createHelpEntries() {
        return List.of(
                help("Open Config", "/redstone_utils config", "Opens this screen. Use the Settings tab for global mod options and the Help tab for this overview."),
                help("Macros", "/redstone_utils macros", "Opens the macro editor. It can bind commands to keys or shorten commands into custom aliases."),
                help("Wire Preview Overlay", "/overlay wire [true|false]", "Toggles the translucent AutoWire preview. With true or false, the state is set directly."),
                help("Sculk Overlay", "/overlay sculk [true|false]", "Shows or hides calculated sculk sensor ranges. Search distance and update rate are configured in Settings."),
                help("All Overlays", "/overlay all [true|false]", "Toggles the HUD layer, wire preview, and sculk overlay together. /overlay without a subcommand also toggles all overlays."),
                help("Calculator", "/calc", "Opens the ingame calculator. It supports basic operators, parentheses, percent, powers, sqrt, and ans for the last result."),
                help("Signal Barrel", "/signal <0-15>", "Gives you a barrel that outputs the selected comparator signal strength."),
                help("Optimal Signal Block Item", "/signal <0-15> optimal", "Gives a compact block item for the selected comparator signal, such as a composter, cake, lectern, or respawn anchor."),
                help("Specific Signal Block", "/signal <0-15> block <type>", "Creates a specific signal block. Supported examples include barrel, chest, shulker_box, hopper, lectern, crafter, composter, cake, beehive, and cauldron."),
                help("Set Container Content", "/set-content <amount>", "Sets the targeted container with a setblock command so it contains the requested amount of the item in your main hand."),
                help("Set Container Signal", "/set-signal <0-15>", "Fills the targeted container so a comparator reads the requested signal strength. Uses the item in your main hand."),
                help("Teleport Keybind", "Keybind: Teleport to targeted block", "Teleports you to the block you are looking at, or forward to the maximum configured range."),
                help("Wire Menu Keybind", "Keybind: Open wire menu", "Opens the radial wire menu. Releasing the key activates the currently selected AutoWire mode."),
                help("AutoWire Modes", "None, Normal, Auto, Fast Auto, Only Repeaters, Only Comparators, Fast Comparators", "Controls what is automatically placed above each support block. The Settings tab explains the currently selected mode."),
                help("Feedback Output", "Config: Feedback Output", "Controls whether RedstoneUtils feedback appears as a popup, chat message, action bar message, or a combination.")
        );
    }

    private static HelpEntry help(String title, String usage, String description) {
        return new HelpEntry(title, usage, description);
    }

    private static ConfigOption toggle(String title, String description, String valueDescription, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return new ConfigOption(
                title,
                description,
                () -> getter.get() ? "On" : "Off",
                () -> valueDescription,
                () -> setter.accept(!getter.get())
        );
    }

    private static <T> ConfigOption choice(String title, String description, List<OptionValue<T>> values, Supplier<T> getter, Consumer<T> setter) {
        return new ConfigOption(
                title,
                description,
                () -> labelFor(values, getter.get()),
                () -> descriptionFor(values, getter.get()),
                () -> cycle(values, getter, setter)
        );
    }

    private static <T> void cycle(List<OptionValue<T>> values, Supplier<T> getter, Consumer<T> setter) {
        if (values.isEmpty()) return;

        T current = getter.get();
        int currentIndex = 0;
        for (int index = 0; index < values.size(); index++) {
            if (values.get(index).value().equals(current)) {
                currentIndex = index;
                break;
            }
        }

        int nextIndex = (currentIndex + 1) % values.size();
        setter.accept(values.get(nextIndex).value());
    }

    private static <T> String labelFor(List<OptionValue<T>> values, T value) {
        for (OptionValue<T> option : values) {
            if (option.value().equals(value)) return option.label();
        }

        return values.isEmpty() ? "" : values.getFirst().label();
    }

    private static <T> String descriptionFor(List<OptionValue<T>> values, T value) {
        for (OptionValue<T> option : values) {
            if (option.value().equals(value)) return option.description();
        }

        return values.isEmpty() ? "" : values.getFirst().description();
    }

    private static String wireTypeDescription(WireType wireType) {
        return switch (wireType) {
            case NONE -> "AutoWire is disabled and does not change your placements.";
            case NORMAL -> "Places redstone dust on top of every newly placed support block.";
            case AUTO -> "Places dust and inserts repeaters when the signal can no longer safely continue.";
            case FAST_AUTO -> "Builds fast booster steps with a block, repeater, and redstone when the signal is nearly depleted.";
            case ONLY_REPEATERS -> "Places a repeater on every support block in the travel direction.";
            case ONLY_COMPARATORS -> "Places a comparator on every support block in the travel direction.";
            case FAST_COMPARATORS -> "Repeatedly builds block, comparator, block, and redstone steps for fast comparator chains.";
        };
    }

    private static String messageTargetName(RedstoneMessages.MessageTarget target) {
        return switch (target) {
            case POPUP -> "Popup";
            case CHAT -> "Chat";
            case ACTION_BAR -> "Action Bar";
            case POPUP_AND_CHAT -> "Popup + Chat";
            case POPUP_AND_ACTION_BAR -> "Popup + Action";
        };
    }

    private static String messageTargetDescription(RedstoneMessages.MessageTarget target) {
        return switch (target) {
            case POPUP -> "Small RedstoneUtils popup in the top-left corner.";
            case CHAT -> "Writes feedback messages to chat.";
            case ACTION_BAR -> "Shows feedback above the hotbar.";
            case POPUP_AND_CHAT -> "Shows a popup and also writes to chat.";
            case POPUP_AND_ACTION_BAR -> "Shows a popup and an action bar message at the same time.";
        };
    }

    private record ConfigOption(String title, String description, Supplier<String> valueLabel,
                                Supplier<String> valueDescription, Runnable next) {
    }

    private record HelpEntry(String title, String usage, String description) {
    }

    private record OptionValue<T>(T value, String label, String description) {
    }

    private enum Tab {
        SETTINGS,
        HELP
    }

    private record Layout(int x, int y, int width, int height, int contentY, int contentHeight,
                          int controlX, int footerButtonY) {
        private int tabY() {
            return y + 42;
        }

        private int settingsTabX() {
            return x + PANEL_PADDING;
        }

        private int helpTabX() {
            return settingsTabX() + TAB_WIDTH + GAP;
        }

        private int contentLeft() {
            return x + PANEL_PADDING;
        }

        private int contentRight() {
            return scrollbarX() - SCROLLBAR_GAP;
        }

        private int contentWidth() {
            return contentRight() - contentLeft();
        }

        private int scrollbarX() {
            return x + width - PANEL_PADDING - SCROLLBAR_WIDTH;
        }

        private int resetX() {
            return x + PANEL_PADDING;
        }

        private int doneX() {
            return x + width - PANEL_PADDING - CONTROL_RIGHT_INSET - BUTTON_WIDTH;
        }
    }
}
