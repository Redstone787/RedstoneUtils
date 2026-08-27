package org.main.redstoneutils.client.autowire;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.resources.Identifier;
import org.main.redstoneutils.client.config.RedstoneUtilsConfig;
import org.main.redstoneutils.client.RedstoneUtilsClientNetworking;
import org.main.redstoneutils.client.ui.RedstoneMessages;

import java.util.*;

public class AutoWireHandler {

    private static final int PLACEMENT_CONFIRMATION_TICKS = 20;
    private static final int AUTOWIRE_DELAY_TICKS = 1;
    private static final List<PendingPlacement> pendingPlacements = new ArrayList<>();
    private static final List<PendingAutoWire> pendingAutoWires = new ArrayList<>();

    private static boolean initialized = false;
    private static WireType activeWireType = RedstoneUtilsConfig.getActiveWireType();

    public static void init() {
        if (initialized) return;
        initialized = true;

        AutoWirePlacement.init();
        UseBlockCallback.EVENT.register(AutoWireHandler::onUseBlock);
        ClientTickEvents.END_CLIENT_TICK.register(AutoWireHandler::tick);
    }

    public static WireType getActiveWireType() {
        return activeWireType;
    }

    public static void setActiveWireType(WireType wireType) {
        activeWireType = wireType == null ? WireType.NONE : wireType;
        RedstoneUtilsConfig.setActiveWireType(activeWireType);
        AutoWire.reset();
        RedstoneUtilsClientNetworking.setServerAutoWire(activeWireType);
        RedstoneMessages.popup(net.minecraft.network.chat.Component.translatable(
                "message.redstoneutils.autowire.active",
                activeWireType.getDisplayName()
        ));
    }

    /** Replays the persisted/profiled mode after the play connection becomes writable. */
    public static void syncServerMode() {
        RedstoneUtilsClientNetworking.setServerAutoWire(activeWireType);
    }

    public static void applyServerMode(String mode, boolean accepted) {
        WireType serverMode = WireType.find(mode).orElse(WireType.NONE);
        if (accepted && serverMode == activeWireType) return;

        activeWireType = serverMode;
        RedstoneUtilsConfig.setActiveWireType(serverMode);
        AutoWire.reset();
        if (!accepted) {
            RedstoneMessages.popup(net.minecraft.network.chat.Component.translatable(
                    "message.redstoneutils.autowire.rejected",
                    serverMode.getDisplayName()
            ));
        }
    }

    public static void reloadFromProfile() {
        activeWireType = RedstoneUtilsConfig.getActiveWireType();
        AutoWire.reset();
        syncServerMode();
    }

    public static List<WireType> getSelectableWireTypes() {
        List<WireType> wireTypes = new ArrayList<>();

        Collections.addAll(wireTypes, WireType.values());

        wireTypes.sort(Comparator.comparingInt(WireType::getIndex));
        return wireTypes;
    }

    public static WireType getSelectableWireType(int segmentIndex) {
        if (segmentIndex < 0) return WireType.NONE;

        List<WireType> wireTypes = getSelectableWireTypes();
        if (segmentIndex >= wireTypes.size()) return WireType.NONE;

        return wireTypes.get(segmentIndex);
    }

    public static int getSelectableWireTypeIndex(WireType wireType) {
        if (wireType == null) return -1;

        List<WireType> wireTypes = getSelectableWireTypes();
        for (int index = 0; index < wireTypes.size(); index++) {
            if (wireTypes.get(index) == wireType) return index;
        }

        return -1;
    }

    public static List<Identifier> getSelectableWireTextures() {
        List<Identifier> textures = new ArrayList<>();

        for (WireType wireType : getSelectableWireTypes()) {
            textures.add(wireType.getTextureIdentifier());
        }

        return textures;
    }

