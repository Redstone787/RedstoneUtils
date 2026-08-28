/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstone_utils.server.gamerule;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import io.github.redstone787.redstone_utils.RedstoneUtils;

public final class WaterproofRedstone {
    public static final TagKey<Block> COMPONENTS = TagKey.create(
            Registries.BLOCK,
            RedstoneUtils.id("waterproof_redstone_components")
    );

    private WaterproofRedstone() {
    }

    public static boolean preventsWaterReplacement(ServerLevel level, BlockState state, Fluid fluid) {
        return fluid.defaultFluidState().is(FluidTags.WATER)
                && state.is(COMPONENTS)
                && level.getGameRules().get(RedstoneUtilsGameRules.WATERPROOF_REDSTONE);
    }
}
