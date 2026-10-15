package dev.tc4port.thaumadditions.platform;

import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/**
 * The version/loader abstraction for everything whose vanilla API drifted
 * between 1.20.1 and 1.21.1.
 *
 * <p>Tool tiers and armor materials are the two areas where the vanilla
 * contracts are genuinely incompatible: {@code Tier} gained
 * {@code getIncorrectBlocksForDrops()} and lost {@code getLevel()};
 * {@code ArmorMaterial} became a registered record and {@code ArmorItem}
 * now takes a {@code Holder}. Everything else (item classes, attack damage,
 * armor durability) is supplied per-node behind this port so the shared
 * content declarations stay loader-agnostic.</p>
 *
 * <p>Attack damage is expressed 1.12-style: the tier carries the material
 * bonus and {@code attackDamage} is the per-shape modifier added on top, so
 * {@code SwordItem} totals {@code attackDamage + damageBonus}.</p>
 */
public interface TaPlatform {
    /** Builds a tool {@link Tier} from 1.12-style raw material values. */
    Tier toolTier(int level, int uses, float speed, float damageBonus, int enchantability,
                  TagKey<Block> incorrectForDrops, Supplier<Ingredient> repair);

    /** Creates a tool of the given shape. */
    Item tool(ToolKind kind, Tier tier, float attackDamage, float attackSpeed, Item.Properties properties);

    /** Builds (1.20.1) or registers (1.21.1) an armor material and returns an opaque handle. */
    TaArmorMaterial armorMaterial(String id, ResourceLocation texture, Map<ArmorItem.Type, Integer> defense,
                                  int durabilityMultiplier, int enchantability, TaArmorSound equipSound,
                                  Supplier<Ingredient> repair, float toughness, float knockbackResistance,
                                  Map<ArmorItem.Type, Integer> visDiscounts, boolean gogglesOverlay);

    /** Creates one armor piece for a material built by {@link #armorMaterial}. */
    Item armor(TaArmorMaterial material, ArmorItem.Type type, int durabilityMultiplier, Item.Properties properties,
               TaArmorTick behavior);

    /** Applies or refreshes a potion effect (ambient, no particles — the 1.12 armor style). */
    void applyEffect(LivingEntity target, TaEffect effect, int durationTicks, int amplifier);

    boolean hasEffect(LivingEntity target, TaEffect effect);

    void removeEffect(LivingEntity target, TaEffect effect);

    /** Removes every harmful effect, the mithminite full-set cleanse. */
    void clearHarmfulEffects(LivingEntity target);
}
