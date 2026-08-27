package org.main.redstoneutils.client.macro;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.main.redstoneutils.client.ui.RedstoneUi;

import java.util.List;
import java.util.UUID;

final class MacroEditScreen extends Screen {

    private static final int PANEL_MIN_WIDTH = 300;
    private static final int PANEL_MAX_WIDTH = 580;
    private static final int PANEL_MIN_HEIGHT = 246;
    private static final int PANEL_MAX_HEIGHT = 430;
    private static final int FIELD_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 24;
    private static final int FOOTER_BUTTON_WIDTH = 90;

    private final Screen parent;
    private final String originalId;
    private final boolean editing;

    private MacroType type;
    private int keyCode;
    private boolean mouseButton;
    private int modifiers;
    private Macro.MacroTrigger trigger;
    private boolean enabled;
    private String categoryValue;
    private String nameValue;
    private String commandValue;
    private String aliasValue;
    private String error = "";
    private boolean capturingKey;

    private EditBox nameBox;
    private EditBox commandBox;
    private EditBox aliasBox;
    private EditBox categoryBox;

    private MacroEditScreen(Screen parent, Macro macro, MacroType fallbackType) {
        super(Component.translatable("screen.redstoneutils.macro_edit"));
        this.parent = parent;
        this.editing = macro != null;
        this.originalId = macro == null ? null : macro.id();
        this.type = macro == null ? fallbackType : macro.type();
        this.keyCode = macro == null ? Macro.UNBOUND_KEY : macro.keyCode();
        this.mouseButton = macro != null && macro.mouseButton();
        this.modifiers = macro == null ? 0 : macro.modifiers();
        this.trigger = macro == null ? Macro.MacroTrigger.PRESSED : macro.trigger();
        this.enabled = macro == null ? Macro.DEFAULT_ENABLED : macro.enabled();
        this.categoryValue = macro == null ? "General" : macro.category();
        this.nameValue = macro == null ? "" : macro.name();
        this.commandValue = macro == null ? "" : MacroCommandText.formatCommand(macro.command());
        this.aliasValue = macro == null ? "" : MacroCommandText.formatCommand(macro.alias());
    }

    static MacroEditScreen createKeybind(Screen parent) {
        return new MacroEditScreen(parent, null, MacroType.KEYBIND);
    }

    static MacroEditScreen createCommandAlias(Screen parent) {
        return new MacroEditScreen(parent, null, MacroType.COMMAND);
    }

    static MacroEditScreen edit(Screen parent, Macro macro) {
        return new MacroEditScreen(parent, macro, MacroType.KEYBIND);
    }

