/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.server.gamerule;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import io.github.redstone787.redstonelabworks.RedstoneLabworks;

public final class RedstoneLabworksGameRules {
    public static final GameRule<Boolean> WATERPROOF_REDSTONE = GameRuleBuilder.forBoolean(false)
            .category(GameRuleCategory.UPDATES)
            .buildAndRegister(RedstoneLabworks.id("waterproof_redstone"));

    private RedstoneLabworksGameRules() {
    }

    public static void init() {
        // Loading this class registers the game rules before a server creates its worlds.
    }
}
