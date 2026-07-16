package org.main.redstoneutils.server;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.main.redstoneutils.network.RedstoneUtilsNetworking;
import org.main.redstoneutils.server.autowire.ServerAutoWire;
import org.main.redstoneutils.server.autowire.WireType;
import org.main.redstoneutils.server.signal.ComparatorSignal;
import org.main.redstoneutils.server.signal.SignalBlockVariant;
import org.main.redstoneutils.server.signal.SignalItemFactory;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class RedstoneUtilsCommands {

    private static final double DEFAULT_TARGET_RANGE = 128.0D;
    private static final double DEFAULT_TELEPORT_RANGE = 100.0D;
    private static final double MIN_TELEPORT_RANGE = 10.0D;
    private static final double MAX_TELEPORT_RANGE = 1000.0D;
    private static final String ARG_VISIBLE = "visible";
    private static final String CLIENT_CONFIG = "config";
    private static final String CLIENT_MACROS = "macros";
    private static final String CLIENT_WIRE_OVERLAY = "wire_overlay";
    private static final String CLIENT_SCULK_OVERLAY = "sculk_overlay";
    private static final String CLIENT_ALL_OVERLAYS = "all_overlays";

    private RedstoneUtilsCommands() {
    }

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, environment) -> {
            dispatcher.register(Commands.literal("redstone_utils")
                    .then(Commands.literal("wire_overlay")
                            .executes(context -> sendClientAction(context.getSource(), CLIENT_WIRE_OVERLAY, -1))
                            .then(Commands.argument(ARG_VISIBLE, BoolArgumentType.bool())
                                    .executes(context -> sendClientAction(
                                            context.getSource(),
                                            CLIENT_WIRE_OVERLAY,
                                            booleanValue(context, ARG_VISIBLE)
                                    ))))
                    .then(Commands.literal("sculk_overlay")
                            .executes(context -> sendClientAction(context.getSource(), CLIENT_SCULK_OVERLAY, -1))
                            .then(Commands.argument(ARG_VISIBLE, BoolArgumentType.bool())
                                    .executes(context -> sendClientAction(
                                            context.getSource(),
                                            CLIENT_SCULK_OVERLAY,
                                            booleanValue(context, ARG_VISIBLE)
                                    ))))
                    .then(Commands.literal("all_overlays")
                            .executes(context -> sendClientAction(context.getSource(), CLIENT_ALL_OVERLAYS, -1))
                            .then(Commands.argument(ARG_VISIBLE, BoolArgumentType.bool())
                                    .executes(context -> sendClientAction(
                                            context.getSource(),
                                            CLIENT_ALL_OVERLAYS,
                                            booleanValue(context, ARG_VISIBLE)
                                    ))))
                    .then(Commands.literal("config")
                            .executes(context -> sendClientAction(context.getSource(), CLIENT_CONFIG, -1)))
                    .then(Commands.literal("macros")
                            .executes(context -> sendClientAction(context.getSource(), CLIENT_MACROS, -1)))
                    .then(Commands.literal("autowire")
                            .requires(RedstoneUtilsCommands::canUse)
                            .executes(RedstoneUtilsCommands::showAutoWire)
                            .then(Commands.argument("mode", StringArgumentType.word())
                                    .suggests(RedstoneUtilsCommands::suggestWireTypes)
                                    .executes(RedstoneUtilsCommands::setAutoWire)))
                    .then(Commands.literal("reset_autowire")
                            .requires(RedstoneUtilsCommands::canUse)
                            .executes(RedstoneUtilsCommands::resetAutoWire))
                    .then(Commands.literal("tp")
                            .requires(RedstoneUtilsCommands::canUse)
                            .executes(context -> teleport(context.getSource(), DEFAULT_TELEPORT_RANGE))
                            .then(Commands.argument("range", DoubleArgumentType.doubleArg(MIN_TELEPORT_RANGE, MAX_TELEPORT_RANGE))
                                    .executes(context -> teleport(context.getSource(), DoubleArgumentType.getDouble(context, "range")))))
            );

            dispatcher.register(Commands.literal("signal")
                    .requires(RedstoneUtilsCommands::canUse)
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
            );

            dispatcher.register(Commands.literal("set-content")
                    .requires(RedstoneUtilsCommands::canUse)
                    .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                            .executes(context -> setContent(
                                    context.getSource(),
                                    IntegerArgumentType.getInteger(context, "amount")
                            )))
            );

            dispatcher.register(Commands.literal("set-signal")
                    .requires(RedstoneUtilsCommands::canUse)
                    .then(Commands.argument("strength", IntegerArgumentType.integer(ComparatorSignal.MIN, ComparatorSignal.MAX))
                            .executes(context -> setSignal(
                                    context.getSource(),
                                    IntegerArgumentType.getInteger(context, "strength")
                            )))
            );
        });
    }

    public static boolean canUse(CommandSourceStack source) {
        if (Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source)) return true;

        ServerPlayer player = source.getPlayer();
        return player != null && player.getAbilities().instabuild;
    }

    private static int sendClientAction(CommandSourceStack source, String action, int value) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!ServerPlayNetworking.canSend(player, RedstoneUtilsNetworking.ClientCommandPayload.TYPE)) {
            source.sendFailure(Component.literal("This RedstoneUtils command needs the RedstoneUtils client mod"));
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
        context.getSource().sendSuccess(() -> Component.literal("AutoWire: " + wireType.displayName()), false);
        return 1;
    }

    private static int setAutoWire(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String mode = StringArgumentType.getString(context, "mode");
        WireType wireType = WireType.find(mode).orElse(null);
        if (wireType == null) {
            context.getSource().sendFailure(Component.literal("Unknown AutoWire mode. Supported: " + WireType.suggestions()));
            return 0;
        }

        ServerAutoWire.setWireType(player, wireType);
        context.getSource().sendSuccess(() -> Component.literal("AutoWire: " + wireType.displayName()), false);
        return 1;
    }

    private static int resetAutoWire(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerAutoWire.reset(context.getSource().getPlayerOrException());
        context.getSource().sendSuccess(() -> Component.literal("AutoWire state reset"), false);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestWireTypes(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        for (WireType wireType : WireType.values()) {
            builder.suggest(wireType.key());
        }
        return builder.buildFuture();
    }

    private static int giveSignalStrength(CommandSourceStack source, int strength) throws CommandSyntaxException {
        ItemStack stack = SignalBlockVariant.BARREL.createItem(strength, source.registryAccess());
        return giveStack(source, stack, "Gave signal barrel for strength " + strength);
    }

    private static int giveOptimalSignalStrength(CommandSourceStack source, int strength) throws CommandSyntaxException {
        ItemStack stack = SignalItemFactory.createOptimal(strength, source.registryAccess());
        return giveStack(source, stack, "Gave optimal signal block for strength " + strength);
    }

    private static int createSignalBlock(CommandSourceStack source, int strength, String blockName) throws CommandSyntaxException {
        SignalBlockVariant variant = SignalBlockVariant.find(blockName).orElse(null);
        if (variant == null) {
            source.sendFailure(Component.literal("Unknown signal block. Supported: " + SignalBlockVariant.suggestions()));
            return 0;
        }
        if (!variant.supports(strength)) {
            source.sendFailure(Component.literal(variant.unsupportedStrengthMessage()));
            return 0;
        }

        if (variant.targetBlock()) {
            return setTargetedSignalBlock(source, strength, variant);
        }

        ItemStack stack = variant.createItem(strength, source.registryAccess());
        return giveStack(source, stack, "Gave " + variant.displayName() + " for strength " + strength);
    }

    private static int setTargetedSignalBlock(CommandSourceStack source, int strength, SignalBlockVariant variant) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.level();
        BlockHitResult hitResult = targetedBlock(player, DEFAULT_TARGET_RANGE).orElse(null);
        if (hitResult == null) {
            source.sendFailure(Component.literal("Look at a block first"));
            return 0;
        }

        BlockPos blockPos = hitResult.getBlockPos();
        if (!player.mayInteract(level, blockPos)) {
            source.sendFailure(Component.literal("You may not edit that block"));
            return 0;
        }

        BlockState blockState = variant.createBlockState(strength);
        if (!level.setBlockAndUpdate(blockPos, blockState)) {
            source.sendFailure(Component.literal("Could not set target block"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Set " + variant.displayName() + " for strength " + strength), false);
        return 1;
    }

    private static int showSignalBlockOptions(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Usage: /signal <strength> block <type>"), false);
        source.sendSuccess(() -> Component.literal("Supported: " + SignalBlockVariant.suggestions()), false);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestSignalBlocks(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        for (SignalBlockVariant variant : SignalBlockVariant.orderedValues()) {
            builder.suggest(variant.key());
        }
        return builder.buildFuture();
    }

    private static int giveStack(CommandSourceStack source, ItemStack stack, String feedback) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack remaining = stack.copy();
        if (!player.addItem(remaining) && !remaining.isEmpty()) {
            player.drop(remaining, false, true);
        }

        source.sendSuccess(() -> Component.literal(feedback), false);
        return 1;
    }

    private static int setContent(CommandSourceStack source, int amount) throws CommandSyntaxException {
        return setContainerContent(source, amount, "Set target container content to " + amount + " items");
    }

    private static int setSignal(CommandSourceStack source, int strength) throws CommandSyntaxException {
        TargetedContainer target = resolveTargetedContainer(source, strength > 0).orElse(null);
        if (target == null) return 0;

        int slotMaxStackSize = slotMaxStackSize(target.container(), target.heldStack());
        int amount = ComparatorSignal.amountForSignal(strength, target.container().getContainerSize(), slotMaxStackSize);
        return setContainerContent(source, target, amount, "Set target container signal to " + strength + " (" + amount + " items)");
    }

    private static int setContainerContent(CommandSourceStack source, int amount, String feedback) throws CommandSyntaxException {
        TargetedContainer target = resolveTargetedContainer(source, amount > 0).orElse(null);
        if (target == null) return 0;

        return setContainerContent(source, target, amount, feedback);
    }

    private static int setContainerContent(CommandSourceStack source, TargetedContainer target, int amount, String feedback) {
        target.container().clearContent();

        int remaining = amount;
        int slotMaxStackSize = slotMaxStackSize(target.container(), target.heldStack());
        for (int slot = 0; slot < target.container().getContainerSize() && remaining > 0 && slotMaxStackSize > 0; slot++) {
            int stackSize = Math.min(remaining, slotMaxStackSize);
            target.container().setItem(slot, target.heldStack().copyWithCount(stackSize));
            remaining -= stackSize;
        }

        target.container().setChanged();
        target.blockEntity().setChanged();
        target.level().sendBlockUpdated(target.blockPos(), target.blockState(), target.level().getBlockState(target.blockPos()), 3);
        target.level().updateNeighbourForOutputSignal(target.blockPos(), target.blockState().getBlock());

        source.sendSuccess(() -> Component.literal(feedback), false);
        return 1;
    }

    private static Optional<TargetedContainer> resolveTargetedContainer(CommandSourceStack source, boolean requiresHeldItem) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.level();
        BlockHitResult hitResult = targetedBlock(player, DEFAULT_TARGET_RANGE).orElse(null);
        if (hitResult == null) {
            source.sendFailure(Component.literal("Look at a container block first"));
            return Optional.empty();
        }

        ItemStack heldStack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (requiresHeldItem && heldStack.isEmpty()) {
            source.sendFailure(Component.literal("Hold the item you want to set in your main hand"));
            return Optional.empty();
        }

        BlockPos blockPos = hitResult.getBlockPos();
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (!(blockEntity instanceof Container container)) {
            source.sendFailure(Component.literal("Target block has no item container"));
            return Optional.empty();
        }

        return Optional.of(new TargetedContainer(level, blockPos, level.getBlockState(blockPos), blockEntity, container, heldStack));
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
        double clampedRange = Math.clamp(maxRange, MIN_TELEPORT_RANGE, MAX_TELEPORT_RANGE);
        Vec3 targetPos = targetedTeleportPosition(player, clampedRange);
        player.teleportTo(targetPos.x, targetPos.y, targetPos.z);
        player.sendSystemMessage(Component.literal("Teleported to " + formatPos(targetPos)));
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

    private record TargetedContainer(
            ServerLevel level,
            BlockPos blockPos,
            BlockState blockState,
            BlockEntity blockEntity,
            Container container,
            ItemStack heldStack
    ) {
    }
}