    @Override
    protected void init() {
        captureFieldValues();

        nameBox = addWidget(createBox(text("macro.redstoneutils.name"), nameValue, Macro.MAX_NAME_LENGTH));
        commandBox = addWidget(createBox(text("macro.redstoneutils.command"), commandValue, Macro.MAX_COMMAND_LENGTH));
        aliasBox = addWidget(createBox(text("macro.redstoneutils.alias"), aliasValue, Macro.MAX_ALIAS_LENGTH));
        categoryBox = addWidget(createBox(text("macro.redstoneutils.category"), categoryValue, 48));
        positionFields(layout(width, height));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        Layout layout = layout(graphics.guiWidth(), graphics.guiHeight());
        positionFields(layout);

        graphics.nextStratum();
        RedstoneUi.drawPanel(graphics, layout.x(), layout.y(), layout.width(), layout.height());
        drawHeader(graphics, layout);
        drawTypeSelector(graphics, layout, mouseX, mouseY);
        drawFields(graphics, layout, mouseX, mouseY, deltaTicks);
        drawFooter(graphics, layout, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Layout layout = layout(width, height);
        double mouseX = event.x();
        double mouseY = event.y();

        if (capturingKey) {
            keyCode = event.button();
            mouseButton = true;
            modifiers = event.modifiers() & MacroKeys.ALL_MODIFIERS;
            capturingKey = false;
            error = "";
            playClick();
            return true;
        }

        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return true;

        if (RedstoneUi.contains(mouseX, mouseY, layout.keybindTypeX(), layout.typeY(), layout.typeButtonWidth(), BUTTON_HEIGHT)) {
            setType(MacroType.KEYBIND);
            playClick();
            return true;
        }

        if (RedstoneUi.contains(mouseX, mouseY, layout.commandTypeX(), layout.typeY(), layout.typeButtonWidth(), BUTTON_HEIGHT)) {
            setType(MacroType.COMMAND);
            playClick();
            return true;
        }

        if (type == MacroType.KEYBIND && RedstoneUi.contains(mouseX, mouseY, layout.bindingX(), layout.bindingFieldY(), layout.columnWidth(), FIELD_HEIGHT)) {
            setFieldFocus(null);
            capturingKey = true;
            error = "";
            playClick();
            return true;
        }

        if (type == MacroType.KEYBIND && RedstoneUi.contains(mouseX, mouseY, layout.triggerX(), layout.triggerY(), layout.columnWidth(), BUTTON_HEIGHT)) {
            trigger = Macro.MacroTrigger.values()[(trigger.ordinal() + 1) % Macro.MacroTrigger.values().length];
            playClick();
            return true;
        }

        if (RedstoneUi.contains(mouseX, mouseY, layout.enabledX(), layout.triggerY(), layout.columnWidth(), BUTTON_HEIGHT)) {
            enabled = !enabled;
            playClick();
            return true;
        }

        if (RedstoneUi.contains(mouseX, mouseY, layout.saveX(), layout.footerButtonY(), layout.footerButtonWidth(), BUTTON_HEIGHT)) {
            saveAndClose();
            playClick();
            return true;
        }

        if (RedstoneUi.contains(mouseX, mouseY, layout.cancelX(), layout.footerButtonY(), layout.footerButtonWidth(), BUTTON_HEIGHT)) {
            returnToList();
            playClick();
            return true;
        }

        if (editing && RedstoneUi.contains(mouseX, mouseY, layout.deleteX(), layout.footerButtonY(), layout.footerButtonWidth(), BUTTON_HEIGHT)) {
            captureFieldValues();
            Minecraft.getInstance().gui.setScreen(new ConfirmScreen(confirmed -> {
                if (confirmed) MacroStore.delete(originalId);
                Minecraft.getInstance().gui.setScreen(confirmed ? parent : this);
            }, Component.translatable("macros.redstoneutils.delete_confirm.title"),
                    Component.translatable("macros.redstoneutils.delete_confirm.message", nameValue)));
            playClick();
            return true;
        }

        if (super.mouseClicked(event, doubleClick)) {
            capturingKey = false;
            return true;
        }

        setFieldFocus(null);
        capturingKey = false;
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();

        if (capturingKey) {
            captureKey(event);
            return true;
        }

        if (key == InputConstants.KEY_ESCAPE) {
            returnToList();
            return true;
        }

        if (key == InputConstants.KEY_TAB) {
            focusNext((event.modifiers() & InputConstants.MOD_SHIFT) != 0);
            return true;
        }

        if (super.keyPressed(event)) return true;

        if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
            saveAndClose();
            return true;
        }

        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        super.charTyped(event);
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
        String title = text(editing ? "macro.redstoneutils.edit_title" : "macro.redstoneutils.new_title");
        graphics.text(font, title, layout.x() + RedstoneUi.PANEL_PADDING, layout.y() + 8, RedstoneUi.TEXT_COLOR, false);

        String subtitle = error.isBlank()
                ? text("macro.redstoneutils.subtitle")
                : error;
        int subtitleColor = error.isBlank() ? RedstoneUi.MUTED_TEXT_COLOR : RedstoneUi.ERROR_TEXT_COLOR;
        RedstoneUi.drawFittedText(graphics, font, subtitle, layout.x() + RedstoneUi.PANEL_PADDING, layout.y() + 24, layout.width() - RedstoneUi.PANEL_PADDING * 2, subtitleColor);
    }

    private void drawTypeSelector(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        boolean keybindHovered = RedstoneUi.contains(mouseX, mouseY, layout.keybindTypeX(), layout.typeY(), layout.typeButtonWidth(), BUTTON_HEIGHT);
        boolean commandHovered = RedstoneUi.contains(mouseX, mouseY, layout.commandTypeX(), layout.typeY(), layout.typeButtonWidth(), BUTTON_HEIGHT);

        RedstoneUi.drawButton(graphics, font, text("macro.redstoneutils.keybind"), layout.keybindTypeX(), layout.typeY(), layout.typeButtonWidth(), BUTTON_HEIGHT, keybindHovered, type == MacroType.KEYBIND ? RedstoneUi.ButtonTone.ACTIVE : RedstoneUi.ButtonTone.NORMAL);
        RedstoneUi.drawButton(graphics, font, text("macro.redstoneutils.command_type"), layout.commandTypeX(), layout.typeY(), layout.typeButtonWidth(), BUTTON_HEIGHT, commandHovered, type == MacroType.COMMAND ? RedstoneUi.ButtonTone.ACTIVE : RedstoneUi.ButtonTone.NORMAL);
    }

