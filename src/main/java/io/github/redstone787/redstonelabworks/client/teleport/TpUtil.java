/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.client.teleport;

import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import io.github.redstone787.redstonelabworks.client.config.RedstoneLabworksConfig;
import io.github.redstone787.redstonelabworks.client.RedstoneLabworksClientNetworking;
import io.github.redstone787.redstonelabworks.client.ui.RedstoneMessages;

import java.util.Locale;
import java.util.UUID;

public final class TpUtil {

    private TpUtil() {
    }

    public static void teleportToBlock() {
        teleportToBlock(RedstoneLabworksConfig.getTeleportMaxRange());
    }

    public static void teleportToBlock(double requestedRange) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) return;

        double maxRange = Math.clamp(
                Double.isFinite(requestedRange) ? requestedRange : RedstoneLabworksConfig.getTeleportMaxRange(),
                10.0D,
                1_000.0D
        );
        if (RedstoneLabworksClientNetworking.teleport(maxRange)) {
            return;
        }

        Vec3 eyePos = player.getEyePosition();
        Vec3 viewVec = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(viewVec.scale(maxRange));

        BlockHitResult hitResult = player.level().clip(new ClipContext(
                eyePos,
                endPos,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));

        Vec3 targetPos = hitResult.getType() == HitResult.Type.BLOCK
                ? hitResult.getLocation()
                : endPos;
        teleportLikeCommand(minecraft, player, targetPos);
    }

    private static void teleportLikeCommand(Minecraft minecraft, Player player, Vec3 targetPos) {
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (server != null) {
            UUID playerId = player.getUUID();
            server.execute(() -> {
                ServerPlayer serverPlayer = server.getPlayerList().getPlayer(playerId);
                if (serverPlayer != null) {
                    serverPlayer.teleportTo(targetPos.x, targetPos.y, targetPos.z);
                    sendTeleportFeedback(targetPos);
                }
            });
            return;
        }

        if (minecraft.getConnection() != null) {
            minecraft.getConnection().sendCommand(formatTeleportCommand(targetPos));
            RedstoneMessages.popup(Component.translatable("message.redstonelabworks.teleport.requested"));
            return;
        }

        player.setPos(targetPos);
    }

    private static void sendTeleportFeedback(Vec3 targetPos) {
        RedstoneMessages.popup(Component.translatable(
                "message.redstonelabworks.teleported_exact",
                String.format(Locale.ROOT, "%.2f %.2f %.2f", targetPos.x, targetPos.y, targetPos.z)
        ));
    }

    private static String formatTeleportCommand(Vec3 targetPos) {
        return String.format(
                Locale.ROOT,
                "tp @s %.12f %.12f %.12f",
                targetPos.x,
                targetPos.y,
                targetPos.z
        );
    }
}
