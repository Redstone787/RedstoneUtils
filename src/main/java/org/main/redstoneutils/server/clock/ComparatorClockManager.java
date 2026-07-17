package org.main.redstoneutils.server.clock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ComparatorMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ComparatorClockManager {

    private ComparatorClockManager() {
    }

    public static ClockManager.Result create(ServerPlayer player, ClockInterval interval) {
        ServerLevel level = player.level();
        Direction forward = player.getDirection();
        Direction right = forward.getClockWise();
        BlockPos origin = player.blockPosition();
        BlockState supportState = ClockManager.supportState(player, level, origin.below());
        ClockManager.BuildPlan plan = createPlan(origin, forward, right, supportState, interval.ticks());

        Component description = Component.translatable(
                "history.redstoneutils.clock.comparator",
                interval.ticks(),
                supportState.getBlock().getName(),
                Component.translatable("direction.minecraft." + forward.getName())
        );
        return ClockManager.build(player, plan, description);
    }

    private static ClockManager.BuildPlan createPlan(
            BlockPos origin,
            Direction forward,
            Direction right,
            BlockState supportState,
            int periodTicks
    ) {
        int totalRepeaterDelay = periodTicks / ClockInterval.COMPARATOR_TICK_STEP - 1;
        int repeaterCount = Math.ceilDiv(totalRepeaterDelay, 4);
        int loopLength = Math.ceilDiv(repeaterCount, 2);

        Map<BlockPos, BlockState> supports = new LinkedHashMap<>();
        Map<BlockPos, BlockState> components = new LinkedHashMap<>();

        for (int forwardOffset = -1; forwardOffset <= loopLength + 1; forwardOffset++) {
            for (int rightOffset = 0; rightOffset <= 1; rightOffset++) {
                BlockPos componentPos = ClockManager.localPos(origin, forward, right, forwardOffset, rightOffset);
                supports.put(componentPos.below(), supportState);
                components.put(componentPos, Blocks.AIR.defaultBlockState());
            }
        }

        BlockPos comparatorPos = ClockManager.localPos(origin, forward, right, 0, 0);
        components.put(ClockManager.localPos(origin, forward, right, -1, 0), Blocks.REDSTONE_BLOCK.defaultBlockState());
        components.put(comparatorPos, Blocks.COMPARATOR.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, forward.getOpposite())
                .setValue(ComparatorBlock.MODE, ComparatorMode.SUBTRACT));
        components.put(ClockManager.localPos(origin, forward, right, 0, 1), ClockManager.redstoneWireState());

        for (int forwardOffset = 1; forwardOffset <= loopLength; forwardOffset++) {
            components.put(
                    ClockManager.localPos(origin, forward, right, forwardOffset, 0),
                    ClockManager.redstoneWireState()
            );
            components.put(
                    ClockManager.localPos(origin, forward, right, forwardOffset, 1),
                    ClockManager.redstoneWireState()
            );
        }

        List<PathStep> additionalRepeaterSlots = new ArrayList<>();
        for (int forwardOffset = 1; forwardOffset <= loopLength; forwardOffset++) {
            additionalRepeaterSlots.add(new PathStep(
                    ClockManager.localPos(origin, forward, right, forwardOffset, 0),
                    forward.getOpposite()
            ));
        }
        for (int forwardOffset = loopLength; forwardOffset >= 2; forwardOffset--) {
            additionalRepeaterSlots.add(new PathStep(
                    ClockManager.localPos(origin, forward, right, forwardOffset, 1),
                    forward
            ));
        }

        int additionalRepeaterCount = Math.max(0, repeaterCount - 1);
        for (int index = 0; index < additionalRepeaterCount; index++) {
            PathStep step = additionalRepeaterSlots.get(index);
            components.put(step.blockPos(), Blocks.REPEATER.defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, step.inputDirection())
                    .setValue(RepeaterBlock.DELAY, 4));
        }

        if (repeaterCount > 0) {
            int finalRepeaterDelay = totalRepeaterDelay - additionalRepeaterCount * 4;
            components.put(ClockManager.localPos(origin, forward, right, 1, 1), Blocks.REPEATER.defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, forward)
                    .setValue(RepeaterBlock.DELAY, finalRepeaterDelay));
        }

        int front = loopLength + 1;
        components.put(ClockManager.localPos(origin, forward, right, front, 0), ClockManager.redstoneWireState());
        components.put(ClockManager.localPos(origin, forward, right, front, 1), ClockManager.redstoneWireState());

        return new ClockManager.BuildPlan(
                supports,
                components,
                Map.of(),
                Map.of(comparatorPos, Blocks.COMPARATOR)
        );
    }

    private record PathStep(BlockPos blockPos, Direction inputDirection) {
    }
}
