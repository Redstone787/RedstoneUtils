package org.main.redstoneutils.server.clock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ComparatorMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HopperClockManager {

    private HopperClockManager() {
    }

    public static ClockManager.Result create(ServerPlayer player, ClockInterval interval) {
        ServerLevel level = player.level();
        Direction forward = player.getDirection();
        Direction right = forward.getClockWise();
        BlockPos origin = player.blockPosition();
        BlockState supportState = ClockManager.supportState(player, level, origin.below());
        int itemCount = interval.ticks() / ClockInterval.HOPPER_TICK_STEP;
        ClockManager.BuildPlan plan = createPlan(origin, forward, right, supportState, itemCount);

        Component description = Component.translatable(
                "history.redstoneutils.clock.hopper",
                interval.ticks(),
                itemCount,
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
            int itemCount
    ) {
        Map<BlockPos, BlockState> supports = new LinkedHashMap<>();
        Map<BlockPos, BlockState> components = new LinkedHashMap<>();

        for (int forwardOffset = -2; forwardOffset <= 3; forwardOffset++) {
            for (int rightOffset = 0; rightOffset <= 1; rightOffset++) {
                BlockPos componentPos = ClockManager.localPos(origin, forward, right, forwardOffset, rightOffset);
                supports.put(componentPos.below(), supportState);
                components.put(componentPos, Blocks.AIR.defaultBlockState());
            }
        }

        BlockPos leftComparator = ClockManager.localPos(origin, forward, right, -1, 0);
        BlockPos leftHopper = ClockManager.localPos(origin, forward, right, 0, 0);
        BlockPos rightHopper = ClockManager.localPos(origin, forward, right, 1, 0);
        BlockPos rightComparator = ClockManager.localPos(origin, forward, right, 2, 0);

        components.put(ClockManager.localPos(origin, forward, right, -2, 0), supportState);
        components.put(leftComparator, comparatorState(forward));
        components.put(leftHopper, hopperState(forward));
        components.put(rightHopper, hopperState(forward.getOpposite()));
        components.put(rightComparator, comparatorState(forward.getOpposite()));
        components.put(ClockManager.localPos(origin, forward, right, 3, 0), supportState);

        components.put(
                ClockManager.localPos(origin, forward, right, -2, 1),
                ClockManager.redstoneWireState()
        );
        components.put(
                ClockManager.localPos(origin, forward, right, -1, 1),
                Blocks.STICKY_PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, forward)
        );
        components.put(
                ClockManager.localPos(origin, forward, right, 0, 1),
                Blocks.REDSTONE_BLOCK.defaultBlockState()
        );
        components.put(
                ClockManager.localPos(origin, forward, right, 2, 1),
                Blocks.STICKY_PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, forward.getOpposite())
        );
        components.put(
                ClockManager.localPos(origin, forward, right, 3, 1),
                ClockManager.redstoneWireState()
        );

        return new ClockManager.BuildPlan(
                supports,
                components,
                Map.of(rightHopper, counterStacks(itemCount)),
                Map.of(leftComparator, Blocks.COMPARATOR, rightComparator, Blocks.COMPARATOR)
        );
    }

    private static BlockState comparatorState(Direction inputDirection) {
        return Blocks.COMPARATOR.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, inputDirection)
                .setValue(ComparatorBlock.MODE, ComparatorMode.COMPARE);
    }

    private static BlockState hopperState(Direction outputDirection) {
        return Blocks.HOPPER.defaultBlockState()
                .setValue(HopperBlock.FACING, outputDirection)
                .setValue(HopperBlock.ENABLED, true);
    }

    private static List<ItemStack> counterStacks(int itemCount) {
        List<ItemStack> stacks = new ArrayList<>();
        int remaining = itemCount;
        while (remaining > 0) {
            int stackSize = Math.min(remaining, Items.STICK.getDefaultMaxStackSize());
            stacks.add(new ItemStack(Items.STICK, stackSize));
            remaining -= stackSize;
        }
        return List.copyOf(stacks);
    }
}
