package org.main.redstoneutils.server.history;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.main.redstoneutils.server.config.RedstoneUtilsServerConfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;

/** Shared, per-player block and block-entity undo/redo history. */
public final class ChangeHistory {

    private static final int SET_BLOCK_FLAGS = Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS;
    private static final Map<MinecraftServer, Map<UUID, PlayerHistory>> HISTORIES = new WeakHashMap<>();
    private static boolean initialized;

    private ChangeHistory() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        ServerLifecycleEvents.SERVER_STOPPED.register(HISTORIES::remove);
    }

    public static Transaction begin(ServerPlayer player, Component description) {
        return new Transaction(player, description == null
                ? Component.translatable("history.redstoneutils.change")
                : description);
    }

    public static Result undo(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        PlayerHistory history = historiesFor(server).get(player.getUUID());
        if (history == null || history.undo.isEmpty()) {
            return Result.failure(Component.translatable("history.redstoneutils.undo.empty"));
        }

        ChangeSet changeSet = history.undo.peekFirst();
        Result restored = restore(server, changeSet.before());
        if (!restored.successful()) return restored;

        history.undo.removeFirst();
        history.redo.addFirst(changeSet);
        return Result.success(Component.translatable(
                "history.redstoneutils.undo.success",
                changeSet.description(),
                changeSet.before().size()
        ));
    }

    public static Result redo(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        PlayerHistory history = historiesFor(server).get(player.getUUID());
        if (history == null || history.redo.isEmpty()) {
            return Result.failure(Component.translatable("history.redstoneutils.redo.empty"));
        }

        ChangeSet changeSet = history.redo.peekFirst();
        Result restored = restore(server, changeSet.after());
        if (!restored.successful()) return restored;

        history.redo.removeFirst();
        history.undo.addFirst(changeSet);
        trim(history.undo);
        return Result.success(Component.translatable(
                "history.redstoneutils.redo.success",
                changeSet.description(),
                changeSet.after().size()
        ));
    }

    private static Map<UUID, PlayerHistory> historiesFor(MinecraftServer server) {
        if (server == null) return Map.of();
        return HISTORIES.computeIfAbsent(server, ignored -> new LinkedHashMap<>());
    }

    private static void remember(MinecraftServer server, UUID playerId, ChangeSet changeSet) {
        if (server == null) return;
        PlayerHistory history = historiesFor(server).computeIfAbsent(playerId, ignored -> new PlayerHistory());
        history.undo.addFirst(changeSet);
        history.redo.clear();
        trim(history.undo);
    }

    private static void trim(Deque<ChangeSet> history) {
        int maximum = RedstoneUtilsServerConfig.historySize();
        while (history.size() > maximum) history.removeLast();
    }

    private static Result restore(MinecraftServer server, List<SavedBlock> blocks) {
        if (server == null) {
            return Result.failure(Component.translatable("history.redstoneutils.dimension_missing"));
        }

        for (SavedBlock savedBlock : blocks) {
            ServerLevel level = server.getLevel(savedBlock.dimension());
            if (level == null) {
                return Result.failure(Component.translatable(
                        "history.redstoneutils.dimension_missing_named",
                        savedBlock.dimension().identifier().toString()
                ));
            }
            if (!level.isInWorldBounds(savedBlock.blockPos())) {
                return Result.failure(Component.translatable("history.redstoneutils.restore_failed"));
            }
        }

        List<SavedBlock> rollback = blocks.stream()
                .map(savedBlock -> capture(server.getLevel(savedBlock.dimension()), savedBlock.blockPos()))
                .toList();
        if (!apply(server, blocks)) {
            apply(server, rollback);
            return Result.failure(Component.translatable("history.redstoneutils.restore_failed"));
        }
        return Result.success(Component.empty());
    }

    private static boolean apply(MinecraftServer server, List<SavedBlock> blocks) {
        for (SavedBlock savedBlock : blocks) {
            ServerLevel level = server.getLevel(savedBlock.dimension());
            if (level == null || !level.setBlock(savedBlock.blockPos(), savedBlock.blockState(), SET_BLOCK_FLAGS)) {
                return false;
            }
        }

        for (SavedBlock savedBlock : blocks) {
            if (savedBlock.blockEntityTag() == null) continue;
            ServerLevel level = server.getLevel(savedBlock.dimension());
            BlockEntity blockEntity = BlockEntity.loadStatic(
                    savedBlock.blockPos(),
                    savedBlock.blockState(),
                    savedBlock.blockEntityTag().copy(),
                    level.registryAccess()
            );
            if (blockEntity == null) return false;
            level.setBlockEntity(blockEntity);
            blockEntity.setChanged();
        }

        for (SavedBlock savedBlock : blocks) {
            ServerLevel level = server.getLevel(savedBlock.dimension());
            BlockState state = level.getBlockState(savedBlock.blockPos());
            level.updateNeighborsAt(savedBlock.blockPos(), state.getBlock());
            level.sendBlockUpdated(savedBlock.blockPos(), state, state, Block.UPDATE_ALL);
        }
        return true;
    }

    private static SavedBlock capture(ServerLevel level, BlockPos blockPos) {
        BlockState blockState = level.getBlockState(blockPos);
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        CompoundTag tag = blockEntity == null ? null : blockEntity.saveWithFullMetadata(level.registryAccess());
        return new SavedBlock(level.dimension(), blockPos.immutable(), blockState, tag);
    }

    public static final class Transaction {
        private final ServerPlayer player;
        private final Component description;
        private final Map<BlockReference, SavedBlock> before = new LinkedHashMap<>();
        private boolean closed;

        private Transaction(ServerPlayer player, Component description) {
            this.player = Objects.requireNonNull(player, "player");
            this.description = description.copy();
        }

        public void capture(ServerLevel level, BlockPos blockPos) {
            if (closed || level == null || blockPos == null) return;
            BlockReference reference = new BlockReference(level.dimension(), blockPos.immutable());
            before.computeIfAbsent(reference, ignored -> ChangeHistory.capture(level, blockPos));
        }

        public boolean commit() {
            if (closed) return false;
            closed = true;

            MinecraftServer server = player.level().getServer();
            if (server == null || before.isEmpty()) return false;
            List<SavedBlock> previous = new ArrayList<>();
            List<SavedBlock> current = new ArrayList<>();
            for (Map.Entry<BlockReference, SavedBlock> entry : before.entrySet()) {
                ServerLevel level = server.getLevel(entry.getKey().dimension());
                if (level == null) continue;
                SavedBlock after = ChangeHistory.capture(level, entry.getKey().blockPos());
                if (!entry.getValue().sameContent(after)) {
                    previous.add(entry.getValue());
                    current.add(after);
                }
            }
            if (previous.isEmpty()) return false;

            remember(server, player.getUUID(), new ChangeSet(description.copy(), List.copyOf(previous), List.copyOf(current)));
            return true;
        }

        public void rollback() {
            if (closed) return;
            closed = true;
            restore(player.level().getServer(), List.copyOf(before.values()));
        }
    }

    public record Result(boolean successful, Component message) {
        public static Result success(Component message) {
            return new Result(true, message);
        }

        public static Result failure(Component message) {
            return new Result(false, message);
        }
    }

    private static final class PlayerHistory {
        private final Deque<ChangeSet> undo = new ArrayDeque<>();
        private final Deque<ChangeSet> redo = new ArrayDeque<>();
    }

    private record ChangeSet(Component description, List<SavedBlock> before, List<SavedBlock> after) {
    }

    private record BlockReference(ResourceKey<Level> dimension, BlockPos blockPos) {
    }

    private record SavedBlock(
            ResourceKey<Level> dimension,
            BlockPos blockPos,
            BlockState blockState,
            CompoundTag blockEntityTag
    ) {
        private boolean sameContent(SavedBlock other) {
            return other != null
                    && blockState.equals(other.blockState)
                    && Objects.equals(blockEntityTag, other.blockEntityTag);
        }
    }
}
