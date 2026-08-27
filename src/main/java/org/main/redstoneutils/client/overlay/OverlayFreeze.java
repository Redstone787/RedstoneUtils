package org.main.redstoneutils.client.overlay;

import net.minecraft.network.chat.Component;
import org.main.redstoneutils.client.ui.RedstoneMessages;

/** Shared snapshot state. Overlay renderers retain their last analysis while frozen. */
public final class OverlayFreeze {

    private static boolean wire;
    private static boolean bud;
    private static boolean sculk;

    private OverlayFreeze() {
    }

    public static boolean wireFrozen() { return wire; }
    public static boolean budFrozen() { return bud; }
    public static boolean sculkFrozen() { return sculk; }
    public static boolean anyFrozen() { return wire || bud || sculk; }

    public static void setWireFrozen(boolean value) { wire = value; }
    public static void setBudFrozen(boolean value) { bud = value; }
    public static void setSculkFrozen(boolean value) { sculk = value; }

    public static void toggleAll() {
        setAllFrozen(!anyFrozen());
    }

    public static void setAllFrozen(boolean frozen) {
        wire = frozen;
        bud = frozen;
        sculk = frozen;
        RedstoneMessages.popup(Component.translatable(
                "message.redstoneutils.overlays.frozen",
                Component.translatable(frozen ? "state.redstoneutils.on" : "state.redstoneutils.off")
        ));
    }
}
