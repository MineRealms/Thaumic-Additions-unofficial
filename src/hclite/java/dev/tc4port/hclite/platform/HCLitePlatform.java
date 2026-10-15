package dev.tc4port.hclite.platform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
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
