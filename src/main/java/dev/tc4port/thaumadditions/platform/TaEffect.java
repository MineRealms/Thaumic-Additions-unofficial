package dev.tc4port.thaumadditions.platform;

/**
 * The potion effects ThaumicAdditions gear applies.
 *
 * <p>{@code MobEffects.NIGHT_VISION} and friends are a {@code MobEffect} on
 * 1.20.1 but a {@code Holder<MobEffect>} on 1.21.1, and {@code hasEffect} /
 * {@code removeEffect} take whichever type the running version uses, so the
 * shared spec names the effect and each node resolves it.</p>
 */
public enum TaEffect {
    NIGHT_VISION,
    WATER_BREATHING,
    FIRE_RESISTANCE
}
