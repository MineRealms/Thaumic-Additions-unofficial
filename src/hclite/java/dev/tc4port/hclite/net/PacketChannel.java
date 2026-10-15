package dev.tc4port.hclite.net;

import java.util.function.Function;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Platform transport for HCLite packets. Implemented per node (Forge SimpleChannel,
 * NeoForge PayloadRegistrar); every implementation enqueues handlers onto the main thread.
 */
public interface PacketChannel {
    /** Registers a packet type under a fixed numeric id. Ids are wire-format: append only. */
    <T extends HCPacket> void register(int id, Class<T> type, Function<FriendlyByteBuf, T> decoder);

    void sendToServer(HCPacket packet);

    void sendToPlayer(ServerPlayer player, HCPacket packet);

    /** Sends to every player tracking the given block entity's chunk. */
    void sendToTracking(BlockEntity be, HCPacket packet);
}
