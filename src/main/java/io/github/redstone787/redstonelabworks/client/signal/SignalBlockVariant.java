/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.client.signal;

import net.minecraft.network.chat.Component;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

enum SignalBlockVariant {
    BARREL(
            "barrel",
            "Barrel",
            "0-15",
            false,
            allSignals(),
            strength -> SignalBlockItem.createContainer("minecraft:barrel", strength, 27),
            "barrels"
    ),
    CHEST(
            "chest",
            "Chest",
            "0-15",
            false,
            allSignals(),
            strength -> SignalBlockItem.createContainer("minecraft:chest", strength, 27),
            "chests"
    ),
    TRAPPED_CHEST(
            "trapped_chest",
            "Trapped Chest",
            "0-15",
            false,
            allSignals(),
            strength -> SignalBlockItem.createContainer("minecraft:trapped_chest", strength, 27),
            "trapped-chest"
    ),
    SHULKER_BOX(
            "shulker_box",
            "Shulker Box",
            "0-15",
            false,
            allSignals(),
            strength -> SignalBlockItem.createContainer("minecraft:shulker_box", strength, 27),
            "shulker",
            "shulker-box"
    ),
    DISPENSER(
            "dispenser",
            "Dispenser",
            "0-15",
            false,
            allSignals(),
            strength -> SignalBlockItem.createContainer("minecraft:dispenser", strength, 9)
    ),
    DROPPER(
            "dropper",
            "Dropper",
            "0-15",
            false,
            allSignals(),
            strength -> SignalBlockItem.createContainer("minecraft:dropper", strength, 9)
    ),
    HOPPER(
            "hopper",
            "Hopper",
            "0-15",
            false,
            allSignals(),
            strength -> SignalBlockItem.createContainer("minecraft:hopper", strength, 5)
    ),
    FURNACE(
            "furnace",
            "Furnace",
            "0-15",
            false,
            allSignals(),
            strength -> SignalBlockItem.createContainer("minecraft:furnace", strength, 3)
    ),
    BLAST_FURNACE(
            "blast_furnace",
            "Blast Furnace",
            "0-15",
            false,
            allSignals(),
            strength -> SignalBlockItem.createContainer("minecraft:blast_furnace", strength, 3),
            "blast-furnace"
    ),
    SMOKER(
            "smoker",
            "Smoker",
            "0-15",
            false,
            allSignals(),
            strength -> SignalBlockItem.createContainer("minecraft:smoker", strength, 3)
    ),
    BREWING_STAND(
            "brewing_stand",
            "Brewing Stand",
            "0-15",
            false,
            allSignals(),
            strength -> SignalBlockItem.createContainer("minecraft:brewing_stand", strength, 5),
            "brewing-stand"
    ),
    LECTERN(
            "lectern",
            "Lectern",
            "0-15",
            false,
            allSignals(),
            SignalBlockItem::createLectern,
            "lecturn",
            "leckturn"
    ),
    CRAFTER(
            "crafter",
            "Crafter",
            "0-9",
            false,
            range(0, 9),
            SignalBlockItem::createCrafter
    ),
    COMPOSTER(
            "composter",
            "Composter",
            "0-6,8",
            false,
            strength -> strength >= 0 && strength <= 8 && strength != 7,
            SignalBlockItem::createComposter
    ),
    CAKE(
            "cake",
            "Cake",
            "2,4,6,8,10,12,14",
            false,
            strength -> strength >= 2 && strength <= 14 && strength % 2 == 0,
            SignalBlockItem::createCake
    ),
    BEEHIVE(
            "beehive",
            "Beehive",
            "0-5",
            false,
            range(0, 5),
            strength -> SignalBlockItem.createBeehive("minecraft:beehive", strength),
            "bee_hive",
            "bee-hive"
    ),
    BEE_NEST(
            "bee_nest",
            "Bee Nest",
            "0-5",
            false,
            range(0, 5),
            strength -> SignalBlockItem.createBeehive("minecraft:bee_nest", strength),
            "beenest",
            "bee-nest"
    ),
    RESPAWN_ANCHOR(
            "respawn_anchor",
            "Respawn Anchor",
            "0,3,7,11,15",
            false,
            strength -> strength == 0 || strength == 3 || strength == 7 || strength == 11 || strength == 15,
            SignalBlockItem::createRespawnAnchor,
            "respawn-anchor"
    ),
    CAULDRON(
            "cauldron",
            "Cauldron",
            "0-3",
            true,
            range(0, 3),
            SignalBlockVariant::createWaterCauldronBlock,
            "caldron",
            "caldrons",
            "caldrin",
            "caldrins",
            "cauldrons"
    ),
    WATER_CAULDRON(
            "water_cauldron",
            "Water Cauldron",
            "0-3",
            true,
            range(0, 3),
            SignalBlockVariant::createWaterCauldronBlock,
            "water-cauldron"
    ),
    POWDER_SNOW_CAULDRON(
            "powder_snow_cauldron",
            "Powder Snow Cauldron",
            "0-3",
            true,
            range(0, 3),
            SignalBlockVariant::createPowderSnowCauldronBlock,
            "powder-cauldron",
            "powder_snow",
            "powder-snow-cauldron"
    ),
    LAVA_CAULDRON(
            "lava_cauldron",
            "Lava Cauldron",
            "0 or 3",
            true,
            strength -> strength == 0 || strength == 3,
            SignalBlockVariant::createLavaCauldronBlock,
            "lava-cauldron"
    );

