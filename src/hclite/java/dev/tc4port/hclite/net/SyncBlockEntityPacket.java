package dev.tc4port.hclite.net;

import java.util.function.Function;

import dev.tc4port.hclite.platform.HCLitePlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Server -> client full-NBT resync of a {@link dev.tc4port.hclite.tile.SyncedBlockEntity}.
 * The server captures the block entity at construction (encode runs on the server); the client
 * re-applies the snapshot onto its own instance.
 */
public final class SyncBlockEntityPacket extends HCPacket {
    private final BlockPos pos;
    private final CompoundTag tag;

    /** Decode constructor (client side). */
    public SyncBlockEntityPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.tag = HCLitePlatform.Holder.get().readNbt(buf);
    }

    /** Server-side constructor; {@code be} must have a level (encode runs on the server). */
    public SyncBlockEntityPacket(BlockEntity be) {
        this.pos = be.getBlockPos();
        this.tag = HCLitePlatform.Holder.get().saveBlockEntityNbt(be);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeNbt(tag);
    }

    public static Function<FriendlyByteBuf, SyncBlockEntityPacket> decoder() {
        return SyncBlockEntityPacket::new;
    }

    @Override
    public void handleClient() {
        // Deferred to a class only loaded when this runs on a client, so a dedicated server
        // never resolves client-only classes through this packet's declaration.
        ClientPacketHandlers.resyncBlockEntity(pos, tag);
    }
}
