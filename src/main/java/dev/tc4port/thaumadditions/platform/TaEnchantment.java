package dev.tc4port.thaumadditions.platform;

/**
 * Opaque handle for one enchantment.
 *
 * <p>1.20.1 hands around an {@code Enchantment} and reaches it through the static
 * {@code BuiltInRegistries.ENCHANTMENT}; 1.21.1 makes enchantments data-driven, hands around a
 * {@code Holder<Enchantment>} and reaches them through the level's registry access. Every operation
 * the shadow enchanter needs — compatibility, applicability, level bounds, and writing the
 * enchantment onto a stack — takes the handle on a different type in the two versions, so the
 * shared code names none of them.</p>
 */
public interface TaEnchantment {
}
