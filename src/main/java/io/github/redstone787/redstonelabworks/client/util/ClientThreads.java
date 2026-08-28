/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.client.util;

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
