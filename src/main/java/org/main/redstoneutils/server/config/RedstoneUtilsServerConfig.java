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
                backup(path);
                data = new Data();
                RedstoneUtils.LOGGER.error("Could not read {}; preserved it as a .bak file", path, exception);
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
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
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
        data.maxTeleportRange = Math.clamp(data.maxTeleportRange, 10.0D, 100_000.0D);
        data.maxTargetRange = Math.clamp(data.maxTargetRange, 4.0D, 1_024.0D);
        data.maxContainerItems = Math.clamp(data.maxContainerItems, 0, 1_000_000);
        data.maxComparatorClockTicks = Math.clamp(data.maxComparatorClockTicks, 2, 600);
        data.maxHopperClockTicks = Math.clamp(data.maxHopperClockTicks, 7, 2_554);
        data.historySize = Math.clamp(data.historySize, 1, 100);
    }

    private static void backup(Path path) {
        try {
            Files.copy(path, path.resolveSibling(path.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            RedstoneUtils.LOGGER.error("Could not back up damaged server config {}", path, exception);
        }
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
