package org.main.redstoneutils.client.macro;

import java.util.UUID;

public final class Macro {

    public static final int UNBOUND_KEY = -1;

    private String id = UUID.randomUUID().toString();
    private MacroType type = MacroType.KEYBIND;
    private String name = "";
    private String command = "";
    private int keyCode = UNBOUND_KEY;
    private String alias = "";

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

    public static Macro keybind(String id, String name, String command, int keyCode) {
        return new Macro(id, MacroType.KEYBIND, name, command, keyCode, "");
    }

    public static Macro commandAlias(String id, String name, String command, String alias) {
        return new Macro(id, MacroType.COMMAND, name, command, UNBOUND_KEY, alias);
    }

    public Macro copy() {
        return new Macro(id, type, name, command, keyCode, alias);
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

        command = MacroCommandText.normalizeCommand(command);

        if (type == MacroType.COMMAND) {
            alias = MacroCommandText.normalizeAlias(alias);
            keyCode = UNBOUND_KEY;
        } else {
            alias = "";
            if (keyCode < 0) keyCode = UNBOUND_KEY;
        }
    }
}
