package org.main.redstoneutils.server.autowire;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

final class AutoWirePower {

    private static final int MIN_POWER_TO_CONTINUE = 2;

    private AutoWirePower() {
    }

    static boolean hasPowerToContinue(Level level, BlockPos blockPos) {
        BlockState blockState = level.getBlockState(blockPos);
        if (isRedstoneWire(blockState)) {
            return getRedstoneWirePower(blockState) >= MIN_POWER_TO_CONTINUE;
        }

        int directPower = getPower(level, blockPos);
        int supportPower = getPower(level, blockPos.below());

        return Math.max(directPower, supportPower) >= MIN_POWER_TO_CONTINUE;
    }

    static int getPower(Level level, BlockPos blockPos) {
        BlockState blockState = level.getBlockState(blockPos);

        if (isRedstoneWire(blockState)) {
            return getRedstoneWirePower(blockState);
        }

        if (blockState.hasProperty(BlockStateProperties.POWERED) && blockState.getValue(BlockStateProperties.POWERED)) {
            return 15;
        }

        int power = 0;
        power = Math.max(power, level.getBestNeighborSignal(blockPos));
        power = Math.max(power, level.getDirectSignalTo(blockPos));

        return power;
    }

    private static boolean isRedstoneWire(BlockState blockState) {
        return blockState.getBlock() == Blocks.REDSTONE_WIRE && blockState.hasProperty(BlockStateProperties.POWER);
    }

    private static int getRedstoneWirePower(BlockState blockState) {
        return blockState.getValue(BlockStateProperties.POWER);
    }
}
