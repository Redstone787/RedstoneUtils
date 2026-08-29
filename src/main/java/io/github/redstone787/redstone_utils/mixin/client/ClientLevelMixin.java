/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstone_utils.mixin.client;

import io.github.redstone787.redstone_utils.client.sculk.SculkSensorOverlay;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @Inject(method = "setBlocksDirty", at = @At("TAIL"))
    private void redstone_utils$invalidateSculkOverlayMesh(
            BlockPos blockPos,
            BlockState oldState,
            BlockState newState,
            CallbackInfo callback
    ) {
        SculkSensorOverlay.onBlockStateChanged((ClientLevel) (Object) this, blockPos, oldState, newState);
    }
}
