package org.main.redstoneutils.server.gamerule;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.main.redstoneutils.RedstoneUtils;

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
