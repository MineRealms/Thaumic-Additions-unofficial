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

        /**
         * What using the sword does, for the one weapon here that is also thrown. Returning
         * {@code null} keeps the vanilla swing.
         */
        default net.minecraft.world.InteractionResultHolder<ItemStack> use(
                net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player,
                net.minecraft.world.InteractionHand hand) {
            return null;
        }
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
     * Registers a fluid: its type and the two fluid objects that flow and stand still.
     *
     * <p>Modern Minecraft split a fluid in two — a {@code FluidType} that carries the look and the
     * physics, and the {@code Fluid} itself that carries the states. Both are loader objects on all
     * three of these loaders and cannot be named from shared code, and the textures go on the type
     * through a client extension that the two loaders spell differently. What comes back is the two
     * fluids; the block and the bucket that stand for them are ordinary classes the shared source
     * registers itself, out of the suppliers this returns.</p>
     *
     * <p>{@code tint} is the fluid's colour as ARGB, which upstream carried as {@code Fluid#getColor}.</p>
     */
    TaFluid fluid(String name, ResourceLocation stillTexture, ResourceLocation flowingTexture,
                  int tint, int lightLevel, int density, int viscosity);

    /**
     * Builds the bucket that carries a fluid, out of the supplier {@link TaFluid#source()} hands
     * back rather than out of the fluid itself.
     *
     * <p>1.20.1's {@code BucketItem} keeps the supplier and resolves it when the bucket is first
     * used; 1.21.1's takes the fluid there and then. That matters because the item registry can be
     * filled before the fluid one: a bucket built by resolving the fluid at registration time
     * either comes out empty or, on the loader that refuses an unbound holder, never comes out at
     * all. Each loader gets the form it can honour.</p>
     */
    Item bucketItem(Supplier<? extends net.minecraft.world.level.material.FlowingFluid> fluid,
                    Item.Properties properties);

    /**
     * Creates an item whose icon and held model come from a client renderer, for the items whose
     * 1.12 form was a model the modern loaders cannot read. The behaviour goes on it as usual.
     */
    Item modelledItem(Item.Properties properties, ToolIcon icon,
                      dev.tc4port.thaumadditions.items.ItemBehavior behavior);

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

    // ------------------------------------------------------------------ enchantments

    /**
     * Resolves an enchantment by registry id, or {@code null} when nothing holds that id. The
     * level is needed because 1.21 enchantments live in a data-driven registry.
     */
    TaEnchantment enchantment(net.minecraft.world.level.Level level, ResourceLocation id);

    /** The id an enchantment was registered under. */
    ResourceLocation enchantmentId(TaEnchantment enchantment);

    /** Whether two enchantments may sit on the same item. */
    boolean enchantmentsCompatible(TaEnchantment first, TaEnchantment second);

    /** Whether this enchantment can be written onto this stack. */
    boolean enchantmentAppliesTo(TaEnchantment enchantment, ItemStack stack);

    int enchantmentMinLevel(TaEnchantment enchantment);

    int enchantmentMaxLevel(TaEnchantment enchantment);

    /** Writes the enchantment onto the stack, replacing any existing level of it. */
    void enchantItem(ItemStack stack, TaEnchantment enchantment, int level);

    // ------------------------------------------------------------------ menus

    /**
     * Builds a {@code MenuType} from a factory that also receives the block position the menu was
     * opened at. The two loaders spell the constructor and the extended factory differently, so the
     * shared menu classes name neither.
     */
    <T extends net.minecraft.world.inventory.AbstractContainerMenu>
    net.minecraft.world.inventory.MenuType<T> menuType(TaMenuFactory<T> factory);

    /**
     * Opens a menu, handing the client whatever it needs to rebuild it. 1.20.1 Forge spells this
     * {@code NetworkHooks.openScreen}; 1.21.1 NeoForge puts the same two-argument overload on the
     * player itself.
     */
    void openMenu(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.MenuProvider provider,
                  java.util.function.Consumer<net.minecraft.network.FriendlyByteBuf> extraData);

    // ------------------------------------------------------------------ entity inventories

    /**
     * Writes a list of stacks into a tag. 1.21 made the stack codecs registry-aware and so the
     * loader's helper grew a parameter; the level supplies it here, which is why one is needed.
     */
    void saveStackList(net.minecraft.world.level.Level level, net.minecraft.nbt.CompoundTag tag,
                       net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> slots);

    /** Reads back what {@link #saveStackList} wrote, leaving the slots empty when absent. */
    void loadStackList(net.minecraft.world.level.Level level, net.minecraft.nbt.CompoundTag tag,
                       net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> slots);
}
