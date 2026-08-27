package org.main.redstoneutils.client.calculator;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.Minecraft;

public final class Calculator {

    private Calculator() {
    }

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(ClientCommands.literal("calc")
                    .executes(context -> openCalculator()));
            dispatcher.register(ClientCommands.literal("redstoneutils")
                    .then(ClientCommands.literal("calc")
                            .executes(context -> openCalculator())));
        });
    }

    public static int openCalculator() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.gui.setScreen(new CalculatorScreen()));
        return 1;
    }
}
