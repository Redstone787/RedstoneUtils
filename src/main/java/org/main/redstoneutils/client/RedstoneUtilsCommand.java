package org.main.redstoneutils.client;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import org.main.redstoneutils.client.autowire.AutoWireHandler;
import org.main.redstoneutils.client.autowire.WireType;
import org.main.redstoneutils.client.teleport.TpUtil;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class RedstoneUtilsCommand {

    private static final String ARG_VISIBLE = "visible";
    private static final String ARG_MODE = "mode";
    private static final String ARG_RANGE = "range";

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, buildContext) -> dispatcher.register(ClientCommands.literal("redstone_utils")
                        .then(ClientCommands.literal("wire_overlay")
                                .executes(context -> setWireOverlay(context, !RedstoneUtilsClientActions.wireOverlayVisible()))
                                .then(ClientCommands.argument(ARG_VISIBLE, BoolArgumentType.bool())
                                        .executes(context -> setWireOverlay(context, BoolArgumentType.getBool(context, ARG_VISIBLE)))))
                        .then(ClientCommands.literal("sculk_overlay")
                                .executes(context -> setSculkOverlay(context, !RedstoneUtilsClientActions.sculkOverlayVisible()))
                                .then(ClientCommands.argument(ARG_VISIBLE, BoolArgumentType.bool())
                                        .executes(context -> setSculkOverlay(context, BoolArgumentType.getBool(context, ARG_VISIBLE)))))
                        .then(ClientCommands.literal("all_overlays")
                                .executes(context -> setAllOverlays(context, !allOverlaysVisible()))
                                .then(ClientCommands.argument(ARG_VISIBLE, BoolArgumentType.bool())
                                        .executes(context -> setAllOverlays(context, BoolArgumentType.getBool(context, ARG_VISIBLE)))))
                        .then(ClientCommands.literal("config")
                                .executes(RedstoneUtilsCommand::openConfig))
                        .then(ClientCommands.literal("macros")
                                .executes(RedstoneUtilsCommand::openMacros))
                        .then(ClientCommands.literal("autowire")
                                .executes(RedstoneUtilsCommand::showAutoWire)
                                .then(ClientCommands.argument(ARG_MODE, StringArgumentType.word())
                                        .suggests(RedstoneUtilsCommand::suggestWireTypes)
                                        .executes(RedstoneUtilsCommand::setAutoWire)))
                        .then(ClientCommands.literal("reset_autowire")
                                .executes(RedstoneUtilsCommand::resetAutoWire))
                        .then(ClientCommands.literal("tp")
                                .executes(RedstoneUtilsCommand::teleport)
                                .then(ClientCommands.argument(ARG_RANGE, DoubleArgumentType.doubleArg(10.0D, 1000.0D))
                                        .executes(RedstoneUtilsCommand::teleportRange)))
                )
        );
    }

    private static int setWireOverlay(CommandContext<FabricClientCommandSource> context, boolean visible) {
        RedstoneUtilsClientActions.setWireOverlay(visible);
        return feedback(context, "Wire overlay: " + stateName(visible));
    }

    private static int setSculkOverlay(CommandContext<FabricClientCommandSource> context, boolean visible) {
        RedstoneUtilsClientActions.setSculkOverlay(visible);
        return feedback(context, "Sculk overlay: " + stateName(visible));
    }

    private static int setAllOverlays(CommandContext<FabricClientCommandSource> context, boolean visible) {
        RedstoneUtilsClientActions.setAllOverlays(visible);
        return feedback(context, "All overlays: " + stateName(visible));
    }

    private static boolean allOverlaysVisible() {
        return RedstoneUtilsClientActions.allOverlaysVisible();
    }

    private static int feedback(CommandContext<FabricClientCommandSource> context, String message) {
        context.getSource().sendFeedback(Component.literal(message));
        return 1;
    }

    private static int openConfig(CommandContext<FabricClientCommandSource> context) {
        RedstoneUtilsClientActions.openConfig();
        return feedback(context, "Opened RedstoneUtils config");
    }

    private static int openMacros(CommandContext<FabricClientCommandSource> context) {
        RedstoneUtilsClientActions.openMacros();
        return feedback(context, "Opened RedstoneUtils macros");
    }

    private static int showAutoWire(CommandContext<FabricClientCommandSource> context) {
        return feedback(context, "AutoWire: " + AutoWireHandler.getActiveWireType().getDisplayName());
    }

    private static int setAutoWire(CommandContext<FabricClientCommandSource> context) {
        String mode = StringArgumentType.getString(context, ARG_MODE);
        WireType wireType = findWireType(mode);
        if (wireType == null) {
            return feedback(context, "Unknown AutoWire mode. Supported: " + wireTypeSuggestions());
        }

        AutoWireHandler.setActiveWireType(wireType);
        return 1;
    }

    private static int resetAutoWire(CommandContext<FabricClientCommandSource> context) {
        AutoWireHandler.setActiveWireType(WireType.NONE);
        return 1;
    }

    private static int teleport(CommandContext<FabricClientCommandSource> context) {
        TpUtil.teleportToBlock();
        return 1;
    }

    private static int teleportRange(CommandContext<FabricClientCommandSource> context) {
        double range = DoubleArgumentType.getDouble(context, ARG_RANGE);
        if (RedstoneUtilsClientNetworking.teleport(range)) {
            return 1;
        }

        return feedback(context, "Server-side RedstoneUtils teleport is not available");
    }

    private static CompletableFuture<Suggestions> suggestWireTypes(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        for (WireType wireType : WireType.values()) {
            builder.suggest(wireType.name().toLowerCase(Locale.ROOT));
        }
        return builder.buildFuture();
    }

    private static WireType findWireType(String mode) {
        String normalized = mode.toUpperCase(Locale.ROOT).replace('-', '_');
        for (WireType wireType : WireType.values()) {
            if (wireType.name().equals(normalized)) return wireType;
        }
        return null;
    }

    private static String wireTypeSuggestions() {
        StringBuilder suggestions = new StringBuilder();
        for (WireType wireType : WireType.values()) {
            if (!suggestions.isEmpty()) suggestions.append(", ");
            suggestions.append(wireType.name().toLowerCase(Locale.ROOT));
        }
        return suggestions.toString();
    }

    private static String stateName(boolean visible) {
        return visible ? "on" : "off";
    }
}
