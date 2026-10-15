package dev.tc4port.hclite;

/**
 * HCLite — in-repo reimplementation of the HammerCore essentials Thaumic Additions leaned on
 * (synced block entities, simple networking, GUI routing, device block bases, small utils).
 *
 * <p>Shipped as a separate mod embedded into the release jar via jar-in-jar. Loader bindings
 * live in {@code versions/<node>/src/hclite/java/.../impl}.
 */
public final class HCLite {
    public static final String MOD_ID = "hclite";
    public static final String VERSION = "1.0.0";

    private HCLite() {
    }
}
