package org.main.redstoneutils.client.macro;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import org.main.redstoneutils.RedstoneUtils;
import org.main.redstoneutils.client.config.RedstoneUtilsConfig;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class MacroStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "redstoneutils_macros.json";
    private static final String EXPORT_FILE_NAME = "redstoneutils_macros_export.json";

    private static MacroData data = new MacroData();
    private static String activeProfile = RedstoneUtilsConfig.GLOBAL_PROFILE;
    private static Path recoveryBackup;
    private static boolean loaded;
    private static boolean skipPreviousBackup;

    private MacroStore() {
    }

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        Path path = path();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                MacroData loadedData = GSON.fromJson(reader, MacroData.class);
                if (loadedData == null) throw new IOException("Empty macro file");
                data = loadedData;
            } catch (IOException | RuntimeException exception) {
                recoveryBackup = preserveDamaged(path);
                data = loadLastValidBackup(path);
                skipPreviousBackup = true;
                RedstoneUtils.LOGGER.error("Could not read {}; the last valid macro backup or an empty store will be used", path, exception);
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
            preservePreviousValid(path);
            moveAtomically(temporary, path);
            skipPreviousBackup = false;
        } catch (IOException | RuntimeException exception) {
            RedstoneUtils.LOGGER.error("Could not save RedstoneUtils macros", exception);
        }
    }

    public static synchronized Path consumeRecoveryBackup() {
        Path backup = recoveryBackup;
        recoveryBackup = null;
        return backup;
    }

    public static synchronized void activateProfile(String profileKey) {
        load();
        activeProfile = RedstoneUtilsConfig.normalizeProfileKey(profileKey);
        data.profiles.computeIfAbsent(activeProfile, ignored -> data.profiles
                .getOrDefault(RedstoneUtilsConfig.GLOBAL_PROFILE, List.of())
                .stream().map(Macro::copy).collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
        save();
    }

    public static synchronized String activeProfile() {
        return activeProfile;
    }

    public static synchronized List<Macro> macros() {
        load();
        return sortedMacros().stream().map(Macro::copy).toList();
    }

    public static synchronized List<Macro> macros(MacroType type) {
        return macros().stream().filter(macro -> macro.type() == type).toList();
    }

    public static synchronized Optional<Macro> get(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return activeMacros().stream().filter(macro -> id.equals(macro.id())).findFirst().map(Macro::copy);
    }

    public static synchronized void upsert(Macro macro) {
        if (!isValidBeforeSanitize(macro)) return;
        Macro sanitized = macro.copy();
        sanitized.sanitize();
        if (!isValidAfterSanitize(sanitized)) return;
        if (sanitized.isCommandAlias() && aliasExists(sanitized.alias(), sanitized.id())) return;
        if (sanitized.isKeybind() && MacroKeys.isBound(sanitized.keyCode())
                && bindingExists(sanitized.keyCode(), sanitized.mouseButton(), sanitized.modifiers(), sanitized.id())) return;
        List<Macro> macros = activeMacros();
        for (int index = 0; index < macros.size(); index++) {
            if (macros.get(index).id().equals(sanitized.id())) {
                macros.set(index, sanitized);
                save();
                return;
            }
        }
        macros.add(sanitized);
        save();
    }

    public static synchronized Optional<Macro> duplicate(String id) {
        Optional<Macro> source = get(id);
        if (source.isEmpty()) return Optional.empty();
        Macro macro = source.get();
        String alias = macro.isCommandAlias() ? uniqueCopyAlias(macro) : macro.alias();
        int keyCode = macro.isKeybind() ? Macro.UNBOUND_KEY : macro.keyCode();
        boolean enabled = macro.isCommandAlias() && macro.enabled();
        Macro duplicate = new Macro(
                UUID.randomUUID().toString(), macro.type(), Component.translatable("macros.redstoneutils.copy_name", macro.name()).getString(), macro.command(), keyCode, alias,
                macro.mouseButton(), macro.modifiers(), macro.trigger(), enabled, macro.category()
        );
        upsert(duplicate);
        return Optional.of(duplicate.copy());
    }

    private static String uniqueCopyAlias(Macro macro) {
        String original = MacroCommandText.normalizeAlias(macro.alias());
        String stem = original.isBlank() ? "macro" : original;
        String copySuffix = "_copy";
        stem = stem.substring(0, Math.min(stem.length(), 64 - copySuffix.length())) + copySuffix;

        for (int attempt = 1; ; attempt++) {
            String numberSuffix = attempt == 1 ? "" : Integer.toString(attempt);
            String candidate = stem.substring(0, Math.min(stem.length(), 64 - numberSuffix.length())) + numberSuffix;
            if (!aliasExists(candidate, null)
                    && !CommandCommand.isReservedAlias(candidate)
                    && !candidate.equals(MacroCommandText.normalizeAlias(macro.command()))) {
                return candidate;
            }
        }
    }

    public static synchronized void setEnabled(String id, boolean enabled) {
        get(id).ifPresent(macro -> upsert(new Macro(
                macro.id(), macro.type(), macro.name(), macro.command(), macro.keyCode(), macro.alias(),
                macro.mouseButton(), macro.modifiers(), macro.trigger(), enabled, macro.category()
        )));
    }

    public static synchronized void delete(String id) {
        if (id != null && activeMacros().removeIf(macro -> id.equals(macro.id()))) save();
    }

    public static synchronized Optional<Macro> findCommandAlias(String alias) {
        String normalized = MacroCommandText.normalizeAlias(alias);
        if (normalized.isBlank()) return Optional.empty();
        return activeMacros().stream()
                .filter(Macro::enabled)
                .filter(Macro::isCommandAlias)
                .filter(macro -> normalized.equals(macro.alias()))
                .findFirst().map(Macro::copy);
    }

    public static synchronized boolean aliasExists(String alias, String excludingId) {
        String normalized = MacroCommandText.normalizeAlias(alias);
        if (normalized.isBlank()) return false;
        return activeMacros().stream()
                .filter(Macro::isCommandAlias)
                .filter(macro -> !macro.id().equals(excludingId))
                .anyMatch(macro -> normalized.equals(macro.alias()));
    }

    public static synchronized boolean keyExists(int keyCode, String excludingId) {
        return bindingExists(keyCode, false, 0, excludingId);
    }

    public static synchronized boolean bindingExists(int code, boolean mouseButton, int modifiers, String excludingId) {
        if (!MacroKeys.isBound(code)) return false;
        return activeMacros().stream()
                .filter(Macro::isKeybind)
                .filter(macro -> !macro.id().equals(excludingId))
                .anyMatch(macro -> macro.keyCode() == code
                        && macro.mouseButton() == mouseButton
                        && macro.modifiers() == (modifiers & MacroKeys.ALL_MODIFIERS));
    }

    public static synchronized Path defaultExportPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(EXPORT_FILE_NAME);
    }

    public static synchronized int exportActiveProfile(Path exportPath) throws IOException {
        Path target = exportPath == null ? defaultExportPath() : exportPath;
        Files.createDirectories(target.toAbsolutePath().getParent());
        try (Writer writer = Files.newBufferedWriter(target)) {
            GSON.toJson(macros(), writer);
        }
        return activeMacros().size();
    }

    public static synchronized int importIntoActiveProfile(Path importPath) throws IOException {
        Path source = importPath == null ? defaultExportPath() : importPath;
        Macro[] imported;
        try (Reader reader = Files.newBufferedReader(source)) {
            imported = GSON.fromJson(reader, Macro[].class);
        } catch (RuntimeException exception) {
            throw new IOException("Invalid macro import", exception);
        }
        if (imported == null) return 0;
        int count = 0;
        for (Macro macro : imported) {
            if (macro != null && (macro.type() == null || macro.type() == MacroType.KEYBIND) && !MacroKeys.isBound(macro.keyCode())) continue;
            if (!isValidBeforeSanitize(macro)) continue;
            macro.sanitize();
            if (!isValidAfterSanitize(macro)) continue;
            Macro unique = new Macro(
                    UUID.randomUUID().toString(), macro.type(), macro.name(), macro.command(), macro.keyCode(), macro.alias(),
                    macro.mouseButton(), macro.modifiers(), macro.trigger(), macro.enabled(), macro.category()
            );
            if (unique.isCommandAlias() && aliasExists(unique.alias(), null)) continue;
            if (unique.isKeybind() && bindingExists(unique.keyCode(), unique.mouseButton(), unique.modifiers(), null)) continue;
            activeMacros().add(unique);
            count++;
        }
        if (count > 0) save();
        return count;
    }

    private static List<Macro> sortedMacros() {
        return activeMacros().stream()
                .sorted(Comparator
                        .comparing((Macro macro) -> macro.category().toLowerCase(Locale.ROOT))
                        .thenComparing(macro -> macro.type().ordinal())
                        .thenComparing(macro -> macro.name().toLowerCase(Locale.ROOT))
                        .thenComparing(Macro::id))
                .toList();
    }

    private static List<Macro> activeMacros() {
        load();
        return data.profiles.computeIfAbsent(activeProfile, ignored -> new ArrayList<>());
    }

    private static void migrateAndSanitize() {
        if (data == null) data = new MacroData();
        if (data.profiles == null) data.profiles = new LinkedHashMap<>();
        if (data.macros != null && !data.macros.isEmpty() && data.profiles.isEmpty()) {
            data.profiles.put(RedstoneUtilsConfig.GLOBAL_PROFILE, new ArrayList<>(data.macros));
        }
        data.macros = null;
        data.profiles.computeIfAbsent(RedstoneUtilsConfig.GLOBAL_PROFILE, ignored -> new ArrayList<>());
        data.profiles.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        for (List<Macro> macros : data.profiles.values()) {
            List<Macro> sanitized = new ArrayList<>();
            java.util.Set<String> ids = new java.util.HashSet<>();
            java.util.Set<String> aliases = new java.util.HashSet<>();
            java.util.Set<String> bindings = new java.util.HashSet<>();
            for (Macro macro : macros) {
                if (!isValidBeforeSanitize(macro)) continue;
                macro.sanitize();
                if (!isValidAfterSanitize(macro)) continue;

                Macro clean = macro.copy();
                if (!ids.add(clean.id())) {
                    clean = new Macro(
                            UUID.randomUUID().toString(), clean.type(), clean.name(), clean.command(), clean.keyCode(), clean.alias(),
                            clean.mouseButton(), clean.modifiers(), clean.trigger(), clean.enabled(), clean.category()
                    );
                    ids.add(clean.id());
                }
                if (clean.isCommandAlias() && !aliases.add(clean.alias())) continue;
                if (clean.isKeybind() && !bindings.add(bindingKey(clean))) continue;
                sanitized.add(clean);
            }
            macros.clear();
            macros.addAll(sanitized);
        }
    }

    private static boolean isValidBeforeSanitize(Macro macro) {
        if (macro == null || MacroCommandText.normalizeCommand(macro.command()).isBlank()) return false;
        MacroType type = macro.type() == null ? MacroType.KEYBIND : macro.type();
        if (type == MacroType.KEYBIND) return MacroKeys.isBound(macro.keyCode()) || !macro.enabled();

        String alias = macro.alias();
        String normalizedAlias = MacroCommandText.normalizeAlias(alias);
        return MacroCommandText.isValidAliasInput(alias)
                && !CommandCommand.isReservedAlias(normalizedAlias)
                && !MacroCommandText.normalizeAlias(macro.command()).equals(normalizedAlias);
    }

    private static boolean isValidAfterSanitize(Macro macro) {
        if (macro.command().isBlank()) return false;
        if (macro.isKeybind()) return MacroKeys.isBound(macro.keyCode()) || !macro.enabled();
        return MacroCommandText.isValidAliasInput(macro.alias())
                && !CommandCommand.isReservedAlias(macro.alias())
                && !MacroCommandText.normalizeAlias(macro.command()).equals(macro.alias());
    }

    private static String bindingKey(Macro macro) {
        return macro.keyCode() + ":" + macro.mouseButton() + ":" + macro.modifiers();
    }

    private static MacroData loadLastValidBackup(Path path) {
        Path backup = validBackup(path);
        if (!Files.exists(backup)) return new MacroData();
        try (Reader reader = Files.newBufferedReader(backup)) {
            MacroData restored = GSON.fromJson(reader, MacroData.class);
            return restored == null ? new MacroData() : restored;
        } catch (IOException | RuntimeException exception) {
            RedstoneUtils.LOGGER.error("Could not read last valid macro backup {}", backup, exception);
            return new MacroData();
        }
    }

    private static Path preserveDamaged(Path path) {
        Path backup = path.resolveSibling(path.getFileName() + ".corrupt-" + System.currentTimeMillis() + ".bak");
        try {
            Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
            return backup;
        } catch (IOException exception) {
            RedstoneUtils.LOGGER.error("Could not back up damaged macro file {}", path, exception);
            return null;
        }
    }

    private static void preservePreviousValid(Path path) {
        if (skipPreviousBackup || !Files.exists(path)) return;
        try {
            Files.copy(path, validBackup(path), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            RedstoneUtils.LOGGER.error("Could not update last valid macro backup", exception);
        }
    }

    private static Path validBackup(Path path) {
        return path.resolveSibling(path.getFileName() + ".bak");
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

    private static final class MacroData {
        private Map<String, List<Macro>> profiles = new LinkedHashMap<>();
        private List<Macro> macros;
    }
}
