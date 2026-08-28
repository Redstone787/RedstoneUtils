/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstone_utils.client.bud;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.CuboidGizmo;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import io.github.redstone787.redstone_utils.client.config.RedstoneUtilsConfig;
import io.github.redstone787.redstone_utils.client.overlay.OverlayFreeze;

import java.util.ArrayList;
import java.util.List;

public final class BudSwitchOverlay {

    private static final long MIN_REBUILD_INTERVAL_NANOS = 100_000_000L;
    private static final long MEDIUM_REBUILD_INTERVAL_NANOS = 250_000_000L;
    private static final long LARGE_REBUILD_INTERVAL_NANOS = 500_000_000L;
    private static final double BOX_INFLATE = 0.006D;
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final Direction[] QUASI_POWER_DIRECTIONS = {
            Direction.UP,
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST
    };

    private static BudRenderData renderData = null;
    private static ClientLevel lastLevel = null;
    private static BlockPos lastScanCenter = null;
    private static boolean scanValid = false;
    private static boolean visible = RedstoneUtilsConfig.isBudOverlayVisible();
    private static boolean initialized = false;
    private static long nextRebuildNanos = 0L;

    private BudSwitchOverlay() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        LevelExtractionEvents.END_EXTRACTION.register(BudSwitchOverlay::extract);
        LevelRenderEvents.COLLECT_SUBMITS.register(BudSwitchOverlay::render);
    }

    public static boolean isVisible() {
        return visible;
    }

    public static void setVisible(boolean visible) {
        BudSwitchOverlay.visible = visible;
        RedstoneUtilsConfig.setBudOverlayVisible(visible);
        requestRefresh();
    }

    public static boolean toggleVisible() {
        setVisible(!visible);
        return visible;
    }

    public static void clear() {
        requestRefresh();
    }

    public static void requestRefresh() {
        renderData = null;
        lastLevel = null;
        lastScanCenter = null;
        scanValid = false;
        nextRebuildNanos = 0L;
    }

    private static void extract(LevelExtractionContext context) {
        if (!visible) {
            requestRefresh();
            return;
        }
        if (OverlayFreeze.budFrozen()) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = context.level();
        if (player == null) {
            requestRefresh();
            return;
        }

        BlockPos playerPos = player.blockPosition();
        long now = System.nanoTime();
        if (scanValid
                && lastLevel == level
                && playerPos.equals(lastScanCenter)
                && now < nextRebuildNanos) {
            return;
        }

        lastLevel = level;
        lastScanCenter = playerPos.immutable();
        scanValid = true;
        int range = Math.min(RedstoneUtilsConfig.getBudTestRange(), RedstoneUtilsConfig.getOverlayMaxDistance());
        nextRebuildNanos = now + rebuildIntervalNanos(range);
        renderData = buildRenderData(level, playerPos, range);
    }

    private static long rebuildIntervalNanos(int range) {
        if (range > 32) return LARGE_REBUILD_INTERVAL_NANOS;
        if (range > 16) return MEDIUM_REBUILD_INTERVAL_NANOS;
        return MIN_REBUILD_INTERVAL_NANOS;
    }

    private static BudRenderData buildRenderData(ClientLevel level, BlockPos center, int range) {
        LongSet affectedBlocks = new LongOpenHashSet();
        LongSet causeBlocks = new LongOpenHashSet();
        int rangeSquared = range * range;
        int minChunkX = (center.getX() - range) >> 4;
        int maxChunkX = (center.getX() + range) >> 4;
        int minChunkZ = (center.getZ() - range) >> 4;
        int maxChunkZ = (center.getZ() + range) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (chunk == null) {
                    continue;
                }

                chunk.findBlocks(BudSwitchOverlay::isQuasiConnectivityComponent, (componentPos, componentState) -> {
                    if (!isInsideSphere(componentPos, center, rangeSquared)
                            || !isArmedBud(level, componentPos, componentState)) {
                        return;
                    }

                    affectedBlocks.add(componentPos.asLong());
                    collectCurrentQuasiPowerCauses(level, componentPos, causeBlocks);
                });
            }
        }

        causeBlocks.removeAll(affectedBlocks);
        if (affectedBlocks.isEmpty()) {
            return null;
        }

        return new BudRenderData(createBoxes(causeBlocks), createBoxes(affectedBlocks));
    }

    private static boolean isQuasiConnectivityComponent(BlockState blockState) {
        return blockState.getBlock() instanceof PistonBaseBlock
                || blockState.getBlock() instanceof DispenserBlock;
    }

    private static boolean isInsideSphere(BlockPos blockPos, BlockPos center, int rangeSquared) {
        long dx = blockPos.getX() - center.getX();
        long dy = blockPos.getY() - center.getY();
        long dz = blockPos.getZ() - center.getZ();
        return dx * dx + dy * dy + dz * dz <= rangeSquared;
    }

    private static boolean isArmedBud(Level level, BlockPos componentPos, BlockState componentState) {
        if (componentState.getBlock() instanceof PistonBaseBlock) {
            Direction facing = componentState.getValue(PistonBaseBlock.FACING);
            boolean directlyPowered = hasDirectPistonPower(level, componentPos, facing);
            boolean quasiPowered = hasQuasiPistonPower(level, componentPos);
            boolean extended = componentState.getValue(PistonBaseBlock.EXTENDED);
            return BudStateDetector.isArmedQuasiState(directlyPowered, quasiPowered, extended);
        }

        if (componentState.getBlock() instanceof DispenserBlock) {
            boolean directlyPowered = level.hasNeighborSignal(componentPos);
            boolean quasiPowered = level.hasNeighborSignal(componentPos.above());
            boolean triggered = componentState.getValue(DispenserBlock.TRIGGERED);
            return BudStateDetector.isArmedQuasiState(directlyPowered, quasiPowered, triggered);
        }

        return false;
    }

    private static boolean hasDirectPistonPower(Level level, BlockPos pistonPos, Direction facing) {
        for (Direction direction : DIRECTIONS) {
            if (direction != facing && level.hasSignal(pistonPos.relative(direction), direction)) {
                return true;
            }
        }

        return level.hasSignal(pistonPos, Direction.DOWN);
    }

    private static boolean hasQuasiPistonPower(Level level, BlockPos pistonPos) {
        BlockPos quasiPowerCenter = pistonPos.above();
        for (Direction direction : QUASI_POWER_DIRECTIONS) {
            if (level.hasSignal(quasiPowerCenter.relative(direction), direction)) {
                return true;
            }
        }

        return false;
    }

    private static void collectCurrentQuasiPowerCauses(Level level, BlockPos componentPos, LongSet causes) {
        BlockPos quasiPowerCenter = componentPos.above();
        BlockPos.MutableBlockPos causePos = new BlockPos.MutableBlockPos();
        for (Direction direction : QUASI_POWER_DIRECTIONS) {
            causePos.setWithOffset(quasiPowerCenter, direction);
            if (!level.isOutsideBuildHeight(causePos)
                    && level.isLoaded(causePos)
                    && level.hasSignal(causePos, direction)) {
                causes.add(causePos.asLong());
            }
        }
    }

    private static List<AABB> createBoxes(LongSet positions) {
        List<AABB> boxes = new ArrayList<>(positions.size());
        LongIterator iterator = positions.iterator();
        while (iterator.hasNext()) {
            boxes.add(new AABB(BlockPos.of(iterator.nextLong())).inflate(BOX_INFLATE));
        }
        return List.copyOf(boxes);
    }

    private static void render(LevelRenderContext context) {
        BudRenderData data = renderData;
        if (data == null) {
            return;
        } else {
            context.levelState();
        }

        DrawableGizmoPrimitives primitives = new DrawableGizmoPrimitives();
        GizmoStyle causeStyle = style(RedstoneUtilsConfig.budSourceColor());
        GizmoStyle affectedStyle = style(RedstoneUtilsConfig.budRiskColor());
        for (AABB box : data.causeBoxes()) {
            new CuboidGizmo(box, causeStyle, false).emit(primitives, 1.0F);
        }
        for (AABB box : data.affectedBoxes()) {
            new CuboidGizmo(box, affectedStyle, false).emit(primitives, 1.0F);
        }

        primitives.submit(context.submitNodeCollector(), context.levelState().cameraRenderState, RedstoneUtilsConfig.renderOverlaysThroughWalls());
    }

    private static GizmoStyle style(int color) {
        return GizmoStyle.strokeAndFill(
                color(color, 1.0F),
                RedstoneUtilsConfig.getOverlayLineWidth(),
                color(color, 0.28F)
        );
    }

    private static int color(int color, float alphaMultiplier) {
        int alpha = Math.clamp(Math.round((color >>> 24) * RedstoneUtilsConfig.getOverlayOpacity() * alphaMultiplier), 0, 255);
        return color & 0x00FFFFFF | alpha << 24;
    }

    private record BudRenderData(List<AABB> causeBoxes, List<AABB> affectedBoxes) {
    }
}
