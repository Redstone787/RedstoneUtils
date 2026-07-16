package org.main.redstoneutils.server.autowire;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ServerAutoWire {

    private static final int PLACEMENT_CONFIRMATION_TICKS = 20;
    private static final int AUTOWIRE_DELAY_TICKS = 1;
    private static final int MAX_REDSTONE_DUST_RUN = 15;
    private static final int FAST_AUTO_REPEATER_SIGNAL = 1;

    private static final Map<UUID, PlayerWireState> playerStates = new HashMap<>();
    private static final List<PendingPlacement> pendingPlacements = new ArrayList<>();
    private static final List<PendingAutoWire> pendingAutoWires = new ArrayList<>();

    private static boolean initialized = false;

    private ServerAutoWire() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        UseBlockCallback.EVENT.register(ServerAutoWire::onUseBlock);
        ServerTickEvents.END_SERVER_TICK.register(ServerAutoWire::tick);
    }

    public static WireType getWireType(ServerPlayer player) {
        return stateFor(player.getUUID()).wireType;
    }

    public static void setWireType(ServerPlayer player, WireType wireType) {
        PlayerWireState state = stateFor(player.getUUID());
        state.wireType = wireType == null ? WireType.NONE : wireType;
        state.reset();
    }

    public static void reset(ServerPlayer player) {
        stateFor(player.getUUID()).reset();
    }

    private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        WireType wireType = getWireType(serverPlayer);
        if (wireType == WireType.NONE) return InteractionResult.PASS;

        ItemStack itemStack = player.getItemInHand(hand);
        if (!(itemStack.getItem() instanceof BlockItem blockItem)) {
            return InteractionResult.PASS;
        }

        BlockPlaceContext placementContext = new BlockPlaceContext(player, hand, itemStack, hitResult);
        if (!placementContext.canPlace()) {
            return InteractionResult.PASS;
        }

        BlockState placementState = blockItem.getBlock().getStateForPlacement(placementContext);
        if (placementState == null) {
            return InteractionResult.PASS;
        }

        BlockPos placedBlockPos = placementContext.getClickedPos().immutable();
        BlockState previousState = serverLevel.getBlockState(placedBlockPos);
        pendingPlacements.add(new PendingPlacement(
                serverLevel,
                serverPlayer.getUUID(),
                placedBlockPos,
                previousState,
                placementState.getBlock(),
                itemStack.getItem(),
                wireType
        ));

        return InteractionResult.PASS;
    }

    private static void tick(MinecraftServer server) {
        tickPendingPlacements(server);
        tickPendingAutoWires(server);
    }

    private static void tickPendingPlacements(MinecraftServer server) {
        if (pendingPlacements.isEmpty()) return;

        Iterator<PendingPlacement> iterator = pendingPlacements.iterator();
        while (iterator.hasNext()) {
            PendingPlacement placement = iterator.next();
            if (placement.level.getServer() != server) {
                iterator.remove();
                continue;
            }

            BlockState currentState = placement.level.getBlockState(placement.blockPos);
            if (placement.isConfirmed(currentState)) {
                pendingAutoWires.add(new PendingAutoWire(
                        placement.level,
                        placement.playerUuid,
                        placement.blockPos,
                        currentState,
                        placement.expectedItem,
                        placement.wireType
                ));
                iterator.remove();
                continue;
            }

            placement.remainingTicks--;
            if (placement.remainingTicks <= 0) {
                iterator.remove();
            }
        }
    }

    private static void tickPendingAutoWires(MinecraftServer server) {
        if (pendingAutoWires.isEmpty()) return;

        Iterator<PendingAutoWire> iterator = pendingAutoWires.iterator();
        while (iterator.hasNext()) {
            PendingAutoWire autoWire = iterator.next();
            if (autoWire.level.getServer() != server) {
                iterator.remove();
                continue;
            }

            autoWire.remainingTicks--;
            if (autoWire.remainingTicks > 0) continue;

            ServerPlayer player = server.getPlayerList().getPlayer(autoWire.playerUuid);
            if (player != null) {
                BlockState currentState = autoWire.level.getBlockState(autoWire.blockPos);
                if (currentState.getBlock() == autoWire.supportBlockState.getBlock()) {
                    stateFor(autoWire.playerUuid).wire(
                            autoWire.wireType,
                            autoWire.level,
                            player,
                            autoWire.blockPos,
                            currentState,
                            autoWire.supportItem
                    );
                }
            }

            iterator.remove();
        }
    }

    private static PlayerWireState stateFor(UUID uuid) {
        return playerStates.computeIfAbsent(uuid, ignored -> new PlayerWireState());
    }

    private static boolean placeOnTop(ServerLevel level, ServerPlayer player, BlockPos supportBlockPos, PlaceableBlock block) {
        return placeOnTop(level, player, supportBlockPos, block.defaultState(playerFacing(player), player), block.item());
    }

    private static boolean placeOnTop(ServerLevel level, ServerPlayer player, BlockPos supportBlockPos, PlaceableBlock block, Direction facing) {
        return placeOnTop(level, player, supportBlockPos, block.defaultState(facing, player), block.item());
    }

    private static boolean placeOnTop(ServerLevel level, ServerPlayer player, BlockPos supportBlockPos, BlockState blockState, Item item) {
        BlockPos blockPos = supportBlockPos.above();
        BlockState placementState = resolvePlacementState(player, supportBlockPos, blockState, item);

        if (!canPlace(level, blockPos, placementState)) {
            return false;
        }

        if (!level.setBlockAndUpdate(blockPos, placementState)) {
            return false;
        }

        updatePlacedDiode(level, blockPos, placementState);
        refreshNearbyRedstoneWires(level, blockPos, player);
        return true;
    }

    private static BlockState resolvePlacementState(ServerPlayer player, BlockPos supportBlockPos, BlockState fallbackState, Item item) {
        if (fallbackState.getBlock() != Blocks.REDSTONE_WIRE || player == null || item == null) return fallbackState;

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

        return placementState;
    }

    private static boolean canPlace(Level level, BlockPos blockPos, BlockState blockState) {
        BlockState currentState = level.getBlockState(blockPos);
        return canReplace(currentState, blockState) && blockState.canSurvive(level, blockPos);
    }

    private static boolean canReplace(BlockState currentState, BlockState replacementState) {
        return currentState.isAir()
                || currentState.canBeReplaced()
                || canReplaceRedstoneWire(currentState, replacementState);
    }

    private static boolean canReplaceRedstoneWire(BlockState currentState, BlockState replacementState) {
        return currentState.getBlock() == Blocks.REDSTONE_WIRE && replacementState.getBlock() != Blocks.REDSTONE_WIRE;
    }

    private static void updatePlacedDiode(ServerLevel serverLevel, BlockPos blockPos, BlockState blockState) {
        if (blockState.getBlock() != Blocks.REPEATER && blockState.getBlock() != Blocks.COMPARATOR) return;
        serverLevel.scheduleTick(blockPos, blockState.getBlock(), 1);
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

        BlockState refreshedState = resolvePlacementState(serverPlayer, wireBlockPos.below(), currentState, Items.REDSTONE);
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

    private static BlockState withFacing(BlockState blockState, Direction facing, ServerPlayer player) {
        if (!blockState.hasProperty(HorizontalDirectionalBlock.FACING)) return blockState;
        return blockState.setValue(HorizontalDirectionalBlock.FACING, horizontalFacing(facing, player));
    }

    private static Direction horizontalFacing(Direction facing, ServerPlayer player) {
        if (facing == Direction.NORTH || facing == Direction.SOUTH || facing == Direction.EAST || facing == Direction.WEST) {
            return facing;
        }

        return playerFacing(player);
    }

    private static Direction playerFacing(ServerPlayer player) {
        return player == null ? Direction.NORTH : player.getDirection();
    }

    private enum PlaceableBlock {
        REDSTONE_WIRE(Blocks.REDSTONE_WIRE.defaultBlockState(), Items.REDSTONE),
        REPEATER(Blocks.REPEATER.defaultBlockState(), Items.REPEATER),
        COMPARATOR(Blocks.COMPARATOR.defaultBlockState(), Items.COMPARATOR);

        private final BlockState defaultState;
        private final Item item;

        PlaceableBlock(BlockState defaultState, Item item) {
            this.defaultState = defaultState;
            this.item = item;
        }

        private BlockState defaultState(Direction facing, ServerPlayer player) {
            return ServerAutoWire.withFacing(defaultState, facing, player);
        }

        private Item item() {
            return item;
        }
    }

    private static final class PlayerWireState {
        private WireType wireType = WireType.NONE;
        private BlockPos lastAutoWiredBlockPos = null;
        private int redstoneDustRunLength = 0;
        private FastAutoStep fastAutoStep = FastAutoStep.NONE;
        private FastComparatorStep fastComparatorStep = FastComparatorStep.INPUT_BLOCK;

        private void wire(WireType wireType, ServerLevel level, ServerPlayer player, BlockPos placedBlockPos, BlockState supportBlockState, Item supportItem) {
            switch (wireType) {
                case NORMAL -> {
                    normalWire(level, player, placedBlockPos);
                    reset();
                }
                case AUTO -> autoWire(level, player, placedBlockPos);
                case FAST_AUTO -> fastAutoWire(level, player, placedBlockPos, supportBlockState, supportItem);
                case ONLY_REPEATERS -> repeatedRepeaterWire(level, player, placedBlockPos);
                case ONLY_COMPARATORS -> repeatedComparatorWire(level, player, placedBlockPos);
                case FAST_COMPARATORS -> fastComparatorWire(level, player, placedBlockPos, supportBlockState, supportItem);
                case NONE -> reset();
            }
        }

        private boolean normalWire(ServerLevel level, ServerPlayer player, BlockPos placedBlockPos) {
            return ServerAutoWire.placeOnTop(level, player, placedBlockPos, PlaceableBlock.REDSTONE_WIRE);
        }

        private void reset() {
            lastAutoWiredBlockPos = null;
            redstoneDustRunLength = 0;
            fastAutoStep = FastAutoStep.NONE;
            fastComparatorStep = FastComparatorStep.INPUT_BLOCK;
        }

        private void autoWire(ServerLevel level, ServerPlayer player, BlockPos placedBlockPos) {
            fastAutoStep = FastAutoStep.NONE;
            fastComparatorStep = FastComparatorStep.INPUT_BLOCK;
            BlockPos targetBlockPos = placedBlockPos.above();

            if (lastAutoWiredBlockPos == null) {
                placeRedstoneWire(level, player, placedBlockPos, targetBlockPos);
                return;
            }

            Direction facing = AutoWireDirection.fromTo(lastAutoWiredBlockPos, targetBlockPos);
            if (shouldPlaceRepeater(level)) {
                placeRepeater(level, player, placedBlockPos, targetBlockPos, facing);
                return;
            }

            placeRedstoneWire(level, player, placedBlockPos, targetBlockPos);
        }

        private void fastAutoWire(ServerLevel level, ServerPlayer player, BlockPos placedBlockPos, BlockState supportBlockState, Item supportItem) {
            fastComparatorStep = FastComparatorStep.INPUT_BLOCK;
            BlockPos targetBlockPos = placedBlockPos.above();

            if (lastAutoWiredBlockPos == null) {
                placeRedstoneWire(level, player, placedBlockPos, targetBlockPos);
                return;
            }

            Direction facing = AutoWireDirection.fromTo(lastAutoWiredBlockPos, targetBlockPos);
            switch (fastAutoStep) {
                case REPEATER -> {
                    if (placeRepeater(level, player, placedBlockPos, targetBlockPos, facing)) {
                        fastAutoStep = FastAutoStep.OUTPUT_BLOCK;
                    }
                    return;
                }
                case OUTPUT_BLOCK -> {
                    if (placeElevatedBlock(level, player, placedBlockPos, targetBlockPos, supportBlockState, supportItem)) {
                        fastAutoStep = FastAutoStep.OUTPUT_REDSTONE;
                    }
                    return;
                }
                case OUTPUT_REDSTONE -> {
                    if (placeRedstoneWire(level, player, placedBlockPos, targetBlockPos)) {
                        fastAutoStep = FastAutoStep.NONE;
                    }
                    return;
                }
            }

            if (shouldStartFastAutoBooster(level)) {
                if (placeElevatedBlock(level, player, placedBlockPos, targetBlockPos, supportBlockState, supportItem)) {
                    fastAutoStep = FastAutoStep.REPEATER;
                }
                return;
            }

            placeRedstoneWire(level, player, placedBlockPos, targetBlockPos);
        }

        private void repeatedRepeaterWire(ServerLevel level, ServerPlayer player, BlockPos placedBlockPos) {
            fastAutoStep = FastAutoStep.NONE;
            fastComparatorStep = FastComparatorStep.INPUT_BLOCK;

            BlockPos targetBlockPos = placedBlockPos.above();
            Direction facing = diodeFacing(player, targetBlockPos);
            placeRepeater(level, player, placedBlockPos, targetBlockPos, facing);
        }

        private void repeatedComparatorWire(ServerLevel level, ServerPlayer player, BlockPos placedBlockPos) {
            fastAutoStep = FastAutoStep.NONE;
            fastComparatorStep = FastComparatorStep.INPUT_BLOCK;

            BlockPos targetBlockPos = placedBlockPos.above();
            Direction facing = diodeFacing(player, targetBlockPos);
            placeComparator(level, player, placedBlockPos, targetBlockPos, facing);
        }

        private void fastComparatorWire(ServerLevel level, ServerPlayer player, BlockPos placedBlockPos, BlockState supportBlockState, Item supportItem) {
            fastAutoStep = FastAutoStep.NONE;

            BlockPos targetBlockPos = placedBlockPos.above();
            Direction facing = diodeFacing(player, targetBlockPos);

            switch (fastComparatorStep) {
                case INPUT_BLOCK -> {
                    if (placeElevatedBlock(level, player, placedBlockPos, targetBlockPos, supportBlockState, supportItem)) {
                        fastComparatorStep = FastComparatorStep.COMPARATOR;
                    }
                }
                case COMPARATOR -> {
                    if (placeComparator(level, player, placedBlockPos, targetBlockPos, facing)) {
                        fastComparatorStep = FastComparatorStep.OUTPUT_BLOCK;
                    }
                }
                case OUTPUT_BLOCK -> {
                    if (placeElevatedBlock(level, player, placedBlockPos, targetBlockPos, supportBlockState, supportItem)) {
                        fastComparatorStep = FastComparatorStep.OUTPUT_REDSTONE;
                    }
                }
                case OUTPUT_REDSTONE -> {
                    if (placeRedstoneWire(level, player, placedBlockPos, targetBlockPos)) {
                        fastComparatorStep = FastComparatorStep.INPUT_BLOCK;
                    }
                }
            }
        }

        private boolean shouldPlaceRepeater(Level level) {
            if (redstoneDustRunLength >= MAX_REDSTONE_DUST_RUN) return true;
            return redstoneDustRunLength > 0 && !AutoWirePower.hasPowerToContinue(level, lastAutoWiredBlockPos);
        }

        private boolean shouldStartFastAutoBooster(Level level) {
            return redstoneDustRunLength > 0
                    && AutoWirePower.getPower(level, lastAutoWiredBlockPos) == FAST_AUTO_REPEATER_SIGNAL;
        }

        private Direction diodeFacing(ServerPlayer player, BlockPos targetBlockPos) {
            if (lastAutoWiredBlockPos == null) {
                return ServerAutoWire.playerFacing(player);
            }

            return AutoWireDirection.fromTo(lastAutoWiredBlockPos, targetBlockPos);
        }

        private boolean placeRedstoneWire(ServerLevel level, ServerPlayer player, BlockPos placedBlockPos, BlockPos targetBlockPos) {
            if (!normalWire(level, player, placedBlockPos)) {
                return false;
            }

            lastAutoWiredBlockPos = targetBlockPos;
            redstoneDustRunLength++;
            return true;
        }

        private boolean placeRepeater(ServerLevel level, ServerPlayer player, BlockPos placedBlockPos, BlockPos targetBlockPos, Direction facing) {
            boolean placed = ServerAutoWire.placeOnTop(level, player, placedBlockPos, PlaceableBlock.REPEATER, facing);
            if (!placed) {
                return false;
            }

            lastAutoWiredBlockPos = targetBlockPos;
            redstoneDustRunLength = 0;
            return true;
        }

        private boolean placeComparator(ServerLevel level, ServerPlayer player, BlockPos placedBlockPos, BlockPos targetBlockPos, Direction facing) {
            boolean placed = ServerAutoWire.placeOnTop(level, player, placedBlockPos, PlaceableBlock.COMPARATOR, facing);
            if (!placed) {
                return false;
            }

            lastAutoWiredBlockPos = targetBlockPos;
            redstoneDustRunLength = 0;
            return true;
        }

        private boolean placeElevatedBlock(ServerLevel level, ServerPlayer player, BlockPos placedBlockPos, BlockPos targetBlockPos, BlockState blockState, Item item) {
            if (blockState == null) {
                return false;
            }
            if (!ServerAutoWire.placeOnTop(level, player, placedBlockPos, blockState, item)) {
                return false;
            }

            lastAutoWiredBlockPos = targetBlockPos;
            redstoneDustRunLength = 0;
            return true;
        }
    }

    private static final class PendingPlacement {
        private final ServerLevel level;
        private final UUID playerUuid;
        private final BlockPos blockPos;
        private final BlockState previousState;
        private final Block expectedBlock;
        private final Item expectedItem;
        private final WireType wireType;
        private int remainingTicks = PLACEMENT_CONFIRMATION_TICKS;

        private PendingPlacement(ServerLevel level, UUID playerUuid, BlockPos blockPos, BlockState previousState, Block expectedBlock, Item expectedItem, WireType wireType) {
            this.level = level;
            this.playerUuid = playerUuid;
            this.blockPos = blockPos;
            this.previousState = previousState;
            this.expectedBlock = expectedBlock;
            this.expectedItem = expectedItem;
            this.wireType = wireType;
        }

        private boolean isConfirmed(BlockState currentState) {
            return currentState.getBlock() == expectedBlock && !currentState.equals(previousState);
        }
    }

    private static final class PendingAutoWire {
        private final ServerLevel level;
        private final UUID playerUuid;
        private final BlockPos blockPos;
        private final BlockState supportBlockState;
        private final Item supportItem;
        private final WireType wireType;
        private int remainingTicks = AUTOWIRE_DELAY_TICKS;

        private PendingAutoWire(ServerLevel level, UUID playerUuid, BlockPos blockPos, BlockState supportBlockState, Item supportItem, WireType wireType) {
            this.level = level;
            this.playerUuid = playerUuid;
            this.blockPos = blockPos;
            this.supportBlockState = supportBlockState;
            this.supportItem = supportItem;
            this.wireType = wireType;
        }
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
