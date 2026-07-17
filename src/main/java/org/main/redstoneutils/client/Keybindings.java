package org.main.redstoneutils.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.main.redstoneutils.RedstoneUtils;
import org.main.redstoneutils.client.autowire.AutoWireHandler;
import org.main.redstoneutils.client.autowire.WireType;
import org.main.redstoneutils.client.teleport.TpUtil;
import org.main.redstoneutils.client.ui.CircleSegment;
import org.main.redstoneutils.client.ui.RedstoneOverlay;
import org.main.redstoneutils.client.ui.ToolboxScreen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Keybindings {

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(RedstoneUtils.id("general"));
    private static final Map<KeyMapping, Runnable> keyBindings = new HashMap<>();

    private static KeyMapping wireMenuKey;
    private static boolean wasWireMenuKeyDown;

    public static void init() {
        register(generateKey("key.redstoneutils.teleport"), TpUtil::teleportToBlock);
        register(generateKey("key.redstoneutils.toolbox"), ToolboxScreen::open);
        wireMenuKey = generateKey("key.redstoneutils.wire");

        keyBindingExe();
    }

    public static CircleSegment getSelectedWireMenuSegment() {
        return RedstoneOverlay.getSelectedWheelSegment();
    }

    public static WireType getSelectedWireType() {
        return AutoWireHandler.getSelectableWireType(getSelectedWireMenuSegment().index());
    }

    public static CircleSegment consumeConfirmedWireMenuSegment() {
        return RedstoneOverlay.consumeConfirmedWheelSegment();
    }

    public static WireType consumeConfirmedWireType() {
        return AutoWireHandler.getSelectableWireType(consumeConfirmedWireMenuSegment().index());
    }

    private static void keyBindingExe() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            keyBindings.keySet().forEach(key -> {
                while (key.consumeClick()) {
                    Runnable runnable;
                    if ((runnable = keyBindings.get(key)) != null) runnable.run();
                }
            });

            updateWireMenuKey();
        });
    }

    private static void updateWireMenuKey() {
        if (wireMenuKey == null) return;
        if (isWireMenuScreenOpen()) return;

        boolean isWireMenuKeyDown = wireMenuKey.isDown();
        if (isWireMenuKeyDown && !wasWireMenuKeyDown) {
            openWireMenuScreen();
        } else if (!isWireMenuKeyDown && wasWireMenuKeyDown) {
            closeWireMenuScreen();
        }

        wasWireMenuKeyDown = isWireMenuKeyDown;
    }

    private static boolean isWireMenuScreenOpen() {
        return Minecraft.getInstance().gui.screen() instanceof WireMenuScreen;
    }

    private static void openWireMenuScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() != null) return;

        List<WireType> wireTypes = AutoWireHandler.getSelectableWireTypes();
        if (wireTypes.isEmpty()) return;

        CircleSegment activeSegment = CircleSegment.fromIndex(
                AutoWireHandler.getSelectableWireTypeIndex(AutoWireHandler.getActiveWireType())
        );

        RedstoneOverlay.setSegmentWheel(
                true,
                wireTypes.size(),
                AutoWireHandler.getSelectableWireTextures(),
                activeSegment
        );
        minecraft.gui.setScreen(new WireMenuScreen());
    }

    private static void closeWireMenuScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        finishWireMenuSelection();

        if (minecraft.gui.screen() instanceof WireMenuScreen) {
            minecraft.gui.setScreen(null);
        }

        wasWireMenuKeyDown = false;
    }

    private static void finishWireMenuSelection() {
        CircleSegment selectedSegment = RedstoneOverlay.finishSegmentWheelSelection();
        WireType selectedWireType = AutoWireHandler.getSelectableWireType(selectedSegment.index());

        if (selectedSegment != CircleSegment.NONE) {
            AutoWireHandler.setActiveWireType(selectedWireType);
        }
    }

    private static KeyMapping generateKey(String name) {
        return KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                name,
                InputConstants.Type.KEYSYM.ordinal(),
                CATEGORY
            )
        );
    }

    private static void register(KeyMapping key, Runnable runnable) {
        if (key == null || runnable == null) return;
        keyBindings.put(key, runnable);
    }

    private static final class WireMenuScreen extends Screen {

        private WireMenuScreen() {
            super(Component.empty());
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            setMovementKeyState(event, true);
            return true;
        }

        @Override
        public boolean keyReleased(KeyEvent event) {
            if (wireMenuKey != null && wireMenuKey.matches(event)) {
                closeWireMenuScreen();
            } else {
                setMovementKeyState(event, false);
            }

            return true;
        }

        @Override
        protected void init() {
            super.init();
            restoreHeldMovementKeys();
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return true;
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            if (wireMenuKey != null && wireMenuKey.matchesMouse(event)) {
                closeWireMenuScreen();
            }

            return true;
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
            return true;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            return true;
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return false;
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public boolean isInGameUi() {
            return true;
        }

        @Override
        public void removed() {
            if (RedstoneOverlay.isSegmentWheelVisible()) {
                finishWireMenuSelection();
            }
        }

        private void setMovementKeyState(KeyEvent event, boolean pressed) {
            for (KeyMapping movementKey : movementKeys()) {
                if (movementKey.matches(event)) {
                    movementKey.setDown(pressed);
                }
            }
        }

        private void restoreHeldMovementKeys() {
            for (KeyMapping movementKey : movementKeys()) {
                InputConstants.Key boundKey = InputConstants.getKey(movementKey.saveString());
                boolean pressed = !movementKey.isUnbound()
                        && boundKey.getType() == InputConstants.Type.KEYSYM
                        && InputConstants.isKeyDown(minecraft.getWindow(), boundKey.getValue());
                movementKey.setDown(pressed);
            }
        }

        private KeyMapping[] movementKeys() {
            return new KeyMapping[]{
                    minecraft.options.keyUp,
                    minecraft.options.keyDown,
                    minecraft.options.keyLeft,
                    minecraft.options.keyRight
            };
        }
    }
}
