package org.main.redstoneutils.client.util;

import net.minecraft.client.Minecraft;

public final class ClientThreads {

    private ClientThreads() {
    }

    public static void run(Runnable runnable) {
        if (runnable == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(runnable);
            return;
        }

        runnable.run();
    }
}
