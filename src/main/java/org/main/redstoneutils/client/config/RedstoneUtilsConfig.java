package org.main.redstoneutils.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.main.redstoneutils.RedstoneUtils;
import org.main.redstoneutils.client.autowire.WireType;
import org.main.redstoneutils.client.ui.RedstoneMessages;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RedstoneUtilsConfig {

    public static final String GLOBAL_PROFILE = "global";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "redstoneutils.json";

    private static ConfigData data = new ConfigData();
    private static String activeProfile = GLOBAL_PROFILE;
    private static Path recoveryBackup;
    private static boolean loaded;

    private RedstoneUtilsConfig() {
    }

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;

        Path path = path();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                ConfigData loadedData = GSON.fromJson(reader, ConfigData.class);
                if (loadedData == null) throw new IOException("Empty client config");
                data = loadedData;
            } catch (IOException | RuntimeException exception) {
                recoveryBackup = backup(path);
                data = new ConfigData();
                RedstoneUtils.LOGGER.error("Could not read {}; defaults will be used", path, exception);
            }
        }

        migrateAndSanitize();
        save();
    }

    public static synchronized void save() {
        migrateAndSanitize();
        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                GSON.toJson(data, writer);
            }
            moveAtomically(temporary, path);
        } catch (IOException exception) {
            RedstoneUtils.LOGGER.error("Could not save RedstoneUtils client config", exception);
        }
    }

    public static synchronized Path consumeRecoveryBackup() {
        Path backup = recoveryBackup;
        recoveryBackup = null;
        return backup;
    }

    public static synchronized String activeProfile() {
        return activeProfile;
    }

    public static synchronized void activateProfile(String profileKey) {
        load();
        activeProfile = sanitizeProfileKey(profileKey);
        data.profiles.computeIfAbsent(activeProfile, ignored -> currentGlobalProfile().copy());
        sanitizeProfile(currentProfile());
        save();
    }

    public static synchronized void resetActiveProfile() {
        if (GLOBAL_PROFILE.equals(activeProfile)) {
            data.profiles.put(GLOBAL_PROFILE, new ProfileData());
        } else {
            data.profiles.put(activeProfile, currentGlobalProfile().copy());
        }
        save();
    }

    public static synchronized void resetToDefaults() {
        data = new ConfigData();
        activeProfile = GLOBAL_PROFILE;
        migrateAndSanitize();
        save();
    }

    public static boolean isHudOverlayVisible() { return currentProfile().hudOverlayVisible; }
    public static void setHudOverlayVisible(boolean value) { currentProfile().hudOverlayVisible = value; save(); }
    public static boolean isWirePreviewOverlayVisible() { return currentProfile().wirePreviewOverlayVisible; }
    public static void setWirePreviewOverlayVisible(boolean value) { currentProfile().wirePreviewOverlayVisible = value; save(); }
    public static boolean isSculkOverlayVisible() { return currentProfile().sculkOverlayVisible; }
    public static void setSculkOverlayVisible(boolean value) { currentProfile().sculkOverlayVisible = value; save(); }
    public static boolean isBudOverlayVisible() { return currentProfile().budOverlayVisible; }
    public static void setBudOverlayVisible(boolean value) { currentProfile().budOverlayVisible = value; save(); }
    public static WireType getActiveWireType() { return currentProfile().activeWireType; }
    public static void setActiveWireType(WireType value) { currentProfile().activeWireType = value == null ? WireType.NONE : value; save(); }

    public static int getBudTestRange() { return data.budTestRange; }
    public static void setBudTestRange(int value) { data.budTestRange = Math.clamp(value, 4, 64); save(); }
    public static RedstoneMessages.MessageTarget getMessageTarget() { return data.messageTarget; }
    public static void setMessageTarget(RedstoneMessages.MessageTarget value) { data.messageTarget = value == null ? RedstoneMessages.MessageTarget.POPUP : value; save(); }
    public static double getTeleportMaxRange() { return data.teleportMaxRange; }
    public static void setTeleportMaxRange(double value) { data.teleportMaxRange = Math.clamp(value, 10.0D, 1_000.0D); save(); }
    public static int getSculkSensorSearchDistance() { return data.sculkSensorSearchDistance; }
    public static void setSculkSensorSearchDistance(int value) { data.sculkSensorSearchDistance = Math.clamp(value, 16, 256); save(); }
    public static int getSculkRebuildIntervalTicks() { return data.sculkRebuildIntervalTicks; }
    public static void setSculkRebuildIntervalTicks(int value) { data.sculkRebuildIntervalTicks = Math.clamp(value, 1, 100); save(); }

    public static boolean isStatusHudVisible() { return data.statusHudVisible; }
    public static void setStatusHudVisible(boolean value) { data.statusHudVisible = value; save(); }
    public static HudAnchor getStatusHudAnchor() { return data.statusHudAnchor; }
    public static void setStatusHudAnchor(HudAnchor value) { data.statusHudAnchor = value == null ? HudAnchor.TOP_RIGHT : value; save(); }
    public static PopupAnchor getPopupAnchor() { return data.popupAnchor; }
    public static void setPopupAnchor(PopupAnchor value) { data.popupAnchor = value == null ? PopupAnchor.TOP_LEFT : value; save(); }
    public static int getPopupDurationMillis() { return data.popupDurationMillis; }
    public static void setPopupDurationMillis(int value) { data.popupDurationMillis = Math.clamp(value, 1_000, 15_000); save(); }
    public static float getOverlayOpacity() { return data.overlayOpacity; }
    public static void setOverlayOpacity(float value) { data.overlayOpacity = Math.clamp(value, 0.1F, 1.0F); save(); }
    public static float getOverlayLineWidth() { return data.overlayLineWidth; }
    public static void setOverlayLineWidth(float value) { data.overlayLineWidth = Math.clamp(value, 1.0F, 8.0F); save(); }
    public static boolean renderOverlaysThroughWalls() { return data.overlayThroughWalls; }
    public static void setOverlayThroughWalls(boolean value) { data.overlayThroughWalls = value; save(); }
    public static int getOverlayMaxDistance() { return data.overlayMaxDistance; }
    public static void setOverlayMaxDistance(int value) { data.overlayMaxDistance = Math.clamp(value, 8, 256); save(); }
    public static ColorPalette getColorPalette() { return data.colorPalette; }
    public static void setColorPalette(ColorPalette value) {
        data.colorPalette = value == null ? ColorPalette.DEFAULT : value;
        data.customWireColor = null;
        data.customBudRiskColor = null;
        data.customBudSourceColor = null;
        data.customSculkColor = null;
        save();
    }

    public static int wirePreviewColor() { return data.customWireColor == null ? palette().wire : data.customWireColor; }
    public static int budRiskColor() { return data.customBudRiskColor == null ? palette().risk : data.customBudRiskColor; }
    public static int budSourceColor() { return data.customBudSourceColor == null ? palette().source : data.customBudSourceColor; }
    public static int sculkColor() { return data.customSculkColor == null ? palette().sculk : data.customSculkColor; }
    public static void setWirePreviewColor(int value) { data.customWireColor = opaque(value); save(); }
    public static void setBudRiskColor(int value) { data.customBudRiskColor = opaque(value); save(); }
    public static void setBudSourceColor(int value) { data.customBudSourceColor = opaque(value); save(); }
    public static void setSculkColor(int value) { data.customSculkColor = opaque(value); save(); }
    public static void resetWirePreviewColor() { data.customWireColor = null; save(); }
    public static void resetBudRiskColor() { data.customBudRiskColor = null; save(); }
    public static void resetBudSourceColor() { data.customBudSourceColor = null; save(); }
    public static void resetSculkColor() { data.customSculkColor = null; save(); }

    private static int opaque(int value) {
        return 0xFF000000 | value & 0x00FFFFFF;
    }

    private static PaletteValues palette() {
        return switch (getColorPalette()) {
            case DEFAULT -> new PaletteValues(0xD933DD55, 0xE6FF3838, 0xE6FFD43B, 0xD94EC9FF);
            case DEUTERANOPIA -> new PaletteValues(0xD956B4E9, 0xE6D55E00, 0xE6F0E442, 0xD90079A7);
            case PROTANOPIA -> new PaletteValues(0xD900A6D6, 0xE6CC79A7, 0xE6E69F00, 0xD956B4E9);
            case TRITANOPIA -> new PaletteValues(0xD9009E73, 0xE6D55E00, 0xE6CC79A7, 0xD9007F5F);
            case HIGH_CONTRAST -> new PaletteValues(0xFFFFFFFF, 0xFFFF4B4B, 0xFFFFFF00, 0xFF00FFFF);
        };
    }

    private static synchronized ProfileData currentProfile() {
        load();
        return data.profiles.computeIfAbsent(activeProfile, ignored -> currentGlobalProfile().copy());
    }

    private static ProfileData currentGlobalProfile() {
        return data.profiles.computeIfAbsent(GLOBAL_PROFILE, ignored -> new ProfileData());
    }

    private static void migrateAndSanitize() {
        if (data == null) data = new ConfigData();
        if (data.profiles == null) data.profiles = new LinkedHashMap<>();
        if (data.profiles.isEmpty()) {
            ProfileData legacy = new ProfileData();
            if (data.hudOverlayVisible != null) legacy.hudOverlayVisible = data.hudOverlayVisible;
            if (data.wirePreviewOverlayVisible != null) legacy.wirePreviewOverlayVisible = data.wirePreviewOverlayVisible;
            if (data.sculkOverlayVisible != null) legacy.sculkOverlayVisible = data.sculkOverlayVisible;
            if (data.budOverlayVisible != null) legacy.budOverlayVisible = data.budOverlayVisible;
            if (data.activeWireType != null) legacy.activeWireType = data.activeWireType;
            data.profiles.put(GLOBAL_PROFILE, legacy);
        }
        data.hudOverlayVisible = null;
        data.wirePreviewOverlayVisible = null;
        data.sculkOverlayVisible = null;
        data.budOverlayVisible = null;
        data.activeWireType = null;
        data.profiles.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        data.profiles.values().forEach(RedstoneUtilsConfig::sanitizeProfile);
        currentGlobalProfile();

        if (data.messageTarget == null) data.messageTarget = RedstoneMessages.MessageTarget.POPUP;
        if (data.statusHudAnchor == null) data.statusHudAnchor = HudAnchor.TOP_RIGHT;
        if (data.popupAnchor == null) data.popupAnchor = PopupAnchor.TOP_LEFT;
        if (data.colorPalette == null) data.colorPalette = ColorPalette.DEFAULT;
        data.teleportMaxRange = Math.clamp(data.teleportMaxRange, 10.0D, 1_000.0D);
        data.sculkSensorSearchDistance = Math.clamp(data.sculkSensorSearchDistance, 16, 256);
        data.sculkRebuildIntervalTicks = Math.clamp(data.sculkRebuildIntervalTicks, 1, 100);
        data.budTestRange = Math.clamp(data.budTestRange, 4, 64);
        data.popupDurationMillis = Math.clamp(data.popupDurationMillis, 1_000, 15_000);
        data.overlayOpacity = Math.clamp(data.overlayOpacity, 0.1F, 1.0F);
        data.overlayLineWidth = Math.clamp(data.overlayLineWidth, 1.0F, 8.0F);
        data.overlayMaxDistance = Math.clamp(data.overlayMaxDistance, 8, 256);
    }

    private static void sanitizeProfile(ProfileData profile) {
        if (profile.activeWireType == null) profile.activeWireType = WireType.NONE;
    }

    private static String sanitizeProfileKey(String value) {
        String key = value == null ? "" : value.strip();
        return key.isEmpty() ? GLOBAL_PROFILE : key.substring(0, Math.min(key.length(), 256));
    }

    private static Path backup(Path path) {
        Path backup = path.resolveSibling(path.getFileName() + ".bak");
        try {
            Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
            return backup;
        } catch (IOException exception) {
            RedstoneUtils.LOGGER.error("Could not back up damaged client config {}", path, exception);
            return null;
        }
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public enum HudAnchor { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }
    public enum PopupAnchor { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }
    public enum ColorPalette { DEFAULT, DEUTERANOPIA, PROTANOPIA, TRITANOPIA, HIGH_CONTRAST }

    private record PaletteValues(int wire, int risk, int source, int sculk) {
    }

    private static final class ProfileData {
        private boolean hudOverlayVisible = true;
        private boolean wirePreviewOverlayVisible = true;
        private boolean sculkOverlayVisible;
        private boolean budOverlayVisible = true;
        private WireType activeWireType = WireType.NONE;

        private ProfileData copy() {
            ProfileData copy = new ProfileData();
            copy.hudOverlayVisible = hudOverlayVisible;
            copy.wirePreviewOverlayVisible = wirePreviewOverlayVisible;
            copy.sculkOverlayVisible = sculkOverlayVisible;
            copy.budOverlayVisible = budOverlayVisible;
            copy.activeWireType = activeWireType;
            return copy;
        }
    }

    private static final class ConfigData {
        private Map<String, ProfileData> profiles = new LinkedHashMap<>();
        private RedstoneMessages.MessageTarget messageTarget = RedstoneMessages.MessageTarget.POPUP;
        private double teleportMaxRange = 100.0D;
        private int sculkSensorSearchDistance = 96;
        private int sculkRebuildIntervalTicks = 5;
        private int budTestRange = 8;
        private boolean statusHudVisible = true;
        private HudAnchor statusHudAnchor = HudAnchor.TOP_RIGHT;
        private PopupAnchor popupAnchor = PopupAnchor.TOP_LEFT;
        private int popupDurationMillis = 3_000;
        private float overlayOpacity = 0.85F;
        private float overlayLineWidth = 2.0F;
        private boolean overlayThroughWalls = true;
        private int overlayMaxDistance = 128;
        private ColorPalette colorPalette = ColorPalette.DEFAULT;
        private Integer customWireColor;
        private Integer customBudRiskColor;
        private Integer customBudSourceColor;
        private Integer customSculkColor;

        // Read-only migration fields for pre-profile versions; Gson omits them after migration.
        private Boolean hudOverlayVisible;
        private Boolean wirePreviewOverlayVisible;
        private Boolean sculkOverlayVisible;
        private Boolean budOverlayVisible;
        private WireType activeWireType;
    }
}
