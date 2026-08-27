package org.main.redstoneutils.client.autowire;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.main.redstoneutils.client.ui.RedstoneMessages;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AutoWirePlacement {

    private static final int INTEGRATED_PLACEMENT_RETRY_TICKS = 10;
    private static final List<PendingIntegratedPlacement> pendingIntegratedPlacements = new ArrayList<>();

    private static boolean managedPlacementInProgress = false;
    private static boolean initialized = false;

    private AutoWirePlacement() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        ServerTickEvents.END_SERVER_TICK.register(AutoWirePlacement::tickPendingIntegratedPlacements);
        ServerLifecycleEvents.SERVER_STOPPED.register(ignored -> pendingIntegratedPlacements.clear());
    }

    public static boolean placeOnTop(Level level, BlockPos supportBlockPos, PlaceableBlock block) {
        return placeOnTop(level, supportBlockPos, block.defaultState(playerFacing()), block.item());
    }

    public static boolean placeOnTop(Level level, BlockPos supportBlockPos, PlaceableBlock block, Direction facing) {
        return placeOnTop(level, supportBlockPos, block.defaultState(facing), block.item());
    }

    public static boolean placeOnTop(Level level, BlockPos supportBlockPos, BlockState blockState, Item item) {
        BlockPos blockPos = supportBlockPos.above();

        BlockState currentState = level.getBlockState(blockPos);
        if (!canReplace(currentState, blockState)) {
            reportFailure("target_occupied", blockState);
            return false;
        }
        if (!blockState.canSurvive(level, blockPos)) {
            reportFailure("cannot_survive", blockState);
            return false;
        }
        if (placeOnIntegratedServer(level, supportBlockPos, blockPos, blockState, item)) {
            return true;
        }

        boolean placed = placeWithItem(supportBlockPos, item);
        if (!placed) reportFailure("missing_item", blockState);
        return placed;
    }

    public static boolean placeOnTop(Level level, BlockPos supportBlockPos, BlockState blockState, Item item, Direction facing) {
        return placeOnTop(level, supportBlockPos, withFacing(blockState, facing), item);
    }

    public static BlockState resolvePreviewPlacementState(Level level, BlockPos supportBlockPos, BlockState fallbackState, Item item) {
        if (fallbackState == null || fallbackState.getBlock() != Blocks.REDSTONE_WIRE || item == null) {
            return fallbackState;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return fallbackState;
        }

        BlockState placementState = fallbackState.getBlock().getStateForPlacement(new BlockPlaceContext(
                player,
                InteractionHand.MAIN_HAND,
                new ItemStack(item),
                hitResultForTopOf(supportBlockPos)
        ));
        if (placementState == null) return fallbackState;

        if (fallbackState.hasProperty(BlockStateProperties.POWER) && placementState.hasProperty(BlockStateProperties.POWER)) {
            placementState = placementState.setValue(BlockStateProperties.POWER, fallbackState.getValue(BlockStateProperties.POWER));
        }

        return fixPreviewRedstoneClimbs(level, supportBlockPos.above(), placementState);
    }

    public static boolean canPreviewPlaceOnTop(Level level, BlockPos supportBlockPos, BlockState blockState, BlockState previewSupportBlockState) {
        if (level == null || supportBlockPos == null || blockState == null) {
            return false;
        }

        BlockPos blockPos = supportBlockPos.above();
        BlockState currentState = level.getBlockState(blockPos);
        return canReplace(currentState, blockState) && canSurvivePreviewPlacement(level, supportBlockPos, blockPos, blockState, previewSupportBlockState);
    }

    public static boolean isManagedPlacementItem(Item item) {
        for (PlaceableBlock block : PlaceableBlock.values()) {
            if (block.item() == item) return true;
        }

        return false;
    }

    public static boolean isManagedPlacementInProgress() {
        return managedPlacementInProgress;
    }

    private static boolean placeOnIntegratedServer(Level clientLevel, BlockPos supportBlockPos, BlockPos blockPos, BlockState blockState, Item item) {
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) return false;

        LocalPlayer clientPlayer = Minecraft.getInstance().player;
        UUID playerUuid = clientPlayer == null ? null : clientPlayer.getUUID();

        server.execute(() -> {
            ServerLevel serverLevel = server.getLevel(clientLevel.dimension());
            if (serverLevel == null) {
                return;
            }

            ServerPlayer serverPlayer = playerUuid == null ? null : server.getPlayerList().getPlayer(playerUuid);
            BlockState placementState = resolveServerPlacementState(serverPlayer, supportBlockPos, blockState, item);
            placeOrRetryIntegrated(
                    serverLevel,
                    playerUuid,
                    supportBlockPos,
                    blockPos,
                    placementState,
                    item,
                    INTEGRATED_PLACEMENT_RETRY_TICKS
            );
        });
        return true;
    }

    private static void tickPendingIntegratedPlacements(MinecraftServer server) {
        if (pendingIntegratedPlacements.isEmpty()) return;

        List<PendingIntegratedPlacement> placements = new ArrayList<>(pendingIntegratedPlacements);
        pendingIntegratedPlacements.clear();

        for (PendingIntegratedPlacement placement : placements) {
            if (placement.level.getServer() != server) {
                continue;
            }

            placeOrRetryIntegrated(
                    placement.level,
                    placement.playerUuid,
                    placement.supportBlockPos,
                    placement.blockPos,
                    placement.blockState,
                    placement.item,
                    placement.remainingRetries
            );
        }
    }

    private static void placeOrRetryIntegrated(ServerLevel serverLevel, UUID playerUuid, BlockPos supportBlockPos, BlockPos blockPos, BlockState blockState, Item item, int remainingRetries) {
        if (!canPlace(serverLevel, blockPos, blockState)) {
            if (remainingRetries > 0) {
                pendingIntegratedPlacements.add(new PendingIntegratedPlacement(
                        serverLevel,
                        playerUuid,
                        supportBlockPos,
                        blockPos,
                        blockState,
                        item,
                        remainingRetries - 1
                ));
            } else reportFailure(
                    canReplace(serverLevel.getBlockState(blockPos), blockState) ? "cannot_survive" : "target_occupied",
                    blockState
            );
            return;
        }

        ServerPlayer serverPlayer = playerUuid == null ? null : serverLevel.getServer().getPlayerList().getPlayer(playerUuid);
        if (serverPlayer == null || !hasPlacementItem(serverPlayer, item)) {
            reportFailure("missing_item", blockState);
            return;
        }
        if (!serverLevel.setBlockAndUpdate(blockPos, blockState)) {
            reportFailure("placement_rejected", blockState);
            return;
        }
        updatePlacedDiode(serverLevel, blockPos, blockState);
        refreshNearbyRedstoneWires(serverLevel, blockPos, serverPlayer);
        if (!serverLevel.getBlockState(blockPos).is(blockState.getBlock())) {
            reportFailure("cannot_survive", blockState);
            return;
        }
        consumePlacementItem(serverPlayer, item);
    }

    private static boolean hasPlacementItem(ServerPlayer player, Item item) {
        if (player.hasInfiniteMaterials()) return true;
        Inventory inventory = player.getInventory();
        for (ItemStack stack : inventory.getNonEquipmentItems()) {
            if (!stack.isEmpty() && stack.is(item)) return true;
        }
        ItemStack offhand = inventory.getItem(Inventory.SLOT_OFFHAND);
        return !offhand.isEmpty() && offhand.is(item);
    }

    private static void consumePlacementItem(ServerPlayer player, Item item) {
        if (player.hasInfiniteMaterials()) return;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getNonEquipmentItems().size(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.is(item)) {
                stack.shrink(1);
                inventory.setChanged();
                player.containerMenu.broadcastChanges();
                return;
            }
        }
        ItemStack offhand = inventory.getItem(Inventory.SLOT_OFFHAND);
        if (!offhand.isEmpty() && offhand.is(item)) {
            offhand.shrink(1);
            inventory.setChanged();
            player.containerMenu.broadcastChanges();
        }
    }

    private static void updatePlacedDiode(ServerLevel serverLevel, BlockPos blockPos, BlockState blockState) {
        if (blockState.getBlock() != Blocks.REPEATER && blockState.getBlock() != Blocks.COMPARATOR) return;

        serverLevel.scheduleTick(blockPos, blockState.getBlock(), 1);
    }

    private static boolean placeWithItem(BlockPos supportBlockPos, Item item) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        MultiPlayerGameMode gameMode = minecraft.gameMode;
        if (player == null || gameMode == null || item == null) {
            return false;
        }

        if (isItem(player.getItemInHand(InteractionHand.MAIN_HAND), item)) {
            return useItemOnBlock(player, gameMode, InteractionHand.MAIN_HAND, supportBlockPos);
        }

        if (isItem(player.getItemInHand(InteractionHand.OFF_HAND), item)) {
            return useItemOnBlock(player, gameMode, InteractionHand.OFF_HAND, supportBlockPos);
        }

        Inventory inventory = player.getInventory();
        int itemSlot = findHotbarSlot(inventory, item);
        if (itemSlot == -1) {
            return placeWithTemporaryCreativeItem(player, gameMode, inventory, supportBlockPos, item);
        }

        int previousSlot = inventory.getSelectedSlot();
        inventory.setSelectedSlot(itemSlot);
        boolean placed = useItemOnBlock(player, gameMode, InteractionHand.MAIN_HAND, supportBlockPos);
        inventory.setSelectedSlot(previousSlot);
        return placed;
    }

    private static boolean placeWithTemporaryCreativeItem(
            LocalPlayer player,
            MultiPlayerGameMode gameMode,
            Inventory inventory,
            BlockPos supportBlockPos,
            Item item
    ) {
        if (!player.hasInfiniteMaterials()) {
            return false;
        }

        int selectedSlot = inventory.getSelectedSlot();
        int menuSlot = InventoryMenu.USE_ROW_SLOT_START + selectedSlot;
        ItemStack previousStack = inventory.getItem(selectedSlot).copy();
        ItemStack placementStack = new ItemStack(item);

        inventory.setItem(selectedSlot, placementStack);
        gameMode.handleCreativeModeItemAdd(placementStack, menuSlot);
        try {
            return useItemOnBlock(player, gameMode, InteractionHand.MAIN_HAND, supportBlockPos);
        } finally {
            inventory.setItem(selectedSlot, previousStack);
            gameMode.handleCreativeModeItemAdd(previousStack, menuSlot);
        }
    }

    private static boolean useItemOnBlock(LocalPlayer player, MultiPlayerGameMode gameMode, InteractionHand hand, BlockPos supportBlockPos) {
        BlockHitResult hitResult = hitResultForTopOf(supportBlockPos);

        managedPlacementInProgress = true;
        try {
            InteractionResult result = gameMode.useItemOn(player, hand, hitResult);
            if (result.consumesAction()) {
                player.swing(hand);
                return true;
            }
        } finally {
            managedPlacementInProgress = false;
        }

        return false;
    }

    private static int findHotbarSlot(Inventory inventory, Item item) {
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            if (isItem(inventory.getItem(slot), item)) return slot;
        }

        return -1;
    }

    private static boolean canPlace(Level level, BlockPos blockPos, BlockState blockState) {
        BlockState currentState = level.getBlockState(blockPos);
        boolean canReplace = canReplace(currentState, blockState);

        return canReplace && blockState.canSurvive(level, blockPos);
    }

    private static boolean canReplace(BlockState currentState, BlockState replacementState) {
        return currentState.isAir()
                || currentState.canBeReplaced()
                || canReplaceRedstoneWire(currentState, replacementState);
    }

    private static boolean canReplaceRedstoneWire(BlockState currentState, BlockState replacementState) {
        return currentState.getBlock() == Blocks.REDSTONE_WIRE && replacementState.getBlock() != Blocks.REDSTONE_WIRE;
    }

    static void reportFailure(String reason, BlockState intendedState) {
        String key = intendedState == null
                ? Blocks.AIR.getDescriptionId()
                : intendedState.getBlock().getDescriptionId();
        RedstoneMessages.popup(Component.translatable(
                "message.redstoneutils.autowire.failure." + reason,
                Component.translatable(key)
        ));
    }

    private static boolean canSurvivePreviewPlacement(Level level, BlockPos supportBlockPos, BlockPos blockPos, BlockState blockState, BlockState previewSupportBlockState) {
        if (blockState.getBlock() == Blocks.REDSTONE_WIRE) {
            return supportFaceIsSturdy(level, supportBlockPos, previewSupportBlockState, SupportType.CENTER);
        }
        if (blockState.getBlock() == Blocks.REPEATER || blockState.getBlock() == Blocks.COMPARATOR) {
            return supportFaceIsSturdy(level, supportBlockPos, previewSupportBlockState, SupportType.RIGID);
        }

        return blockState.canSurvive(level, blockPos);
    }

    private static boolean supportFaceIsSturdy(Level level, BlockPos supportBlockPos, BlockState previewSupportBlockState, SupportType supportType) {
        BlockState supportBlockState = previewSupportBlockState == null
                ? level.getBlockState(supportBlockPos)
                : previewSupportBlockState;

        return supportBlockState.isFaceSturdy(level, supportBlockPos, Direction.UP, supportType);
    }

    private static BlockState fixPreviewRedstoneClimbs(Level level, BlockPos wireBlockPos, BlockState placementState) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            EnumProperty<RedstoneSide> property = RedStoneWireBlock.PROPERTY_BY_DIRECTION.get(direction);
            if (property == null || placementState.getValue(property) != RedstoneSide.UP) {
                continue;
            }

            BlockState neighborState = level.getBlockState(wireBlockPos.relative(direction));
            if (neighborState.getBlock() == Blocks.REDSTONE_WIRE) {
                placementState = placementState.setValue(property, RedstoneSide.SIDE);
            }
        }

        return placementState;
    }

    private static BlockState resolveServerPlacementState(ServerPlayer serverPlayer, BlockPos supportBlockPos, BlockState fallbackState, Item item) {
        if (fallbackState.getBlock() != Blocks.REDSTONE_WIRE || serverPlayer == null || item == null) return fallbackState;

        BlockState placementState = fallbackState.getBlock().getStateForPlacement(new BlockPlaceContext(
                serverPlayer,
                InteractionHand.MAIN_HAND,
                new ItemStack(item),
                hitResultForTopOf(supportBlockPos)
        ));
        if (placementState == null) return fallbackState;

        if (fallbackState.hasProperty(BlockStateProperties.POWER) && placementState.hasProperty(BlockStateProperties.POWER)) {
            placementState = placementState.setValue(BlockStateProperties.POWER, fallbackState.getValue(BlockStateProperties.POWER));
        }

        return placementState;
    }

    private static void refreshNearbyRedstoneWires(ServerLevel serverLevel, BlockPos blockPos, ServerPlayer serverPlayer) {
        refreshRedstoneWire(serverLevel, blockPos, serverPlayer);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            refreshRedstoneWire(serverLevel, blockPos.relative(direction), serverPlayer);
        }
    }

    private static void refreshRedstoneWire(ServerLevel serverLevel, BlockPos wireBlockPos, ServerPlayer serverPlayer) {
        if (serverPlayer == null) return;

        BlockState currentState = serverLevel.getBlockState(wireBlockPos);
        if (currentState.getBlock() != Blocks.REDSTONE_WIRE) return;

        BlockState refreshedState = resolveServerPlacementState(serverPlayer, wireBlockPos.below(), currentState, Items.REDSTONE);
        if (!refreshedState.equals(currentState) && refreshedState.canSurvive(serverLevel, wireBlockPos)) {
            serverLevel.setBlockAndUpdate(wireBlockPos, refreshedState);
        }
    }

    private static BlockHitResult hitResultForTopOf(BlockPos supportBlockPos) {
        return new BlockHitResult(
                Vec3.upFromBottomCenterOf(supportBlockPos, 1.0D),
                Direction.UP,
                supportBlockPos,
                false
        );
    }

    public static BlockState withFacing(BlockState blockState, Direction facing) {
        if (!blockState.hasProperty(HorizontalDirectionalBlock.FACING)) return blockState;
        return blockState.setValue(HorizontalDirectionalBlock.FACING, horizontalFacing(facing));
    }

    public static Direction playerFacing() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        return player == null ? Direction.NORTH : player.getDirection();
    }

    private static Direction horizontalFacing(Direction facing) {
        if (facing == Direction.NORTH || facing == Direction.SOUTH || facing == Direction.EAST || facing == Direction.WEST) {
            return facing;
        }

        return playerFacing();
    }

    private static boolean isItem(ItemStack itemStack, Item item) {
        return !itemStack.isEmpty() && itemStack.getItem() == item;
    }

    public enum PlaceableBlock {
        REDSTONE_WIRE(Blocks.REDSTONE_WIRE.defaultBlockState(), Items.REDSTONE),
        REPEATER(Blocks.REPEATER.defaultBlockState(), Items.REPEATER),
        COMPARATOR(Blocks.COMPARATOR.defaultBlockState(), Items.COMPARATOR);

        private final BlockState defaultState;
        private final Item item;

        PlaceableBlock(BlockState defaultState, Item item) {
            this.defaultState = defaultState;
            this.item = item;
        }

        public BlockState defaultState() {
            return defaultState;
        }

        public BlockState defaultState(Direction facing) {
            return AutoWirePlacement.withFacing(defaultState, facing);
        }

        public Item item() {
            return item;
        }
    }

    private record PendingIntegratedPlacement(ServerLevel level, UUID playerUuid, BlockPos supportBlockPos, BlockPos blockPos,
                                              BlockState blockState, Item item, int remainingRetries) {
    }
}
