package dev.tc4port.thaumadditions.platform;

/**
 * The equip sounds ThaumicAdditions armor uses.
 *
 * <p>{@code SoundEvents.ARMOR_EQUIP_*} is a {@code SoundEvent} on 1.20.1 but a
 * {@code Holder<SoundEvent>} on 1.21.1, so the shared spec names the sound and
 * each node resolves it to whatever its version expects.</p>
 */
public enum TaArmorSound {
    LEATHER,
    IRON,
    GOLD,
    CHAIN
}
