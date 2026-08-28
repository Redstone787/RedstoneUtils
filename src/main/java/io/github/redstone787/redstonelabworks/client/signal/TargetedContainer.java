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
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

record TargetedContainer(
        FabricClientCommandSource source,
        Minecraft client,
        LocalPlayer player,
        BlockPos blockPos,
        BlockState blockState,
        BlockEntity blockEntity,
        Container container,
        ItemStack itemStack
) {

    static Optional<TargetedContainer> resolve(
            FabricClientCommandSource source,
            BlockPos blockPos,
            ItemStack itemStack
    ) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            source.sendFeedback(Component.translatable("message.redstonelabworks.no_player"));
            return Optional.empty();
        }

        BlockState blockState = client.level.getBlockState(blockPos);
        BlockEntity blockEntity = client.level.getBlockEntity(blockPos);
        if (!(blockEntity instanceof Container container)) {
            source.sendFeedback(Component.translatable("message.redstonelabworks.not_container"));
            return Optional.empty();
        }

        return Optional.of(new TargetedContainer(
                source,
                client,
                player,
                blockPos,
                blockState,
                blockEntity,
                container,
                itemStack
        ));
    }
}
