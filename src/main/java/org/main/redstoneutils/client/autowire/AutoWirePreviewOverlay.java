package org.main.redstoneutils.client.autowire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.CuboidGizmo;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.main.redstoneutils.client.config.RedstoneUtilsConfig;
import org.main.redstoneutils.client.overlay.OverlayFreeze;

import java.util.ArrayList;
import java.util.List;

public final class AutoWirePreviewOverlay {

    private static final int FILL_COLOR = 0x202E9DFF;
    private static final int STROKE_COLOR = 0x809EDBFF;
    private static final int MODEL_COLOR = 0x4D9EDBFF;
    private static final int FULL_BRIGHT_LIGHT = 0x00F000F0;
    private static final float STROKE_WIDTH = 2.0F;
    private static final double BOX_INFLATE = 0.002D;
    private static final Direction[] MODEL_DIRECTIONS = Direction.values();

    private static PreviewRenderData previewRenderData = null;
    private static boolean visible = RedstoneUtilsConfig.isWirePreviewOverlayVisible();
    private static boolean initialized = false;

    private AutoWirePreviewOverlay() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        LevelExtractionEvents.END_EXTRACTION.register(AutoWirePreviewOverlay::extract);
        LevelRenderEvents.COLLECT_SUBMITS.register(AutoWirePreviewOverlay::render);
    }

    public static boolean isVisible() {
        return visible;
    }

    public static void setVisible(boolean visible) {
        AutoWirePreviewOverlay.visible = visible;
        RedstoneUtilsConfig.setWirePreviewOverlayVisible(visible);
        if (!visible) {
            previewRenderData = null;
        }
    }

    public static boolean toggleVisible() {
        setVisible(!visible);
        return visible;
    }

    private static void extract(LevelExtractionContext context) {
        if (OverlayFreeze.wireFrozen() && previewRenderData != null) return;
        previewRenderData = null;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = context.level();
        WireType wireType = AutoWireHandler.getActiveWireType();
        if (!visible || wireType == WireType.NONE || player == null || minecraft.hitResult == null) {
            return;
        }
        if (minecraft.hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        PlacementPreview placementPreview = findPlacementPreview(player, (BlockHitResult) minecraft.hitResult);
        if (placementPreview == null) {
            return;
        }

        AutoWire.AutoWirePreview autoWirePreview = AutoWire.preview(
                wireType,
                level,
                placementPreview.blockPos(),
                placementPreview.blockState(),
                placementPreview.item()
        );
        if (autoWirePreview == null) {
            return;
        }

        List<PreviewPart> parts = new ArrayList<>();
        addPreviewPart(level, parts, placementPreview.blockPos(), placementPreview.blockState());
        addPreviewPart(level, parts, autoWirePreview.blockPos(), autoWirePreview.blockState());
        if (!parts.isEmpty()) {
            previewRenderData = new PreviewRenderData(parts);
        }
    }

    private static PlacementPreview findPlacementPreview(LocalPlayer player, BlockHitResult hitResult) {
        PlacementPreview mainHandPreview = placementPreview(player, InteractionHand.MAIN_HAND, hitResult);
        if (mainHandPreview != null) {
            return mainHandPreview;
        }

        return placementPreview(player, InteractionHand.OFF_HAND, hitResult);
    }

    private static PlacementPreview placementPreview(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult) {
        ItemStack itemStack = player.getItemInHand(hand);
        Item item = itemStack.getItem();
        if (AutoWirePlacement.isManagedPlacementItem(item)) {
            return null;
        }
        if (!(item instanceof BlockItem blockItem)) {
            return null;
        }

        BlockPlaceContext placementContext = new BlockPlaceContext(player, hand, itemStack, hitResult);
        if (!placementContext.canPlace()) {
            return null;
        }

        BlockState blockState = blockItem.getBlock().getStateForPlacement(placementContext);
        if (blockState == null) {
            return null;
        }

        return new PlacementPreview(placementContext.getClickedPos().immutable(), blockState, item);
    }

    private static void addPreviewPart(ClientLevel level, List<PreviewPart> parts, BlockPos blockPos, BlockState blockState) {
        List<AABB> boxes = previewBoxes(level, blockPos, blockState);
        List<BlockStateModelPart> modelParts = previewModelParts(blockPos, blockState);
        if (!boxes.isEmpty() || !modelParts.isEmpty()) {
            parts.add(new PreviewPart(blockPos, boxes, modelParts));
        }
    }

    private static List<AABB> previewBoxes(ClientLevel level, BlockPos blockPos, BlockState blockState) {
        VoxelShape shape = blockState.getShape(level, blockPos, CollisionContext.empty());
        if (shape.isEmpty()) {
            shape = Shapes.block();
        }

        List<AABB> boxes = new ArrayList<>();
        for (AABB box : shape.toAabbs()) {
            boxes.add(box.move(blockPos).inflate(BOX_INFLATE));
        }

        return boxes;
    }

    private static List<BlockStateModelPart> previewModelParts(BlockPos blockPos, BlockState blockState) {
        if (blockState.getRenderShape() != RenderShape.MODEL) {
            return List.of();
        }

        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(blockState);
        List<BlockStateModelPart> modelParts = new ArrayList<>();
        model.collectParts(RandomSource.create(blockState.getSeed(blockPos)), modelParts);
        return modelParts;
    }

    private static void render(LevelRenderContext context) {
        PreviewRenderData renderData = previewRenderData;
        if (renderData == null || context.levelState().cameraRenderState == null) {
            return;
        }

        renderModels(context, renderData);
        renderGizmos(context, renderData);
    }

    private static void renderModels(LevelRenderContext context, PreviewRenderData renderData) {
        PoseStack poseStack = context.poseStack();
        Vec3 cameraPos = context.levelState().cameraRenderState.pos;
        if (poseStack == null || cameraPos == null) {
            return;
        }

        for (PreviewPart part : renderData.parts()) {
            if (part.modelParts().isEmpty()) {
                continue;
            }

            poseStack.pushPose();
            poseStack.translate(
                    part.blockPos().getX() - cameraPos.x(),
                    part.blockPos().getY() - cameraPos.y(),
                    part.blockPos().getZ() - cameraPos.z()
            );
            context.submitNodeCollector().submitCustomGeometry(
                    poseStack,
                    RenderTypes.translucentMovingBlock(),
                    (pose, vertexConsumer) -> renderModelParts(part.modelParts(), pose, vertexConsumer)
            );
            poseStack.popPose();
        }
    }

    private static void renderModelParts(List<BlockStateModelPart> modelParts, PoseStack.Pose pose, VertexConsumer vertexConsumer) {
        QuadInstance quadInstance = new QuadInstance();
        quadInstance.setLightCoords(FULL_BRIGHT_LIGHT);
        quadInstance.setOverlayCoords(OverlayTexture.NO_OVERLAY);

        for (BlockStateModelPart modelPart : modelParts) {
            for (Direction direction : MODEL_DIRECTIONS) {
                renderQuads(modelPart.getQuads(direction), pose, vertexConsumer, quadInstance);
            }
            renderQuads(modelPart.getQuads(null), pose, vertexConsumer, quadInstance);
        }
    }

    private static void renderQuads(List<BakedQuad> quads, PoseStack.Pose pose, VertexConsumer vertexConsumer, QuadInstance quadInstance) {
        for (BakedQuad quad : quads) {
            quadInstance.setColor(color(RedstoneUtilsConfig.wirePreviewColor(), 0.38F));
            vertexConsumer.putBakedQuad(pose, quad, quadInstance);
        }
    }

    private static void renderGizmos(LevelRenderContext context, PreviewRenderData renderData) {
        DrawableGizmoPrimitives primitives = new DrawableGizmoPrimitives();
        GizmoStyle style = GizmoStyle.strokeAndFill(
                color(RedstoneUtilsConfig.wirePreviewColor(), 1.0F),
                RedstoneUtilsConfig.getOverlayLineWidth(),
                color(RedstoneUtilsConfig.wirePreviewColor(), 0.22F)
        );
        for (PreviewPart part : renderData.parts()) {
            for (AABB box : part.boxes()) {
                new CuboidGizmo(box, style, false).emit(primitives, 1.0F);
            }
        }

        primitives.submit(context.submitNodeCollector(), context.levelState().cameraRenderState, RedstoneUtilsConfig.renderOverlaysThroughWalls());
    }

    private static int color(int color, float alphaMultiplier) {
        int alpha = Math.clamp(Math.round((color >>> 24) * RedstoneUtilsConfig.getOverlayOpacity() * alphaMultiplier), 0, 255);
        return color & 0x00FFFFFF | alpha << 24;
    }

    private record PlacementPreview(BlockPos blockPos, BlockState blockState, Item item) {
    }

    private record PreviewPart(BlockPos blockPos, List<AABB> boxes, List<BlockStateModelPart> modelParts) {
    }

    private record PreviewRenderData(List<PreviewPart> parts) {
    }
}
