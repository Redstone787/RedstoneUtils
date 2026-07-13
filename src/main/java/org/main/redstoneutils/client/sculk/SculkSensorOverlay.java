package org.main.redstoneutils.client.sculk;

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
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CalibratedSculkSensorBlock;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.main.redstoneutils.client.config.RedstoneUtilsConfig;

import java.util.ArrayList;
import java.util.List;

public final class SculkSensorOverlay {

    private static final int SCULK_SENSOR_RADIUS = 8;
    private static final int CALIBRATED_SENSOR_RADIUS = 16;
    private static final int FILL_COLOR = 0x2632E6D6;
    private static final int STROKE_COLOR = 0xB034FFE8;
    private static final int SENSOR_FILL_COLOR = 0x4021B8AA;
    private static final int SENSOR_STROKE_COLOR = 0xE045FFF0;
    private static final float STROKE_WIDTH = 2.0F;
    private static final double FACE_OFFSET = 0.002D;
    private static final double SENSOR_BOX_INFLATE = 0.004D;
    private static final double OCCLUSION_RAY_OFFSET = 9.999999747378752E-6D;
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final GizmoStyle SENSOR_STYLE = GizmoStyle.strokeAndFill(
            SENSOR_STROKE_COLOR,
            STROKE_WIDTH,
            SENSOR_FILL_COLOR
    );

    private static SculkRenderData renderData = null;
    private static boolean visible = RedstoneUtilsConfig.isSculkOverlayVisible();
    private static boolean initialized = false;
    private static long nextRebuildGameTime = 0L;

