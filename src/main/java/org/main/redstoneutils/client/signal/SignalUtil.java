package org.main.redstoneutils.client.signal;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;

public final class SignalUtil {

    private static boolean initialized = false;

    private SignalUtil() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        ClientCommandRegistrationCallback.EVENT.register(((dispatcher, buildContext) -> {
            dispatcher.register(ClientCommands.literal("signal")
                    .requires(FabricClientCommandSource::attended)
                    .then(ClientCommands.argument("strength", IntegerArgumentType.integer(ComparatorSignal.MIN, ComparatorSignal.MAX))
                            .executes(context ->
                                    giveSignalStrength(context.getSource(), IntegerArgumentType.getInteger(context, "strength"))
                            )
                            .then(ClientCommands.literal("optimal")
                                    .executes(context ->
                                            giveOptimalSignalStrength(context.getSource(), IntegerArgumentType.getInteger(context, "strength"))
                                    )
                            )
                            .then(ClientCommands.literal("block")
                                    .executes(context ->
                                            showSignalBlockOptions(context.getSource())
                                    )
                                    .then(ClientCommands.argument("block", StringArgumentType.word())
                                            .suggests(SignalUtil::suggestSignalBlocks)
                                            .executes(context ->
                                                    createSignalBlock(
                                                            context.getSource(),
                                                            IntegerArgumentType.getInteger(context, "strength"),
                                                            StringArgumentType.getString(context, "block")
                                                    )
                                            )
                                    )
                            )
                            .then(ClientCommands.argument("block", StringArgumentType.word())
                                    .suggests(SignalUtil::suggestSignalBlocks)
                                    .executes(context ->
                                            createSignalBlock(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "strength"),
                                                    StringArgumentType.getString(context, "block")
                                            )
                                    )
                            )
                    )
            );
            dispatcher.register(ClientCommands.literal("set-content")
                    .requires(FabricClientCommandSource::attended)
                    .then(ClientCommands.argument("amount", IntegerArgumentType.integer(0))
                            .executes(context ->
                                    setContent(context.getSource(), IntegerArgumentType.getInteger(context, "amount"))
                            ))
            );
            dispatcher.register(ClientCommands.literal("set-signal")
                    .requires(FabricClientCommandSource::attended)
                    .then(ClientCommands.argument("strength", IntegerArgumentType.integer(ComparatorSignal.MIN, ComparatorSignal.MAX))
                            .executes(context ->
                                    setSignal(context.getSource(), IntegerArgumentType.getInteger(context, "strength"))
                            ))
            );
        }
        ));
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

    private static int setContent(FabricClientCommandSource source, int amount) {
        return setContainerContent(source, amount, Component.translatable("message.redstoneutils.signal.sent_content", amount));
    }

    private static int setSignal(FabricClientCommandSource source, int strength) {
        TargetedContainer target = TargetedContainer.resolve(source, strength > 0).orElse(null);
        if (target == null) return 0;

        int slotMaxStackSize = ContainerBlockArgument.slotMaxStackSize(target.container(), target.heldStack());
        int amount = ComparatorSignal.amountForSignal(strength, target.container().getContainerSize(), slotMaxStackSize);
        return setContainerContent(target, amount, Component.translatable("message.redstoneutils.signal.sent_signal", strength, amount));
    }

    private static int setContainerContent(FabricClientCommandSource source, int amount, Component feedback) {
        TargetedContainer target = TargetedContainer.resolve(source, amount > 0).orElse(null);
        if (target == null) return 0;

        return setContainerContent(target, amount, feedback);
    }

    private static int setContainerContent(TargetedContainer target, int amount, Component feedback) {
        String blockArgument = ContainerBlockArgument.create(
                target.client(),
                target.blockState(),
                target.container(),
                target.heldStack(),
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
}
