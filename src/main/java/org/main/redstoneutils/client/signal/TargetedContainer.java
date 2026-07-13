package org.main.redstoneutils.client.signal;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Optional;

record TargetedContainer(
        FabricClientCommandSource source,
        Minecraft client,
        LocalPlayer player,
        BlockPos blockPos,
        BlockState blockState,
        Container container,
        ItemStack heldStack
) {

    static Optional<TargetedContainer> resolve(FabricClientCommandSource source, boolean requiresHeldItem) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            source.sendFeedback(Component.literal("No active player"));
            return Optional.empty();
        }
        if (!(client.hitResult instanceof BlockHitResult blockHitResult) || blockHitResult.getType() != HitResult.Type.BLOCK) {
            source.sendFeedback(Component.literal("Look at a container block first"));
            return Optional.empty();
        }

        ItemStack heldStack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (requiresHeldItem && heldStack.isEmpty()) {
            source.sendFeedback(Component.literal("Hold the item you want to set in your main hand"));
            return Optional.empty();
        }

        BlockPos blockPos = blockHitResult.getBlockPos();
        BlockState blockState = client.level.getBlockState(blockPos);
        if (!(client.level.getBlockEntity(blockPos) instanceof Container container)) {
            source.sendFeedback(Component.literal("Target block has no item container"));
            return Optional.empty();
        }

        return Optional.of(new TargetedContainer(source, client, player, blockPos, blockState, container, heldStack));
    }
}
