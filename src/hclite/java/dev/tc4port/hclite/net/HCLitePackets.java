package dev.tc4port.hclite.net;

/**
 * The packet catalogue HCLite ships.
 *
 * <p>{@link PacketChannel#register} assigns wire ids in call order and rejects a mismatch, so this
 * is the single place the ids are decided. Every loader binding calls {@link #registerAll} right
 * after {@link HCLiteNet#bootstrap}, before any packet traffic.</p>
 */
public final class HCLitePackets {
    private HCLitePackets() {
    }

    /** Registers every HCLite packet exactly once. */
    public static void registerAll() {
        HCLiteNet.channel().register(0, SyncBlockEntityPacket.class, SyncBlockEntityPacket.decoder());
    }
}
