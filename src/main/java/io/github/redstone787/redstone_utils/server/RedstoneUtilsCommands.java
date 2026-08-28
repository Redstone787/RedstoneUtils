/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstone_utils.server;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import io.github.redstone787.redstone_utils.network.RedstoneUtilsNetworking;
import io.github.redstone787.redstone_utils.server.autowire.ServerAutoWire;
import io.github.redstone787.redstone_utils.server.autowire.WireType;
import io.github.redstone787.redstone_utils.server.clock.ClockInterval;
import io.github.redstone787.redstone_utils.server.clock.ClockManager;
import io.github.redstone787.redstone_utils.server.clock.ComparatorClockManager;
import io.github.redstone787.redstone_utils.server.clock.HopperClockInterval;
import io.github.redstone787.redstone_utils.server.clock.HopperClockManager;
import io.github.redstone787.redstone_utils.server.config.RedstoneUtilsServerConfig;
import io.github.redstone787.redstone_utils.server.config.RedstoneUtilsServerConfig.Tool;
import io.github.redstone787.redstone_utils.server.history.ChangeHistory;
import io.github.redstone787.redstone_utils.server.signal.ComparatorSignal;
import io.github.redstone787.redstone_utils.server.signal.SignalBlockVariant;
import io.github.redstone787.redstone_utils.server.signal.SignalItemFactory;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class RedstoneUtilsCommands {

    private static final double DEFAULT_TELEPORT_RANGE = 100.0D;
    private static final double MIN_TELEPORT_RANGE = 10.0D;
    private static final int MAX_CUSTOM_ITEM_NAME_LENGTH = 256;
    private static final String ARG_VISIBLE = "visible";
    private static final String CLIENT_CONFIG = "config";
    private static final String CLIENT_MACROS = "macros";
    private static final String CLIENT_WIRE_OVERLAY = "wire_overlay";
    private static final String CLIENT_SCULK_OVERLAY = "sculk_overlay";
    private static final String CLIENT_BUD_OVERLAY = "bud_overlay";
    private static final String CLIENT_TOOLBOX = "toolbox";
    private static final String CLIENT_ALL_OVERLAYS = "all_overlays";

    private RedstoneUtilsCommands() {
    }

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, environment) -> {
            dispatcher.register(overlayCommand());

            dispatcher.register(redstoneUtilsCommand());
            dispatcher.register(macroCommand());
            dispatcher.register(autoWireCommand());
            dispatcher.register(signalCommand(buildContext));

            dispatcher.register(Commands.literal("set-content")
                    .requires(RedstoneUtilsCommands::canUseSignalTools)
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                            .then(Commands.argument("amount", IntegerArgumentType.integer(0, RedstoneUtilsServerConfig.maxContainerItems()))
                                    .then(Commands.argument("item", ItemArgument.item(buildContext))
                                            .executes(context -> setContent(context, ""))
                                            .then(Commands.argument("name", StringArgumentType.string())
                                                    .executes(context -> setContent(
                                                            context,
                                                            StringArgumentType.getString(context, "name")
                                                    ))))))
            );

            dispatcher.register(Commands.literal("set-signal")
                    .requires(RedstoneUtilsCommands::canUseSignalTools)
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                            .then(Commands.argument("strength", IntegerArgumentType.integer(ComparatorSignal.MIN, ComparatorSignal.MAX))
                                    .then(Commands.argument("item", ItemArgument.item(buildContext))
                                            .executes(context -> setSignal(context, ""))
                                            .then(Commands.argument("name", StringArgumentType.string())
                                                    .executes(context -> setSignal(
                                                            context,
                                                            StringArgumentType.getString(context, "name")
                                                    ))))))
            );

            dispatcher.register(clockCommand());

            dispatcher.register(historyCommand());

            dispatcher.register(Commands.literal("redstone_utils")
                    .then(overlayCommand())
                    .then(macroCommand())
                    .then(autoWireCommand())
                    .then(signalCommand(buildContext))
                    .then(clockCommand())
                    .then(historyCommand("history")));
        });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> overlayCommand() {
        return Commands.literal("overlay")
                .executes(context -> sendClientAction(context.getSource(), CLIENT_ALL_OVERLAYS, -1))
                .then(Commands.literal("wire")
                        .executes(context -> sendClientAction(context.getSource(), CLIENT_WIRE_OVERLAY, -1))
                        .then(Commands.argument(ARG_VISIBLE, BoolArgumentType.bool())
                                .executes(context -> sendClientAction(context.getSource(), CLIENT_WIRE_OVERLAY, booleanValue(context, ARG_VISIBLE)))))
                .then(Commands.literal("sculk")
                        .executes(context -> sendClientAction(context.getSource(), CLIENT_SCULK_OVERLAY, -1))
                        .then(Commands.argument(ARG_VISIBLE, BoolArgumentType.bool())
                                .executes(context -> sendClientAction(context.getSource(), CLIENT_SCULK_OVERLAY, booleanValue(context, ARG_VISIBLE)))))
                .then(Commands.literal("bud")
                        .executes(context -> sendClientAction(context.getSource(), CLIENT_BUD_OVERLAY, -1))
                        .then(Commands.argument(ARG_VISIBLE, BoolArgumentType.bool())
                                .executes(context -> sendClientAction(context.getSource(), CLIENT_BUD_OVERLAY, booleanValue(context, ARG_VISIBLE)))))
                .then(Commands.literal("all")
                        .executes(context -> sendClientAction(context.getSource(), CLIENT_ALL_OVERLAYS, -1))
                        .then(Commands.argument(ARG_VISIBLE, BoolArgumentType.bool())
                                .executes(context -> sendClientAction(context.getSource(), CLIENT_ALL_OVERLAYS, booleanValue(context, ARG_VISIBLE)))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> macroCommand() {
        return Commands.literal("macro")
                .executes(context -> sendClientAction(context.getSource(), CLIENT_MACROS, -1));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> clockCommand() {
        return Commands.literal("clock")
                .requires(RedstoneUtilsCommands::canUseBuilder)
                .executes(context -> showClockUsage(context.getSource()))
                .then(Commands.literal("undo")
                        .requires(RedstoneUtilsCommands::canUseHistory)
                        .executes(context -> undoClock(context.getSource())))
                .then(Commands.literal("comparator")
                        .executes(context -> showClockUsage(context.getSource()))
                        .then(Commands.argument("interval", StringArgumentType.word())
                                .executes(context -> createComparatorClock(context.getSource(), StringArgumentType.getString(context, "interval")))))
                .then(Commands.literal("hopper")
                        .executes(context -> showClockUsage(context.getSource()))
                        .then(Commands.argument("interval", StringArgumentType.word())
                                .executes(context -> createHopperClock(context.getSource(), StringArgumentType.getString(context, "interval")))))
                .then(Commands.argument("interval", StringArgumentType.word())
                        .executes(context -> createComparatorClock(context.getSource(), StringArgumentType.getString(context, "interval"))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> historyCommand() {
        return historyCommand("redstone");
    }

    private static LiteralArgumentBuilder<CommandSourceStack> historyCommand(String name) {
        return Commands.literal(name)
                .requires(RedstoneUtilsCommands::canUseHistory)
                .then(Commands.literal("undo")
                        .executes(context -> applyHistoryResult(context.getSource(), ChangeHistory.undo(context.getSource().getPlayerOrException()))))
                .then(Commands.literal("redo")
                        .executes(context -> applyHistoryResult(context.getSource(), ChangeHistory.redo(context.getSource().getPlayerOrException()))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> redstoneUtilsCommand() {
        return Commands.literal("redstone_utils")
                .executes(context -> sendClientAction(context.getSource(), CLIENT_TOOLBOX, -1))
                .then(Commands.literal("toolbox")
                        .executes(context -> sendClientAction(context.getSource(), CLIENT_TOOLBOX, -1)))
                .then(Commands.literal("config")
                        .executes(context -> sendClientAction(context.getSource(), CLIENT_CONFIG, -1)))
                .then(Commands.literal("teleport")
                        .requires(RedstoneUtilsCommands::canUseTeleport)
                        .executes(context -> teleport(context.getSource(), DEFAULT_TELEPORT_RANGE))
                        .then(Commands.argument("range", DoubleArgumentType.doubleArg(MIN_TELEPORT_RANGE, RedstoneUtilsServerConfig.maxTeleportRange()))
                                .executes(context -> teleport(context.getSource(), DoubleArgumentType.getDouble(context, "range")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> autoWireCommand() {
        return Commands.literal("autowire")
                .requires(RedstoneUtilsCommands::canUseAutoWire)
                .executes(RedstoneUtilsCommands::showAutoWire)
                .then(Commands.argument("mode", StringArgumentType.word())
                        .suggests(RedstoneUtilsCommands::suggestWireTypes)
                        .executes(RedstoneUtilsCommands::setAutoWire));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> signalCommand(CommandBuildContext buildContext) {
        return Commands.literal("signal")
                .requires(RedstoneUtilsCommands::canUseSignalTools)
                .then(Commands.argument("strength", IntegerArgumentType.integer(ComparatorSignal.MIN, ComparatorSignal.MAX))
                        .executes(context -> giveSignalStrength(
                                context.getSource(),
                                IntegerArgumentType.getInteger(context, "strength")
                        ))
                        .then(Commands.literal("optimal")
                                .executes(context -> giveOptimalSignalStrength(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "strength")
                                )))
                        .then(Commands.literal("block")
                                .executes(context -> showSignalBlockOptions(context.getSource()))
                                .then(Commands.argument("block", StringArgumentType.word())
                                        .suggests(RedstoneUtilsCommands::suggestSignalBlocks)
                                        .executes(context -> createSignalBlock(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "strength"),
                                                StringArgumentType.getString(context, "block")
                                        ))))
                        .then(Commands.argument("block", StringArgumentType.word())
                                .suggests(RedstoneUtilsCommands::suggestSignalBlocks)
                                .executes(context -> createSignalBlock(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "strength"),
                                        StringArgumentType.getString(context, "block")
                                ))))
                .then(Commands.literal("optimal")
                        .then(Commands.argument("strength", IntegerArgumentType.integer(ComparatorSignal.MIN, ComparatorSignal.MAX))
                                .executes(context -> giveOptimalSignalStrength(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "strength")
                                ))))
                .then(Commands.literal("block")
                        .executes(context -> showSignalBlockOptions(context.getSource()))
                        .then(Commands.argument("block", StringArgumentType.word())
                                .suggests(RedstoneUtilsCommands::suggestSignalBlocks)
                                .then(Commands.argument("strength", IntegerArgumentType.integer(ComparatorSignal.MIN, ComparatorSignal.MAX))
                                        .executes(context -> createSignalBlock(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "strength"),
                                                StringArgumentType.getString(context, "block")
                                        )))))
                .then(Commands.literal("container")
                        .then(signalContainerContentCommand(buildContext))
                        .then(signalContainerStrengthCommand(buildContext)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> signalContainerContentCommand(CommandBuildContext buildContext) {
        return Commands.literal("content")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, RedstoneUtilsServerConfig.maxContainerItems()))
                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                        .executes(context -> setContent(context, ""))
                                        .then(Commands.argument("name", StringArgumentType.string())
                                                .executes(context -> setContent(
                                                        context,
                                                        StringArgumentType.getString(context, "name")
                                                ))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> signalContainerStrengthCommand(CommandBuildContext buildContext) {
        return Commands.literal("strength")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(Commands.argument("strength", IntegerArgumentType.integer(ComparatorSignal.MIN, ComparatorSignal.MAX))
                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                        .executes(context -> setSignal(context, ""))
                                        .then(Commands.argument("name", StringArgumentType.string())
                                                .executes(context -> setSignal(
                                                        context,
                                                        StringArgumentType.getString(context, "name")
                                                ))))));
    }

    /** Compatibility check for code that does not yet specify a tool. */
    public static boolean canUse(CommandSourceStack source) {
        return canUseAutoWire(source);
    }

    public static boolean canUseTeleport(CommandSourceStack source) {
        return RedstoneUtilsServerConfig.canUse(Tool.TELEPORT, source);
    }

    public static boolean canUseAutoWire(CommandSourceStack source) {
        return RedstoneUtilsServerConfig.canUse(Tool.AUTOWIRE, source);
    }

    public static boolean canUseSignalTools(CommandSourceStack source) {
        return RedstoneUtilsServerConfig.canUse(Tool.SIGNAL_TOOLS, source);
    }

    public static boolean canUseBuilder(CommandSourceStack source) {
        return RedstoneUtilsServerConfig.canUse(Tool.BUILDER, source);
    }

    public static boolean canUseHistory(CommandSourceStack source) {
        return RedstoneUtilsServerConfig.canUse(Tool.HISTORY, source);
    }

    private static int sendClientAction(CommandSourceStack source, String action, int value) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!ServerPlayNetworking.canSend(player, RedstoneUtilsNetworking.ClientCommandPayload.TYPE)) {
            source.sendFailure(Component.translatable("message.redstone_utils.client_required"));
            return 0;
        }

        ServerPlayNetworking.send(player, new RedstoneUtilsNetworking.ClientCommandPayload(action, value));
        return 1;
    }

    private static int booleanValue(CommandContext<CommandSourceStack> context, String name) {
        return BoolArgumentType.getBool(context, name) ? 1 : 0;
    }

    private static int showAutoWire(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        WireType wireType = ServerAutoWire.getWireType(player);
        context.getSource().sendSuccess(() -> Component.translatable("message.redstone_utils.autowire.active", wireName(wireType)), false);
        return 1;
    }

    private static int setAutoWire(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String mode = StringArgumentType.getString(context, "mode");
        if ("reset".equalsIgnoreCase(mode)) return resetAutoWire(context);
        WireType wireType = WireType.find(mode).orElse(null);
        if (wireType == null) {
            context.getSource().sendFailure(Component.translatable("message.redstone_utils.autowire.unknown", WireType.suggestions()));
            return 0;
        }

        ServerAutoWire.setWireType(player, wireType);
        context.getSource().sendSuccess(() -> Component.translatable("message.redstone_utils.autowire.active", wireName(wireType)), false);
        return 1;
    }

    private static int resetAutoWire(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerAutoWire.reset(context.getSource().getPlayerOrException());
        context.getSource().sendSuccess(() -> Component.translatable("message.redstone_utils.autowire.reset"), false);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestWireTypes(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        for (WireType wireType : WireType.values()) {
            builder.suggest(wireType.key());
        }
        builder.suggest("reset");
        return builder.buildFuture();
    }

    private static int giveSignalStrength(CommandSourceStack source, int strength) throws CommandSyntaxException {
        ItemStack stack = SignalBlockVariant.BARREL.createItem(strength, source.registryAccess());
        return giveStack(source, stack, Component.translatable("message.redstone_utils.signal.gave_barrel", strength));
    }

    private static int giveOptimalSignalStrength(CommandSourceStack source, int strength) throws CommandSyntaxException {
        ItemStack stack = SignalItemFactory.createOptimal(strength, source.registryAccess());
        return giveStack(source, stack, Component.translatable("message.redstone_utils.signal.gave_optimal", strength));
    }

    private static int createSignalBlock(CommandSourceStack source, int strength, String blockName) throws CommandSyntaxException {
        SignalBlockVariant variant = SignalBlockVariant.find(blockName).orElse(null);
        if (variant == null) {
            source.sendFailure(Component.translatable("message.redstone_utils.signal.unknown", SignalBlockVariant.suggestions()));
            return 0;
        }
        if (!variant.supports(strength)) {
            source.sendFailure(Component.translatable("message.redstone_utils.signal.supported_strengths", signalName(variant), variant.supportedStrengths()));
            return 0;
        }

        if (variant.targetBlock()) {
            return setTargetedSignalBlock(source, strength, variant);
        }

        ItemStack stack = variant.createItem(strength, source.registryAccess());
        return giveStack(source, stack, Component.translatable("message.redstone_utils.signal.gave", signalName(variant), strength));
    }

    private static int setTargetedSignalBlock(CommandSourceStack source, int strength, SignalBlockVariant variant) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.level();
        BlockHitResult hitResult = targetedBlock(player, RedstoneUtilsServerConfig.maxTargetRange()).orElse(null);
        if (hitResult == null) {
            source.sendFailure(Component.translatable("message.redstone_utils.look_at_block"));
            return 0;
        }

        BlockPos blockPos = hitResult.getBlockPos();
        if (!player.mayInteract(level, blockPos)) {
            source.sendFailure(Component.translatable("message.redstone_utils.no_edit_permission"));
            return 0;
        }

        BlockState blockState = variant.createBlockState(strength);
        ChangeHistory.Transaction transaction = ChangeHistory.begin(
                player,
                Component.translatable("history.redstone_utils.signal_block")
        );
        transaction.capture(level, blockPos);
        if (!level.setBlockAndUpdate(blockPos, blockState)) {
            transaction.rollback();
            source.sendFailure(Component.translatable("message.redstone_utils.signal.set_failed"));
            return 0;
        }
        transaction.commit();

        source.sendSuccess(() -> Component.translatable("message.redstone_utils.signal.set", signalName(variant), strength), false);
        return 1;
    }

    private static int showSignalBlockOptions(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("message.redstone_utils.signal.usage"), false);
        source.sendSuccess(() -> Component.translatable("message.redstone_utils.signal.supported", SignalBlockVariant.suggestions()), false);
        return 1;
    }

    private static int showClockUsage(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("message.redstone_utils.clock.usage"), false);
        source.sendSuccess(() -> Component.translatable("message.redstone_utils.clock.comparator_range"), false);
        source.sendSuccess(() -> Component.translatable("message.redstone_utils.clock.hopper_range"), false);
        source.sendSuccess(() -> Component.translatable("message.redstone_utils.clock.example"), false);
        return 1;
    }

    private static int createComparatorClock(CommandSourceStack source, String intervalValue) throws CommandSyntaxException {
        ClockInterval.ParseResult parsed = ClockInterval.parse(intervalValue);
        if (!parsed.successful()) {
            source.sendFailure(parsed.error());
            return 0;
        }
        if (parsed.interval().ticks() > RedstoneUtilsServerConfig.maxComparatorClockTicks()) {
            source.sendFailure(Component.translatable(
                    "message.redstone_utils.clock.server_limit",
                    RedstoneUtilsServerConfig.maxComparatorClockTicks()
            ));
            return 0;
        }

        ClockManager.Result result = ComparatorClockManager.create(source.getPlayerOrException(), parsed.interval());
        return sendClockResult(source, result);
    }

    private static int createHopperClock(CommandSourceStack source, String intervalValue) throws CommandSyntaxException {
        HopperClockInterval.ParseResult parsed = HopperClockInterval.parse(intervalValue);
        if (!parsed.successful()) {
            source.sendFailure(parsed.error());
            return 0;
        }
        if (parsed.interval().exceedsRedstoneTickLimit(RedstoneUtilsServerConfig.maxHopperClockTicks())) {
            source.sendFailure(Component.translatable(
                    "message.redstone_utils.clock.server_limit",
                    RedstoneUtilsServerConfig.maxHopperClockTicks()
            ));
            return 0;
        }

        ClockManager.Result result = HopperClockManager.create(source.getPlayerOrException(), parsed.interval());
        return sendClockResult(source, result);
    }

    private static int undoClock(CommandSourceStack source) throws CommandSyntaxException {
        ClockManager.Result result = ClockManager.undo(source.getPlayerOrException());
        return sendClockResult(source, result);
    }

    private static int sendClockResult(CommandSourceStack source, ClockManager.Result result) {
        if (!result.successful()) {
            source.sendFailure(result.message());
            return 0;
        }

        source.sendSuccess(result::message, false);
        return 1;
    }

    private static int applyHistoryResult(CommandSourceStack source, ChangeHistory.Result result) {
        if (!result.successful()) {
            source.sendFailure(result.message());
            return 0;
        }
        source.sendSuccess(result::message, false);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestSignalBlocks(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        for (SignalBlockVariant variant : SignalBlockVariant.orderedValues()) {
            builder.suggest(variant.key());
        }
        return builder.buildFuture();
    }

    private static int giveStack(CommandSourceStack source, ItemStack stack, Component feedback) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack remaining = stack.copy();
        if (!player.addItem(remaining) && !remaining.isEmpty()) {
            player.drop(remaining, false, true);
        }

        source.sendSuccess(() -> feedback, false);
        return 1;
    }

    private static int setContent(
            CommandContext<CommandSourceStack> context,
            String customName
    ) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        BlockPos blockPos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        ItemStack itemStack = containerItem(
                source,
                ItemArgument.getItem(context, "item"),
                customName
        ).orElse(null);
        if (itemStack == null) return 0;

        TargetedContainer target = resolveContainerAt(source, blockPos, itemStack).orElse(null);
        if (target == null) return 0;
        int actualAmount = clampToCapacity(target, amount);
        return setContainerContent(source, target, actualAmount, Component.translatable(
                "message.redstone_utils.signal.set_content",
                blockPos.getX(),
                blockPos.getY(),
                blockPos.getZ(),
                actualAmount,
                itemStack.getHoverName()
        ));
    }

    private static int setSignal(
            CommandContext<CommandSourceStack> context,
            String customName
    ) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        BlockPos blockPos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        int strength = IntegerArgumentType.getInteger(context, "strength");
        ItemStack itemStack = containerItem(
                source,
                ItemArgument.getItem(context, "item"),
                customName
        ).orElse(null);
        if (itemStack == null) return 0;

        TargetedContainer target = resolveContainerAt(source, blockPos, itemStack).orElse(null);
        if (target == null) return 0;

        int slotMaxStackSize = slotMaxStackSize(target.container(), target.itemStack());
        int amount = ComparatorSignal.amountForSignal(strength, target.container().getContainerSize(), slotMaxStackSize);
        return setContainerContent(
                source,
                target,
                amount,
                Component.translatable(
                        "message.redstone_utils.signal.set_signal",
                        blockPos.getX(),
                        blockPos.getY(),
                        blockPos.getZ(),
                        strength,
                        amount,
                        itemStack.getHoverName()
                )
        );
    }

    private static int setContainerContent(CommandSourceStack source, TargetedContainer target, int amount, Component feedback) {
        ServerPlayer player = source.getPlayer();
        if (player == null || !player.mayInteract(target.level(), target.blockPos())) {
            source.sendFailure(Component.translatable("message.redstone_utils.no_edit_permission"));
            return 0;
        }
        ChangeHistory.Transaction transaction = ChangeHistory.begin(
                player,
                Component.translatable("history.redstone_utils.container")
        );
        transaction.capture(target.level(), target.blockPos());
        target.container().clearContent();

        int remaining = clampToCapacity(target, amount);
        int slotMaxStackSize = slotMaxStackSize(target.container(), target.itemStack());
        for (int slot = 0; slot < target.container().getContainerSize() && remaining > 0 && slotMaxStackSize > 0; slot++) {
            int stackSize = Math.min(remaining, slotMaxStackSize);
            target.container().setItem(slot, target.itemStack().copyWithCount(stackSize));
            remaining -= stackSize;
        }

        target.container().setChanged();
        target.blockEntity().setChanged();
        target.level().sendBlockUpdated(target.blockPos(), target.blockState(), target.level().getBlockState(target.blockPos()), 3);
        target.level().updateNeighbourForOutputSignal(target.blockPos(), target.blockState().getBlock());
        transaction.commit();

        source.sendSuccess(() -> feedback, false);
        return 1;
    }

    private static int clampToCapacity(TargetedContainer target, int amount) {
        int slotMaximum = slotMaxStackSize(target.container(), target.itemStack());
        long capacity = (long) target.container().getContainerSize() * slotMaximum;
        return (int) Math.min(Math.max(0L, amount), Math.min(Integer.MAX_VALUE, capacity));
    }

    private static Optional<TargetedContainer> resolveContainerAt(
            CommandSourceStack source,
            BlockPos blockPos,
            ItemStack itemStack
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.level();

        double maxRange = RedstoneUtilsServerConfig.maxTargetRange();
        if (player.getEyePosition().distanceToSqr(Vec3.atCenterOf(blockPos)) > maxRange * maxRange) {
            source.sendFailure(Component.translatable(
                    "message.redstone_utils.container_out_of_range",
                    blockPos.getX(),
                    blockPos.getY(),
                    blockPos.getZ(),
                    maxRange
            ));
            return Optional.empty();
        }

        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (!(blockEntity instanceof Container container)) {
            source.sendFailure(Component.translatable("message.redstone_utils.not_container"));
            return Optional.empty();
        }

        return Optional.of(new TargetedContainer(
                level,
                blockPos,
                level.getBlockState(blockPos),
                blockEntity,
                container,
                itemStack
        ));
    }

    private static Optional<ItemStack> containerItem(
            CommandSourceStack source,
            ItemInput itemInput,
            String customName
    ) throws CommandSyntaxException {
        if (customName.length() > MAX_CUSTOM_ITEM_NAME_LENGTH) {
            source.sendFailure(Component.translatable(
                    "message.redstone_utils.item_name_too_long",
                    MAX_CUSTOM_ITEM_NAME_LENGTH
            ));
            return Optional.empty();
        }

        ItemStack itemStack = itemInput.createItemStack(1);
        if (itemStack.isEmpty()) {
            source.sendFailure(Component.translatable(
                    "message.redstone_utils.invalid_container_item"
            ));
            return Optional.empty();
        }
        if (!customName.isBlank()) {
            itemStack.set(DataComponents.CUSTOM_NAME, Component.literal(customName));
        }
        return Optional.of(itemStack);
    }

    private static int slotMaxStackSize(Container container, ItemStack itemStack) {
        if (itemStack.isEmpty()) return 0;
        return Math.clamp(itemStack.getMaxStackSize(), 0, container.getMaxStackSize(itemStack));
    }

    private static int teleport(CommandSourceStack source, double maxRange) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        teleportPlayer(player, maxRange);
        return 1;
    }

    public static Vec3 teleportPlayer(ServerPlayer player, double maxRange) {
        double clampedRange = Math.clamp(maxRange, MIN_TELEPORT_RANGE, RedstoneUtilsServerConfig.maxTeleportRange());
        Vec3 targetPos = targetedTeleportPosition(player, clampedRange);
        player.teleportTo(targetPos.x, targetPos.y, targetPos.z);
        player.sendSystemMessage(Component.translatable("message.redstone_utils.teleported_exact", formatPos(targetPos)));
        return targetPos;
    }

    private static Vec3 targetedTeleportPosition(ServerPlayer player, double maxRange) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 viewVec = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(viewVec.scale(maxRange));

        BlockHitResult hitResult = player.level().clip(new ClipContext(
                eyePos,
                endPos,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));

        return hitResult.getType() == HitResult.Type.BLOCK ? hitResult.getLocation() : endPos;
    }

    private static Optional<BlockHitResult> targetedBlock(ServerPlayer player, double maxRange) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 viewVec = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(viewVec.scale(maxRange));

        BlockHitResult hitResult = player.level().clip(new ClipContext(
                eyePos,
                endPos,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return Optional.empty();
        }
        return Optional.of(hitResult);
    }

    private static String formatPos(Vec3 pos) {
        return String.format(Locale.ROOT, "%.2f %.2f %.2f", pos.x, pos.y, pos.z);
    }

    private static Component wireName(WireType wireType) {
        return Component.translatable("wire_type.redstone_utils." + wireType.key());
    }

    private static Component signalName(SignalBlockVariant variant) {
        return Component.translatable("signal_block.redstone_utils." + variant.key());
    }

    private record TargetedContainer(
            ServerLevel level,
            BlockPos blockPos,
            BlockState blockState,
            BlockEntity blockEntity,
            Container container,
            ItemStack itemStack
    ) {
    }
}
