package dev.tc4port.thaumadditions.platform;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.network.chat.Component;

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
    /**
     * The active node's platform. Access it as {@code TaPlatform.Active}.
     *
     * <p>Registration passes the port around explicitly, but block entities are constructed by the
     * loader long after that and have no way to reach it. This is the same holder pattern the
     * HCLite core uses.</p>
     */
    final class Active {
        private static TaPlatform instance;

        private Active() {
        }

        /** Called once from the loader adapter's mod constructor. */
        public static void set(TaPlatform platform) {
            instance = platform;
        }

        public static TaPlatform get() {
            return instance;
        }
    }
    /** Builds a tool {@link Tier} from 1.12-style raw material values. */
    Tier toolTier(int level, int uses, float speed, float damageBonus, int enchantability,
                  TagKey<Block> incorrectForDrops, Supplier<Ingredient> repair);

    /** Creates a tool of the given shape. */
    Item tool(ToolKind kind, Tier tier, float attackDamage, float attackSpeed, Item.Properties properties);

    /**
     * Creates a sword-class item that runs shared logic on every hit.
     *
     * <p>The 1.12 weapons {@code the_beheader} (20% head drops) and
     * {@code mithminite_scythe} (+3 reach sweep) both need {@code hurtEnemy},
     * but a shared {@code SwordItem} subclass cannot be constructed: the
     * constructor signature differs per node. Each node therefore supplies its
     * own subclass and delegates to this hook.</p>
     */
    Item sword(Tier tier, float attackDamage, float attackSpeed, Item.Properties properties,
               SwordBehavior behavior);

    /** Shared {@code hurtEnemy} hook for {@link #sword}. */
    @FunctionalInterface
    interface SwordBehavior {
        /** @return the {@code super.hurtEnemy} result, or {@code false} to skip damage */
        boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker);
    }

    /** Builds (1.20.1) or registers (1.21.1) an armor material and returns an opaque handle. */
    TaArmorMaterial armorMaterial(String id, ResourceLocation texture, Map<ArmorItem.Type, Integer> defense,
                                  int durabilityMultiplier, int enchantability, TaArmorSound equipSound,
                                  Supplier<Ingredient> repair, float toughness, float knockbackResistance,
                                  Map<ArmorItem.Type, Integer> visDiscounts, boolean gogglesOverlay);

    /** Creates one armor piece for a material built by {@link #armorMaterial}. */
    Item armor(TaArmorMaterial material, ArmorItem.Type type, int durabilityMultiplier, Item.Properties properties,
               TaArmorTick behavior);

    /**
     * Creates an item whose drifted {@link Item} hooks are forwarded to
     * {@link dev.tc4port.thaumadditions.items.ItemBehavior}.
     *
     * <p>This is the single place the 1.20.1 vs 1.21.1 item-method drift is
     * absorbed: {@code appendHoverText} took a {@code Level} on 1.20.1 and an
     * {@code Item.TooltipContext} on 1.21.1. Every ported item is written against
     * {@code ItemBehavior} instead of overriding those methods directly.</p>
     */
    Item behaviorItem(Item.Properties properties, dev.tc4port.thaumadditions.items.ItemBehavior behavior);

    /**
     * Builds the 1.12 void-fruit food properties. {@code FoodProperties.Builder}
     * spells the always-edible flag {@code alwaysEat} on 1.20.1 and
     * {@code alwaysEdible} on 1.21.1, so it lives on the port too.
     */
    net.minecraft.world.food.FoodProperties food(int nutrition, float saturationModifier, boolean alwaysEdible);

    /**
     * Creates a crop block with vanilla bonemeal disabled, as both 1.12 crops were.
     * The bonemeal hooks changed arity between versions, so this lives on the port.
     */
    Block crop(BlockBehaviour.Properties properties, int maxAge);

    /**
     * The block item for a machine that draws its own inventory icon.
     *
     * <p>Which loader registers the icon differs — NeoForge 1.21.1 through a client extension
     * event, Forge 1.20.1 through {@code Item#initializeClient} — so the item itself comes from
     * the node. See {@link MachineIcon}.</p>
     */
    net.minecraft.world.item.Item machineItem(Block block, MachineIcon icon);

    /**
     * Plays one of the port's shared sound cues at a block position.
     *
     * <p>Same drift {@link TaArmorSound} covers: some {@code SoundEvents} constants are a
     * {@code SoundEvent} on 1.20.1 and a {@code Holder<SoundEvent>} on 1.21.1, so the shared code
     * names the cue and the node resolves it.</p>
     */
    void playSound(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
                   TaSound sound, float volume, float pitch);


    /**
     * Creates the puriflower bush. 1.21 made {@code BushBlock.codec()} abstract (a
     * {@code MapCodec}, which does not exist on 1.20.1), so the concrete shape has to
     * come from the node that has the codec API.
     */
    Block puriflower(BlockBehaviour.Properties properties);

    /**
     * Sets a living entity alight for the given number of seconds. 1.20.1 spells it
     * {@code setSecondsOnFire(int)}; 1.21 renamed it to {@code igniteForSeconds(float)}.
     */
    void ignite(LivingEntity target, float seconds);

    /** Applies or refreshes a potion effect (ambient, no particles — the 1.12 armor style). */
    void applyEffect(LivingEntity target, TaEffect effect, int durationTicks, int amplifier);

    boolean hasEffect(LivingEntity target, TaEffect effect);

    /**
     * Ticks left on the effect, or {@code -1} when the target does not have it. 1.12's
     * {@code EdibleAspect} refused to re-apply an effect that was already running unless it was
     * nearly out, so the remaining duration has to be readable from shared code.
     */
    int effectRemaining(LivingEntity target, TaEffect effect);

    void removeEffect(LivingEntity target, TaEffect effect);

    /** Removes every harmful effect, the mithminite full-set cleanse. */
    void clearHarmfulEffects(LivingEntity target);
}
