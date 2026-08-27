package org.main.redstoneutils.client.signal;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.Property;

final class ContainerBlockArgument {

    private ContainerBlockArgument() {
    }

    static String create(Minecraft client, BlockState blockState, BlockEntity blockEntity, Container container, ItemStack itemStack, int amount) {
        return createBlockStateString(blockState) + createContainerTag(client, blockEntity, container, itemStack, amount);
    }

    static int slotMaxStackSize(Container container, ItemStack itemStack) {
        if (itemStack.isEmpty()) return 0;

        return Math.clamp(itemStack.getMaxStackSize(), 0, container.getMaxStackSize(itemStack));
    }

    private static String createBlockStateString(BlockState blockState) {
        StringBuilder blockStateString = new StringBuilder(BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString());
        if (blockState.getProperties().isEmpty()) return blockStateString.toString();

        blockStateString.append('[');
        boolean first = true;
        for (Property<?> property : blockState.getProperties()) {
            if (!first) blockStateString.append(',');
            first = false;

            blockStateString
                    .append(property.getName())
                    .append('=')
                    .append(getPropertyValueName(blockState, property));
        }

        return blockStateString.append(']').toString();
    }

    private static <T extends Comparable<T>> String getPropertyValueName(BlockState blockState, Property<T> property) {
        return property.getName(blockState.getValue(property));
    }

    private static String createContainerTag(Minecraft client, BlockEntity blockEntity, Container container, ItemStack itemStack, int amount) {
        assert client.level != null;
        CompoundTag tag = blockEntity.saveWithoutMetadata(client.level.registryAccess());
        ListTag items = new ListTag();
        int remaining = amount;
        int slotMaxStackSize = slotMaxStackSize(container, itemStack);

        for (int slot = 0; slot < container.getContainerSize() && remaining > 0 && slotMaxStackSize > 0; slot++) {
            int stackSize = Math.min(remaining, slotMaxStackSize);
            items.add(createItemStackTag(client, itemStack, slot, stackSize));
            remaining -= stackSize;
        }

        tag.put("Items", items);
        return tag.toString();
    }

    private static CompoundTag createItemStackTag(Minecraft client, ItemStack itemStack, int slot, int count) {
        ItemStack stack = itemStack.copyWithCount(count);
        assert client.level != null;
        Tag encodedTag = ItemStack.CODEC
                .encodeStart(RegistryOps.create(NbtOps.INSTANCE, client.level.registryAccess()), stack)
                .result()
                .orElse(null);

        if (encodedTag instanceof CompoundTag compoundTag) {
            compoundTag.putByte("Slot", (byte) slot);
            return compoundTag;
        }

        return createFallbackItemStackTag(itemStack, slot, count);
    }

    private static CompoundTag createFallbackItemStackTag(ItemStack itemStack, int slot, int count) {
        CompoundTag tag = new CompoundTag();
        tag.putByte("Slot", (byte) slot);
        tag.putString("id", BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString());
        tag.putInt("count", count);
        return tag;
    }
}
