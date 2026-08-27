package org.main.redstoneutils.client;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColorCollection;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.main.redstoneutils.client.autowire.AutoWire;
import org.main.redstoneutils.client.autowire.AutoWireHandler;
import org.main.redstoneutils.client.autowire.WireType;
import org.main.redstoneutils.client.sculk.SculkInfoScreen;
import org.main.redstoneutils.client.teleport.TpUtil;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class RedstoneUtilsCommand {

    private static final String ARG_VISIBLE = "visible";
    private static final String ARG_MODE = "mode";
    private static final String ARG_RANGE = "range";

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
            dispatcher.register(overlayCommand());

            dispatcher.register(ClientCommands.literal("color")
                    .executes(RedstoneUtilsCommand::colorCommand));

            dispatcher.register(redstoneUtilsCommand());
            dispatcher.register(ClientCommands.literal("macro")
                    .executes(RedstoneUtilsCommand::openMacros));
            dispatcher.register(ClientCommands.literal("sculkinfo")
                    .executes(RedstoneUtilsCommand::openSculkInfo));
            dispatcher.register(autoWireCommand());
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> overlayCommand() {
        return ClientCommands.literal("overlay")
                    .executes(context -> setAllOverlays(context, !allOverlaysVisible()))
                    .then(ClientCommands.literal("wire")
                            .executes(context -> setWireOverlay(context, !RedstoneUtilsClientActions.wireOverlayVisible()))
                            .then(ClientCommands.argument(ARG_VISIBLE, BoolArgumentType.bool())
                                    .executes(context -> setWireOverlay(context, BoolArgumentType.getBool(context, ARG_VISIBLE)))))
                    .then(ClientCommands.literal("sculk")
                            .executes(context -> setSculkOverlay(context, !RedstoneUtilsClientActions.sculkOverlayVisible()))
                            .then(ClientCommands.argument(ARG_VISIBLE, BoolArgumentType.bool())
                                    .executes(context -> setSculkOverlay(context, BoolArgumentType.getBool(context, ARG_VISIBLE)))))
                    .then(ClientCommands.literal("bud")
                            .executes(context -> setBudOverlay(context, !RedstoneUtilsClientActions.budOverlayVisible()))
                            .then(ClientCommands.argument(ARG_VISIBLE, BoolArgumentType.bool())
                                    .executes(context -> setBudOverlay(context, BoolArgumentType.getBool(context, ARG_VISIBLE)))))
                    .then(ClientCommands.literal("all")
                            .executes(context -> setAllOverlays(context, !allOverlaysVisible()))
                            .then(ClientCommands.argument(ARG_VISIBLE, BoolArgumentType.bool())
                                    .executes(context -> setAllOverlays(context, BoolArgumentType.getBool(context, ARG_VISIBLE)))))
            ;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> redstoneUtilsCommand() {
        return ClientCommands.literal("redstoneutils")
                .executes(RedstoneUtilsCommand::openToolbox)
                .then(ClientCommands.literal("config")
                        .executes(RedstoneUtilsCommand::openConfig))
                .then(ClientCommands.literal("toolbox")
                        .executes(RedstoneUtilsCommand::openToolbox))
                .then(ClientCommands.literal("macro")
                        .executes(RedstoneUtilsCommand::openMacros))
                .then(ClientCommands.literal("sculkinfo")
                        .executes(RedstoneUtilsCommand::openSculkInfo))
                .then(ClientCommands.literal("color")
                        .executes(RedstoneUtilsCommand::colorCommand))
                .then(overlayCommand())
                .then(autoWireCommand())
                .then(ClientCommands.literal("teleport")
                        .executes(RedstoneUtilsCommand::teleport)
                        .then(ClientCommands.argument(ARG_RANGE, DoubleArgumentType.doubleArg(10.0D, 1000.0D))
                                .executes(RedstoneUtilsCommand::teleportRange)));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> autoWireCommand() {
        return ClientCommands.literal("autowire")
                .executes(RedstoneUtilsCommand::showAutoWire)
                .then(ClientCommands.argument(ARG_MODE, StringArgumentType.word())
                        .suggests(RedstoneUtilsCommand::suggestWireTypes)
                        .executes(RedstoneUtilsCommand::setAutoWire));
    }

    private static int setWireOverlay(CommandContext<FabricClientCommandSource> context, boolean visible) {
        RedstoneUtilsClientActions.setWireOverlay(visible);
        return overlayFeedback(context, "wire", visible);
    }

    private static int setSculkOverlay(CommandContext<FabricClientCommandSource> context, boolean visible) {
        RedstoneUtilsClientActions.setSculkOverlay(visible);
        return overlayFeedback(context, "sculk", visible);
    }

    private static int setBudOverlay(CommandContext<FabricClientCommandSource> context, boolean visible) {
        RedstoneUtilsClientActions.setBudOverlay(visible);
        return overlayFeedback(context, "bud", visible);
    }

    private static int setAllOverlays(CommandContext<FabricClientCommandSource> context, boolean visible) {
        RedstoneUtilsClientActions.setAllOverlays(visible);
        return overlayFeedback(context, "all", visible);
    }

    private static boolean allOverlaysVisible() {
        return RedstoneUtilsClientActions.allOverlaysVisible();
    }

    private static int feedback(CommandContext<FabricClientCommandSource> context, String message) {
        context.getSource().sendFeedback(Component.literal(message));
        return 1;
    }

    private static int feedback(CommandContext<FabricClientCommandSource> context, Component message) {
        context.getSource().sendFeedback(message);
        return 1;
    }

    private static int overlayFeedback(CommandContext<FabricClientCommandSource> context, String overlay, boolean visible) {
        return feedback(context, Component.translatable(
                "message.redstoneutils.overlay." + overlay,
                Component.translatable(visible ? "state.redstoneutils.on" : "state.redstoneutils.off")
        ));
    }

    private static int openConfig(CommandContext<FabricClientCommandSource> context) {
        RedstoneUtilsClientActions.openConfig();
        return feedback(context, Component.translatable("message.redstoneutils.opened_config"));
    }

    private static int openToolbox(CommandContext<FabricClientCommandSource> context) {
        org.main.redstoneutils.client.ui.ToolboxScreen.open();
        return 1;
    }

    private static int openMacros(CommandContext<FabricClientCommandSource> context) {
        RedstoneUtilsClientActions.openMacros();
        return feedback(context, Component.translatable("message.redstoneutils.opened_macros"));
    }

    private static int openSculkInfo(CommandContext<FabricClientCommandSource> context) {
        SculkInfoScreen.open();
        return 1;
    }

    private static int showAutoWire(CommandContext<FabricClientCommandSource> context) {
        return feedback(context, Component.translatable("message.redstoneutils.autowire.active", AutoWireHandler.getActiveWireType().getDisplayName()));
    }

    private static int setAutoWire(CommandContext<FabricClientCommandSource> context) {
        String mode = StringArgumentType.getString(context, ARG_MODE);
        if ("reset".equalsIgnoreCase(mode)) return resetAutoWire(context);
        WireType wireType = findWireType(mode);
        if (wireType == null) {
            return feedback(context, Component.translatable("message.redstoneutils.autowire.unknown", wireTypeSuggestions()));
        }

        AutoWireHandler.setActiveWireType(wireType);
        return 1;
    }

    private static int resetAutoWire(CommandContext<FabricClientCommandSource> context) {
        AutoWire.reset();
        if (RedstoneUtilsClientNetworking.hasAutoWireBackend()
                && RedstoneUtilsClientNetworking.sendServerCommand("autowire reset")) {
            return 1;
        }

        return feedback(context, Component.translatable("message.redstoneutils.autowire.reset"));
    }

    private static int teleport(CommandContext<FabricClientCommandSource> context) {
        TpUtil.teleportToBlock();
        return 1;
    }

    private static int teleportRange(CommandContext<FabricClientCommandSource> context) {
        double range = DoubleArgumentType.getDouble(context, ARG_RANGE);
        TpUtil.teleportToBlock(range);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestWireTypes(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        for (WireType wireType : WireType.values()) {
            builder.suggest(wireType.name().toLowerCase(Locale.ROOT));
        }
        builder.suggest("reset");
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

    private static int colorCommand(CommandContext<FabricClientCommandSource> context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            context.getSource().sendFeedback(Component.translatable("message.redstoneutils.no_player"));
            return 0;
        }

        if (!(minecraft.hitResult instanceof BlockHitResult hitResult)
                || hitResult.getType() != HitResult.Type.BLOCK) {
            context.getSource().sendFeedback(Component.translatable("message.redstoneutils.look_at_block"));
            return 0;
        }

        Block hitBlock = minecraft.level.getBlockState(hitResult.getBlockPos()).getBlock();
        DyeColor color = findColor(hitBlock, Blocks.STAINED_GLASS);
        Block targetBlock;

        if (color != null) {
            targetBlock = Blocks.WOOL.pick(color);
        } else {
            color = findColor(hitBlock, Blocks.WOOL);
            if (color == null) color = findColor(hitBlock, Blocks.CONCRETE);
            if (color == null) color = findColor(hitBlock, Blocks.DYED_TERRACOTTA);

            if (color == null) {
                context.getSource().sendFeedback(Component.translatable("message.redstoneutils.color.unsupported"));
                return 0;
            }

            targetBlock = Blocks.STAINED_GLASS.pick(color);
        }

        if (minecraft.getConnection() == null) {
            context.getSource().sendFeedback(Component.translatable("message.redstoneutils.color.no_connection"));
            return 0;
        }

        minecraft.getConnection().sendCommand(
                "give @s " + BuiltInRegistries.BLOCK.getKey(targetBlock)
        );
        return feedback(context, Component.translatable("message.redstoneutils.color.sent", targetBlock.getName()));
    }

    private static DyeColor findColor(Block block, ColorCollection<Block> blocks) {
        for (DyeColor color : DyeColor.values()) {
            if (blocks.pick(color) == block) return color;
        }
        return null;
    }

}
