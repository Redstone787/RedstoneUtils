/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.client.macro;

import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.network.chat.Component;
import io.github.redstone787.redstonelabworks.client.ui.RedstoneMessages;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class CommandCommand {

    private static final int MAX_ALIAS_DEPTH = 8;
    private static final Set<String> RESERVED_ALIASES = Set.of(
            "autowire",
            "calc",
            "clock",
            "color",
            "macro",
            "overlay",
            "redstone",
            "redstonelabworks",
            "sculkinfo",
            "set-content",
            "set-signal",
            "signal"
    );
    private static final ThreadLocal<Boolean> BYPASS_ALIAS_EXPANSION = ThreadLocal.withInitial(() -> false);

    private static boolean initialized;

    private CommandCommand() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        ClientSendMessageEvents.ALLOW_COMMAND.register(CommandCommand::allowCommand);
    }

    public static boolean isReservedAlias(String alias) {
        return RESERVED_ALIASES.contains(MacroCommandText.normalizeAlias(alias));
    }

    public static Expansion expand(String command) {
        String currentCommand = MacroCommandText.normalizeCommand(command);
        if (currentCommand.isBlank()) return new Expansion("", false, null);

        Set<String> visitedAliases = new HashSet<>();
        boolean expanded = false;

        for (int depth = 0; ; depth++) {
            String root = MacroCommandText.commandRoot(currentCommand).toLowerCase(Locale.ROOT);
            if (root.isBlank()) return new Expansion(currentCommand, expanded, null);
            if (isReservedAlias(root)) return new Expansion(currentCommand, expanded, null);

            Macro macro = MacroStore.findCommandAlias(root).orElse(null);
            if (macro == null) return new Expansion(currentCommand, expanded, null);
            if (depth >= MAX_ALIAS_DEPTH) {
                return new Expansion(currentCommand, expanded, Component.translatable("message.redstonelabworks.macro.alias_depth").getString());
            }

            if (!visitedAliases.add(root)) {
                return new Expansion(currentCommand, expanded, Component.translatable("message.redstonelabworks.macro.alias_loop", "/" + root).getString());
            }

            String arguments = MacroCommandText.commandArguments(currentCommand);
            currentCommand = MacroCommandText.appendArguments(macro.command(), arguments);
            expanded = true;
        }
    }

    static void withoutAliasExpansion(Runnable runnable) {
        if (runnable == null) return;

        boolean previous = BYPASS_ALIAS_EXPANSION.get();
        BYPASS_ALIAS_EXPANSION.set(true);
        try {
            runnable.run();
        } finally {
            BYPASS_ALIAS_EXPANSION.set(previous);
        }
    }

    private static boolean allowCommand(String command) {
        if (BYPASS_ALIAS_EXPANSION.get()) return true;

        Expansion expansion = expand(command);
        if (!expansion.expanded()) return true;

        if (!expansion.successful()) {
            RedstoneMessages.send(expansion.error());
            return false;
        }

        MacroExecutor.sendCommandWithoutAliasExpansion(expansion.command());
        return false;
    }

    public record Expansion(String command, boolean expanded, String error) {
        public boolean successful() {
            return error == null || error.isBlank();
        }
    }
}
