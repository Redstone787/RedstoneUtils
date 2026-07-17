package org.main.redstoneutils.client.macro;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CommandKeybind {

    private static final int HELD_REPEAT_TICKS = 4;
    private static Set<Binding> downBindings = new HashSet<>();
    private static final Map<String, Integer> heldCooldowns = new HashMap<>();
    private static boolean initialized;

    private CommandKeybind() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        ClientTickEvents.END_CLIENT_TICK.register(CommandKeybind::tick);
    }

    private static void tick(Minecraft client) {
        if (client == null || client.player == null || client.getConnection() == null || client.gui.screen() != null) {
            downBindings.clear();
            heldCooldowns.clear();
            return;
        }

        List<Macro> macros = MacroStore.macros(MacroType.KEYBIND);
        Set<Binding> currentDown = new HashSet<>();
        int activeModifiers = currentModifiers(client);

        for (Macro macro : macros) {
            if (!macro.enabled() || !MacroKeys.isBound(macro.keyCode())) continue;
            Binding binding = new Binding(macro.keyCode(), macro.mouseButton(), macro.modifiers());
            boolean physicalDown = isDown(client, binding);
            boolean modifiersMatch = activeModifiers == binding.modifiers();
            boolean wasDown = downBindings.contains(binding);
            boolean isDown = physicalDown && modifiersMatch;
            if (isDown) currentDown.add(binding);

            switch (macro.trigger()) {
                case PRESSED -> {
                    if (isDown && !wasDown) MacroExecutor.execute(macro);
                }
                case RELEASED -> {
                    if (!isDown && wasDown) MacroExecutor.execute(macro);
                }
                case HELD -> tickHeld(macro, isDown, wasDown);
            }
        }
        downBindings = currentDown;
        heldCooldowns.keySet().removeIf(id -> macros.stream().noneMatch(macro -> macro.id().equals(id)));
    }

    private static void tickHeld(Macro macro, boolean isDown, boolean wasDown) {
        if (!isDown) {
            heldCooldowns.remove(macro.id());
            return;
        }
        int cooldown = heldCooldowns.getOrDefault(macro.id(), 0);
        if (!wasDown || cooldown <= 0) {
            MacroExecutor.execute(macro);
            heldCooldowns.put(macro.id(), HELD_REPEAT_TICKS);
        } else {
            heldCooldowns.put(macro.id(), cooldown - 1);
        }
    }

    private static boolean isDown(Minecraft client, Binding binding) {
        if (binding.mouseButton()) {
            return GLFW.glfwGetMouseButton(client.getWindow().handle(), binding.code()) == GLFW.GLFW_PRESS;
        }
        return InputConstants.isKeyDown(client.getWindow(), binding.code());
    }

    private static int currentModifiers(Minecraft client) {
        int modifiers = 0;
        if (down(client, InputConstants.KEY_LSHIFT) || down(client, InputConstants.KEY_RSHIFT)) modifiers |= MacroKeys.MOD_SHIFT;
        if (down(client, InputConstants.KEY_LCONTROL) || down(client, InputConstants.KEY_RCONTROL)) modifiers |= MacroKeys.MOD_CONTROL;
        if (down(client, InputConstants.KEY_LALT) || down(client, InputConstants.KEY_RALT)) modifiers |= MacroKeys.MOD_ALT;
        if (down(client, InputConstants.KEY_LSUPER) || down(client, InputConstants.KEY_RSUPER)) modifiers |= MacroKeys.MOD_SUPER;
        return modifiers;
    }

    private static boolean down(Minecraft client, int key) {
        return InputConstants.isKeyDown(client.getWindow(), key);
    }

    private record Binding(int code, boolean mouseButton, int modifiers) {
    }
}
