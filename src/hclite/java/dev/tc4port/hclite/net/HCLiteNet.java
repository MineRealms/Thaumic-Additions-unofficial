package dev.tc4port.hclite.net;

import dev.tc4port.hclite.HCLite;

/** Bootstrap holder for the platform {@link PacketChannel} implementation. */
public final class HCLiteNet {
    private static volatile PacketChannel channel;

    private HCLiteNet() {
    }

    /** Called by the loader binding before any packet traffic. */
    public static void bootstrap(PacketChannel impl) {
        if (channel != null)
            throw new IllegalStateException("HCLiteNet already bootstrapped");
        channel = impl;
    }

    public static PacketChannel channel() {
        PacketChannel c = channel;
        if (c == null)
            throw new IllegalStateException("HCLiteNet not bootstrapped by " + HCLite.MOD_ID + " binding");
        return c;
    }
}
