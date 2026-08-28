/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.client.macro;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import io.github.redstone787.redstonelabworks.client.ui.RedstoneMessages;

public final class MacroExecutor {

    private MacroExecutor() {
    }

    public static boolean execute(Macro macro) {
        if (macro == null || !macro.enabled() || macro.command().isBlank()) return false;
        return executeCommand(macro.command());
    }

    public static boolean executeCommand(String command) {
        CommandCommand.Expansion expansion = CommandCommand.expand(command);
        if (!expansion.successful()) {
            RedstoneMessages.send(expansion.error());
            return false;
        }

        return sendCommandWithoutAliasExpansion(expansion.command());
    }

    static boolean sendCommandWithoutAliasExpansion(String command) {
        String normalized = MacroCommandText.normalizeCommand(command);
        if (normalized.isBlank()) return false;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            RedstoneMessages.send(Component.translatable("message.redstonelabworks.macro.no_connection"));
            return false;
        }

        CommandCommand.withoutAliasExpansion(() -> minecraft.getConnection().sendCommand(normalized));
        return true;
    }
}