    private void drawFields(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY, float deltaTicks) {
        drawLabel(graphics, text("macro.redstoneutils.name"), layout.nameX(), layout.nameLabelY());
        nameBox.extractRenderState(graphics, mouseX, mouseY, deltaTicks);

        drawLabel(graphics, text("macro.redstoneutils.command_to_run"), layout.commandX(), layout.commandLabelY());
        commandBox.extractRenderState(graphics, mouseX, mouseY, deltaTicks);

        if (type == MacroType.COMMAND) {
            drawLabel(graphics, text("macro.redstoneutils.alias"), layout.bindingX(), layout.bindingLabelY());
            aliasBox.extractRenderState(graphics, mouseX, mouseY, deltaTicks);
            if (layout.showHint()) {
                RedstoneUi.drawFittedText(graphics, font, text("macro.redstoneutils.alias_hint"), layout.bindingX(),
                        layout.hintY(), layout.columnWidth(), RedstoneUi.MUTED_TEXT_COLOR);
            }
        } else {
            drawLabel(graphics, text("macro.redstoneutils.binding"), layout.bindingX(), layout.bindingLabelY());
            boolean hovered = RedstoneUi.contains(mouseX, mouseY, layout.bindingX(), layout.bindingFieldY(), layout.columnWidth(), FIELD_HEIGHT);
            String label = capturingKey ? text("macro.redstoneutils.capture") : MacroKeys.displayName(keyCode, mouseButton, modifiers);
            RedstoneUi.drawButton(graphics, font, label, layout.bindingX(), layout.bindingFieldY(), layout.columnWidth(), FIELD_HEIGHT, hovered, capturingKey ? RedstoneUi.ButtonTone.ACTIVE : RedstoneUi.ButtonTone.NORMAL);
            if (layout.showHint()) {
                RedstoneUi.drawFittedText(graphics, font, text("macro.redstoneutils.capture_hint"), layout.bindingX(),
                        layout.hintY(), layout.columnWidth(), RedstoneUi.MUTED_TEXT_COLOR);
            }
        }

        drawLabel(graphics, text("macro.redstoneutils.category"), layout.categoryX(), layout.categoryLabelY());
        categoryBox.extractRenderState(graphics, mouseX, mouseY, deltaTicks);
        if (type == MacroType.KEYBIND) {
            boolean triggerHovered = RedstoneUi.contains(mouseX, mouseY, layout.triggerX(), layout.triggerY(), layout.columnWidth(), BUTTON_HEIGHT);
            RedstoneUi.drawButton(graphics, font, Component.translatable("macro.redstoneutils.trigger", text("enum.redstoneutils." + trigger.name().toLowerCase(java.util.Locale.ROOT))).getString(), layout.triggerX(), layout.triggerY(), layout.columnWidth(), BUTTON_HEIGHT, triggerHovered, RedstoneUi.ButtonTone.NORMAL);
        }
        boolean enabledHovered = RedstoneUi.contains(mouseX, mouseY, layout.enabledX(), layout.triggerY(), layout.columnWidth(), BUTTON_HEIGHT);
        RedstoneUi.drawButton(graphics, font, text(enabled ? "state.redstoneutils.enabled" : "state.redstoneutils.disabled"), layout.enabledX(), layout.triggerY(), layout.columnWidth(), BUTTON_HEIGHT, enabledHovered,
                enabled ? RedstoneUi.ButtonTone.ACTIVE : RedstoneUi.ButtonTone.NORMAL);
    }

    private void drawFooter(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        boolean saveHovered = RedstoneUi.contains(mouseX, mouseY, layout.saveX(), layout.footerButtonY(), layout.footerButtonWidth(), BUTTON_HEIGHT);
        boolean cancelHovered = RedstoneUi.contains(mouseX, mouseY, layout.cancelX(), layout.footerButtonY(), layout.footerButtonWidth(), BUTTON_HEIGHT);
        boolean deleteHovered = editing && RedstoneUi.contains(mouseX, mouseY, layout.deleteX(), layout.footerButtonY(), layout.footerButtonWidth(), BUTTON_HEIGHT);

        if (editing) {
            RedstoneUi.drawButton(graphics, font, text("macros.redstoneutils.delete"), layout.deleteX(), layout.footerButtonY(), layout.footerButtonWidth(), BUTTON_HEIGHT, deleteHovered, RedstoneUi.ButtonTone.DANGER);
        }
        RedstoneUi.drawButton(graphics, font, text("gui.cancel"), layout.cancelX(), layout.footerButtonY(), layout.footerButtonWidth(), BUTTON_HEIGHT, cancelHovered, RedstoneUi.ButtonTone.NORMAL);
        RedstoneUi.drawButton(graphics, font, text("macro.redstoneutils.save"), layout.saveX(), layout.footerButtonY(), layout.footerButtonWidth(), BUTTON_HEIGHT, saveHovered, RedstoneUi.ButtonTone.ACTIVE);
    }

