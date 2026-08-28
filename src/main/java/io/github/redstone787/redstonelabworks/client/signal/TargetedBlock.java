/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.client.signal;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Optional;

record TargetedBlock(
        FabricClientCommandSource source,
        LocalPlayer player,
        BlockPos blockPos
) {

    static Optional<TargetedBlock> resolve(FabricClientCommandSource source) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            source.sendFeedback(Component.translatable("message.redstonelabworks.no_player"));
            return Optional.empty();
        }
        if (!(client.hitResult instanceof BlockHitResult blockHitResult) || blockHitResult.getType() != HitResult.Type.BLOCK) {
            source.sendFeedback(Component.translatable("message.redstonelabworks.look_at_block"));
            return Optional.empty();
        }

        return Optional.of(new TargetedBlock(source, player, blockHitResult.getBlockPos()));
    }
}
