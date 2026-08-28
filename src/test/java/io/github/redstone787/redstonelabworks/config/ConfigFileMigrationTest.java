/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigFileMigrationTest {

    @TempDir
    Path directory;

    @Test
    void copiesLegacyFileAndBackupWithoutDeletingThem() throws Exception {
        Path legacy = directory.resolve("redstoneutils.json");
        Path legacyBackup = directory.resolve("redstoneutils.json.bak");
        Path target = directory.resolve("redstonelabworks.json");
        Files.writeString(legacy, "legacy");
        Files.writeString(legacyBackup, "backup");

        ConfigFileMigration.copyLegacyIfNeeded(target, legacy.getFileName().toString());

        assertEquals("legacy", Files.readString(target));
        assertEquals("backup", Files.readString(directory.resolve("redstonelabworks.json.bak")));
        assertEquals("legacy", Files.readString(legacy));
        assertEquals("backup", Files.readString(legacyBackup));
    }

    @Test
    void neverOverwritesExistingTarget() throws Exception {
        Path legacy = directory.resolve("redstoneutils.json");
        Path target = directory.resolve("redstonelabworks.json");
        Files.writeString(legacy, "legacy");
        Files.writeString(target, "current");

        ConfigFileMigration.copyLegacyIfNeeded(target, legacy.getFileName().toString());

        assertEquals("current", Files.readString(target));
    }
}
