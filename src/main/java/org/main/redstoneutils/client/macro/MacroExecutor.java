package org.main.redstoneutils.client.macro;

import net.minecraft.client.Minecraft;
import org.main.redstoneutils.client.ui.RedstoneMessages;

public final class MacroExecutor {

    private MacroExecutor() {
    }

    public static boolean execute(Macro macro) {
        if (macro == null || macro.command().isBlank()) return false;
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
            RedstoneMessages.send("Cannot run macro without an active connection");
            return false;
        }

        CommandCommand.withoutAliasExpansion(() -> minecraft.getConnection().sendCommand(normalized));
        return true;
    }
}
