/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstone_utils.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import io.github.redstone787.redstone_utils.client.autowire.AutoWireHandler;
import io.github.redstone787.redstone_utils.client.autowire.AutoWirePreviewOverlay;
import io.github.redstone787.redstone_utils.client.bud.BudSwitchOverlay;
import io.github.redstone787.redstone_utils.client.calculator.Calculator;
import io.github.redstone787.redstone_utils.client.config.RedstoneUtilsConfig;
import io.github.redstone787.redstone_utils.client.config.ClientProfiles;
import io.github.redstone787.redstone_utils.client.macro.CommandCommand;
import io.github.redstone787.redstone_utils.client.macro.CommandKeybind;
import io.github.redstone787.redstone_utils.client.macro.MacroStore;
import io.github.redstone787.redstone_utils.client.sculk.SculkSensorOverlay;
import io.github.redstone787.redstone_utils.client.signal.SignalUtil;
import io.github.redstone787.redstone_utils.client.ui.RedstoneMessages;
import io.github.redstone787.redstone_utils.client.ui.RedstoneOverlay;
import io.github.redstone787.redstone_utils.client.ui.StatusHud;
import io.github.redstone787.redstone_utils.client.ui.PauseMenuIntegration;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RedstoneUtilsClient implements ClientModInitializer {
    private static final List<Component> RECOVERY_WARNINGS = new ArrayList<>();
    @Override
    public void onInitializeClient() {
        RedstoneUtilsClientNetworking.init();
        RedstoneUtilsConfig.load();
        MacroStore.load();
        RedstoneMessages.setDefaultTarget(RedstoneUtilsConfig.getMessageTarget());
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
        RedstoneUtilsCommand.init();
        ClientProfiles.init();
        queueRecoveryWarning(RedstoneUtilsConfig.consumeRecoveryBackup(), "message.redstone_utils.config.recovered");
        queueRecoveryWarning(MacroStore.consumeRecoveryBackup(), "message.redstone_utils.macros.recovered");
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