    private static final Map<String, SignalBlockVariant> BY_NAME = Arrays.stream(values())
            .flatMap(variant -> variant.names().map(name -> Map.entry(name, variant)))
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue, (first, ignored) -> first));

    private final String key;
    private final String displayName;
    private final String supportedStrengths;
    private final boolean targetBlock;
    private final IntPredicate supports;
    private final IntFunction<String> argumentFactory;
    private final String[] aliases;

    SignalBlockVariant(
            String key,
            String displayName,
            String supportedStrengths,
            boolean targetBlock,
            IntPredicate supports,
            IntFunction<String> argumentFactory,
            String... aliases
    ) {
        this.key = key;
        this.displayName = displayName;
        this.supportedStrengths = supportedStrengths;
        this.targetBlock = targetBlock;
        this.supports = supports;
        this.argumentFactory = argumentFactory;
        this.aliases = aliases;
    }

    static Optional<SignalBlockVariant> find(String name) {
        return Optional.ofNullable(BY_NAME.get(normalize(name)));
    }

    static String suggestions() {
        return Arrays.stream(values())
                .map(SignalBlockVariant::key)
                .collect(Collectors.joining(", "));
    }

    static SignalBlockVariant[] orderedValues() {
        return values();
    }

    String key() {
        return key;
    }

    String displayName() {
        return Component.translatable("signal_block.redstonelabworks." + key).getString();
    }

    boolean targetBlock() {
        return targetBlock;
    }

    boolean supports(int strength) {
        return supports.test(strength);
    }

    String createArgument(int strength) {
        return argumentFactory.apply(strength);
    }

    String unsupportedStrengthMessage() {
        return Component.translatable("message.redstonelabworks.signal.supported_strengths", displayName(), supportedStrengths).getString();
    }

    private java.util.stream.Stream<String> names() {
        return java.util.stream.Stream.concat(
                java.util.stream.Stream.of(key),
                Arrays.stream(aliases)
        ).map(SignalBlockVariant::normalize);
    }

    private static IntPredicate allSignals() {
        return range(ComparatorSignal.MIN, ComparatorSignal.MAX);
    }

    private static IntPredicate range(int min, int max) {
        return strength -> strength >= min && strength <= max;
    }

    private static String createWaterCauldronBlock(int strength) {
        if (strength == 0) return "minecraft:cauldron";
        return "minecraft:water_cauldron[level=" + strength + "]";
    }

    private static String createPowderSnowCauldronBlock(int strength) {
        if (strength == 0) return "minecraft:cauldron";
        return "minecraft:powder_snow_cauldron[level=" + strength + "]";
    }

    private static String createLavaCauldronBlock(int strength) {
        if (strength == 0) return "minecraft:cauldron";
        return "minecraft:lava_cauldron";
    }

    private static String normalize(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("minecraft:")) normalized = normalized.substring("minecraft:".length());
        return normalized.replace('-', '_');
    }
}
