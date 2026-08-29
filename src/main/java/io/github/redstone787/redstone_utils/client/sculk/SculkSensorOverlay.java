/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstone_utils.client.sculk;

import io.github.redstone787.redstone_utils.client.config.RedstoneUtilsConfig;
import io.github.redstone787.redstone_utils.client.overlay.OverlayFreeze;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
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
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CalibratedSculkSensorBlockEntity;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SculkSensorOverlay {

    private static final int SCULK_SENSOR_RADIUS = 8;
    private static final int CALIBRATED_SENSOR_RADIUS = 16;
    private static final int MAX_RENDERED_SENSORS = 4;
    private static final double SENSOR_BOX_INFLATE = 0.004D;
    private static final double OCCLUSION_RAY_OFFSET = 9.999999747378752E-6D;
    private static final double FACE_OFFSET = 0.002D;
    private static final Direction[] DIRECTIONS = Direction.values();

    private static final Long2ObjectMap<SensorState> SENSORS = new Long2ObjectOpenHashMap<>();
    private static final Long2ObjectMap<LongSet> SENSOR_POSITIONS_BY_CHUNK = new Long2ObjectOpenHashMap<>();

    private static ClientLevel indexedLevel;
    private static long sensorIndexRevision;
    private static long selectedIndexRevision = -1L;
    private static BlockPos lastSelectionCenter;
    private static List<SensorState> selectedSensors = List.of();
    private static long lastMeshRebuildGameTime = Long.MIN_VALUE;

    private static SculkRenderData renderData;
    private static boolean renderDataDirty = true;
    private static DrawableGizmoPrimitives renderPrimitives;
    private static SculkRenderData primitivesRenderData;
    private static RenderStyle primitivesStyle;

    private static boolean visible = RedstoneUtilsConfig.isSculkOverlayVisible();
    private static boolean initialized;

    private SculkSensorOverlay() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register(SculkSensorOverlay::onBlockEntityLoad);
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register(SculkSensorOverlay::onBlockEntityUnload);
        ClientChunkEvents.CHUNK_LOAD.register(SculkSensorOverlay::onChunkLoad);
        ClientChunkEvents.CHUNK_UNLOAD.register(SculkSensorOverlay::onChunkUnload);
        ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> clearIndex());
        LevelExtractionEvents.END_EXTRACTION.register(SculkSensorOverlay::extract);
        LevelRenderEvents.COLLECT_SUBMITS.register(SculkSensorOverlay::render);
    }

    public static boolean isVisible() {
        return visible;
    }

    public static void setVisible(boolean visible) {
        SculkSensorOverlay.visible = visible;
        RedstoneUtilsConfig.setSculkOverlayVisible(visible);
        if (!visible) {
            clearRenderSnapshot();
            return;
        }
        if (OverlayFreeze.sculkFrozen() && renderData != null) return;
        requestRefresh();
    }

    public static boolean toggleVisible() {
        setVisible(!visible);
        return visible;
    }

    public static void requestRefresh() {
        for (SensorState sensor : SENSORS.values()) {
            sensor.forceDirty();
        }
        selectedIndexRevision = -1L;
        lastSelectionCenter = null;
        lastMeshRebuildGameTime = Long.MIN_VALUE;
        renderDataDirty = true;
        invalidatePrimitives();
    }

    /** Called by the client-level mixin after a block state was changed by the server or prediction. */
    public static void onBlockStateChanged(ClientLevel level, BlockPos blockPos, BlockState oldState, BlockState newState) {
        if (oldState.is(BlockTags.OCCLUDES_VIBRATION_SIGNALS) == newState.is(BlockTags.OCCLUDES_VIBRATION_SIGNALS)) {
            return;
        }

        ensureLevel(level);
        markSensorsDirtyNear(blockPos);
    }

    private static void onBlockEntityLoad(BlockEntity blockEntity, ClientLevel level) {
        if (!(blockEntity instanceof SculkSensorBlockEntity)) return;
        ensureLevel(level);
        addSensor(blockEntity);
    }

    private static void onBlockEntityUnload(BlockEntity blockEntity, ClientLevel level) {
        if (!(blockEntity instanceof SculkSensorBlockEntity) || indexedLevel != level) return;
        long positionKey = blockEntity.getBlockPos().asLong();
        SensorState trackedSensor = SENSORS.get(positionKey);
        if (trackedSensor != null && trackedSensor.blockEntity == blockEntity) {
            removeSensor(positionKey);
        }
    }

    private static void onChunkLoad(ClientLevel level, LevelChunk chunk) {
        ensureLevel(level);

        LongSet discoveredSensors = new LongOpenHashSet();
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (blockEntity instanceof SculkSensorBlockEntity) {
                discoveredSensors.add(blockEntity.getBlockPos().asLong());
                addSensor(blockEntity);
            }
        }

        long chunkKey = chunk.getPos().pack();
        LongSet trackedSensors = SENSOR_POSITIONS_BY_CHUNK.get(chunkKey);
        if (trackedSensors != null) {
            LongIterator iterator = trackedSensors.iterator();
            boolean changed = false;
            while (iterator.hasNext()) {
                long sensorPos = iterator.nextLong();
                if (!discoveredSensors.contains(sensorPos)) {
                    iterator.remove();
                    SENSORS.remove(sensorPos);
                    changed = true;
                }
            }
            if (trackedSensors.isEmpty()) {
                SENSOR_POSITIONS_BY_CHUNK.remove(chunkKey);
            }
            if (changed) sensorIndexRevision++;
        }

        markSensorsDirtyNearChunk(chunk.getPos());
    }

    private static void onChunkUnload(ClientLevel level, LevelChunk chunk) {
        if (indexedLevel != level) return;
        markSensorsDirtyNearChunk(chunk.getPos());
        removeChunk(chunk.getPos().pack());
    }

    private static void ensureLevel(ClientLevel level) {
        if (indexedLevel == level) return;
        clearIndex();
        indexedLevel = level;
    }

    private static void clearIndex() {
        indexedLevel = null;
        SENSORS.clear();
        SENSOR_POSITIONS_BY_CHUNK.clear();
        sensorIndexRevision++;
        selectedIndexRevision = -1L;
        lastSelectionCenter = null;
        selectedSensors = List.of();
        lastMeshRebuildGameTime = Long.MIN_VALUE;
        clearRenderSnapshot();
    }

    private static void addSensor(BlockEntity blockEntity) {
        BlockPos blockPos = blockEntity.getBlockPos().immutable();
        long positionKey = blockPos.asLong();
        int radius = blockEntity instanceof CalibratedSculkSensorBlockEntity
                ? CALIBRATED_SENSOR_RADIUS
                : SCULK_SENSOR_RADIUS;
        SensorState previous = SENSORS.get(positionKey);
        if (previous != null && previous.radius == radius) {
            previous.blockEntity = blockEntity;
            return;
        }

        SENSORS.put(positionKey, new SensorState(blockEntity, blockPos, radius));
        long chunkKey = ChunkPos.pack(blockPos);
        LongSet chunkSensors = SENSOR_POSITIONS_BY_CHUNK.get(chunkKey);
        if (chunkSensors == null) {
            chunkSensors = new LongOpenHashSet();
            SENSOR_POSITIONS_BY_CHUNK.put(chunkKey, chunkSensors);
        }
        chunkSensors.add(positionKey);
        sensorIndexRevision++;
    }

    private static void removeSensor(long positionKey) {
        SensorState removed = SENSORS.remove(positionKey);
        if (removed == null) return;

        long chunkKey = ChunkPos.pack(removed.blockPos);
        LongSet chunkSensors = SENSOR_POSITIONS_BY_CHUNK.get(chunkKey);
        if (chunkSensors != null) {
            chunkSensors.remove(positionKey);
            if (chunkSensors.isEmpty()) {
                SENSOR_POSITIONS_BY_CHUNK.remove(chunkKey);
            }
        }
        sensorIndexRevision++;
    }

    private static void removeChunk(long chunkKey) {
        LongSet chunkSensors = SENSOR_POSITIONS_BY_CHUNK.remove(chunkKey);
        if (chunkSensors == null || chunkSensors.isEmpty()) return;

        LongIterator iterator = chunkSensors.iterator();
        while (iterator.hasNext()) {
            SENSORS.remove(iterator.nextLong());
        }
        sensorIndexRevision++;
    }

    private static void markSensorsDirtyNear(BlockPos changedPos) {
        int chunkRadius = (CALIBRATED_SENSOR_RADIUS + 15) >> 4;
        int centerChunkX = changedPos.getX() >> 4;
        int centerChunkZ = changedPos.getZ() >> 4;

        for (int chunkX = centerChunkX - chunkRadius; chunkX <= centerChunkX + chunkRadius; chunkX++) {
            for (int chunkZ = centerChunkZ - chunkRadius; chunkZ <= centerChunkZ + chunkRadius; chunkZ++) {
                LongSet chunkSensors = SENSOR_POSITIONS_BY_CHUNK.get(ChunkPos.pack(chunkX, chunkZ));
                if (chunkSensors == null) continue;

                LongIterator iterator = chunkSensors.iterator();
                while (iterator.hasNext()) {
                    SensorState sensor = SENSORS.get(iterator.nextLong());
                    if (sensor != null && sensor.blockPos.distSqr(changedPos) <= sensor.radius * sensor.radius) {
                        sensor.markDirty();
                    }
                }
            }
        }
    }

    private static void markSensorsDirtyNearChunk(ChunkPos changedChunk) {
        for (int chunkX = changedChunk.x() - 1; chunkX <= changedChunk.x() + 1; chunkX++) {
            for (int chunkZ = changedChunk.z() - 1; chunkZ <= changedChunk.z() + 1; chunkZ++) {
                LongSet chunkSensors = SENSOR_POSITIONS_BY_CHUNK.get(ChunkPos.pack(chunkX, chunkZ));
                if (chunkSensors == null) continue;

                LongIterator iterator = chunkSensors.iterator();
                while (iterator.hasNext()) {
                    SensorState sensor = SENSORS.get(iterator.nextLong());
                    if (sensor != null && sensorRangeIntersectsChunk(sensor, changedChunk)) {
                        sensor.markDirty();
                    }
                }
            }
        }
    }

    private static boolean sensorRangeIntersectsChunk(SensorState sensor, ChunkPos chunkPos) {
        int dx = distanceToRange(sensor.blockPos.getX(), chunkPos.getMinBlockX(), chunkPos.getMaxBlockX());
        int dz = distanceToRange(sensor.blockPos.getZ(), chunkPos.getMinBlockZ(), chunkPos.getMaxBlockZ());
        return dx * dx + dz * dz <= sensor.radius * sensor.radius;
    }

    private static int distanceToRange(int value, int minimum, int maximum) {
        if (value < minimum) return minimum - value;
        if (value > maximum) return value - maximum;
        return 0;
    }

    private static void extract(LevelExtractionContext context) {
        if (!visible) {
            if (renderData != null || renderDataDirty) clearRenderSnapshot();
            return;
        }

        ClientLevel level = context.level();
        ensureLevel(level);
        if (OverlayFreeze.sculkFrozen()) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            clearRenderSnapshot();
            return;
        }

        if (updateSelectedSensors(player.blockPosition())) {
            renderDataDirty = true;
        }
        if (rebuildOneDirtyMesh(level)) {
            renderDataDirty = true;
        }
        if (renderDataDirty) {
            rebuildRenderData();
        }
    }

    private static boolean updateSelectedSensors(BlockPos playerPos) {
        if (sensorIndexRevision == selectedIndexRevision && playerPos.equals(lastSelectionCenter)) {
            return false;
        }

        List<SensorState> nextSelection = findSensors(playerPos);
        boolean changed = !nextSelection.equals(selectedSensors);
        selectedSensors = nextSelection;
        selectedIndexRevision = sensorIndexRevision;
        lastSelectionCenter = playerPos.immutable();
        return changed;
    }

    private static List<SensorState> findSensors(BlockPos playerPos) {
        int maxDistance = Math.min(
                RedstoneUtilsConfig.getSculkSensorSearchDistance(),
                RedstoneUtilsConfig.getOverlayMaxDistance()
        );
        int minChunkX = (playerPos.getX() - maxDistance) >> 4;
        int maxChunkX = (playerPos.getX() + maxDistance) >> 4;
        int minChunkZ = (playerPos.getZ() - maxDistance) >> 4;
        int maxChunkZ = (playerPos.getZ() + maxDistance) >> 4;
        double maxDistanceSquared = (double) maxDistance * maxDistance;

        List<SensorState> sensors = new ArrayList<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LongSet chunkSensors = SENSOR_POSITIONS_BY_CHUNK.get(ChunkPos.pack(chunkX, chunkZ));
                if (chunkSensors == null) continue;

                LongIterator iterator = chunkSensors.iterator();
                while (iterator.hasNext()) {
                    SensorState sensor = SENSORS.get(iterator.nextLong());
                    if (sensor != null && sensor.blockPos.distSqr(playerPos) < maxDistanceSquared) {
                        sensors.add(sensor);
                    }
                }
            }
        }

        sensors.sort(Comparator.comparingDouble(sensor -> sensor.blockPos.distSqr(playerPos)));
        return sensors.size() <= MAX_RENDERED_SENSORS
                ? List.copyOf(sensors)
                : List.copyOf(sensors.subList(0, MAX_RENDERED_SENSORS));
    }

    private static boolean rebuildOneDirtyMesh(ClientLevel level) {
        long gameTime = level.getGameTime();
        if (lastMeshRebuildGameTime == gameTime) return false;

        for (SensorState sensor : selectedSensors) {
            if (!sensor.canRebuild(gameTime)) continue;

            sensor.mesh = buildSensorMesh(level, sensor.blockPos, sensor.radius);
            sensor.dirty = false;
            sensor.nextRebuildGameTime = gameTime + RedstoneUtilsConfig.getSculkRebuildIntervalTicks();
            lastMeshRebuildGameTime = gameTime;
            return true;
        }
        return false;
    }

    private static SensorRangeMesh buildSensorMesh(ClientLevel level, BlockPos sensorPos, int radius) {
        LongSet audibleBlocks = collectAudibleBlocks(level, sensorPos, radius);
        if (audibleBlocks.isEmpty()) {
            return new SensorRangeMesh(sensorPos, List.of(), List.of());
        }

        List<QuadFace> faces = new ArrayList<>();
        Set<LineSegment> lines = new LinkedHashSet<>();
        LongIterator iterator = audibleBlocks.iterator();
        while (iterator.hasNext()) {
            long blockPos = iterator.nextLong();
            for (Direction direction : DIRECTIONS) {
                if (!audibleBlocks.contains(BlockPos.offset(blockPos, direction))) {
                    addFace(faces, lines, BlockPos.of(blockPos), direction);
                }
            }
        }

        return new SensorRangeMesh(sensorPos, List.copyOf(faces), List.copyOf(lines));
    }

    private static LongSet collectAudibleBlocks(ClientLevel level, BlockPos sensorPos, int radius) {
        LongSet audibleBlocks = new LongOpenHashSet();
        Vec3 sensorCenter = Vec3.atCenterOf(sensorPos);
        int radiusSquared = radius * radius;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radiusSquared) continue;

                    mutablePos.set(sensorPos.getX() + dx, sensorPos.getY() + dy, sensorPos.getZ() + dz);
                    if (level.isOutsideBuildHeight(mutablePos) || !level.isLoaded(mutablePos)) continue;
                    if (!isOccluded(level, mutablePos, sensorCenter)) {
                        audibleBlocks.add(mutablePos.asLong());
                    }
                }
            }
        }

        return audibleBlocks;
    }

    private static boolean isOccluded(Level level, BlockPos eventPos, Vec3 listenerPos) {
        Vec3 eventCenter = Vec3.atCenterOf(eventPos);
        for (Direction direction : DIRECTIONS) {
            Vec3 nudgedEventPos = eventCenter.relative(direction, OCCLUSION_RAY_OFFSET);
            BlockHitResult hitResult = level.isBlockInLine(new ClipBlockStateContext(
                    nudgedEventPos,
                    listenerPos,
                    blockState -> blockState.is(BlockTags.OCCLUDES_VIBRATION_SIGNALS)
            ));
            if (hitResult.getType() != HitResult.Type.BLOCK) return false;
        }
        return true;
    }

    private static void addFace(List<QuadFace> faces, Set<LineSegment> lines, BlockPos blockPos, Direction direction) {
        double minX = blockPos.getX();
        double minY = blockPos.getY();
        double minZ = blockPos.getZ();
        double maxX = minX + 1.0D;
        double maxY = minY + 1.0D;
        double maxZ = minZ + 1.0D;

        QuadFace face = switch (direction) {
            case DOWN -> new QuadFace(
                    new Vec3(minX, minY - FACE_OFFSET, minZ),
                    new Vec3(maxX, minY - FACE_OFFSET, minZ),
                    new Vec3(maxX, minY - FACE_OFFSET, maxZ),
                    new Vec3(minX, minY - FACE_OFFSET, maxZ)
            );
            case UP -> new QuadFace(
                    new Vec3(minX, maxY + FACE_OFFSET, maxZ),
                    new Vec3(maxX, maxY + FACE_OFFSET, maxZ),
                    new Vec3(maxX, maxY + FACE_OFFSET, minZ),
                    new Vec3(minX, maxY + FACE_OFFSET, minZ)
            );
            case NORTH -> new QuadFace(
                    new Vec3(maxX, minY, minZ - FACE_OFFSET),
                    new Vec3(minX, minY, minZ - FACE_OFFSET),
                    new Vec3(minX, maxY, minZ - FACE_OFFSET),
                    new Vec3(maxX, maxY, minZ - FACE_OFFSET)
            );
            case SOUTH -> new QuadFace(
                    new Vec3(minX, minY, maxZ + FACE_OFFSET),
                    new Vec3(maxX, minY, maxZ + FACE_OFFSET),
                    new Vec3(maxX, maxY, maxZ + FACE_OFFSET),
                    new Vec3(minX, maxY, maxZ + FACE_OFFSET)
            );
            case WEST -> new QuadFace(
                    new Vec3(minX - FACE_OFFSET, minY, minZ),
                    new Vec3(minX - FACE_OFFSET, minY, maxZ),
                    new Vec3(minX - FACE_OFFSET, maxY, maxZ),
                    new Vec3(minX - FACE_OFFSET, maxY, minZ)
            );
            case EAST -> new QuadFace(
                    new Vec3(maxX + FACE_OFFSET, minY, maxZ),
                    new Vec3(maxX + FACE_OFFSET, minY, minZ),
                    new Vec3(maxX + FACE_OFFSET, maxY, minZ),
                    new Vec3(maxX + FACE_OFFSET, maxY, maxZ)
            );
        };

        faces.add(face);
        lines.add(canonicalLine(face.a, face.b));
        lines.add(canonicalLine(face.b, face.c));
        lines.add(canonicalLine(face.c, face.d));
        lines.add(canonicalLine(face.d, face.a));
    }

    private static LineSegment canonicalLine(Vec3 first, Vec3 second) {
        return compare(first, second) <= 0
                ? new LineSegment(first, second)
                : new LineSegment(second, first);
    }

    private static int compare(Vec3 first, Vec3 second) {
        int x = Double.compare(first.x, second.x);
        if (x != 0) return x;
        int y = Double.compare(first.y, second.y);
        if (y != 0) return y;
        return Double.compare(first.z, second.z);
    }

    private static void rebuildRenderData() {
        List<SensorRangeMesh> meshes = new ArrayList<>();
        for (SensorState sensor : selectedSensors) {
            if (sensor.mesh != null && !sensor.mesh.faces.isEmpty()) {
                meshes.add(sensor.mesh);
            }
        }

        renderData = meshes.isEmpty() ? null : new SculkRenderData(List.copyOf(meshes));
        renderDataDirty = false;
        invalidatePrimitives();
    }

    private static void clearRenderSnapshot() {
        renderData = null;
        renderDataDirty = false;
        invalidatePrimitives();
    }

    private static void invalidatePrimitives() {
        renderPrimitives = null;
        primitivesRenderData = null;
        primitivesStyle = null;
    }

    private static void render(LevelRenderContext context) {
        SculkRenderData data = renderData;
        if (data == null || context.levelState().cameraRenderState == null) return;

        RenderStyle style = new RenderStyle(
                RedstoneUtilsConfig.sculkColor(),
                RedstoneUtilsConfig.getOverlayOpacity(),
                RedstoneUtilsConfig.getOverlayLineWidth()
        );
        if (renderPrimitives == null || primitivesRenderData != data || !style.equals(primitivesStyle)) {
            renderPrimitives = buildPrimitives(data, style);
            primitivesRenderData = data;
            primitivesStyle = style;
        }

        renderPrimitives.submit(
                context.submitNodeCollector(),
                context.levelState().cameraRenderState,
                RedstoneUtilsConfig.renderOverlaysThroughWalls()
        );
    }

    private static DrawableGizmoPrimitives buildPrimitives(SculkRenderData data, RenderStyle style) {
        DrawableGizmoPrimitives primitives = new DrawableGizmoPrimitives();
        int fillColor = color(style.baseColor, style.opacity, 0.18F);
        int strokeColor = color(style.baseColor, style.opacity, 0.78F);
        GizmoStyle sensorStyle = GizmoStyle.strokeAndFill(
                strokeColor,
                style.lineWidth,
                color(style.baseColor, style.opacity, 0.30F)
        );

        for (SensorRangeMesh mesh : data.meshes) {
            for (QuadFace face : mesh.faces) {
                primitives.addQuad(face.a, face.b, face.c, face.d, fillColor);
            }
            for (LineSegment line : mesh.lines) {
                primitives.addLine(line.from, line.to, strokeColor, style.lineWidth);
            }
            new CuboidGizmo(new AABB(mesh.sensorPos).inflate(SENSOR_BOX_INFLATE), sensorStyle, false)
                    .emit(primitives, 1.0F);
        }
        return primitives;
    }

    private static int color(int color, float opacity, float alphaMultiplier) {
        int alpha = Math.clamp(Math.round((color >>> 24) * opacity * alphaMultiplier), 0, 255);
        return color & 0x00FFFFFF | alpha << 24;
    }

    private static final class SensorState {
        private BlockEntity blockEntity;
        private final BlockPos blockPos;
        private final int radius;
        private SensorRangeMesh mesh;
        private boolean dirty = true;
        private long nextRebuildGameTime = Long.MIN_VALUE;

        private SensorState(BlockEntity blockEntity, BlockPos blockPos, int radius) {
            this.blockEntity = blockEntity;
            this.blockPos = blockPos;
            this.radius = radius;
        }

        private void markDirty() {
            dirty = true;
        }

        private void forceDirty() {
            dirty = true;
            nextRebuildGameTime = Long.MIN_VALUE;
        }

        private boolean canRebuild(long gameTime) {
            return dirty && (mesh == null || nextRebuildGameTime == Long.MIN_VALUE || gameTime >= nextRebuildGameTime);
        }
    }

    private record SculkRenderData(List<SensorRangeMesh> meshes) {
    }

    private record SensorRangeMesh(BlockPos sensorPos, List<QuadFace> faces, List<LineSegment> lines) {
    }

    private record QuadFace(Vec3 a, Vec3 b, Vec3 c, Vec3 d) {
    }

    private record LineSegment(Vec3 from, Vec3 to) {
    }

    private record RenderStyle(int baseColor, float opacity, float lineWidth) {
    }
}