    private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        WireType wireType = activeWireType;
        if (wireType == WireType.NONE || !level.isClientSide()) return InteractionResult.PASS;
        if (RedstoneUtilsClientNetworking.hasAutoWireBackend()) return InteractionResult.PASS;
        if (AutoWirePlacement.isManagedPlacementInProgress()) {
            return InteractionResult.PASS;
        }

        ItemStack itemStack = player.getItemInHand(hand);
        if (AutoWirePlacement.isManagedPlacementItem(itemStack.getItem())) {
            return InteractionResult.PASS;
        }
        if (!(itemStack.getItem() instanceof BlockItem blockItem)) {
            return InteractionResult.PASS;
        }

        BlockPlaceContext placementContext = new BlockPlaceContext(player, hand, itemStack, hitResult);
        if (!placementContext.canPlace()) {
            return InteractionResult.PASS;
        }

        BlockPos placedBlockPos = placementContext.getClickedPos().immutable();
        BlockState previousState = level.getBlockState(placedBlockPos);
        pendingPlacements.add(new PendingPlacement(level, placedBlockPos, previousState, blockItem.getBlock(), itemStack.getItem(), wireType));

        return InteractionResult.PASS;
    }

    private static void tick(Minecraft client) {
        tickPendingPlacements(client);
        tickPendingAutoWires(client);
    }

    private static void tickPendingPlacements(Minecraft client) {
        if (pendingPlacements.isEmpty()) return;

        Level level = client.level;
        if (level == null) {
            pendingPlacements.clear();
            return;
        }

        Iterator<PendingPlacement> iterator = pendingPlacements.iterator();
        while (iterator.hasNext()) {
            PendingPlacement placement = iterator.next();
            if (placement.level != level) {
                iterator.remove();
                continue;
            }

            BlockState currentState = level.getBlockState(placement.blockPos);
            if (placement.isConfirmed(currentState)) {
                pendingAutoWires.add(new PendingAutoWire(level, placement.blockPos, placement.expectedBlock, placement.expectedItem, placement.wireType));
                iterator.remove();
                continue;
            }

            placement.remainingTicks--;
            if (placement.remainingTicks <= 0) {
                iterator.remove();
            }
        }
    }

    private static void tickPendingAutoWires(Minecraft client) {
        if (pendingAutoWires.isEmpty()) return;

        Level level = client.level;
        if (level == null) {
            pendingAutoWires.clear();
            return;
        }

        Iterator<PendingAutoWire> iterator = pendingAutoWires.iterator();
        while (iterator.hasNext()) {
            PendingAutoWire autoWire = iterator.next();
            if (autoWire.level != level) {
                iterator.remove();
                continue;
            }

            autoWire.remainingTicks--;
            if (autoWire.remainingTicks > 0) continue;

            BlockState currentState = level.getBlockState(autoWire.blockPos);
            if (currentState.getBlock() == autoWire.expectedBlock) {
                AutoWire.wire(autoWire.wireType, level, autoWire.blockPos, currentState, autoWire.expectedItem);
            }

            iterator.remove();
        }
    }

    private static class PendingPlacement {
        private final Level level;
        private final BlockPos blockPos;
        private final BlockState previousState;
        private final Block expectedBlock;
        private final Item expectedItem;
        private final WireType wireType;
        private int remainingTicks = PLACEMENT_CONFIRMATION_TICKS;

        private PendingPlacement(Level level, BlockPos blockPos, BlockState previousState, Block expectedBlock, Item expectedItem, WireType wireType) {
            this.level = level;
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

    private static class PendingAutoWire {
        private final Level level;
        private final BlockPos blockPos;
        private final Block expectedBlock;
        private final Item expectedItem;
        private final WireType wireType;
        private int remainingTicks = AUTOWIRE_DELAY_TICKS;

        private PendingAutoWire(Level level, BlockPos blockPos, Block expectedBlock, Item expectedItem, WireType wireType) {
            this.level = level;
            this.blockPos = blockPos;
            this.expectedBlock = expectedBlock;
            this.expectedItem = expectedItem;
            this.wireType = wireType;
        }
    }
}
