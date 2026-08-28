/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.client.calculator;

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
            dispatcher.register(ClientCommands.literal("redstonelabworks")
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
