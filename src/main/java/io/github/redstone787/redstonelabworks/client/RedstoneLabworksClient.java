/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import io.github.redstone787.redstonelabworks.client.autowire.AutoWireHandler;
import io.github.redstone787.redstonelabworks.client.autowire.AutoWirePreviewOverlay;
import io.github.redstone787.redstonelabworks.client.bud.BudSwitchOverlay;
import io.github.redstone787.redstonelabworks.client.calculator.Calculator;
import io.github.redstone787.redstonelabworks.client.config.RedstoneLabworksConfig;
import io.github.redstone787.redstonelabworks.client.config.ClientProfiles;
import io.github.redstone787.redstonelabworks.client.macro.CommandCommand;
import io.github.redstone787.redstonelabworks.client.macro.CommandKeybind;
import io.github.redstone787.redstonelabworks.client.macro.MacroStore;
import io.github.redstone787.redstonelabworks.client.sculk.SculkSensorOverlay;
import io.github.redstone787.redstonelabworks.client.signal.SignalUtil;
import io.github.redstone787.redstonelabworks.client.ui.RedstoneMessages;
import io.github.redstone787.redstonelabworks.client.ui.RedstoneOverlay;
import io.github.redstone787.redstonelabworks.client.ui.StatusHud;
import io.github.redstone787.redstonelabworks.client.ui.PauseMenuIntegration;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RedstoneLabworksClient implements ClientModInitializer {
    private static final List<Component> RECOVERY_WARNINGS = new ArrayList<>();
    @Override
    public void onInitializeClient() {
        RedstoneLabworksClientNetworking.init();
        RedstoneLabworksConfig.load();
        MacroStore.load();
        RedstoneMessages.setDefaultTarget(RedstoneLabworksConfig.getMessageTarget());
        RedstoneOverlay.init();
        StatusHud.init();
        PauseMenuIntegration.init();
        AutoWireHandler.init();
        AutoWirePreviewOverlay.init();
        BudSwitchOverlay.init();
        SculkSensorOverlay.init();
        Keybindings.init();
        CommandCommand.init();
        CommandKeybind.init();
        Calculator.init();
        SignalUtil.init();
        RedstoneLabworksCommand.init();
        ClientProfiles.init();
        queueRecoveryWarning(RedstoneLabworksConfig.consumeRecoveryBackup(), "message.redstonelabworks.config.recovered");
        queueRecoveryWarning(MacroStore.consumeRecoveryBackup(), "message.redstonelabworks.macros.recovered");
        ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> client.execute(() -> {
            for (Component warning : List.copyOf(RECOVERY_WARNINGS)) {
                RedstoneMessages.chat(warning);
                RedstoneMessages.popup(warning);
            }
            RECOVERY_WARNINGS.clear();
        }));
    }

    private static void queueRecoveryWarning(Path backup, String translationKey) {
        if (backup != null) {
            RECOVERY_WARNINGS.add(Component.translatable(translationKey, backup.toAbsolutePath().toString()));
        }
    }
}
