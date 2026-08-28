/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstone_utils.server.signal;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

public enum SignalBlockVariant {
    BARREL(
            "barrel",
            "Barrel",
            "0-15",
            allSignals(),
            SignalItemFactory::createBarrel,
            "barrels"
    ),
    CHEST(
            "chest",
            "Chest",
            "0-15",
            allSignals(),
            (strength, registryAccess) -> SignalItemFactory.createContainer(Blocks.CHEST, strength, 27),
            "chests"
    ),
    TRAPPED_CHEST(
            "trapped_chest",
            "Trapped Chest",
            "0-15",
            allSignals(),
            (strength, registryAccess) -> SignalItemFactory.createContainer(Blocks.TRAPPED_CHEST, strength, 27),
            "trapped-chest"
    ),
    SHULKER_BOX(
            "shulker_box",
            "Shulker Box",
            "0-15",
            allSignals(),
            (strength, registryAccess) -> SignalItemFactory.createContainer(Blocks.SHULKER_BOX, strength, 27),
            "shulker",
            "shulker-box"
    ),
    DISPENSER(
            "dispenser",
            "Dispenser",
            "0-15",
            allSignals(),
            (strength, registryAccess) -> SignalItemFactory.createContainer(Blocks.DISPENSER, strength, 9)
    ),
    DROPPER(
            "dropper",
            "Dropper",
            "0-15",
            allSignals(),
            (strength, registryAccess) -> SignalItemFactory.createContainer(Blocks.DROPPER, strength, 9)
    ),
    HOPPER(
            "hopper",
            "Hopper",
            "0-15",
            allSignals(),
            (strength, registryAccess) -> SignalItemFactory.createContainer(Blocks.HOPPER, strength, 5)
    ),
    FURNACE(
            "furnace",
            "Furnace",
            "0-15",
            allSignals(),
            (strength, registryAccess) -> SignalItemFactory.createContainer(Blocks.FURNACE, strength, 3)
    ),
    BLAST_FURNACE(
            "blast_furnace",
            "Blast Furnace",
            "0-15",
            allSignals(),
            (strength, registryAccess) -> SignalItemFactory.createContainer(Blocks.BLAST_FURNACE, strength, 3),
            "blast-furnace"
    ),
    SMOKER(
            "smoker",
            "Smoker",
            "0-15",
            allSignals(),
            (strength, registryAccess) -> SignalItemFactory.createContainer(Blocks.SMOKER, strength, 3)
    ),
    BREWING_STAND(
            "brewing_stand",
            "Brewing Stand",
            "0-15",
            allSignals(),
            (strength, registryAccess) -> SignalItemFactory.createContainer(Blocks.BREWING_STAND, strength, 5),
            "brewing-stand"
    ),
    LECTERN(
            "lectern",
            "Lectern",
            "0-15",
            allSignals(),
            SignalItemFactory::createLectern,
            "lecturn",
            "leckturn"
    ),
    CRAFTER(
            "crafter",
            "Crafter",
            "0-9",
            range(0, 9),
            SignalItemFactory::createCrafter
    ),
    COMPOSTER(
            "composter",
            "Composter",
            "0-6,8",
            strength -> strength >= 0 && strength <= 8 && strength != 7,
            SignalItemFactory::createComposter
    ),
    CAKE(
            "cake",
            "Cake",
            "2,4,6,8,10,12,14",
            strength -> strength >= 2 && strength <= 14 && strength % 2 == 0,
            SignalItemFactory::createCake
    ),
    BEEHIVE(
            "beehive",
            "Beehive",
            "0-5",
            range(0, 5),
            (strength, registryAccess) -> SignalItemFactory.createBeehive(Blocks.BEEHIVE, strength, registryAccess),
            "bee_hive",
            "bee-hive"
    ),
    BEE_NEST(
            "bee_nest",
            "Bee Nest",
            "0-5",
            range(0, 5),
            (strength, registryAccess) -> SignalItemFactory.createBeehive(Blocks.BEE_NEST, strength, registryAccess),
            "beenest",
            "bee-nest"
    ),
    RESPAWN_ANCHOR(
            "respawn_anchor",
            "Respawn Anchor",
            "0,3,7,11,15",
            strength -> strength == 0 || strength == 3 || strength == 7 || strength == 11 || strength == 15,
            SignalItemFactory::createRespawnAnchor,
            "respawn-anchor"
    ),
    CAULDRON(
            "cauldron",
            "Cauldron",
            "0-3",
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
            range(0, 3),
            SignalBlockVariant::createWaterCauldronBlock,
            "water-cauldron"
    ),
    POWDER_SNOW_CAULDRON(
            "powder_snow_cauldron",
            "Powder Snow Cauldron",
            "0-3",
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
    private final SignalItemFactory.ItemFactory itemFactory;
    private final IntFunction<BlockState> blockStateFactory;
    private final String[] aliases;

    SignalBlockVariant(
            String key,
            String displayName,
            String supportedStrengths,
            IntPredicate supports,
            SignalItemFactory.ItemFactory itemFactory,
            String... aliases
    ) {
        this.key = key;
        this.displayName = displayName;
        this.supportedStrengths = supportedStrengths;
        this.targetBlock = false;
        this.supports = supports;
        this.itemFactory = itemFactory;
        this.blockStateFactory = null;
        this.aliases = aliases;
    }

    SignalBlockVariant(
            String key,
            String displayName,
            String supportedStrengths,
            IntPredicate supports,
            IntFunction<BlockState> blockStateFactory,
            String... aliases
    ) {
        this.key = key;
        this.displayName = displayName;
        this.supportedStrengths = supportedStrengths;
        this.targetBlock = true;
        this.supports = supports;
        this.itemFactory = null;
        this.blockStateFactory = blockStateFactory;
        this.aliases = aliases;
    }

    public static Optional<SignalBlockVariant> find(String name) {
        return Optional.ofNullable(BY_NAME.get(normalize(name)));
    }

    public static String suggestions() {
        return Arrays.stream(values())
                .map(SignalBlockVariant::key)
                .collect(Collectors.joining(", "));
    }

    public static SignalBlockVariant[] orderedValues() {
        return values();
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public boolean targetBlock() {
        return targetBlock;
    }

    public boolean supports(int strength) {
        return supports.test(strength);
    }

    public ItemStack createItem(int strength, RegistryAccess registryAccess) {
        if (itemFactory == null) {
            throw new IllegalStateException(displayName + " is a target-block variant");
        }
        return itemFactory.create(strength, registryAccess);
    }

    public BlockState createBlockState(int strength) {
        if (blockStateFactory == null) {
            throw new IllegalStateException(displayName + " is an item variant");
        }
        return blockStateFactory.apply(strength);
    }

    public String unsupportedStrengthMessage() {
        return displayName + " supports signal strengths " + supportedStrengths;
    }

    public String supportedStrengths() {
        return supportedStrengths;
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

    private static BlockState createWaterCauldronBlock(int strength) {
        if (strength == 0) return Blocks.CAULDRON.defaultBlockState();
        return Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, strength);
    }

    private static BlockState createPowderSnowCauldronBlock(int strength) {
        if (strength == 0) return Blocks.CAULDRON.defaultBlockState();
        return Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, strength);
    }

    private static BlockState createLavaCauldronBlock(int strength) {
        if (strength == 0) return Blocks.CAULDRON.defaultBlockState();
        return Blocks.LAVA_CAULDRON.defaultBlockState();
    }

    private static String normalize(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("minecraft:")) normalized = normalized.substring("minecraft:".length());
        return normalized.replace('-', '_');
    }
}
