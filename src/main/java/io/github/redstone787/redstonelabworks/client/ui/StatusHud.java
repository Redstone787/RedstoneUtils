/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.client.ui;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import io.github.redstone787.redstonelabworks.RedstoneLabworks;
import io.github.redstone787.redstonelabworks.client.RedstoneLabworksClientActions;
import io.github.redstone787.redstonelabworks.client.RedstoneLabworksClientNetworking;
import io.github.redstone787.redstonelabworks.client.autowire.AutoWireHandler;
import io.github.redstone787.redstonelabworks.client.config.RedstoneLabworksConfig;
import io.github.redstone787.redstonelabworks.client.overlay.OverlayFreeze;

import java.util.ArrayList;
import java.util.List;

public final class StatusHud implements HudElement {

    private static final Identifier ID = RedstoneLabworks.id("status_hud");
    private static final StatusHud INSTANCE = new StatusHud();

    private StatusHud() {
    }

    public static void init() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE, ID, INSTANCE);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!RedstoneLabworksConfig.isStatusHudVisible()
                || minecraft.player == null
                || minecraft.getDebugOverlay().showDebugScreen()) return;
        Font font = minecraft.font;
        List<String> lines = lines();
        int width = lines.stream().mapToInt(font::width).max().orElse(0) + 12;
        int height = lines.size() * (font.lineHeight + 2) + 8;
        int margin = 6;
        boolean right = RedstoneLabworksConfig.getStatusHudAnchor() == RedstoneLabworksConfig.HudAnchor.TOP_RIGHT
                || RedstoneLabworksConfig.getStatusHudAnchor() == RedstoneLabworksConfig.HudAnchor.BOTTOM_RIGHT;
        boolean bottom = RedstoneLabworksConfig.getStatusHudAnchor() == RedstoneLabworksConfig.HudAnchor.BOTTOM_LEFT
                || RedstoneLabworksConfig.getStatusHudAnchor() == RedstoneLabworksConfig.HudAnchor.BOTTOM_RIGHT;
        int x = right ? graphics.guiWidth() - width - margin : margin;
        int y = bottom ? graphics.guiHeight() - height - margin : margin;
        graphics.fill(x, y, x + width, y + height, 0xC5222428);
        graphics.outline(x, y, width, height, 0xE06B7078);
        for (int index = 0; index < lines.size(); index++) {
            graphics.text(font, lines.get(index), x + 6, y + 5 + index * (font.lineHeight + 2), 0xFFFFFFFF, false);
        }
    }

    private static List<String> lines() {
        List<String> lines = new ArrayList<>();
        lines.add(Component.translatable("hud.redstonelabworks.autowire", AutoWireHandler.getActiveWireType().getDisplayName()).getString());
        List<String> overlays = new ArrayList<>();
        if (RedstoneLabworksClientActions.wireOverlayVisible()) overlays.add(Component.translatable("overlay.redstonelabworks.wire").getString());
        if (RedstoneLabworksClientActions.budOverlayVisible()) overlays.add(Component.translatable("overlay.redstonelabworks.bud").getString());
        if (RedstoneLabworksClientActions.sculkOverlayVisible()) overlays.add(Component.translatable("overlay.redstonelabworks.sculk").getString());
        lines.add(Component.translatable("hud.redstonelabworks.overlays", overlays.isEmpty() ? "-" : String.join(", ", overlays)).getString());
        lines.add(Component.translatable("hud.redstonelabworks.backend", Component.translatable(
                RedstoneLabworksClientNetworking.hasServerBackend() ? "state.redstonelabworks.available" : "state.redstonelabworks.client_only"
        )).getString());
        if (OverlayFreeze.anyFrozen()) lines.add(Component.translatable("hud.redstonelabworks.frozen").getString());
        return lines;
    }
}
