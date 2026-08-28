/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.client.macro;

import java.util.UUID;

public final class Macro {

    public static final int UNBOUND_KEY = -1;
    public static final boolean DEFAULT_ENABLED = true;
    public static final int MAX_NAME_LENGTH = 80;
    public static final int MAX_COMMAND_LENGTH = 512;
    public static final int MAX_ALIAS_LENGTH = 64;

    private String id = UUID.randomUUID().toString();
    private MacroType type = MacroType.KEYBIND;
    private String name = "";
    private String command = "";
    private int keyCode = UNBOUND_KEY;
    private String alias = "";
    private boolean mouseButton;
    private int modifiers;
    private MacroTrigger trigger = MacroTrigger.PRESSED;
    private boolean enabled = DEFAULT_ENABLED;
    private String category = "General";

    public Macro() {
    }

    public Macro(String id, MacroType type, String name, String command, int keyCode, String alias) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.command = command;
        this.keyCode = keyCode;
        this.alias = alias;
        sanitize();
    }

    public Macro(String id, MacroType type, String name, String command, int keyCode, String alias,
                 boolean mouseButton, int modifiers, MacroTrigger trigger, boolean enabled, String category) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.command = command;
        this.keyCode = keyCode;
        this.alias = alias;
        this.mouseButton = mouseButton;
        this.modifiers = modifiers;
        this.trigger = trigger;
        this.enabled = enabled;
        this.category = category;
        sanitize();
    }

    public static Macro keybind(String id, String name, String command, int keyCode) {
        return new Macro(id, MacroType.KEYBIND, name, command, keyCode, "");
    }

    public static Macro commandAlias(String id, String name, String command, String alias) {
        return new Macro(id, MacroType.COMMAND, name, command, UNBOUND_KEY, alias);
    }

    public Macro copy() {
        return new Macro(id, type, name, command, keyCode, alias, mouseButton, modifiers, trigger, enabled, category);
    }

    public String id() {
        return id;
    }

    public MacroType type() {
        return type;
    }

    public String name() {
        return name;
    }

    public String command() {
        return command;
    }

    public int keyCode() {
        return keyCode;
    }

    public String alias() {
        return alias;
    }

    public boolean mouseButton() { return mouseButton; }
    public int modifiers() { return modifiers; }
    public MacroTrigger trigger() { return trigger; }
    public boolean enabled() { return enabled; }
    public String category() { return category; }

    public boolean isKeybind() {
        return type == MacroType.KEYBIND;
    }

    public boolean isCommandAlias() {
        return type == MacroType.COMMAND;
    }

    void sanitize() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (type == null) type = MacroType.KEYBIND;

        name = name == null ? "" : name.trim();
        if (name.isBlank()) name = "Macro";
        if (name.length() > MAX_NAME_LENGTH) name = name.substring(0, MAX_NAME_LENGTH);

        command = MacroCommandText.normalizeCommand(command);
        if (command.length() > MAX_COMMAND_LENGTH) command = command.substring(0, MAX_COMMAND_LENGTH).stripTrailing();

        if (type == MacroType.COMMAND) {
            alias = MacroCommandText.normalizeAlias(alias);
            if (alias.length() > MAX_ALIAS_LENGTH) alias = alias.substring(0, MAX_ALIAS_LENGTH);
            keyCode = UNBOUND_KEY;
            mouseButton = false;
            modifiers = 0;
            trigger = MacroTrigger.PRESSED;
        } else {
            alias = "";
            if (keyCode < 0) keyCode = UNBOUND_KEY;
        }
        modifiers &= MacroKeys.ALL_MODIFIERS;
        if (trigger == null) trigger = MacroTrigger.PRESSED;
        category = category == null ? "" : category.strip();
        if (category.isBlank()) category = "General";
        if (category.length() > 48) category = category.substring(0, 48);
    }

    public enum MacroTrigger {
        PRESSED,
        RELEASED,
        HELD
    }
}
