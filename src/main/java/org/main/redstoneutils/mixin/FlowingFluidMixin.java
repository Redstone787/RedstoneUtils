package org.main.redstoneutils.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import org.main.redstoneutils.server.gamerule.WaterproofRedstone;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {
    @Inject(method = "canHoldSpecificFluid", at = @At("HEAD"), cancellable = true)
    private static void redstoneutils$preventWaterFromReplacingComponents(
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
