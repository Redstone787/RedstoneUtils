/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.config;

import io.github.redstone787.redstonelabworks.RedstoneLabworks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Copies pre-rename configuration into the new namespace without deleting the original. */
public final class ConfigFileMigration {

    private ConfigFileMigration() {
    }

    public static void copyLegacyIfNeeded(Path target, String legacyFileName) {
        if (target == null || legacyFileName == null || legacyFileName.isBlank() || Files.exists(target)) return;

        Path legacy = target.resolveSibling(legacyFileName);
        if (!Files.isRegularFile(legacy)) return;

        try {
            Files.createDirectories(target.toAbsolutePath().getParent());
            Files.copy(legacy, target);
            copyBackupIfPresent(legacy, target);
            RedstoneLabworks.LOGGER.info("Copied legacy configuration {} to {}; the original was preserved", legacy, target);
        } catch (IOException exception) {
            RedstoneLabworks.LOGGER.error("Could not copy legacy configuration {} to {}", legacy, target, exception);
        }
    }

    private static void copyBackupIfPresent(Path legacy, Path target) throws IOException {
        Path legacyBackup = legacy.resolveSibling(legacy.getFileName() + ".bak");
        Path targetBackup = target.resolveSibling(target.getFileName() + ".bak");
        if (Files.isRegularFile(legacyBackup) && !Files.exists(targetBackup)) {
            Files.copy(legacyBackup, targetBackup);
        }
    }
}
