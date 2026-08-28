/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import io.github.redstone787.redstonelabworks.server.gamerule.WaterproofRedstone;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin {
    @Inject(method = "emptyContents", at = @At("HEAD"), cancellable = true)
    private void redstonelabworks$preventWaterFromReplacingComponents(
            LivingEntity user,
            Level level,
            BlockPos pos,
            BlockHitResult hitResult,
            CallbackInfoReturnable<Boolean> callback
    ) {
        BucketItem bucket = (BucketItem) (Object) this;
        if (level instanceof ServerLevel serverLevel
                && WaterproofRedstone.preventsWaterReplacement(serverLevel, level.getBlockState(pos), bucket.getContent())) {
            callback.setReturnValue(false);
        }
    }
}
