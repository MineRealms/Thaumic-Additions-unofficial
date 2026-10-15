package dev.tc4port.hclite.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * Base class for HCLite packets. Instances encode themselves; decoding is a registered
 * factory. Handlers run on the main thread (the platform transport enqueues).
 *
 * <p>{@link #handleClient()} must never name client-only classes in THIS class or its
 * subclass declarations; do the work in a handler class that is only loaded when the method
 * actually runs on the client (see {@code ClientPacketHandlers}).
 */
public abstract class HCPacket {
    /** Serializes this packet. Always executed on the sending side. */
    public abstract void encode(FriendlyByteBuf buf);

    /** Server-side receive. Default: ignore. */
    public void handleServer(ServerPlayer player) {
    }

    /** Client-side receive. Default: ignore. */
    public void handleClient() {
    }
}
