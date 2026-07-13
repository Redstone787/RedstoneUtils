package org.main.redstoneutils.client;

import net.fabricmc.api.ClientModInitializer;
import org.main.redstoneutils.client.autowire.AutoWireHandler;
import org.main.redstoneutils.client.autowire.AutoWirePreviewOverlay;
import org.main.redstoneutils.client.calculator.Calculator;
import org.main.redstoneutils.client.config.RedstoneUtilsConfig;
import org.main.redstoneutils.client.macro.CommandCommand;
import org.main.redstoneutils.client.macro.CommandKeybind;
import org.main.redstoneutils.client.macro.MacroStore;
import org.main.redstoneutils.client.sculk.SculkSensorOverlay;
import org.main.redstoneutils.client.signal.SignalUtil;
import org.main.redstoneutils.client.ui.RedstoneMessages;
import org.main.redstoneutils.client.ui.RedstoneOverlay;

public class RedstoneUtilsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        RedstoneUtilsConfig.load();
        MacroStore.load();
        RedstoneMessages.setDefaultTarget(RedstoneUtilsConfig.getMessageTarget());
        RedstoneOverlay.init();
        AutoWireHandler.init();
        AutoWirePreviewOverlay.init();
        SculkSensorOverlay.init();
        Keybindings.init();
        CommandCommand.init();
        CommandKeybind.init();
        SignalUtil.init();
        Calculator.init();
        RedstoneUtilsCommand.init();
    }
}
