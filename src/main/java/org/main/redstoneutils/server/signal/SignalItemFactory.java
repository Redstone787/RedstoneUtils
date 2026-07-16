package org.main.redstoneutils.server.signal;

import com.mojang.serialization.DynamicOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SignalItemFactory {

    private static final int OPTIMAL_SLOT_COUNT = 27;
    private static final Item UNSTACKABLE_ITEM = Items.WOODEN_SHOVEL;
    private static final Item STACKABLE_ITEM = Items.REDSTONE;
    private static final int UNSTACKABLE_MAX_STACK_SIZE = 1;
    private static final int STACKABLE_MAX_STACK_SIZE = 64;

    private SignalItemFactory() {
    }

    public static ItemStack createOptimal(int strength, RegistryAccess registryAccess) {
        if ((strength >= 0 && strength <= 6) || strength == 8) return createComposter(strength, registryAccess);
        if (strength == 10 || strength == 12 || strength == 14) return createCake(strength, registryAccess);
        if (strength == 7 || strength == 11 || strength == 15) return createRespawnAnchor(strength, registryAccess);
        return createLectern(strength, registryAccess);
    }

    static ItemStack createBarrel(int strength, RegistryAccess registryAccess) {
        return createContainer(Blocks.BARREL, strength, OPTIMAL_SLOT_COUNT);
    }

    static ItemStack createContainer(Block block, int strength, int slotCount) {
        if (slotCount >= ComparatorSignal.MAX - 1) {
            return createContainer(block, strength, slotCount, UNSTACKABLE_ITEM, UNSTACKABLE_MAX_STACK_SIZE);
        }

        return createContainer(block, strength, slotCount, STACKABLE_ITEM, STACKABLE_MAX_STACK_SIZE);
    }

    static ItemStack createCrafter(int strength, RegistryAccess registryAccess) {
        return createContainerWithAmount(Blocks.CRAFTER, strength, 9, UNSTACKABLE_ITEM, UNSTACKABLE_MAX_STACK_SIZE);
    }

    static ItemStack createLectern(int strength, RegistryAccess registryAccess) {
        ItemStack lectern = new ItemStack(Blocks.LECTERN);
        if (strength == ComparatorSignal.MIN) return lectern;

        lectern.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(LecternBlock.HAS_BOOK, true));

        CompoundTag blockEntityTag = new CompoundTag();
        Tag bookTag = encodeItemStack(createWrittenBook(), registryAccess);
        if (bookTag instanceof CompoundTag compoundTag) {
            blockEntityTag.put("Book", compoundTag);
            blockEntityTag.putInt("Page", strength - 1);

            BlockEntityType<?> lecternType = BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("lectern"));
            if (lecternType != null) {
                lectern.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(lecternType, blockEntityTag));
            }
        }

        return lectern;
    }

    static ItemStack createComposter(int strength, RegistryAccess registryAccess) {
        return createBlockStateItem(Blocks.COMPOSTER, ComposterBlock.LEVEL, strength);
    }

    static ItemStack createCake(int strength, RegistryAccess registryAccess) {
        int bites = 7 - strength / 2;
        return createBlockStateItem(Blocks.CAKE, CakeBlock.BITES, bites);
    }

    static ItemStack createBeehive(Block block, int strength, RegistryAccess registryAccess) {
        return createBlockStateItem(block, net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL, strength);
    }

    static ItemStack createRespawnAnchor(int strength, RegistryAccess registryAccess) {
        int charges = switch (strength) {
            case 0 -> 0;
            case 3 -> 1;
            case 7 -> 2;
            case 11 -> 3;
            case 15 -> 4;
            default -> throw new IllegalArgumentException("Unsupported respawn anchor signal " + strength);
        };
        return createBlockStateItem(Blocks.RESPAWN_ANCHOR, RespawnAnchorBlock.CHARGE, charges);
    }

    private static ItemStack createContainer(Block block, int strength, int slotCount, Item item, int itemMaxStackSize) {
        int amount = itemMaxStackSize == UNSTACKABLE_MAX_STACK_SIZE
                ? ComparatorSignal.filledSlotsForSignal(strength, slotCount)
                : ComparatorSignal.amountForSignal(strength, slotCount, itemMaxStackSize);

        return createContainerWithAmount(block, amount, slotCount, item, itemMaxStackSize);
    }

    private static ItemStack createContainerWithAmount(Block block, int amount, int slotCount, Item item, int itemMaxStackSize) {
        ItemStack stack = new ItemStack(block);
        if (amount <= 0) return stack;

        List<ItemStack> contents = new ArrayList<>(Collections.nCopies(slotCount, ItemStack.EMPTY));
        int remaining = amount;
        for (int slot = 0; slot < slotCount && remaining > 0; slot++) {
            int stackSize = Math.min(remaining, itemMaxStackSize);
            contents.set(slot, new ItemStack(item, stackSize));
            remaining -= stackSize;
        }

        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        return stack;
    }

    private static <T extends Comparable<T>> ItemStack createBlockStateItem(Block block, Property<T> property, T value) {
        ItemStack stack = new ItemStack(block);
        stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(property, value));
        return stack;
    }

    private static ItemStack createWrittenBook() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> pages = new ArrayList<>();
        for (int page = 1; page <= ComparatorSignal.MAX; page++) {
            pages.add(Filterable.passThrough(Component.literal(Integer.toString(page))));
        }

        book.set(
                DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(Filterable.passThrough("Signal"), "RedstoneUtils", 0, pages, true)
        );
        return book;
    }

    private static Tag encodeItemStack(ItemStack stack, RegistryAccess registryAccess) {
        DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registryAccess);
        return ItemStack.CODEC.encodeStart(ops, stack).result().orElse(null);
    }

    interface ItemFactory {
        ItemStack create(int strength, RegistryAccess registryAccess);
    }
}
