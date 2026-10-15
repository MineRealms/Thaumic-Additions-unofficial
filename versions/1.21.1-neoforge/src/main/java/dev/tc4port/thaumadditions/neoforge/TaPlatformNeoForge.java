package dev.tc4port.thaumadditions.neoforge;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import dev.tc4port.thaumadditions.ThaumAdditions;
import dev.tc4port.thaumadditions.platform.TaArmorMaterial;
import dev.tc4port.thaumadditions.platform.TaArmorSound;
import dev.tc4port.thaumadditions.platform.TaGearBehavior;
import dev.tc4port.thaumadditions.platform.TaPlatform;
import dev.tc4port.thaumadditions.platform.ToolKind;
import dev.tc4port.thaumcraft.api.aspect.VisChannel;
import dev.tc4port.thaumcraft.api.item.GogglesOverlayGear;
import dev.tc4port.thaumcraft.api.item.VisDiscountGear;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 1.21.1 / NeoForge implementation of {@link TaPlatform}.
 *
 * <p>1.21.1 changed both contracts: {@code Tier} lost {@code getLevel()} and
 * gained {@code getIncorrectBlocksForDrops()}, tools take their attack
 * damage/speed through {@code Item.Properties.attributes(...)}, and
 * {@code ArmorMaterial} became a registered record that {@code ArmorItem}
 * consumes through a {@code Holder}.</p>
 */
public final class TaPlatformNeoForge implements TaPlatform {

    private static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, ThaumAdditions.MOD_ID);

    /** Wires the armor-material registry; call from the mod constructor. */
    public static void register(IEventBus modBus) {
        ARMOR_MATERIALS.register(modBus);
    }

    @Override
    public Tier toolTier(int level, int uses, float speed, float damageBonus, int enchantability,
                         TagKey<Block> incorrectForDrops, Supplier<Ingredient> repair) {
        // 1.21.1 has no tier "level"; it is expressed as the incorrect-for-drops tag, which the
        // shared spec already supplies, so the level argument is intentionally unused here.
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
            public TagKey<Block> getIncorrectBlocksForDrops() {
                return incorrectForDrops;
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
            case PICKAXE -> new PickaxeItem(tier, digger(properties, tier, attackDamage, attackSpeed)) {
                @Override
                public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
                    super.inventoryTick(stack, level, entity, slot, selected);
                    TaGearBehavior.selfRepair(stack, level, entity);
                }
            };
            case AXE -> new AxeItem(tier, digger(properties, tier, attackDamage, attackSpeed)) {
                @Override
                public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
                    super.inventoryTick(stack, level, entity, slot, selected);
                    TaGearBehavior.selfRepair(stack, level, entity);
                }
            };
            case SHOVEL -> new ShovelItem(tier, digger(properties, tier, attackDamage, attackSpeed)) {
                @Override
                public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
                    super.inventoryTick(stack, level, entity, slot, selected);
                    TaGearBehavior.selfRepair(stack, level, entity);
                }
            };
            case HOE -> new HoeItem(tier, digger(properties, tier, attackDamage, attackSpeed)) {
                @Override
                public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
                    super.inventoryTick(stack, level, entity, slot, selected);
                    TaGearBehavior.selfRepair(stack, level, entity);
                }
            };
            // 1.12 gave the adaminite sword 0 max damage (unbreakable) yet kept it enchantable;
            // 1.21.1's default isEnchantable() requires a damageable stack, so it is overridden.
            case SWORD -> new SwordItem(tier, properties.attributes(SwordItem.createAttributes(tier, attackDamage, attackSpeed))) {
                @Override
                public boolean isEnchantable(ItemStack stack) {
                    return true;
                }
            };
        };
    }

    private static Item.Properties digger(Item.Properties properties, Tier tier, float attackDamage, float attackSpeed) {
        return properties.attributes(DiggerItem.createAttributes(tier, attackDamage, attackSpeed));
    }

    @Override
    public TaArmorMaterial armorMaterial(String id, ResourceLocation texture, Map<ArmorItem.Type, Integer> defense,
                                         int durabilityMultiplier, int enchantability, TaArmorSound equipSound,
                                         Supplier<Ingredient> repair, float toughness, float knockbackResistance,
                                         Map<ArmorItem.Type, Integer> visDiscounts, boolean gogglesOverlay) {
        Holder<SoundEvent> sound = switch (equipSound) {
            case LEATHER -> SoundEvents.ARMOR_EQUIP_LEATHER;
            case IRON -> SoundEvents.ARMOR_EQUIP_IRON;
            case GOLD -> SoundEvents.ARMOR_EQUIP_GOLD;
            case CHAIN -> SoundEvents.ARMOR_EQUIP_CHAIN;
        };
        DeferredHolder<ArmorMaterial, ArmorMaterial> holder = ARMOR_MATERIALS.register(id, () -> new ArmorMaterial(
                defense, enchantability, sound, repair,
                List.of(new ArmorMaterial.Layer(texture)), toughness, knockbackResistance));
        return new Material(holder, visDiscounts, gogglesOverlay);
    }

    @Override
    public Item armor(TaArmorMaterial material, ArmorItem.Type type, int durabilityMultiplier, Item.Properties properties) {
        Material handle = (Material) material;
        return new Piece(handle.material(), type, properties.durability(type.getDurability(durabilityMultiplier)),
                handle.visDiscounts(), handle.gogglesOverlay());
    }

    /** Opaque handle: the registered material holder plus the gear hooks it hands to each piece. */
    private record Material(Holder<ArmorMaterial> material, Map<ArmorItem.Type, Integer> visDiscounts,
                            boolean gogglesOverlay) implements TaArmorMaterial {
    }

    private static final class Piece extends ArmorItem implements VisDiscountGear, GogglesOverlayGear {
        private final Map<ArmorItem.Type, Integer> visDiscounts;
        private final boolean gogglesOverlay;

        private Piece(Holder<ArmorMaterial> material, ArmorItem.Type type, Item.Properties properties,
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
