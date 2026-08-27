package org.main.redstoneutils.client.signal;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.main.redstoneutils.client.RedstoneUtilsClientNetworking;

import java.util.concurrent.CompletableFuture;

public final class SignalUtil {

    private static final int MAX_CUSTOM_ITEM_NAME_LENGTH = 256;
    private static boolean initialized = false;

    private SignalUtil() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        ClientCommandRegistrationCallback.EVENT.register(((dispatcher, buildContext) -> {
            dispatcher.register(signalCommand(buildContext));
            dispatcher.register(ClientCommands.literal("redstoneutils").then(signalCommand(buildContext)));
            dispatcher.register(ClientCommands.literal("set-content")
                    .requires(FabricClientCommandSource::attended)
                    .then(ClientCommands.argument("x", StringArgumentType.word())
                            .then(ClientCommands.argument("y", StringArgumentType.word())
                                    .then(ClientCommands.argument("z", StringArgumentType.word())
                                            .then(ClientCommands.argument("amount", IntegerArgumentType.integer(0))
                                                    .then(ClientCommands.argument("item", ItemArgument.item(buildContext))
                                                            .executes(context -> setContent(context, ""))
                                                            .then(ClientCommands.argument("name", StringArgumentType.string())
                                                                    .executes(context -> setContent(
                                                                            context,
                                                                            StringArgumentType.getString(context, "name")
                                                                    ))))))))
            );
            dispatcher.register(ClientCommands.literal("set-signal")
                    .requires(FabricClientCommandSource::attended)
                    .then(ClientCommands.argument("x", StringArgumentType.word())
                            .then(ClientCommands.argument("y", StringArgumentType.word())
                                    .then(ClientCommands.argument("z", StringArgumentType.word())
                                            .then(ClientCommands.argument("strength", IntegerArgumentType.integer(ComparatorSignal.MIN, ComparatorSignal.MAX))
                                                    .then(ClientCommands.argument("item", ItemArgument.item(buildContext))
                                                            .executes(context -> setSignal(context, ""))
                                                            .then(ClientCommands.argument("name", StringArgumentType.string())
                                                                    .executes(context -> setSignal(
                                                                            context,
                                                                            StringArgumentType.getString(context, "name")
                                                                    ))))))))
            );
        }
        ));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> signalCommand(CommandBuildContext buildContext) {
        return ClientCommands.literal("signal")
                    .requires(FabricClientCommandSource::attended)
                    .then(ClientCommands.argument("strength", IntegerArgumentType.integer(ComparatorSignal.MIN, ComparatorSignal.MAX))
                            .executes(SignalUtil::executeSignalStrength)
                            .then(ClientCommands.literal("optimal")
                                    .executes(SignalUtil::executeOptimalSignalStrength)
                            )
                            .then(ClientCommands.literal("block")
                                    .executes(context ->
                                            showSignalBlockOptions(context.getSource())
                                    )
                                    .then(ClientCommands.argument("block", StringArgumentType.word())
                                            .suggests(SignalUtil::suggestSignalBlocks)
                                            .executes(SignalUtil::executeSignalBlock)
                                    )
                            )
                            .then(ClientCommands.argument("block", StringArgumentType.word())
                                    .suggests(SignalUtil::suggestSignalBlocks)
                                    .executes(SignalUtil::executeSignalBlock)
                            )
                    )
                    .then(ClientCommands.literal("optimal")
                            .then(ClientCommands.argument("strength", IntegerArgumentType.integer(ComparatorSignal.MIN, ComparatorSignal.MAX))
                                    .executes(SignalUtil::executeOptimalSignalStrength)))
                    .then(ClientCommands.literal("block")
                            .executes(context -> showSignalBlockOptions(context.getSource()))
                            .then(ClientCommands.argument("block", StringArgumentType.word())
                                    .suggests(SignalUtil::suggestSignalBlocks)
                                    .then(ClientCommands.argument("strength", IntegerArgumentType.integer(ComparatorSignal.MIN, ComparatorSignal.MAX))
                                            .executes(SignalUtil::executeSignalBlock))))
                    .then(ClientCommands.literal("container")
                            .then(signalContainerContentCommand(buildContext))
                            .then(signalContainerStrengthCommand(buildContext)))
            ;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> signalContainerContentCommand(CommandBuildContext buildContext) {
        return ClientCommands.literal("content")
                .then(ClientCommands.argument("x", StringArgumentType.word())
                        .then(ClientCommands.argument("y", StringArgumentType.word())
                                .then(ClientCommands.argument("z", StringArgumentType.word())
                                        .then(ClientCommands.argument("amount", IntegerArgumentType.integer(0))
                                                .then(ClientCommands.argument("item", ItemArgument.item(buildContext))
                                                        .executes(context -> setContent(context, ""))
                                                        .then(ClientCommands.argument("name", StringArgumentType.string())
                                                                .executes(context -> setContent(
                                                                        context,
                                                                        StringArgumentType.getString(context, "name")
                                                                ))))))));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> signalContainerStrengthCommand(CommandBuildContext buildContext) {
        return ClientCommands.literal("strength")
                .then(ClientCommands.argument("x", StringArgumentType.word())
                        .then(ClientCommands.argument("y", StringArgumentType.word())
                                .then(ClientCommands.argument("z", StringArgumentType.word())
                                        .then(ClientCommands.argument("strength", IntegerArgumentType.integer(ComparatorSignal.MIN, ComparatorSignal.MAX))
                                                .then(ClientCommands.argument("item", ItemArgument.item(buildContext))
                                                        .executes(context -> setSignal(context, ""))
                                                        .then(ClientCommands.argument("name", StringArgumentType.string())
                                                                .executes(context -> setSignal(
                                                                        context,
                                                                        StringArgumentType.getString(context, "name")
                                                                ))))))));
    }

    private static int executeSignalStrength(CommandContext<FabricClientCommandSource> context) {
        if (hasServerBackend()) return forwardToServer(context);
        return giveSignalStrength(
                context.getSource(),
                IntegerArgumentType.getInteger(context, "strength")
        );
    }

    private static int executeOptimalSignalStrength(CommandContext<FabricClientCommandSource> context) {
        if (hasServerBackend()) return forwardToServer(context);
        return giveOptimalSignalStrength(
                context.getSource(),
                IntegerArgumentType.getInteger(context, "strength")
        );
    }

    private static int executeSignalBlock(CommandContext<FabricClientCommandSource> context) {
        if (hasServerBackend()) return forwardToServer(context);
        return createSignalBlock(
                context.getSource(),
                IntegerArgumentType.getInteger(context, "strength"),
                StringArgumentType.getString(context, "block")
        );
    }

    private static boolean hasServerBackend() {
        return RedstoneUtilsClientNetworking.hasServerBackend();
    }

    private static int forwardToServer(CommandContext<FabricClientCommandSource> context) {
        return RedstoneUtilsClientNetworking.sendServerCommand(context.getInput()) ? 1 : 0;
    }

    private static int giveSignalStrength(FabricClientCommandSource source, int strength) {
        if (!ComparatorSignal.isValid(strength)) return 0;

        String command = "give @s " + SignalBarrelItem.create(strength);
        source.getPlayer().connection.sendCommand(command);
        source.sendFeedback(Component.translatable("message.redstoneutils.signal.sent_barrel", strength));

        return 1;
    }

    private static int giveOptimalSignalStrength(FabricClientCommandSource source, int strength) {
        if (!ComparatorSignal.isValid(strength)) return 0;

        String command = "give @s " + SignalBlockItem.createOptimal(strength);
        source.getPlayer().connection.sendCommand(command);
        source.sendFeedback(Component.translatable("message.redstoneutils.signal.sent_optimal", strength));

        return 1;
    }

    private static int createSignalBlock(FabricClientCommandSource source, int strength, String blockName) {
        SignalBlockVariant variant = SignalBlockVariant.find(blockName).orElse(null);
        if (variant == null) {
            source.sendFeedback(Component.translatable("message.redstoneutils.signal.unknown", SignalBlockVariant.suggestions()));
            return 0;
        }
        if (!variant.supports(strength)) {
            source.sendFeedback(Component.literal(variant.unsupportedStrengthMessage()));
            return 0;
        }

        String argument = variant.createArgument(strength);
        if (variant.targetBlock()) {
            return setTargetedSignalBlock(source, strength, variant, argument);
        }

        String command = "give @s " + argument;
        source.getPlayer().connection.sendCommand(command);
        source.sendFeedback(Component.translatable("message.redstoneutils.signal.sent_give", variant.displayName(), strength));
        return 1;
    }

    private static int setTargetedSignalBlock(FabricClientCommandSource source, int strength, SignalBlockVariant variant, String blockArgument) {
        TargetedBlock target = TargetedBlock.resolve(source).orElse(null);
        if (target == null) return 0;

        String command = "setblock "
                + target.blockPos().getX() + " "
                + target.blockPos().getY() + " "
                + target.blockPos().getZ() + " "
                + blockArgument
                + " replace";

        target.player().connection.sendCommand(command);
        target.source().sendFeedback(Component.translatable("message.redstoneutils.signal.sent_setblock", variant.displayName(), strength));
        return 1;
    }

    private static int showSignalBlockOptions(FabricClientCommandSource source) {
        source.sendFeedback(Component.translatable("message.redstoneutils.signal.usage"));
        source.sendFeedback(Component.translatable("message.redstoneutils.signal.supported", SignalBlockVariant.suggestions()));
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestSignalBlocks(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        for (SignalBlockVariant variant : SignalBlockVariant.orderedValues()) {
            builder.suggest(variant.key());
        }
        return builder.buildFuture();
    }

    private static int setContent(
            CommandContext<FabricClientCommandSource> context,
            String customName
    ) throws CommandSyntaxException {
        if (hasServerBackend()) return forwardToServer(context);

        ItemStack itemStack = containerItem(
                context.getSource(),
                ItemArgument.getItem(context, "item"),
                customName
        );
        if (itemStack.isEmpty()) return 0;

        TargetedContainer target = TargetedContainer.resolve(context.getSource(), blockPos(context), itemStack).orElse(null);
        if (target == null) return 0;
        int actualAmount = clampToCapacity(target, IntegerArgumentType.getInteger(context, "amount"));
        return setContainerContent(target, actualAmount, Component.translatable(
                "message.redstoneutils.signal.sent_content",
                actualAmount
        ));
    }

    private static int setSignal(
            CommandContext<FabricClientCommandSource> context,
            String customName
    ) throws CommandSyntaxException {
        if (hasServerBackend()) return forwardToServer(context);

        FabricClientCommandSource source = context.getSource();
        int strength = IntegerArgumentType.getInteger(context, "strength");
        ItemStack itemStack = containerItem(
                source,
                ItemArgument.getItem(context, "item"),
                customName
        );
        if (itemStack.isEmpty()) return 0;

        TargetedContainer target = TargetedContainer.resolve(
                source,
                blockPos(context),
                itemStack
        ).orElse(null);
        if (target == null) return 0;

        int slotMaxStackSize = ContainerBlockArgument.slotMaxStackSize(target.container(), target.itemStack());
        int amount = ComparatorSignal.amountForSignal(strength, target.container().getContainerSize(), slotMaxStackSize);
        return setContainerContent(target, amount, Component.translatable("message.redstoneutils.signal.sent_signal", strength, amount));
    }

    private static BlockPos blockPos(CommandContext<FabricClientCommandSource> context) throws CommandSyntaxException {
        BlockPos origin = context.getSource().getPlayer().blockPosition();
        return new BlockPos(
                blockCoordinate(context, "x", origin.getX()),
                blockCoordinate(context, "y", origin.getY()),
                blockCoordinate(context, "z", origin.getZ())
        );
    }

    private static int blockCoordinate(
            CommandContext<FabricClientCommandSource> context,
            String name,
            int origin
    ) throws CommandSyntaxException {
        String value = StringArgumentType.getString(context, name);
        return (int) Math.floor(WorldCoordinate.parseInt(new StringReader(value)).get(origin));
    }

    private static ItemStack containerItem(
            FabricClientCommandSource source,
            ItemInput itemInput,
            String customName
    ) throws CommandSyntaxException {
        if (customName.length() > MAX_CUSTOM_ITEM_NAME_LENGTH) {
            source.sendFeedback(Component.translatable(
                    "message.redstoneutils.item_name_too_long",
                    MAX_CUSTOM_ITEM_NAME_LENGTH
            ));
            return ItemStack.EMPTY;
        }

        ItemStack itemStack = itemInput.createItemStack(1);
        if (itemStack.isEmpty()) {
            source.sendFeedback(Component.translatable(
                    "message.redstoneutils.invalid_container_item"
            ));
            return ItemStack.EMPTY;
        }
        if (!customName.isBlank()) {
            itemStack.set(DataComponents.CUSTOM_NAME, Component.literal(customName));
        }
        return itemStack;
    }

    private static int setContainerContent(TargetedContainer target, int amount, Component feedback) {
        amount = clampToCapacity(target, amount);
        String blockArgument = ContainerBlockArgument.create(
                target.client(),
                target.blockState(),
                target.blockEntity(),
                target.container(),
                target.itemStack(),
                amount
        );
        String command = "setblock "
                + target.blockPos().getX() + " "
                + target.blockPos().getY() + " "
                + target.blockPos().getZ() + " "
                + blockArgument
                + " replace";

        target.player().connection.sendCommand(command);
        target.source().sendFeedback(feedback);
        return 1;
    }

    private static int clampToCapacity(TargetedContainer target, int amount) {
        int slotMaximum = ContainerBlockArgument.slotMaxStackSize(target.container(), target.itemStack());
        long capacity = (long) target.container().getContainerSize() * slotMaximum;
        return (int) Math.min(Math.max(0L, amount), Math.min(Integer.MAX_VALUE, capacity));
    }
}
