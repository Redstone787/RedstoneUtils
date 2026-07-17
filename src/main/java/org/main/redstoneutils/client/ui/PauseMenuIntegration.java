package org.main.redstoneutils.client.ui;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;

public final class PauseMenuIntegration {

    private PauseMenuIntegration() {
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof PauseScreen)) return;
            Screens.getWidgets(screen).add(Button.builder(
                    Component.translatable("button.redstoneutils.toolbox"),
                    ignored -> ToolboxScreen.open()
            ).bounds(6, 6, 128, 20).build());
        });
    }
}
