/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.client.autowire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class AutoWire {

    private static final int MAX_REDSTONE_DUST_RUN = 15;
    private static final int FAST_AUTO_REPEATER_SIGNAL = 1;

    public static BlockPos lastAutoWiredBlockPos = null;
    private static int redstoneDustRunLength = 0;
    private static FastAutoStep fastAutoStep = FastAutoStep.NONE;
    private static FastComparatorStep fastComparatorStep = FastComparatorStep.INPUT_BLOCK;

    public static void wire(WireType wireType, Level level, BlockPos placedBlockPos) {
        wire(wireType, level, placedBlockPos, null, null);
    }

    public static AutoWirePreview preview(WireType wireType, Level level, BlockPos placedBlockPos, BlockState supportBlockState, Item supportItem) {
        if (wireType == null || wireType == WireType.NONE || level == null || placedBlockPos == null) {
            return null;
        }

        return switch (wireType) {
            case NORMAL -> previewRedstoneWire(level, placedBlockPos, supportBlockState);
            case AUTO -> previewAutoWire(level, placedBlockPos, supportBlockState);
            case FAST_AUTO -> previewFastAutoWire(level, placedBlockPos, supportBlockState, supportItem);
            case ONLY_REPEATERS -> previewRepeatedRepeaterWire(level, placedBlockPos, supportBlockState);
            case ONLY_COMPARATORS -> previewRepeatedComparatorWire(level, placedBlockPos, supportBlockState);
            case FAST_COMPARATORS -> previewFastComparatorWire(level, placedBlockPos, supportBlockState, supportItem);
            case NONE -> null;
        };
    }

    public static void wire(WireType wireType, Level level, BlockPos placedBlockPos, BlockState supportBlockState, Item supportItem) {
        switch (wireType) {
            case NORMAL -> {
                normalWire(level, placedBlockPos);
                reset();
            }
            case AUTO -> autoWire(level, placedBlockPos);
            case FAST_AUTO -> fastAutoWire(level, placedBlockPos, supportBlockState, supportItem);
            case ONLY_REPEATERS -> repeatedRepeaterWire(level, placedBlockPos);
            case ONLY_COMPARATORS -> repeatedComparatorWire(level, placedBlockPos);
            case FAST_COMPARATORS -> fastComparatorWire(level, placedBlockPos, supportBlockState, supportItem);
            case NONE -> reset();
        }
    }

    public static boolean normalWire(Level level, BlockPos placedBlockPos) {
        return AutoWirePlacement.placeOnTop(level, placedBlockPos, AutoWirePlacement.PlaceableBlock.REDSTONE_WIRE);
    }

    public static void reset() {
        lastAutoWiredBlockPos = null;
        redstoneDustRunLength = 0;
        fastAutoStep = FastAutoStep.NONE;
        fastComparatorStep = FastComparatorStep.INPUT_BLOCK;
    }

    private static AutoWirePreview previewAutoWire(Level level, BlockPos placedBlockPos, BlockState supportBlockState) {
        BlockPos targetBlockPos = placedBlockPos.above();

        if (lastAutoWiredBlockPos == null) {
            return previewRedstoneWire(level, placedBlockPos, supportBlockState);
        }

        Direction facing = AutoWireDirection.fromTo(lastAutoWiredBlockPos, targetBlockPos);
        if (shouldPlaceRepeater(level)) {
            return previewRepeater(level, placedBlockPos, supportBlockState, facing);
        }

        return previewRedstoneWire(level, placedBlockPos, supportBlockState);
    }

    private static AutoWirePreview previewFastAutoWire(Level level, BlockPos placedBlockPos, BlockState supportBlockState, Item supportItem) {
        BlockPos targetBlockPos = placedBlockPos.above();

        if (lastAutoWiredBlockPos == null) {
            return previewRedstoneWire(level, placedBlockPos, supportBlockState);
        }

        Direction facing = AutoWireDirection.fromTo(lastAutoWiredBlockPos, targetBlockPos);
        switch (fastAutoStep) {
            case REPEATER -> {
                return previewRepeater(level, placedBlockPos, supportBlockState, facing);
            }
            case OUTPUT_BLOCK -> {
                return previewElevatedBlock(level, placedBlockPos, supportBlockState, supportItem);
            }
            case OUTPUT_REDSTONE -> {
                return previewRedstoneWire(level, placedBlockPos, supportBlockState);
            }
        }

        if (shouldStartFastAutoBooster(level)) {
            return previewElevatedBlock(level, placedBlockPos, supportBlockState, supportItem);
        }

        return previewRedstoneWire(level, placedBlockPos, supportBlockState);
    }

    private static AutoWirePreview previewRepeatedRepeaterWire(Level level, BlockPos placedBlockPos, BlockState supportBlockState) {
        BlockPos targetBlockPos = placedBlockPos.above();
        Direction facing = diodeFacing(targetBlockPos);
        return previewRepeater(level, placedBlockPos, supportBlockState, facing);
    }

    private static AutoWirePreview previewRepeatedComparatorWire(Level level, BlockPos placedBlockPos, BlockState supportBlockState) {
        BlockPos targetBlockPos = placedBlockPos.above();
        Direction facing = diodeFacing(targetBlockPos);
        return previewComparator(level, placedBlockPos, supportBlockState, facing);
    }

    private static AutoWirePreview previewFastComparatorWire(Level level, BlockPos placedBlockPos, BlockState supportBlockState, Item supportItem) {
        BlockPos targetBlockPos = placedBlockPos.above();
        Direction facing = diodeFacing(targetBlockPos);

        return switch (fastComparatorStep) {
            case INPUT_BLOCK, OUTPUT_BLOCK -> previewElevatedBlock(level, placedBlockPos, supportBlockState, supportItem);
            case COMPARATOR -> previewComparator(level, placedBlockPos, supportBlockState, facing);
            case OUTPUT_REDSTONE -> previewRedstoneWire(level, placedBlockPos, supportBlockState);
        };
    }

    public static void autoWire(Level level, BlockPos placedBlockPos) {
        fastAutoStep = FastAutoStep.NONE;
        fastComparatorStep = FastComparatorStep.INPUT_BLOCK;
        BlockPos targetBlockPos = placedBlockPos.above();

        if (lastAutoWiredBlockPos == null) {
            placeRedstoneWire(level, placedBlockPos, targetBlockPos);
            return;
        }

        Direction facing = AutoWireDirection.fromTo(lastAutoWiredBlockPos, targetBlockPos);
        if (shouldPlaceRepeater(level)) {
            placeRepeater(level, placedBlockPos, targetBlockPos, facing);
            return;
        }

        placeRedstoneWire(level, placedBlockPos, targetBlockPos);
    }

    public static void fastAutoWire(Level level, BlockPos placedBlockPos, BlockState supportBlockState, Item supportItem) {
        fastComparatorStep = FastComparatorStep.INPUT_BLOCK;
        BlockPos targetBlockPos = placedBlockPos.above();

        if (lastAutoWiredBlockPos == null) {
            placeRedstoneWire(level, placedBlockPos, targetBlockPos);
            return;
        }

        Direction facing = AutoWireDirection.fromTo(lastAutoWiredBlockPos, targetBlockPos);
        switch (fastAutoStep) {
            case REPEATER -> {
                if (placeRepeater(level, placedBlockPos, targetBlockPos, facing)) {
                    fastAutoStep = FastAutoStep.OUTPUT_BLOCK;
                }
                return;
            }
            case OUTPUT_BLOCK -> {
                if (placeElevatedBlock(level, placedBlockPos, targetBlockPos, supportBlockState, supportItem)) {
                    fastAutoStep = FastAutoStep.OUTPUT_REDSTONE;
                }
                return;
            }
            case OUTPUT_REDSTONE -> {
                if (placeRedstoneWire(level, placedBlockPos, targetBlockPos)) {
                    fastAutoStep = FastAutoStep.NONE;
                }
                return;
            }
        }

        if (shouldStartFastAutoBooster(level)) {
            if (placeElevatedBlock(level, placedBlockPos, targetBlockPos, supportBlockState, supportItem)) {
                fastAutoStep = FastAutoStep.REPEATER;
            }
            return;
        }

        placeRedstoneWire(level, placedBlockPos, targetBlockPos);
    }

    public static void repeatedRepeaterWire(Level level, BlockPos placedBlockPos) {
        fastAutoStep = FastAutoStep.NONE;
        fastComparatorStep = FastComparatorStep.INPUT_BLOCK;

        BlockPos targetBlockPos = placedBlockPos.above();
        Direction facing = diodeFacing(targetBlockPos);
        placeRepeater(level, placedBlockPos, targetBlockPos, facing);
    }

    public static void repeatedComparatorWire(Level level, BlockPos placedBlockPos) {
        fastAutoStep = FastAutoStep.NONE;
        fastComparatorStep = FastComparatorStep.INPUT_BLOCK;

        BlockPos targetBlockPos = placedBlockPos.above();
        Direction facing = diodeFacing(targetBlockPos);
        placeComparator(level, placedBlockPos, targetBlockPos, facing);
    }

    public static void fastComparatorWire(Level level, BlockPos placedBlockPos, BlockState supportBlockState, Item supportItem) {
        fastAutoStep = FastAutoStep.NONE;

        BlockPos targetBlockPos = placedBlockPos.above();
        Direction facing = diodeFacing(targetBlockPos);

        switch (fastComparatorStep) {
            case INPUT_BLOCK -> {
                if (placeElevatedBlock(level, placedBlockPos, targetBlockPos, supportBlockState, supportItem)) {
                    fastComparatorStep = FastComparatorStep.COMPARATOR;
                }
            }
            case COMPARATOR -> {
                if (placeComparator(level, placedBlockPos, targetBlockPos, facing)) {
                    fastComparatorStep = FastComparatorStep.OUTPUT_BLOCK;
                }
            }
            case OUTPUT_BLOCK -> {
                if (placeElevatedBlock(level, placedBlockPos, targetBlockPos, supportBlockState, supportItem)) {
                    fastComparatorStep = FastComparatorStep.OUTPUT_REDSTONE;
                }
            }
            case OUTPUT_REDSTONE -> {
                if (placeRedstoneWire(level, placedBlockPos, targetBlockPos)) {
                    fastComparatorStep = FastComparatorStep.INPUT_BLOCK;
                }
            }
        }
    }

    private static boolean shouldPlaceRepeater(Level level) {
        if (redstoneDustRunLength >= MAX_REDSTONE_DUST_RUN) return true;

        return redstoneDustRunLength > 0 && !AutoWirePower.hasPowerToContinue(level, lastAutoWiredBlockPos);
    }

    private static boolean shouldStartFastAutoBooster(Level level) {
        return redstoneDustRunLength > 0
                && AutoWirePower.getPower(level, lastAutoWiredBlockPos) == FAST_AUTO_REPEATER_SIGNAL;
    }

    private static Direction diodeFacing(BlockPos targetBlockPos) {
        if (lastAutoWiredBlockPos == null) {
            return AutoWirePlacement.playerFacing();
        }

        return AutoWireDirection.fromTo(lastAutoWiredBlockPos, targetBlockPos);
    }

    private static boolean placeRedstoneWire(Level level, BlockPos placedBlockPos, BlockPos targetBlockPos) {
        if (!normalWire(level, placedBlockPos)) {
            return false;
        }

        lastAutoWiredBlockPos = targetBlockPos;
        redstoneDustRunLength++;
        return true;
    }

    private static boolean placeRepeater(Level level, BlockPos placedBlockPos, BlockPos targetBlockPos, Direction facing) {
        boolean placed = facing == null
                ? placeRepeater(level, placedBlockPos)
                : placeRepeater(level, placedBlockPos, facing);
        if (!placed) {
            return false;
        }

        lastAutoWiredBlockPos = targetBlockPos;
        redstoneDustRunLength = 0;
        return true;
    }

    private static boolean placeComparator(Level level, BlockPos placedBlockPos, BlockPos targetBlockPos, Direction facing) {
        boolean placed = facing == null
                ? placeComparator(level, placedBlockPos)
                : placeComparator(level, placedBlockPos, facing);
        if (!placed) {
            return false;
        }

        lastAutoWiredBlockPos = targetBlockPos;
        redstoneDustRunLength = 0;
        return true;
    }

    private static boolean placeElevatedBlock(Level level, BlockPos placedBlockPos, BlockPos targetBlockPos, BlockState blockState, Item item) {
        if (blockState == null) {
            AutoWirePlacement.reportFailure("missing_support", Blocks.AIR.defaultBlockState());
            return false;
        }
        if (!AutoWirePlacement.placeOnTop(level, placedBlockPos, blockState, item)) {
            return false;
        }

        lastAutoWiredBlockPos = targetBlockPos;
        redstoneDustRunLength = 0;
        return true;
    }

    public static boolean placeRepeater(Level level, BlockPos placedBlockPos) {
        return AutoWirePlacement.placeOnTop(level, placedBlockPos, AutoWirePlacement.PlaceableBlock.REPEATER);
    }

    public static boolean placeRepeater(Level level, BlockPos placedBlockPos, Direction facing) {
        return AutoWirePlacement.placeOnTop(level, placedBlockPos, AutoWirePlacement.PlaceableBlock.REPEATER, facing);
    }

    public static boolean placeComparator(Level level, BlockPos placedBlockPos) {
        return AutoWirePlacement.placeOnTop(level, placedBlockPos, AutoWirePlacement.PlaceableBlock.COMPARATOR);
    }

    public static boolean placeComparator(Level level, BlockPos placedBlockPos, Direction facing) {
        return AutoWirePlacement.placeOnTop(level, placedBlockPos, AutoWirePlacement.PlaceableBlock.COMPARATOR, facing);
    }

    private static AutoWirePreview previewRedstoneWire(Level level, BlockPos placedBlockPos, BlockState supportBlockState) {
        return previewPlaceable(level, placedBlockPos, supportBlockState, AutoWirePlacement.PlaceableBlock.REDSTONE_WIRE.defaultState(), AutoWirePlacement.PlaceableBlock.REDSTONE_WIRE.item());
    }

    private static AutoWirePreview previewRepeater(Level level, BlockPos placedBlockPos, BlockState supportBlockState, Direction facing) {
        BlockState blockState = AutoWirePlacement.PlaceableBlock.REPEATER.defaultState(facing);
        return previewPlaceable(level, placedBlockPos, supportBlockState, blockState, AutoWirePlacement.PlaceableBlock.REPEATER.item());
    }

    private static AutoWirePreview previewComparator(Level level, BlockPos placedBlockPos, BlockState supportBlockState, Direction facing) {
        BlockState blockState = AutoWirePlacement.PlaceableBlock.COMPARATOR.defaultState(facing);
        return previewPlaceable(level, placedBlockPos, supportBlockState, blockState, AutoWirePlacement.PlaceableBlock.COMPARATOR.item());
    }

    private static AutoWirePreview previewElevatedBlock(Level level, BlockPos placedBlockPos, BlockState supportBlockState, Item supportItem) {
        if (supportBlockState == null) {
            return null;
        }

        return previewPlaceable(level, placedBlockPos, supportBlockState, supportBlockState, supportItem);
    }

    private static AutoWirePreview previewPlaceable(Level level, BlockPos placedBlockPos, BlockState supportBlockState, BlockState blockState, Item item) {
        if (blockState == null) {
            return null;
        }

        BlockState placementState = AutoWirePlacement.resolvePreviewPlacementState(level, placedBlockPos, blockState, item);
        if (!AutoWirePlacement.canPreviewPlaceOnTop(level, placedBlockPos, placementState, supportBlockState)) {
            return null;
        }

        return new AutoWirePreview(placedBlockPos.above(), placementState);
    }

    public record AutoWirePreview(BlockPos blockPos, BlockState blockState) {
    }

    private enum FastAutoStep {
        NONE,
        REPEATER,
        OUTPUT_BLOCK,
        OUTPUT_REDSTONE
    }

    private enum FastComparatorStep {
        INPUT_BLOCK,
        COMPARATOR,
        OUTPUT_BLOCK,
        OUTPUT_REDSTONE
    }
}
