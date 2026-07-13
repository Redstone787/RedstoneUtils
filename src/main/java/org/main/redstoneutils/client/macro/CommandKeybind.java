package org.main.redstoneutils.client.macro;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CommandKeybind {

    private static Set<Integer> downKeys = new HashSet<>();
    private static boolean initialized;

    private CommandKeybind() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        ClientTickEvents.END_CLIENT_TICK.register(CommandKeybind::tick);
    }

    private static void tick(Minecraft client) {
        if (client == null || client.player == null || client.getConnection() == null || client.gui.screen() != null) {
            downKeys.clear();
            return;
        }

        List<Macro> macros = MacroStore.macros(MacroType.KEYBIND);
        Set<Integer> currentDownKeys = new HashSet<>();

        for (Macro macro : macros) {
            int keyCode = macro.keyCode();
            if (!MacroKeys.isBound(keyCode)) continue;

            boolean isDown = InputConstants.isKeyDown(client.getWindow(), keyCode);
            if (!isDown) continue;

            currentDownKeys.add(keyCode);
            if (!downKeys.contains(keyCode)) {
                MacroExecutor.execute(macro);
            }
        }

        downKeys = currentDownKeys;
    }
}