    private void drawLabel(GuiGraphicsExtractor graphics, String label, int x, int y) {
        graphics.text(font, label, x, y, RedstoneUi.DETAIL_TEXT_COLOR, false);
    }

    private void saveAndClose() {
        captureFieldValues();
        String validationError = validate();
        if (!validationError.isBlank()) {
            error = validationError;
            return;
        }

        String id = originalId == null ? UUID.randomUUID().toString() : originalId;
        Macro macro = new Macro(
                id, type, nameValue, commandValue, keyCode, aliasValue,
                mouseButton, modifiers, trigger, enabled, categoryValue
        );
        MacroStore.upsert(macro);
        returnToList();
    }

    private String validate() {
        nameValue = nameValue.trim();
        commandValue = MacroCommandText.normalizeCommand(commandValue);
        aliasValue = MacroCommandText.normalizeAlias(aliasValue);

        if (nameValue.isBlank()) return text("macro.redstoneutils.error.name");
        if (commandValue.isBlank()) return text("macro.redstoneutils.error.command");

        if (type == MacroType.KEYBIND) {
            if (!MacroKeys.isBound(keyCode)) return text("macro.redstoneutils.error.binding");
            if (MacroStore.bindingExists(keyCode, mouseButton, modifiers, originalId)) return text("macro.redstoneutils.error.binding_used");
            return "";
        }

        if (!MacroCommandText.isValidAliasInput(aliasBox.getValue())) return text("macro.redstoneutils.error.alias");
        if (CommandCommand.isReservedAlias(aliasValue)) return Component.translatable("macro.redstoneutils.error.reserved", "/" + aliasValue).getString();
        if (MacroCommandText.normalizeAlias(commandValue).equals(aliasValue)) return text("macro.redstoneutils.error.self");
        if (MacroStore.aliasExists(aliasValue, originalId)) return text("macro.redstoneutils.error.alias_used");

        return "";
    }

    private void returnToList() {
        Minecraft.getInstance().gui.setScreen(parent == null ? new MacrosScreen() : parent);
    }

    private void setType(MacroType type) {
        if (type == null || this.type == type) return;
        captureFieldValues();
        this.type = type;
        capturingKey = false;
        error = "";
        setFieldFocus(null);
    }

    private void captureKey(KeyEvent event) {
        int key = event.key();
        if (key == InputConstants.KEY_ESCAPE || key == InputConstants.KEY_BACKSPACE || key == InputConstants.KEY_DELETE) {
            keyCode = Macro.UNBOUND_KEY;
            mouseButton = false;
            modifiers = 0;
        } else {
            keyCode = key;
            mouseButton = false;
            modifiers = event.modifiers() & MacroKeys.ALL_MODIFIERS;
        }

        capturingKey = false;
        error = "";
    }

    private void focusNext(boolean backwards) {
        List<EditBox> fields = visibleFields();
        if (fields.isEmpty()) return;

        int currentIndex = -1;
        for (int index = 0; index < fields.size(); index++) {
            if (fields.get(index).isFocused()) {
                currentIndex = index;
                break;
            }
        }

        int nextIndex = currentIndex < 0
                ? 0
                : Math.floorMod(currentIndex + (backwards ? -1 : 1), fields.size());
        setFieldFocus(fields.get(nextIndex));
    }

    private void setFieldFocus(EditBox focusedField) {
        setFocused((GuiEventListener) focusedField);
    }

    private List<EditBox> visibleFields() {
        return type == MacroType.COMMAND
                ? List.of(nameBox, commandBox, aliasBox, categoryBox)
                : List.of(nameBox, commandBox, categoryBox);
    }

    private List<EditBox> allFields() {
        return List.of(nameBox, commandBox, aliasBox, categoryBox);
    }

    private EditBox createBox(String label, String value, int maxLength) {
        EditBox box = new EditBox(font, Component.literal(label));
        box.setMaxLength(maxLength);
        box.setValue(value == null ? "" : value);
        box.setTextColor(RedstoneUi.TEXT_COLOR);
        box.setTextColorUneditable(RedstoneUi.MUTED_TEXT_COLOR);
        box.setResponder(ignored -> error = "");
        return box;
    }

