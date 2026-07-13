package org.main.redstoneutils.client;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import org.main.redstoneutils.client.autowire.AutoWirePreviewOverlay;
import org.main.redstoneutils.client.config.ConfigScreen;
import org.main.redstoneutils.client.macro.MacrosScreen;
import org.main.redstoneutils.client.sculk.SculkSensorOverlay;
import org.main.redstoneutils.client.ui.RedstoneOverlay;

public class RedstoneUtilsCommand {

    private static final String ARG_VISIBLE = "visible";

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, buildContext) -> dispatcher.register(ClientCommands.literal("redstone_utils")
                        .then(ClientCommands.literal("wire_overlay")
                                .executes(context -> setWireOverlay(context, !AutoWirePreviewOverlay.isVisible()))
                                .then(ClientCommands.argument(ARG_VISIBLE, BoolArgumentType.bool())
                                        .executes(context -> setWireOverlay(context, BoolArgumentType.getBool(context, ARG_VISIBLE)))))
                        .then(ClientCommands.literal("sculk_overlay")
                                .executes(context -> setSculkOverlay(context, !SculkSensorOverlay.isVisible()))
                                .then(ClientCommands.argument(ARG_VISIBLE, BoolArgumentType.bool())
                                        .executes(context -> setSculkOverlay(context, BoolArgumentType.getBool(context, ARG_VISIBLE)))))
                        .then(ClientCommands.literal("all_overlays")
                                .executes(context -> setAllOverlays(context, !allOverlaysVisible()))
                                .then(ClientCommands.argument(ARG_VISIBLE, BoolArgumentType.bool())
                                        .executes(context -> setAllOverlays(context, BoolArgumentType.getBool(context, ARG_VISIBLE)))))
                        .then(ClientCommands.literal("config")
                                .executes(context -> openConfig(context)))
                        .then(ClientCommands.literal("macros")
                                .executes(context -> openMacros(context)))
                )
        );
    }

    private static int setWireOverlay(CommandContext<FabricClientCommandSource> context, boolean visible) {
        AutoWirePreviewOverlay.setVisible(visible);
        return feedback(context, "Wire overlay: " + stateName(visible));
    }

    private static int setSculkOverlay(CommandContext<FabricClientCommandSource> context, boolean visible) {
        SculkSensorOverlay.setVisible(visible);
        return feedback(context, "Sculk overlay: " + stateName(visible));
    }

    private static int setAllOverlays(CommandContext<FabricClientCommandSource> context, boolean visible) {
        RedstoneOverlay.setVisible(visible);
        AutoWirePreviewOverlay.setVisible(visible);
        SculkSensorOverlay.setVisible(visible);
        return feedback(context, "All overlays: " + stateName(visible));
    }

    private static boolean allOverlaysVisible() {
        return AutoWirePreviewOverlay.isVisible() && RedstoneOverlay.isVisible() && SculkSensorOverlay.isVisible();
    }

    private static int feedback(CommandContext<FabricClientCommandSource> context, String message) {
        context.getSource().sendFeedback(Component.literal(message));
        return 1;
    }

    private static int openConfig(CommandContext<FabricClientCommandSource> context) {
        ConfigScreen.open();
        return feedback(context, "Opened RedstoneUtils config");
    }

    private static int openMacros(CommandContext<FabricClientCommandSource> context) {
        MacrosScreen.open();
        return feedback(context, "Opened RedstoneUtils macros");
    }

    private static String stateName(boolean visible) {
        return visible ? "on" : "off";
    }
}
