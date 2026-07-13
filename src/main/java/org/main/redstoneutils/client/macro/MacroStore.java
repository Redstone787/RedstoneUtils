package org.main.redstoneutils.client.macro;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class MacroStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "redstoneutils_macros.json";

    private static MacroData data = new MacroData();
    private static boolean loaded;

    private MacroStore() {
    }

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;

        Path path = path();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                MacroData loadedData = GSON.fromJson(reader, MacroData.class);
                if (loadedData != null) {
                    data = loadedData;
                }
            } catch (IOException | RuntimeException ignored) {
                data = new MacroData();
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
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException ignored) {
        }
    }

    public static synchronized List<Macro> macros() {
        load();
        return sortedMacros().stream().map(Macro::copy).toList();
    }

    public static synchronized List<Macro> macros(MacroType type) {
        load();
        return sortedMacros().stream()
                .filter(macro -> macro.type() == type)
                .map(Macro::copy)
                .toList();
    }

    public static synchronized Optional<Macro> get(String id) {
        load();
        if (id == null || id.isBlank()) return Optional.empty();

        return data.macros.stream()
                .filter(macro -> id.equals(macro.id()))
                .findFirst()
                .map(Macro::copy);
    }

    public static synchronized void upsert(Macro macro) {
        load();
        if (macro == null) return;

        Macro sanitized = macro.copy();
        sanitized.sanitize();

        for (int index = 0; index < data.macros.size(); index++) {
            if (data.macros.get(index).id().equals(sanitized.id())) {
                data.macros.set(index, sanitized);
                save();
                return;
            }
        }

        data.macros.add(sanitized);
        save();
    }

    public static synchronized void delete(String id) {
        load();
        if (id == null || id.isBlank()) return;

        if (data.macros.removeIf(macro -> id.equals(macro.id()))) {
            save();
        }
    }

    public static synchronized Optional<Macro> findCommandAlias(String alias) {
        load();
        String normalized = MacroCommandText.normalizeAlias(alias);
        if (normalized.isBlank()) return Optional.empty();

        return data.macros.stream()
                .filter(Macro::isCommandAlias)
                .filter(macro -> normalized.equals(macro.alias()))
                .findFirst()
                .map(Macro::copy);
    }

    public static synchronized boolean aliasExists(String alias, String excludingId) {
        load();
        String normalized = MacroCommandText.normalizeAlias(alias);
        if (normalized.isBlank()) return false;

        return data.macros.stream()
                .filter(Macro::isCommandAlias)
                .filter(macro -> !macro.id().equals(excludingId))
                .anyMatch(macro -> normalized.equals(macro.alias()));
    }

    public static synchronized boolean keyExists(int keyCode, String excludingId) {
        load();
        if (!MacroKeys.isBound(keyCode)) return false;

        return data.macros.stream()
                .filter(Macro::isKeybind)
                .filter(macro -> !macro.id().equals(excludingId))
                .anyMatch(macro -> macro.keyCode() == keyCode);
    }

    private static List<Macro> sortedMacros() {
        return data.macros.stream()
                .sorted(Comparator
                        .comparing((Macro macro) -> macro.type().ordinal())
                        .thenComparing(macro -> macro.name().toLowerCase(Locale.ROOT))
                        .thenComparing(Macro::id))
                .toList();
    }

    private static void sanitize() {
        if (data == null) data = new MacroData();
        if (data.macros == null) data.macros = new ArrayList<>();

        List<Macro> sanitized = new ArrayList<>();
        for (Macro macro : data.macros) {
            if (macro == null) continue;
            macro.sanitize();
            sanitized.add(macro.copy());
        }

        data.macros = sanitized;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    private static final class MacroData {
        private List<Macro> macros = new ArrayList<>();
    }
}
