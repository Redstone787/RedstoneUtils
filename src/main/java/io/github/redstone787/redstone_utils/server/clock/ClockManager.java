/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstone_utils.server.clock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RedstoneSide;

import io.github.redstone787.redstone_utils.server.history.ChangeHistory;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ClockManager {

    private static final int SET_BLOCK_FLAGS = Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS;

    private ClockManager() {
    }

    static Result build(ServerPlayer player, BuildPlan plan, Component description) {
        ServerLevel level = player.level();

        for (BlockPos blockPos : plan.positions()) {
            if (!level.isInWorldBounds(blockPos)) {
                return Result.failure(Component.translatable("message.redstone_utils.clock.outside_world"));
            }
            if (!player.mayInteract(level, blockPos)) {
                return Result.failure(Component.translatable("message.redstone_utils.clock.no_permission", formatPos(blockPos)));
            }
        }

        ChangeHistory.Transaction transaction = ChangeHistory.begin(
                player,
                Component.translatable("history.redstone_utils.clock")
        );
        for (BlockPos blockPos : plan.positions()) transaction.capture(level, blockPos);
        try {
            place(level, plan.supportBlocks());
            place(level, plan.components());
            initializeContainers(level, plan.containerContents());
            refreshRedstone(level, plan.components());
            for (Map.Entry<BlockPos, Block> scheduledTick : plan.scheduledTicks().entrySet()) {
                level.scheduleTick(scheduledTick.getKey(), scheduledTick.getValue(), 1);
            }
        } catch (RuntimeException exception) {
            transaction.rollback();
            return Result.failure(Component.translatable("message.redstone_utils.clock.failed_restored"));
        }

        transaction.commit();
        return Result.success(
                Component.translatable("message.redstone_utils.clock.created", description, plan.positions().size())
        );
    }

    public static Result undo(ServerPlayer player) {
        ChangeHistory.Result result = ChangeHistory.undo(player);
        return new Result(result.successful(), result.message());
    }

    static BlockPos localPos(
            BlockPos origin,
            Direction forward,
            Direction right,
            int forwardOffset,
            int rightOffset
    ) {
        return origin.relative(forward, forwardOffset).relative(right, rightOffset).immutable();
    }

    static BlockState supportState(ServerPlayer player, ServerLevel level, BlockPos supportPos) {
        ItemStack heldStack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (heldStack.getItem() instanceof BlockItem blockItem) {
            BlockState heldBlockState = blockItem.getBlock().defaultBlockState();
            if (!(blockItem.getBlock() instanceof FallingBlock)
                    && !heldBlockState.isSignalSource()
                    && heldBlockState.isRedstoneConductor(level, supportPos)
                    && heldBlockState.isCollisionShapeFullBlock(level, supportPos)
                    && heldBlockState.isFaceSturdy(level, supportPos, Direction.UP, SupportType.RIGID)) {
                return heldBlockState;
            }
        }

        return Blocks.WOOL.white().defaultBlockState();
    }

    static BlockState redstoneWireState() {
        BlockState blockState = Blocks.REDSTONE_WIRE.defaultBlockState();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            blockState = blockState.setValue(
                    RedStoneWireBlock.PROPERTY_BY_DIRECTION.get(direction),
                    RedstoneSide.SIDE
            );
        }
        return blockState;
    }

    private static void place(ServerLevel level, Map<BlockPos, BlockState> blocks) {
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            level.setBlock(entry.getKey(), entry.getValue(), SET_BLOCK_FLAGS);
            if (!level.getBlockState(entry.getKey()).is(entry.getValue().getBlock())) {
                throw new IllegalStateException("Block placement failed at " + entry.getKey());
            }
        }
    }

    private static void initializeContainers(ServerLevel level, Map<BlockPos, List<ItemStack>> containerContents) {
        for (Map.Entry<BlockPos, List<ItemStack>> entry : containerContents.entrySet()) {
            BlockEntity blockEntity = level.getBlockEntity(entry.getKey());
            if (!(blockEntity instanceof Container container)) {
                throw new IllegalStateException("Container placement failed at " + entry.getKey());
            }

            container.clearContent();
            List<ItemStack> stacks = entry.getValue();
            if (stacks.size() > container.getContainerSize()) {
                throw new IllegalStateException("Too many item stacks for container at " + entry.getKey());
            }
            for (int slot = 0; slot < stacks.size(); slot++) {
                container.setItem(slot, stacks.get(slot).copy());
            }

            container.setChanged();
            blockEntity.setChanged();
            BlockState blockState = level.getBlockState(entry.getKey());
            level.sendBlockUpdated(entry.getKey(), blockState, blockState, Block.UPDATE_ALL);
            level.updateNeighbourForOutputSignal(entry.getKey(), blockState.getBlock());
        }
    }

    private static void refreshRedstone(ServerLevel level, Map<BlockPos, BlockState> components) {
        for (BlockPos blockPos : components.keySet()) {
            BlockState currentState = level.getBlockState(blockPos);
            level.updateNeighborsAt(blockPos, currentState.getBlock());
        }
    }

    private static String formatPos(BlockPos blockPos) {
        return blockPos.getX() + " " + blockPos.getY() + " " + blockPos.getZ();
    }

    public record Result(boolean successful, Component message) {

        private static Result success(Component message) {
            return new Result(true, message);
        }

        private static Result failure(Component message) {
            return new Result(false, message);
        }
    }

    record BuildPlan(
            Map<BlockPos, BlockState> supportBlocks,
            Map<BlockPos, BlockState> components,
            Map<BlockPos, List<ItemStack>> containerContents,
            Map<BlockPos, Block> scheduledTicks
    ) {

        Set<BlockPos> positions() {
            Set<BlockPos> positions = new LinkedHashSet<>(supportBlocks.keySet());
            positions.addAll(components.keySet());
            positions.addAll(containerContents.keySet());
            return positions;
        }
    }

}