    private void captureFieldValues() {
        if (nameBox != null) nameValue = nameBox.getValue();
        if (commandBox != null) commandValue = commandBox.getValue();
        if (aliasBox != null) aliasValue = aliasBox.getValue();
        if (categoryBox != null) categoryValue = categoryBox.getValue();
    }

    private void positionFields(Layout layout) {
        positionField(nameBox, layout.nameX(), layout.nameFieldY(), layout.columnWidth());
        positionField(commandBox, layout.commandX(), layout.commandFieldY(), layout.columnWidth());
        positionField(aliasBox, layout.bindingX(), layout.bindingFieldY(), layout.columnWidth());
        positionField(categoryBox, layout.categoryX(), layout.categoryFieldY(), layout.columnWidth());
        updateFieldVisibility();
    }

    private void positionField(EditBox field, int x, int y, int width) {
        if (field == null) return;

        field.setX(x);
        field.setY(y);
        field.setSize(width, FIELD_HEIGHT);
    }

    private void updateFieldVisibility() {
        if (nameBox != null) {
            nameBox.visible = true;
            nameBox.active = true;
        }
        if (commandBox != null) {
            commandBox.visible = true;
            commandBox.active = true;
        }
        if (aliasBox != null) {
            boolean aliasVisible = type == MacroType.COMMAND;
            aliasBox.setVisible(aliasVisible);
            aliasBox.active = aliasVisible;
        }
        if (categoryBox != null) {
            categoryBox.visible = true;
            categoryBox.active = true;
        }
    }

    private static Layout layout(int screenWidth, int screenHeight) {
        int width = Mth.clamp(screenWidth - 16, PANEL_MIN_WIDTH, PANEL_MAX_WIDTH);
        width = Math.min(width, Math.max(1, screenWidth - 8));
        int height = Mth.clamp(screenHeight - 16, PANEL_MIN_HEIGHT, PANEL_MAX_HEIGHT);
        height = Math.min(height, Math.max(1, screenHeight - 8));
        int x = (screenWidth - width) / 2;
        int y = (screenHeight - height) / 2;
        return new Layout(x, y, width, height);
    }

    private static void playClick() {
        AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
    }

    private static String text(String key) {
        return Component.translatable(key).getString();
    }

    private record Layout(int x, int y, int width, int height) {
        private int fieldX() {
            return x + RedstoneUi.PANEL_PADDING;
        }

        private int columnWidth() {
            return (width - RedstoneUi.PANEL_PADDING * 2 - RedstoneUi.GAP) / 2;
        }

        private int rightColumnX() {
            return fieldX() + columnWidth() + RedstoneUi.GAP;
        }

        private int typeY() {
            return y + 48;
        }

        private int keybindTypeX() {
            return fieldX();
        }

        private int commandTypeX() {
            return keybindTypeX() + typeButtonWidth() + RedstoneUi.GAP;
        }

        private int typeButtonWidth() {
            return columnWidth();
        }

        private int nameLabelY() {
            return y + 82;
        }

        private int nameFieldY() {
            return nameLabelY() + 12;
        }

        private int commandLabelY() {
            return y + 126;
        }

        private int commandFieldY() {
            return commandLabelY() + 12;
        }

        private int bindingLabelY() {
            return commandLabelY();
        }

        private int bindingFieldY() {
            return bindingLabelY() + 12;
        }

        private int categoryLabelY() {
            return nameLabelY();
        }

        private int categoryFieldY() {
            return categoryLabelY() + 12;
        }

        private int nameX() { return fieldX(); }
        private int commandX() { return fieldX(); }
        private int bindingX() { return rightColumnX(); }
        private int categoryX() { return rightColumnX(); }
        private int hintY() { return bindingFieldY() + FIELD_HEIGHT + 7; }
        private boolean showHint() { return height >= 260; }
        private int triggerX() { return fieldX(); }
        private int enabledX() { return rightColumnX(); }

        private int triggerY() {
            return showHint()
                    ? y + 184
                    : bindingFieldY() + FIELD_HEIGHT + 8;
        }

        private int footerButtonY() {
            return y + height - 36;
        }

        private int deleteX() {
            return fieldX();
        }

        private int footerButtonWidth() {
            return Math.min(FOOTER_BUTTON_WIDTH,
                    (width - RedstoneUi.PANEL_PADDING * 2 - RedstoneUi.GAP * 2) / 3);
        }

        private int saveX() {
            return x + width - RedstoneUi.PANEL_PADDING - footerButtonWidth();
        }

        private int cancelX() {
            return saveX() - RedstoneUi.GAP - footerButtonWidth();
        }
    }
}
