package org.main.redstoneutils.client.signal;

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
            source.sendFeedback(Component.translatable("message.redstoneutils.no_player"));
            return Optional.empty();
        }
        if (!(client.hitResult instanceof BlockHitResult blockHitResult) || blockHitResult.getType() != HitResult.Type.BLOCK) {
            source.sendFeedback(Component.translatable("message.redstoneutils.look_at_block"));
            return Optional.empty();
        }

        return Optional.of(new TargetedBlock(source, player, blockHitResult.getBlockPos()));
    }
}
