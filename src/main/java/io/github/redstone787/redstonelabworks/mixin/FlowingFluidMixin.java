/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import io.github.redstone787.redstonelabworks.server.gamerule.WaterproofRedstone;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {
    @Inject(method = "canHoldSpecificFluid", at = @At("HEAD"), cancellable = true)
    private static void redstonelabworks$preventWaterFromReplacingComponents(
            BlockGetter level,
            BlockPos pos,
            BlockState state,
            Fluid fluid,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (level instanceof ServerLevel serverLevel
                && WaterproofRedstone.preventsWaterReplacement(serverLevel, state, fluid)) {
            callback.setReturnValue(false);
        }
    }
}
