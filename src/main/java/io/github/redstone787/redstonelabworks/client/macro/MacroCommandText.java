/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.client.macro;

import java.util.Locale;

public final class MacroCommandText {

    private static final String ALIAS_PATTERN = "[a-z0-9_\\-]+";

    private MacroCommandText() {
    }

    public static String normalizeCommand(String command) {
        if (command == null) return "";

        String normalized = command.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1).trim();
        }

        return normalized;
    }

    public static String formatCommand(String command) {
        String normalized = normalizeCommand(command);
        return normalized.isBlank() ? "" : "/" + normalized;
    }

    public static String normalizeAlias(String alias) {
        return commandRoot(alias).toLowerCase(Locale.ROOT);
    }

    public static boolean isValidAliasInput(String alias) {
        String normalized = normalizeCommand(alias);
        if (normalized.isBlank()) return false;
        if (!normalized.equals(commandRoot(normalized))) return false;

        return normalizeAlias(normalized).matches(ALIAS_PATTERN);
    }

    public static String commandRoot(String command) {
        String normalized = normalizeCommand(command);
        if (normalized.isBlank()) return "";

        int splitIndex = firstWhitespace(normalized);
        return splitIndex < 0 ? normalized : normalized.substring(0, splitIndex);
    }

    public static String commandArguments(String command) {
        String normalized = normalizeCommand(command);
        if (normalized.isBlank()) return "";

        int splitIndex = firstWhitespace(normalized);
        return splitIndex < 0 ? "" : normalized.substring(splitIndex).trim();
    }

    public static String appendArguments(String command, String arguments) {
        String normalizedCommand = normalizeCommand(command);
        String normalizedArguments = arguments == null ? "" : arguments.trim();

        if (normalizedCommand.isBlank()) return normalizedArguments;
        if (normalizedArguments.isBlank()) return normalizedCommand;

        return normalizedCommand + " " + normalizedArguments;
    }

    private static int firstWhitespace(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index))) return index;
        }

        return -1;
    }
}
