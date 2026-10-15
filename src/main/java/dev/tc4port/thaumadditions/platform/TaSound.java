package dev.tc4port.thaumadditions.platform;

/**
 * The sound cues the machines share, resolved per node.
 *
 * <p>{@code SoundEvents.GENERIC_EXPLODE} is a {@code SoundEvent} on 1.20.1 and a
 * {@code Holder<SoundEvent>} on 1.21.1 — the same drift as {@link TaArmorSound} — so the shared
 * code names the cue and the node picks the constant.</p>
 */
public enum TaSound {
    /** 1.12 {@code TileAdaminiteJar}: {@code SoundEvents.ENTITY_GENERIC_EXPLODE}. */
    EXPLODE
}
