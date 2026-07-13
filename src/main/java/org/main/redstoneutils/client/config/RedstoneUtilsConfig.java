package org.main.redstoneutils.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.main.redstoneutils.client.autowire.WireType;
import org.main.redstoneutils.client.ui.RedstoneMessages;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RedstoneUtilsConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "redstoneutils.json";

    private static ConfigData data = new ConfigData();
    private static boolean loaded;

    private RedstoneUtilsConfig() {
    }

    public static void load() {
        if (loaded) return;
        loaded = true;

        Path path = path();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                ConfigData loadedData = GSON.fromJson(reader, ConfigData.class);
                if (loadedData != null) {
                    data = loadedData;
                }
            } catch (IOException | RuntimeException ignored) {
                data = new ConfigData();
            }
        }

        sanitize();
        save();
    }

    public static void save() {
        sanitize();

        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException ignored) {
        }
    }

    public static void resetToDefaults() {
        data = new ConfigData();
        save();
    }

    public static boolean isHudOverlayVisible() {
        return data.hudOverlayVisible;
    }

    public static void setHudOverlayVisible(boolean visible) {
        data.hudOverlayVisible = visible;
        save();
    }

    public static boolean isWirePreviewOverlayVisible() {
        return data.wirePreviewOverlayVisible;
    }

    public static void setWirePreviewOverlayVisible(boolean visible) {
        data.wirePreviewOverlayVisible = visible;
        save();
    }

    public static boolean isSculkOverlayVisible() {
        return data.sculkOverlayVisible;
    }

    public static void setSculkOverlayVisible(boolean visible) {
        data.sculkOverlayVisible = visible;
        save();
    }

    public static WireType getActiveWireType() {
        return data.activeWireType;
    }

    public static void setActiveWireType(WireType wireType) {
        data.activeWireType = wireType == null ? WireType.NONE : wireType;
        save();
    }

    public static RedstoneMessages.MessageTarget getMessageTarget() {
        return data.messageTarget;
    }

    public static void setMessageTarget(RedstoneMessages.MessageTarget target) {
        data.messageTarget = target == null ? RedstoneMessages.MessageTarget.POPUP : target;
        save();
    }

    public static double getTeleportMaxRange() {
        return data.teleportMaxRange;
    }

    public static void setTeleportMaxRange(double range) {
        data.teleportMaxRange = Math.clamp(range, 10.0D, 1000.0D);
        save();
    }

    public static int getSculkSensorSearchDistance() {
        return data.sculkSensorSearchDistance;
    }

    public static void setSculkSensorSearchDistance(int distance) {
        data.sculkSensorSearchDistance = Math.clamp(distance, 16, 256);
        save();
    }

    public static int getSculkRebuildIntervalTicks() {
        return data.sculkRebuildIntervalTicks;
    }

    public static void setSculkRebuildIntervalTicks(int ticks) {
        data.sculkRebuildIntervalTicks = Math.clamp(ticks, 1, 100);
        save();
    }

    private static void sanitize() {
        if (data.activeWireType == null) data.activeWireType = WireType.NONE;
        if (data.messageTarget == null) data.messageTarget = RedstoneMessages.MessageTarget.POPUP;
        data.teleportMaxRange = Math.clamp(data.teleportMaxRange, 10.0D, 1000.0D);
        data.sculkSensorSearchDistance = Math.clamp(data.sculkSensorSearchDistance, 16, 256);
        data.sculkRebuildIntervalTicks = Math.clamp(data.sculkRebuildIntervalTicks, 1, 100);
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    private static final class ConfigData {
        private boolean hudOverlayVisible = true;
        private boolean wirePreviewOverlayVisible = true;
        private boolean sculkOverlayVisible = false;
        private WireType activeWireType = WireType.NONE;
        private RedstoneMessages.MessageTarget messageTarget = RedstoneMessages.MessageTarget.POPUP;
        private double teleportMaxRange = 100.0D;
        private int sculkSensorSearchDistance = 96;
        private int sculkRebuildIntervalTicks = 5;
    }
}
