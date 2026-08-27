package org.main.redstoneutils.server.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.main.redstoneutils.RedstoneUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Server-owned limits and access rules for every world-changing tool. */
public final class RedstoneUtilsServerConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "redstoneutils-server.json";

    private static Data data = new Data();
    private static boolean loaded;
    private static boolean skipPreviousBackup;

    private RedstoneUtilsServerConfig() {
    }

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;

        Path path = path();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                Data loadedData = GSON.fromJson(reader, Data.class);
                if (loadedData == null) throw new IOException("Empty server config");
                data = loadedData;
            } catch (IOException | RuntimeException exception) {
                Path damaged = preserveDamaged(path);
                data = loadLastValidBackup(path);
                skipPreviousBackup = true;
                RedstoneUtils.LOGGER.error("Could not read {}; preserved it as {} and loaded the last valid backup or defaults", path, damaged, exception);
            }
        }

        sanitize();
        save();
    }

    public static synchronized void save() {
        sanitize();
        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                GSON.toJson(data, writer);
            }
            preservePreviousValid(path);
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            skipPreviousBackup = false;
        } catch (IOException | RuntimeException exception) {
            RedstoneUtils.LOGGER.error("Could not save RedstoneUtils server config", exception);
        }
    }

    public static boolean canUse(Tool tool, CommandSourceStack source) {
        load();
        if (source.getEntity() == null) return true;
        return rule(tool).allows(source);
    }

    public static double maxTeleportRange() {
        load();
        return data.maxTeleportRange;
    }

    public static double maxTargetRange() {
        load();
        return data.maxTargetRange;
    }

    public static int maxContainerItems() {
        load();
        return data.maxContainerItems;
    }

    public static int maxComparatorClockTicks() {
        load();
        return data.maxComparatorClockTicks;
    }

    public static int maxHopperClockTicks() {
        load();
        return data.maxHopperClockTicks;
    }

    public static int historySize() {
        load();
        return data.historySize;
    }

    private static AccessRule rule(Tool tool) {
        AccessRule rule = switch (tool) {
            case TELEPORT -> data.teleportPermission;
            case AUTOWIRE -> data.autoWirePermission;
            case SIGNAL_TOOLS -> data.signalToolsPermission;
            case BUILDER -> data.builderPermission;
            case HISTORY -> data.historyPermission;
        };
        return rule == null ? AccessRule.OP_OR_CREATIVE : rule;
    }

    private static void sanitize() {
        if (data == null) data = new Data();
        if (data.teleportPermission == null) data.teleportPermission = AccessRule.OP_OR_CREATIVE;
        if (data.autoWirePermission == null) data.autoWirePermission = AccessRule.OP_OR_CREATIVE;
        if (data.signalToolsPermission == null) data.signalToolsPermission = AccessRule.OP_OR_CREATIVE;
        if (data.builderPermission == null) data.builderPermission = AccessRule.OP_OR_CREATIVE;
        if (data.historyPermission == null) data.historyPermission = AccessRule.OP_OR_CREATIVE;
        data.maxTeleportRange = Math.clamp(finite(data.maxTeleportRange, 1_000.0D), 10.0D, 100_000.0D);
        data.maxTargetRange = Math.clamp(finite(data.maxTargetRange, 128.0D), 4.0D, 1_024.0D);
        data.maxContainerItems = Math.clamp(data.maxContainerItems, 0, 1_000_000);
        data.maxComparatorClockTicks = Math.clamp(data.maxComparatorClockTicks, 2, 600);
        data.maxHopperClockTicks = Math.clamp(data.maxHopperClockTicks, 7, 2_554);
        data.historySize = Math.clamp(data.historySize, 1, 100);
    }

    private static double finite(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    private static Data loadLastValidBackup(Path path) {
        Path backup = validBackup(path);
        if (!Files.exists(backup)) return new Data();
        try (Reader reader = Files.newBufferedReader(backup)) {
            Data restored = GSON.fromJson(reader, Data.class);
            return restored == null ? new Data() : restored;
        } catch (IOException | RuntimeException exception) {
            RedstoneUtils.LOGGER.error("Could not read last valid server config backup {}", backup, exception);
            return new Data();
        }
    }

    private static Path preserveDamaged(Path path) {
        Path backup = path.resolveSibling(path.getFileName() + ".corrupt-" + System.currentTimeMillis() + ".bak");
        try {
            Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
            return backup;
        } catch (IOException exception) {
            RedstoneUtils.LOGGER.error("Could not back up damaged server config {}", path, exception);
            return path;
        }
    }

    private static void preservePreviousValid(Path path) {
        if (skipPreviousBackup || !Files.exists(path)) return;
        try {
            Files.copy(path, validBackup(path), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            RedstoneUtils.LOGGER.error("Could not update last valid server config backup", exception);
        }
    }

    private static Path validBackup(Path path) {
        return path.resolveSibling(path.getFileName() + ".bak");
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public enum Tool {
        TELEPORT,
        AUTOWIRE,
        SIGNAL_TOOLS,
        BUILDER,
        HISTORY
    }

    public enum AccessRule {
        OP_OR_CREATIVE,
        OP_ONLY,
        CREATIVE_ONLY,
        EVERYONE,
        DISABLED;

        private boolean allows(CommandSourceStack source) {
            boolean operator = Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source);
            ServerPlayer player = source.getPlayer();
            boolean creative = player != null && player.getAbilities().instabuild;
            return switch (this) {
                case OP_OR_CREATIVE -> operator || creative;
                case OP_ONLY -> operator;
                case CREATIVE_ONLY -> creative;
                case EVERYONE -> true;
                case DISABLED -> false;
            };
        }
    }

    private static final class Data {
        private AccessRule teleportPermission = AccessRule.OP_OR_CREATIVE;
        private AccessRule autoWirePermission = AccessRule.OP_OR_CREATIVE;
        private AccessRule signalToolsPermission = AccessRule.OP_OR_CREATIVE;
        private AccessRule builderPermission = AccessRule.OP_OR_CREATIVE;
        private AccessRule historyPermission = AccessRule.OP_OR_CREATIVE;
        private double maxTeleportRange = 1_000.0D;
        private double maxTargetRange = 128.0D;
        private int maxContainerItems = 100_000;
        private int maxComparatorClockTicks = 600;
        private int maxHopperClockTicks = 2_554;
        private int historySize = 20;
    }
}
