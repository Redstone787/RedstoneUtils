package org.main.redstoneutils.client.macro;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.main.redstoneutils.client.ui.RedstoneUi;

import java.util.List;
import java.util.UUID;

final class MacroEditScreen extends Screen {

    private static final int PANEL_MIN_WIDTH = 430;
    private static final int PANEL_MAX_WIDTH = 580;
    private static final int PANEL_MIN_HEIGHT = 310;
    private static final int PANEL_MAX_HEIGHT = 360;
    private static final int FIELD_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 24;
    private static final int TYPE_BUTTON_WIDTH = 126;
    private static final int FOOTER_BUTTON_WIDTH = 90;

    private final Screen parent;
    private final String originalId;
    private final boolean editing;

    private MacroType type;
    private int keyCode;
    private String nameValue;
    private String commandValue;
    private String aliasValue;
    private String error = "";
    private boolean capturingKey;

    private EditBox nameBox;
    private EditBox commandBox;
    private EditBox aliasBox;

    private MacroEditScreen(Screen parent, Macro macro, MacroType fallbackType) {
        super(Component.literal("RedstoneUtils Macro"));
        this.parent = parent;
        this.editing = macro != null;
        this.originalId = macro == null ? null : macro.id();
        this.type = macro == null ? fallbackType : macro.type();
        this.keyCode = macro == null ? Macro.UNBOUND_KEY : macro.keyCode();
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

        nameBox = addWidget(createBox("Name", nameValue, 80));
        commandBox = addWidget(createBox("Command", commandValue, 512));
        aliasBox = addWidget(createBox("New command", aliasValue, 64));
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

        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return true;

        if (RedstoneUi.contains(mouseX, mouseY, layout.keybindTypeX(), layout.typeY(), TYPE_BUTTON_WIDTH, BUTTON_HEIGHT)) {
            setType(MacroType.KEYBIND);
            playClick();
            return true;
        }

        if (RedstoneUi.contains(mouseX, mouseY, layout.commandTypeX(), layout.typeY(), TYPE_BUTTON_WIDTH, BUTTON_HEIGHT)) {
            setType(MacroType.COMMAND);
            playClick();
            return true;
        }

        if (type == MacroType.KEYBIND && RedstoneUi.contains(mouseX, mouseY, layout.fieldX(), layout.bindingFieldY(), layout.fieldWidth(), FIELD_HEIGHT)) {
            setFieldFocus(null);
            capturingKey = true;
            error = "";
            playClick();
            return true;
        }

        if (RedstoneUi.contains(mouseX, mouseY, layout.saveX(), layout.footerButtonY(), FOOTER_BUTTON_WIDTH, BUTTON_HEIGHT)) {
            saveAndClose();
            playClick();
            return true;
        }

        if (RedstoneUi.contains(mouseX, mouseY, layout.cancelX(), layout.footerButtonY(), FOOTER_BUTTON_WIDTH, BUTTON_HEIGHT)) {
            returnToList();
            playClick();
            return true;
        }

        if (editing && RedstoneUi.contains(mouseX, mouseY, layout.deleteX(), layout.footerButtonY(), FOOTER_BUTTON_WIDTH, BUTTON_HEIGHT)) {
            MacroStore.delete(originalId);
            returnToList();
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
            captureKey(key);
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
        String title = editing ? "Edit Macro" : "New Macro";
        graphics.text(font, title, layout.x() + RedstoneUi.PANEL_PADDING, layout.y() + 8, RedstoneUi.TEXT_COLOR, false);

        String subtitle = error.isBlank()
                ? "Commands may be entered with or without a leading slash."
                : error;
        int subtitleColor = error.isBlank() ? RedstoneUi.MUTED_TEXT_COLOR : RedstoneUi.ERROR_TEXT_COLOR;
        RedstoneUi.drawFittedText(graphics, font, subtitle, layout.x() + RedstoneUi.PANEL_PADDING, layout.y() + 24, layout.width() - RedstoneUi.PANEL_PADDING * 2, subtitleColor);
    }

    private void drawTypeSelector(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        boolean keybindHovered = RedstoneUi.contains(mouseX, mouseY, layout.keybindTypeX(), layout.typeY(), TYPE_BUTTON_WIDTH, BUTTON_HEIGHT);
        boolean commandHovered = RedstoneUi.contains(mouseX, mouseY, layout.commandTypeX(), layout.typeY(), TYPE_BUTTON_WIDTH, BUTTON_HEIGHT);

        RedstoneUi.drawButton(graphics, font, "Keybind", layout.keybindTypeX(), layout.typeY(), TYPE_BUTTON_WIDTH, BUTTON_HEIGHT, keybindHovered, type == MacroType.KEYBIND ? RedstoneUi.ButtonTone.ACTIVE : RedstoneUi.ButtonTone.NORMAL);
        RedstoneUi.drawButton(graphics, font, "Command", layout.commandTypeX(), layout.typeY(), TYPE_BUTTON_WIDTH, BUTTON_HEIGHT, commandHovered, type == MacroType.COMMAND ? RedstoneUi.ButtonTone.ACTIVE : RedstoneUi.ButtonTone.NORMAL);
    }

    private void drawFields(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY, float deltaTicks) {
        drawLabel(graphics, "Name", layout.fieldX(), layout.nameLabelY());
        nameBox.extractRenderState(graphics, mouseX, mouseY, deltaTicks);

        drawLabel(graphics, "Command to run", layout.fieldX(), layout.commandLabelY());
        commandBox.extractRenderState(graphics, mouseX, mouseY, deltaTicks);

        if (type == MacroType.COMMAND) {
            drawLabel(graphics, "New command", layout.fieldX(), layout.bindingLabelY());
            aliasBox.extractRenderState(graphics, mouseX, mouseY, deltaTicks);
            graphics.text(font, "One command word, for example /wire.", layout.fieldX(), layout.bindingFieldY() + FIELD_HEIGHT + 7, RedstoneUi.MUTED_TEXT_COLOR, false);
        } else {
            drawLabel(graphics, "Keybinding", layout.fieldX(), layout.bindingLabelY());
            boolean hovered = RedstoneUi.contains(mouseX, mouseY, layout.fieldX(), layout.bindingFieldY(), layout.fieldWidth(), FIELD_HEIGHT);
            String label = capturingKey ? "Press a key..." : MacroKeys.displayName(keyCode);
            RedstoneUi.drawButton(graphics, font, label, layout.fieldX(), layout.bindingFieldY(), layout.fieldWidth(), FIELD_HEIGHT, hovered, capturingKey ? RedstoneUi.ButtonTone.ACTIVE : RedstoneUi.ButtonTone.NORMAL);
            graphics.text(font, "Escape, Backspace, or Delete clears the binding while capturing.", layout.fieldX(), layout.bindingFieldY() + FIELD_HEIGHT + 7, RedstoneUi.MUTED_TEXT_COLOR, false);
        }
    }

    private void drawFooter(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        boolean saveHovered = RedstoneUi.contains(mouseX, mouseY, layout.saveX(), layout.footerButtonY(), FOOTER_BUTTON_WIDTH, BUTTON_HEIGHT);
        boolean cancelHovered = RedstoneUi.contains(mouseX, mouseY, layout.cancelX(), layout.footerButtonY(), FOOTER_BUTTON_WIDTH, BUTTON_HEIGHT);
        boolean deleteHovered = editing && RedstoneUi.contains(mouseX, mouseY, layout.deleteX(), layout.footerButtonY(), FOOTER_BUTTON_WIDTH, BUTTON_HEIGHT);

        if (editing) {
            RedstoneUi.drawButton(graphics, font, "Delete", layout.deleteX(), layout.footerButtonY(), FOOTER_BUTTON_WIDTH, BUTTON_HEIGHT, deleteHovered, RedstoneUi.ButtonTone.DANGER);
        }
        RedstoneUi.drawButton(graphics, font, "Cancel", layout.cancelX(), layout.footerButtonY(), FOOTER_BUTTON_WIDTH, BUTTON_HEIGHT, cancelHovered, RedstoneUi.ButtonTone.NORMAL);
        RedstoneUi.drawButton(graphics, font, "Save", layout.saveX(), layout.footerButtonY(), FOOTER_BUTTON_WIDTH, BUTTON_HEIGHT, saveHovered, RedstoneUi.ButtonTone.ACTIVE);
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
        Macro macro = type == MacroType.KEYBIND
                ? Macro.keybind(id, nameValue, commandValue, keyCode)
                : Macro.commandAlias(id, nameValue, commandValue, aliasValue);
        MacroStore.upsert(macro);
        returnToList();
    }

    private String validate() {
        nameValue = nameValue.trim();
        commandValue = MacroCommandText.normalizeCommand(commandValue);
        aliasValue = MacroCommandText.normalizeAlias(aliasValue);

        if (nameValue.isBlank()) return "Name is required.";
        if (commandValue.isBlank()) return "Command to run is required.";

        if (type == MacroType.KEYBIND) {
            if (!MacroKeys.isBound(keyCode)) return "Choose a keybinding.";
            if (MacroStore.keyExists(keyCode, originalId)) return "This keybinding is already used by another macro.";
            return "";
        }

        if (!MacroCommandText.isValidAliasInput(aliasBox.getValue())) return "New command must be one word using a-z, 0-9, underscore, or hyphen.";
        if (CommandCommand.isReservedAlias(aliasValue)) return "/" + aliasValue + " is reserved by RedstoneUtils.";
        if (MacroCommandText.normalizeAlias(commandValue).equals(aliasValue)) return "A command macro cannot call itself.";
        if (MacroStore.aliasExists(aliasValue, originalId)) return "This command macro already exists.";

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

    private void captureKey(int key) {
        if (key == InputConstants.KEY_ESCAPE || key == InputConstants.KEY_BACKSPACE || key == InputConstants.KEY_DELETE) {
            keyCode = Macro.UNBOUND_KEY;
        } else {
            keyCode = key;
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
                ? List.of(nameBox, commandBox, aliasBox)
                : List.of(nameBox, commandBox);
    }

    private List<EditBox> allFields() {
        return List.of(nameBox, commandBox, aliasBox);
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
    }

    private void positionFields(Layout layout) {
        positionField(nameBox, layout.fieldX(), layout.nameFieldY(), layout.fieldWidth());
        positionField(commandBox, layout.fieldX(), layout.commandFieldY(), layout.fieldWidth());
        positionField(aliasBox, layout.fieldX(), layout.bindingFieldY(), layout.fieldWidth());
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
    }

    private static Layout layout(int screenWidth, int screenHeight) {
        int width = Mth.clamp(screenWidth - 24, PANEL_MIN_WIDTH, PANEL_MAX_WIDTH);
        int height = Mth.clamp(screenHeight - 24, PANEL_MIN_HEIGHT, PANEL_MAX_HEIGHT);
        int x = (screenWidth - width) / 2;
        int y = (screenHeight - height) / 2;
        return new Layout(x, y, width, height);
    }

    private static void playClick() {
        AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
    }

    private record Layout(int x, int y, int width, int height) {
        private int fieldX() {
            return x + RedstoneUi.PANEL_PADDING;
        }

        private int fieldWidth() {
            return width - RedstoneUi.PANEL_PADDING * 2;
        }

        private int typeY() {
            return y + 50;
        }

        private int keybindTypeX() {
            return fieldX();
        }

        private int commandTypeX() {
            return keybindTypeX() + TYPE_BUTTON_WIDTH + RedstoneUi.GAP;
        }

        private int nameLabelY() {
            return y + 86;
        }

        private int nameFieldY() {
            return nameLabelY() + 12;
        }

        private int commandLabelY() {
            return nameFieldY() + FIELD_HEIGHT + 16;
        }

        private int commandFieldY() {
            return commandLabelY() + 12;
        }

        private int bindingLabelY() {
            return commandFieldY() + FIELD_HEIGHT + 16;
        }

        private int bindingFieldY() {
            return bindingLabelY() + 12;
        }

        private int footerButtonY() {
            return y + height - 36;
        }

        private int deleteX() {
            return fieldX();
        }

        private int saveX() {
            return x + width - RedstoneUi.PANEL_PADDING - FOOTER_BUTTON_WIDTH;
        }

        private int cancelX() {
            return saveX() - RedstoneUi.GAP - FOOTER_BUTTON_WIDTH;
        }
    }
}
