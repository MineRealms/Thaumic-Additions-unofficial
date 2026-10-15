package dev.tc4port.hclite.platform;

import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * NBT boundary between the mappings of the two nodes. Bootstrapped by the loader binding;
 * every call site is loader-agnostic shared code.
 *
 * <p>The shared hclite source is compiled raw on the stonecutter-active node, so it must not
 * use {@code //?} conditionals - mapping drift lives behind this interface instead.
 */
public interface HCLitePlatform {
    /** Server side: full-metadata save of a block entity. */
    CompoundTag saveBlockEntityNbt(BlockEntity be);

    /** Client side: re-applies a server snapshot onto an existing block entity. */
    void loadBlockEntityNbt(BlockEntity be, CompoundTag tag);

    /** Reads a CompoundTag off the wire (1.20.1 readAnySizeNbt vs 1.21.1 readNbt). */
    CompoundTag readNbt(FriendlyByteBuf buf);

    /**
     * The item's custom NBT, or {@code null} when absent. 1.20.1 keeps it in
     * {@code ItemStack.getTag()}; 1.21 moved it to the {@code CustomData} component.
     */
    CompoundTag itemTag(ItemStack stack);

    /** Mutates the item's custom NBT, creating and attaching it when absent. */
    void updateItemTag(ItemStack stack, Consumer<CompoundTag> mutator);

    /**
     * Writes a block entity's item slots into {@code tag} under {@code key}.
     *
     * <p>Item stack serialisation is the other mapping-drift point: 1.20.1 saves a stack straight
     * into a tag, while 1.21 needs a registry lookup provider, which the node takes from the block
     * entity's level. Machines with an inventory go through here so the shared tile code never
     * names either shape.</p>
     */
    void saveItemStacks(BlockEntity be, CompoundTag tag, String key, net.minecraft.core.NonNullList<ItemStack> slots);

    /** Reads back what {@link #saveItemStacks} wrote, leaving the slots empty when absent. */
    void loadItemStacks(BlockEntity be, CompoundTag tag, String key, net.minecraft.core.NonNullList<ItemStack> slots);

    final class Holder {
        private static volatile HCLitePlatform platform;

        private Holder() {
        }

        public static void bootstrap(HCLitePlatform impl) {
            if (platform != null)
                throw new IllegalStateException("HCLitePlatform already bootstrapped");
            platform = impl;
        }

        public static HCLitePlatform get() {
            HCLitePlatform p = platform;
            if (p == null)
                throw new IllegalStateException("HCLitePlatform not bootstrapped");
            return p;
        }
    }
}
