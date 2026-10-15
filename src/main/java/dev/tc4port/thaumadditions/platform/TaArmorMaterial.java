package dev.tc4port.thaumadditions.platform;

/**
 * Opaque per-node handle for an armor material.
 *
 * <p>1.20.1 passes a plain {@code ArmorMaterial} object to {@code ArmorItem};
 * 1.21.1 requires a registered {@code Holder<ArmorMaterial>} instead. Shared
 * code never inspects the handle — it only hands it back to the platform.</p>
 */
public interface TaArmorMaterial {
}
