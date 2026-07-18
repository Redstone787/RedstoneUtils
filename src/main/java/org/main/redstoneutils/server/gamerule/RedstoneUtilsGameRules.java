package org.main.redstoneutils.server.gamerule;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import org.main.redstoneutils.RedstoneUtils;

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
