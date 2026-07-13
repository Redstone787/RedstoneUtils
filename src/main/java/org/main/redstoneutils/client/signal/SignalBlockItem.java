package org.main.redstoneutils.client.signal;

final class SignalBlockItem {

    private static final String OPTIMAL_BLOCK_ID = "minecraft:barrel";
    private static final int OPTIMAL_SLOT_COUNT = 27;
    private static final String UNSTACKABLE_ITEM_ID = "minecraft:wooden_shovel";
    private static final String STACKABLE_ITEM_ID = "minecraft:redstone";
    private static final int UNSTACKABLE_MAX_STACK_SIZE = 1;
    private static final int STACKABLE_MAX_STACK_SIZE = 64;

    private SignalBlockItem() {
    }

    static String createOptimal(int strength) {
        if ((strength >= 0 && strength <= 6) || strength == 8) return createComposter(strength);
        if (strength == 10 || strength == 12 || strength == 14) return createCake(strength);
        if (strength == 7 || strength == 11 || strength == 15) return createRespawnAnchor(strength);
        return createLectern(strength);
    }

    static String createBarrel(int strength) {
        return createContainer(OPTIMAL_BLOCK_ID, strength, OPTIMAL_SLOT_COUNT, UNSTACKABLE_ITEM_ID, UNSTACKABLE_MAX_STACK_SIZE);
    }

    static String createContainer(String blockId, int strength, int slotCount) {
        if (slotCount >= ComparatorSignal.MAX - 1) {
            return createContainer(blockId, strength, slotCount, UNSTACKABLE_ITEM_ID, UNSTACKABLE_MAX_STACK_SIZE);
        }

        return createContainer(blockId, strength, slotCount, STACKABLE_ITEM_ID, STACKABLE_MAX_STACK_SIZE);
    }

    static String createCrafter(int strength) {
        return createContainerWithAmount("minecraft:crafter", strength, 9, UNSTACKABLE_ITEM_ID, UNSTACKABLE_MAX_STACK_SIZE);
    }

    static String createLectern(int strength) {
        if (strength == ComparatorSignal.MIN) return "minecraft:lectern";

        return "minecraft:lectern["
                + "minecraft:block_state={has_book:\"true\"},"
                + "minecraft:block_entity_data={id:\"minecraft:lectern\",Book:"
                + createWrittenBookStack()
                + ",Page:"
                + (strength - 1)
                + "}"
                + "]";
    }

    static String createComposter(int strength) {
        return createBlockStateItem("minecraft:composter", "level", strength);
    }

    static String createCake(int strength) {
        int bites = 7 - strength / 2;
        return createBlockStateItem("minecraft:cake", "bites", bites);
    }

    static String createBeehive(String blockId, int strength) {
        return createBlockStateItem(blockId, "honey_level", strength);
    }

    static String createRespawnAnchor(int strength) {
        int charges = switch (strength) {
            case 0 -> 0;
            case 3 -> 1;
            case 7 -> 2;
            case 11 -> 3;
            case 15 -> 4;
            default -> throw new IllegalArgumentException("Unsupported respawn anchor signal " + strength);
        };
        return createBlockStateItem("minecraft:respawn_anchor", "charges", charges);
    }

    private static String createContainer(String blockId, int strength, int slotCount, String itemId, int itemMaxStackSize) {
        int amount = itemMaxStackSize == UNSTACKABLE_MAX_STACK_SIZE
                ? ComparatorSignal.filledSlotsForSignal(strength, slotCount)
                : ComparatorSignal.amountForSignal(strength, slotCount, itemMaxStackSize);

        return createContainerWithAmount(blockId, amount, slotCount, itemId, itemMaxStackSize);
    }

    private static String createContainerWithAmount(String blockId, int amount, int slotCount, String itemId, int itemMaxStackSize) {
        StringBuilder itemString = new StringBuilder(blockId).append("[minecraft:container=[");
        int remaining = amount;

        for (int slot = 0; slot < slotCount && remaining > 0; slot++) {
            if (slot > 0) itemString.append(',');

            int stackSize = Math.min(remaining, itemMaxStackSize);
            itemString
                    .append("{slot:")
                    .append(slot)
                    .append(",item:{id:\"")
                    .append(itemId)
                    .append("\",count:")
                    .append(stackSize)
                    .append("}}");
            remaining -= stackSize;
        }

        return itemString.append("]]").toString();
    }

    private static String createBlockStateItem(String blockId, String property, int value) {
        return blockId + "[minecraft:block_state={" + property + ":\"" + value + "\"}]";
    }

    private static String createWrittenBookStack() {
        StringBuilder pages = new StringBuilder("[");
        for (int page = 1; page <= ComparatorSignal.MAX; page++) {
            if (page > 1) pages.append(',');
            pages.append("'{\"text\":\"").append(page).append("\"}'");
        }
        pages.append(']');

        return "{id:\"minecraft:written_book\",count:1,components:{"
                + "\"minecraft:written_book_content\":{"
                + "title:\"Signal\","
                + "author:\"RedstoneUtils\","
                + "pages:"
                + pages
                + ",generation:0"
                + "}"
                + "}}";
    }
}
