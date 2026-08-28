/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.client.macro;

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
    private static Set<Binding> physicalDownBindings = new HashSet<>();
    private static Set<Binding> armedBindings = new HashSet<>();
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
            physicalDownBindings.clear();
            armedBindings.clear();
            heldCooldowns.clear();
            return;
        }

        List<Macro> macros = MacroStore.macros(MacroType.KEYBIND);
        Set<Binding> currentPhysicalDown = new HashSet<>();
        Set<Binding> currentArmed = new HashSet<>();
        int activeModifiers = currentModifiers(client);

        for (Macro macro : macros) {
            if (!macro.enabled() || !MacroKeys.isBound(macro.keyCode())) continue;
            Binding binding = new Binding(macro.keyCode(), macro.mouseButton(), macro.modifiers());
            boolean physicalDown = isDown(client, binding);
            boolean modifiersMatch = activeModifiers == binding.modifiers();
            boolean wasPhysicalDown = physicalDownBindings.contains(binding);
            boolean armed = armedBindings.contains(binding);
            if (physicalDown) currentPhysicalDown.add(binding);
            if (physicalDown && !wasPhysicalDown && modifiersMatch) armed = true;
            if (physicalDown && armed) currentArmed.add(binding);

            switch (macro.trigger()) {
                case PRESSED -> {
                    if (physicalDown && !wasPhysicalDown && modifiersMatch) MacroExecutor.execute(macro);
                }
                case RELEASED -> {
                    if (!physicalDown && wasPhysicalDown && armed) MacroExecutor.execute(macro);
                }
                case HELD -> tickHeld(macro, physicalDown && armed && modifiersMatch);
            }
        }
        physicalDownBindings = currentPhysicalDown;
        armedBindings = currentArmed;
        heldCooldowns.keySet().removeIf(id -> macros.stream().noneMatch(macro -> macro.id().equals(id)));
    }

    private static void tickHeld(Macro macro, boolean active) {
        if (!active) {
            heldCooldowns.remove(macro.id());
            return;
        }
        int cooldown = heldCooldowns.getOrDefault(macro.id(), 0);
        if (cooldown <= 0) {
            MacroExecutor.execute(macro);
            heldCooldowns.put(macro.id(), HELD_REPEAT_TICKS - 1);
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
