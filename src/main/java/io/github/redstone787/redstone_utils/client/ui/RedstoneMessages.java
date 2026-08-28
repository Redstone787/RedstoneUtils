/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstone_utils.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import io.github.redstone787.redstone_utils.client.config.RedstoneUtilsConfig;
import io.github.redstone787.redstone_utils.client.util.ClientThreads;

public final class RedstoneMessages {

    private static MessageTarget defaultTarget = MessageTarget.POPUP;

    private RedstoneMessages() {
    }

    public static void send(String message) {
        send(Component.literal(message));
    }

    public static void send(Component message) {
        send(defaultTarget, message);
    }

    public static void popup(String message) {
        send(MessageTarget.POPUP, message);
    }

    public static void popup(Component message) {
        send(MessageTarget.POPUP, message);
    }

    public static void chat(String message) {
        send(MessageTarget.CHAT, message);
    }

    public static void chat(Component message) {
        send(MessageTarget.CHAT, message);
    }

    public static void actionBar(String message) {
        send(MessageTarget.ACTION_BAR, message);
    }

    public static void actionBar(Component message) {
        send(MessageTarget.ACTION_BAR, message);
    }

    public static void send(MessageTarget target, String message) {
        send(target, Component.literal(message));
    }

    public static void send(MessageTarget target, Component message) {
        if (message == null || message.getString().isBlank()) return;

        ClientThreads.run(() -> dispatch(target == null ? defaultTarget : target, message));
    }

    public static void setDefaultTarget(MessageTarget target) {
        if (target == null) return;
        defaultTarget = target;
        RedstoneUtilsConfig.setMessageTarget(target);
    }

    public static MessageTarget getDefaultTarget() {
        return defaultTarget;
    }

    private static void dispatch(MessageTarget target, Component message) {
        switch (target) {
            case POPUP -> RedstoneOverlay.showPopup(message.getString());
            case CHAT -> sendChat(message);
            case ACTION_BAR -> sendActionBar(message);
            case POPUP_AND_CHAT -> {
                RedstoneOverlay.showPopup(message.getString());
                sendChat(message);
            }
            case POPUP_AND_ACTION_BAR -> {
                RedstoneOverlay.showPopup(message.getString());
                sendActionBar(message);
            }
        }
    }

    private static void sendChat(Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            RedstoneOverlay.showPopup(message.getString());
            return;
        }

        minecraft.player.sendSystemMessage(message);
    }

    private static void sendActionBar(Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            RedstoneOverlay.showPopup(message.getString());
            return;
        }

        minecraft.player.sendOverlayMessage(message);
    }

    public enum MessageTarget {
        POPUP,
        CHAT,
        ACTION_BAR,
        POPUP_AND_CHAT,
        POPUP_AND_ACTION_BAR
    }
}