    private SculkSensorOverlay() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

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
            renderData = null;
        }
        nextRebuildGameTime = 0L;
    }

    public static boolean toggleVisible() {
        setVisible(!visible);
        return visible;
    }

    public static void requestRefresh() {
        renderData = null;
        nextRebuildGameTime = 0L;
    }

    private static void extract(LevelExtractionContext context) {
        if (!visible) {
            renderData = null;
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = context.level();
        if (player == null || level == null) {
            renderData = null;
            return;
        }

        long gameTime = level.getGameTime();
        if (renderData != null && gameTime < nextRebuildGameTime) {
            return;
        }

        nextRebuildGameTime = gameTime + RedstoneUtilsConfig.getSculkRebuildIntervalTicks();
        renderData = buildRenderData(level, player.blockPosition());
    }

    private static SculkRenderData buildRenderData(ClientLevel level, BlockPos playerPos) {
        List<SculkSensor> sensors = findSensors(level, playerPos);
        if (sensors.isEmpty()) {
            return null;
        }

        List<SensorRangeMesh> meshes = new ArrayList<>();
        for (SculkSensor sensor : sensors) {
            SensorRangeMesh mesh = buildSensorMesh(level, sensor);
            if (!mesh.faces().isEmpty()) {
                meshes.add(mesh);
            }
        }

        return meshes.isEmpty() ? null : new SculkRenderData(meshes);
    }

    private static List<SculkSensor> findSensors(ClientLevel level, BlockPos playerPos) {
        int maxSensorDistance = RedstoneUtilsConfig.getSculkSensorSearchDistance();
        int minChunkX = (playerPos.getX() - maxSensorDistance) >> 4;
        int maxChunkX = (playerPos.getX() + maxSensorDistance) >> 4;
        int minChunkZ = (playerPos.getZ() - maxSensorDistance) >> 4;
        int maxChunkZ = (playerPos.getZ() + maxSensorDistance) >> 4;

        List<SculkSensor> sensors = new ArrayList<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                ChunkAccess chunkAccess = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (!(chunkAccess instanceof LevelChunk chunk)) {
                    continue;
                }

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    BlockPos blockPos = blockEntity.getBlockPos();
                    if (!blockPos.closerThan(playerPos, maxSensorDistance)) {
                        continue;
                    }

                    BlockState blockState = level.getBlockState(blockPos);
                    int radius = listenerRadius(blockState);
                    if (radius > 0) {
                        sensors.add(new SculkSensor(blockPos.immutable(), radius));
                    }
                }
            }
        }

        return sensors;
    }

    private static int listenerRadius(BlockState blockState) {
        if (blockState.getBlock() instanceof CalibratedSculkSensorBlock) {
            return CALIBRATED_SENSOR_RADIUS;
        }
        if (blockState.getBlock() instanceof SculkSensorBlock) {
            return SCULK_SENSOR_RADIUS;
        }
        return 0;
    }

    private static SensorRangeMesh buildSensorMesh(ClientLevel level, SculkSensor sensor) {
        LongSet audibleBlocks = collectAudibleBlocks(level, sensor);
        if (audibleBlocks.isEmpty()) {
            return new SensorRangeMesh(sensor.blockPos(), List.of(), List.of());
        }

        List<QuadFace> faces = new ArrayList<>();
        List<LineSegment> lines = new ArrayList<>();
        LongIterator iterator = audibleBlocks.iterator();
        while (iterator.hasNext()) {
            BlockPos blockPos = BlockPos.of(iterator.nextLong());
            for (Direction direction : DIRECTIONS) {
                if (!audibleBlocks.contains(blockPos.relative(direction).asLong())) {
                    addFace(faces, lines, blockPos, direction);
                }
            }
        }

        return new SensorRangeMesh(sensor.blockPos(), faces, lines);
    }

    private static LongSet collectAudibleBlocks(ClientLevel level, SculkSensor sensor) {
        LongSet audibleBlocks = new LongOpenHashSet();
        BlockPos sensorPos = sensor.blockPos();
        Vec3 sensorCenter = Vec3.atCenterOf(sensorPos);
        int radius = sensor.radius();
        int radiusSqr = radius * radius;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radiusSqr) {
                        continue;
                    }

                    mutablePos.set(sensorPos.getX() + dx, sensorPos.getY() + dy, sensorPos.getZ() + dz);
                    if (level.isOutsideBuildHeight(mutablePos) || !level.isLoaded(mutablePos)) {
                        continue;
                    }

                    Vec3 eventCenter = Vec3.atCenterOf(mutablePos);
                    if (!isOccluded(level, eventCenter, sensorCenter)) {
                        audibleBlocks.add(mutablePos.asLong());
                    }
                }
            }
        }

        return audibleBlocks;
    }

    private static boolean isOccluded(Level level, Vec3 eventPos, Vec3 listenerPos) {
        Vec3 eventBlockCenter = blockCenter(eventPos);
        Vec3 listenerBlockCenter = blockCenter(listenerPos);

        for (Direction direction : DIRECTIONS) {
            Vec3 nudgedEventPos = eventBlockCenter.relative(direction, OCCLUSION_RAY_OFFSET);
            BlockHitResult hitResult = level.isBlockInLine(new ClipBlockStateContext(
                    nudgedEventPos,
                    listenerBlockCenter,
                    blockState -> blockState.is(BlockTags.OCCLUDES_VIBRATION_SIGNALS)
            ));
            if (hitResult.getType() != HitResult.Type.BLOCK) {
                return false;
            }
        }

        return true;
    }

    private static Vec3 blockCenter(Vec3 pos) {
        return new Vec3(
                Mth.floor(pos.x) + 0.5D,
                Mth.floor(pos.y) + 0.5D,
                Mth.floor(pos.z) + 0.5D
        );
    }

    private static void addFace(List<QuadFace> faces, List<LineSegment> lines, BlockPos blockPos, Direction direction) {
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
        lines.add(new LineSegment(face.a(), face.b()));
        lines.add(new LineSegment(face.b(), face.c()));
        lines.add(new LineSegment(face.c(), face.d()));
        lines.add(new LineSegment(face.d(), face.a()));
    }

    private static void render(LevelRenderContext context) {
        SculkRenderData data = renderData;
        if (data == null || context.levelState().cameraRenderState == null) {
            return;
        }

        DrawableGizmoPrimitives primitives = new DrawableGizmoPrimitives();
        for (SensorRangeMesh mesh : data.meshes()) {
            for (QuadFace face : mesh.faces()) {
                primitives.addQuad(face.a(), face.b(), face.c(), face.d(), FILL_COLOR);
            }
            for (LineSegment line : mesh.lines()) {
                primitives.addLine(line.from(), line.to(), STROKE_COLOR, STROKE_WIDTH);
            }

            new CuboidGizmo(new AABB(mesh.sensorPos()).inflate(SENSOR_BOX_INFLATE), SENSOR_STYLE, false)
                    .emit(primitives, 1.0F);
        }

        primitives.submit(context.submitNodeCollector(), context.levelState().cameraRenderState, false);
    }

    private record SculkSensor(BlockPos blockPos, int radius) {
    }

    private record SculkRenderData(List<SensorRangeMesh> meshes) {
    }

    private record SensorRangeMesh(BlockPos sensorPos, List<QuadFace> faces, List<LineSegment> lines) {
    }

    private record QuadFace(Vec3 a, Vec3 b, Vec3 c, Vec3 d) {
    }

    private record LineSegment(Vec3 from, Vec3 to) {
    }
}
