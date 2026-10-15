package dev.tc4port.thaumadditions.forge;

import java.util.Map;
import java.util.function.Supplier;

import dev.tc4port.thaumadditions.platform.TaArmorMaterial;
import dev.tc4port.thaumadditions.platform.TaArmorSound;
import dev.tc4port.thaumadditions.platform.TaGearBehavior;
import dev.tc4port.thaumadditions.platform.TaPlatform;
import dev.tc4port.thaumadditions.platform.ToolKind;
import dev.tc4port.thaumcraft.api.aspect.VisChannel;
import dev.tc4port.thaumcraft.api.item.GogglesOverlayGear;
import dev.tc4port.thaumcraft.api.item.VisDiscountGear;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 1.20.1 / Forge implementation of {@link TaPlatform}.
 *
 * <p>1.20.1 keeps the pre-1.21 contracts: {@code Tier} is a hand-written
 * interface with {@code getLevel()}, tools take attack damage and speed as
 * constructor arguments, and {@code ArmorMaterial} is an unregistered object
 * whose {@code getName()} is the armor texture base.</p>
 */
public final class TaPlatformForge implements TaPlatform {

    @Override
    public Tier toolTier(int level, int uses, float speed, float damageBonus, int enchantability,
                         TagKey<Block> incorrectForDrops, Supplier<Ingredient> repair) {
        return new Tier() {
            @Override
            public int getUses() {
                return uses;
            }

            @Override
            public float getSpeed() {
                return speed;
            }

            @Override
            public float getAttackDamageBonus() {
                return damageBonus;
            }

            @Override
            public int getLevel() {
                return level;
            }

            @Override
            public int getEnchantmentValue() {
                return enchantability;
            }

            @Override
            public Ingredient getRepairIngredient() {
                return repair.get();
            }
        };
    }

    @Override
    public Item tool(ToolKind kind, Tier tier, float attackDamage, float attackSpeed, Item.Properties properties) {
        return switch (kind) {
            case PICKAXE -> new PickaxeItem(tier, (int) attackDamage, attackSpeed, properties) {
                @Override
                public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
                    super.inventoryTick(stack, level, entity, slot, selected);
                    TaGearBehavior.selfRepair(stack, level, entity);
                }
            };
            case AXE -> new AxeItem(tier, attackDamage, attackSpeed, properties) {
                @Override
                public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
                    super.inventoryTick(stack, level, entity, slot, selected);
                    TaGearBehavior.selfRepair(stack, level, entity);
                }
            };
            case SHOVEL -> new ShovelItem(tier, attackDamage, attackSpeed, properties) {
                @Override
                public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
                    super.inventoryTick(stack, level, entity, slot, selected);
                    TaGearBehavior.selfRepair(stack, level, entity);
                }
            };
            case HOE -> new HoeItem(tier, (int) attackDamage, attackSpeed, properties) {
                @Override
                public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
                    super.inventoryTick(stack, level, entity, slot, selected);
                    TaGearBehavior.selfRepair(stack, level, entity);
                }
            };
            // 1.12 gave the adaminite sword 0 max damage (unbreakable) yet kept it enchantable;
            // 1.20.1's default isEnchantable() requires a damageable stack, so it is overridden.
            case SWORD -> new SwordItem(tier, (int) attackDamage, attackSpeed, properties) {
                @Override
                public boolean isEnchantable(ItemStack stack) {
                    return true;
                }
            };
        };
    }

    @Override
    public TaArmorMaterial armorMaterial(String id, ResourceLocation texture, Map<ArmorItem.Type, Integer> defense,
                                         int durabilityMultiplier, int enchantability, TaArmorSound equipSound,
                                         Supplier<Ingredient> repair, float toughness, float knockbackResistance,
                                         Map<ArmorItem.Type, Integer> visDiscounts, boolean gogglesOverlay) {
        SoundEvent sound = switch (equipSound) {
            case LEATHER -> SoundEvents.ARMOR_EQUIP_LEATHER;
            case IRON -> SoundEvents.ARMOR_EQUIP_IRON;
            case GOLD -> SoundEvents.ARMOR_EQUIP_GOLD;
            case CHAIN -> SoundEvents.ARMOR_EQUIP_CHAIN;
        };
        ArmorMaterial material = new ArmorMaterial() {
            @Override
            public int getDurabilityForType(ArmorItem.Type type) {
                return switch (type) {
                    case HELMET -> 11 * durabilityMultiplier;
                    case CHESTPLATE -> 16 * durabilityMultiplier;
                    case LEGGINGS -> 15 * durabilityMultiplier;
                    case BOOTS -> 13 * durabilityMultiplier;
                };
            }

            @Override
            public int getDefenseForType(ArmorItem.Type type) {
                return defense.getOrDefault(type, 0);
            }

            @Override
            public int getEnchantmentValue() {
                return enchantability;
            }

            @Override
            public SoundEvent getEquipSound() {
                return sound;
            }

            @Override
            public Ingredient getRepairIngredient() {
                return repair.get();
            }

            @Override
            public String getName() {
                // Vanilla splits this on ':' and resolves <ns>:textures/models/armor/<path>_layer_N.png.
                return texture.toString();
            }

            @Override
            public float getToughness() {
                return toughness;
            }

            @Override
            public float getKnockbackResistance() {
                return knockbackResistance;
            }
        };
        return new Material(material, visDiscounts, gogglesOverlay);
    }

    @Override
    public Item armor(TaArmorMaterial material, ArmorItem.Type type, int durabilityMultiplier, Item.Properties properties) {
        Material handle = (Material) material;
        return new Piece(handle.material(), type, properties, handle.visDiscounts(), handle.gogglesOverlay());
    }

    /** Opaque handle: the 1.20.1 material object plus the gear hooks it hands to each piece. */
    private record Material(ArmorMaterial material, Map<ArmorItem.Type, Integer> visDiscounts,
                            boolean gogglesOverlay) implements TaArmorMaterial {
    }

    private static final class Piece extends ArmorItem implements VisDiscountGear, GogglesOverlayGear {
        private final Map<ArmorItem.Type, Integer> visDiscounts;
        private final boolean gogglesOverlay;

        private Piece(ArmorMaterial material, ArmorItem.Type type, Item.Properties properties,
                      Map<ArmorItem.Type, Integer> visDiscounts, boolean gogglesOverlay) {
            super(material, type, properties);
            this.visDiscounts = visDiscounts;
            this.gogglesOverlay = gogglesOverlay;
        }

        @Override
        public int thaumcraftVisDiscount(ItemStack stack, Player wearer, VisChannel channel) {
            return visDiscounts.getOrDefault(getType(), 0);
        }

        @Override
        public boolean showsThaumcraftGogglesOverlay(ItemStack stack, Player wearer) {
            return gogglesOverlay && getType() == ArmorItem.Type.HELMET;
        }
    }
}
