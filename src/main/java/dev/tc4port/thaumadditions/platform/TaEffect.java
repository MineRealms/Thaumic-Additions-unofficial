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
    FIRE_RESISTANCE,
    /** 1.12 belt/amulet speed boost, reimplemented as a status effect. */
    MOVEMENT_SPEED,
    /** 1.12 belt jump boost, reimplemented as a status effect. */
    JUMP,
    /** 1.12 meteor belt dive / traveller-belt fall damping. */
    SLOW_FALLING,
    /** TC4R's warp ward, applied by the fragnant pendant. */
    WARP_WARD,

    // The rest are what 1.12's EdibleAspect handed out when a player ate salt essence, one per
    // aspect. They are named here rather than at the call site for the same reason as the rest:
    // 1.20.1 spells them MobEffect and 1.21.1 spells them Holder<MobEffect>.
    SLOWNESS,
    BLINDNESS,
    NAUSEA,
    STRENGTH,
    HASTE,
    HUNGER,
    RESISTANCE,
    /** TC4R's flux taint, the effect 1.12's FLUX aspect applied. */
    FLUX_TAINT
}
