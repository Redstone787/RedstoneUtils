/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstone_utils.server.gamerule;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import io.github.redstone787.redstone_utils.RedstoneUtils;

public final class RedstoneUtilsGameRules {
    public static final GameRule<Boolean> WATERPROOF_REDSTONE = GameRuleBuilder.forBoolean(false)
            .category(GameRuleCategory.UPDATES)
            .buildAndRegister(RedstoneUtils.id("waterproof_redstone"));

    private RedstoneUtilsGameRules() {
    }

    public static void init() {
        // Loading this class registers the game rules before a server creates its worlds.
    }
}
