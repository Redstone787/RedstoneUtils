package org.main.redstoneutils.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.main.redstoneutils.client.autowire.AutoWireHandler;
import org.main.redstoneutils.client.autowire.AutoWirePreviewOverlay;
import org.main.redstoneutils.client.bud.BudSwitchOverlay;
import org.main.redstoneutils.client.calculator.Calculator;
import org.main.redstoneutils.client.config.RedstoneUtilsConfig;
import org.main.redstoneutils.client.config.ClientProfiles;
import org.main.redstoneutils.client.macro.CommandCommand;
import org.main.redstoneutils.client.macro.CommandKeybind;
import org.main.redstoneutils.client.macro.MacroStore;
import org.main.redstoneutils.client.sculk.SculkSensorOverlay;
import org.main.redstoneutils.client.signal.SignalUtil;
import org.main.redstoneutils.client.ui.RedstoneMessages;
import org.main.redstoneutils.client.ui.RedstoneOverlay;
import org.main.redstoneutils.client.ui.StatusHud;
import org.main.redstoneutils.client.ui.PauseMenuIntegration;
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
        queueRecoveryWarning(RedstoneUtilsConfig.consumeRecoveryBackup(), "message.redstoneutils.config.recovered");
        queueRecoveryWarning(MacroStore.consumeRecoveryBackup(), "message.redstoneutils.macros.recovered");
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
