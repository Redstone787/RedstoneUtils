package org.main.redstoneutils.client.bud;

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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.main.redstoneutils.client.config.RedstoneUtilsConfig;
import org.main.redstoneutils.client.overlay.OverlayFreeze;

import java.util.ArrayList;
import java.util.List;

public final class BudSwitchOverlay {

    private static final long REBUILD_INTERVAL_NANOS = 100_000_000L;
    private static final int MAX_SIGNAL_POWER = 15;
    private static final int CAUSE_FILL_COLOR = 0x48FFD21A;
    private static final int CAUSE_STROKE_COLOR = 0xF0FFE052;
    private static final int AFFECTED_FILL_COLOR = 0x48FF2018;
    private static final int AFFECTED_STROKE_COLOR = 0xF0FF5148;
    private static final float STROKE_WIDTH = 2.5F;
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
        if (OverlayFreeze.budFrozen() && renderData != null) return;

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
        nextRebuildNanos = now + REBUILD_INTERVAL_NANOS;
        renderData = buildRenderData(level, playerPos, Math.min(RedstoneUtilsConfig.getBudTestRange(), RedstoneUtilsConfig.getOverlayMaxDistance()));
    }

    private static BudRenderData buildRenderData(ClientLevel level, BlockPos center, int range) {
        LongSet affectedBlocks = new LongOpenHashSet();
        LongSet causeBlocks = new LongOpenHashSet();
        int rangeSquared = range * range;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    if (dx * dx + dy * dy + dz * dz > rangeSquared) {
                        continue;
                    }

                    mutablePos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (level.isOutsideBuildHeight(mutablePos) || !level.isLoaded(mutablePos)) {
                        continue;
                    }

                    BlockState blockState = level.getBlockState(mutablePos);
                    if (!isQuasiConnectivityComponent(blockState)) {
                        continue;
                    }

                    LongSet componentCauses = findPotentialQuasiPowerCauses(level, mutablePos);
                    if (componentCauses.isEmpty()) {
                        continue;
                    }

                    affectedBlocks.add(mutablePos.asLong());
                    causeBlocks.addAll(componentCauses);
                }
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

    private static LongSet findPotentialQuasiPowerCauses(Level level, BlockPos componentPos) {
        LongSet causes = new LongOpenHashSet();
        BlockPos quasiPowerCenter = componentPos.above();

        for (Direction direction : QUASI_POWER_DIRECTIONS) {
            BlockPos causePos = quasiPowerCenter.relative(direction);
            if (!level.isOutsideBuildHeight(causePos)
                    && level.isLoaded(causePos)
                    && canCurrentlyOrPotentiallyPower(level, causePos, direction)) {
                causes.add(causePos.asLong());
            }
        }

        return causes;
    }

    private static boolean canCurrentlyOrPotentiallyPower(Level level, BlockPos causePos, Direction signalDirection) {
        if (level.hasSignal(causePos, signalDirection)) {
            return true;
        }

        BlockState causeState = level.getBlockState(causePos);
        if (canEmitPotentialSignal(causeState, level, causePos, signalDirection, false)) {
            return true;
        }

        return causeState.isRedstoneConductor(level, causePos)
                && hasCurrentOrPotentialDirectInput(level, causePos);
    }

    private static boolean hasCurrentOrPotentialDirectInput(Level level, BlockPos conductorPos) {
        for (Direction direction : DIRECTIONS) {
            BlockPos inputPos = conductorPos.relative(direction);
            if (!level.isOutsideBuildHeight(inputPos) && level.isLoaded(inputPos)) {
                BlockState inputState = level.getBlockState(inputPos);
                if (canEmitPotentialSignal(inputState, level, inputPos, direction, true)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean canEmitPotentialSignal(BlockState blockState, Level level, BlockPos blockPos,
                                                  Direction signalDirection, boolean direct) {
        if (!blockState.isSignalSource()) {
            return false;
        }

        if (signal(blockState, level, blockPos, signalDirection, direct) > 0) {
            return true;
        }

        BlockState poweredState = maximallyPoweredState(blockState);
        return poweredState != blockState
                && signal(poweredState, level, blockPos, signalDirection, direct) > 0;
    }

    private static int signal(BlockState blockState, Level level, BlockPos blockPos,
                              Direction signalDirection, boolean direct) {
        return direct
                ? blockState.getDirectSignal(level, blockPos, signalDirection)
                : blockState.getSignal(level, blockPos, signalDirection);
    }

    private static BlockState maximallyPoweredState(BlockState blockState) {
        BlockState poweredState = blockState;
        if (poweredState.hasProperty(BlockStateProperties.POWERED)) {
            poweredState = poweredState.setValue(BlockStateProperties.POWERED, true);
        }
        if (poweredState.hasProperty(BlockStateProperties.LIT)) {
            poweredState = poweredState.setValue(BlockStateProperties.LIT, true);
        }
        if (poweredState.hasProperty(BlockStateProperties.POWER)) {
            poweredState = poweredState.setValue(BlockStateProperties.POWER, MAX_SIGNAL_POWER);
        }
        return poweredState;
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
