package dev.tc4port.thaumadditions.neoforge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import dev.tc4port.thaumadditions.ThaumAdditions;
import dev.tc4port.thaumadditions.platform.TaArmorMaterial;
import dev.tc4port.thaumadditions.platform.TaArmorSetMember;
import dev.tc4port.thaumadditions.platform.TaArmorSound;
import dev.tc4port.thaumadditions.platform.TaArmorTick;
import dev.tc4port.thaumadditions.platform.TaEffect;
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
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 1.21.1 / NeoForge implementation of {@link TaPlatform}.
 *
 * <p>1.21.1 changed every contract this port touches: {@code Tier} lost
 * {@code getLevel()} and gained {@code getIncorrectBlocksForDrops()}, tools
 * take their attack damage/speed through {@code Item.Properties.attributes(...)},
 * {@code ArmorMaterial} became a registered record that {@code ArmorItem}
 * consumes through a {@code Holder}, and potion effects became
 * {@code Holder<MobEffect>}.</p>
 */
public final class TaPlatformNeoForge implements TaPlatform {
    /** Modern Minecraft keeps a fluid's type in a registry of its own, beside the fluid itself. */
    private static final net.neoforged.neoforge.registries.DeferredRegister<
            net.neoforged.neoforge.fluids.FluidType> FLUID_TYPES =
            net.neoforged.neoforge.registries.DeferredRegister.create(
                    net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.FLUID_TYPES,
                    dev.tc4port.thaumadditions.ThaumAdditions.MOD_ID);
    private static final net.neoforged.neoforge.registries.DeferredRegister<
            net.minecraft.world.level.material.Fluid> FLUIDS =
            net.neoforged.neoforge.registries.DeferredRegister.create(
                    net.minecraft.core.registries.Registries.FLUID, dev.tc4port.thaumadditions.ThaumAdditions.MOD_ID);


    private static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, ThaumAdditions.MOD_ID);

    /** Wires the armor-material registry; call from the mod constructor. */
    public static void register(IEventBus modBus) {
        ARMOR_MATERIALS.register(modBus);
        FLUIDS.register(modBus);
        FLUID_TYPES.register(modBus);
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

    @Override
    public Item behaviorItem(Item.Properties properties,
                             dev.tc4port.thaumadditions.items.ItemBehavior behavior) {
        return new Item(properties) {
            @Override
            public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                return behavior.use(level, player, hand);
            }

            @Override
            public InteractionResult useOn(UseOnContext context) {
                return behavior.useOn(context);
            }

            @Override
            public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                          InteractionHand hand) {
                return behavior.interactLivingEntity(stack, player, target, hand);
            }

            @Override
            public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
                behavior.inventoryTick(stack, level, entity, slot, selected);
            }

            @Override
            public void onCraftedBy(ItemStack stack, Level level, Player player) {
                behavior.onCreated(stack, level, player);
            }

            @Override
            public boolean isFoil(ItemStack stack) {
                return behavior.isFoil(stack);
            }

            @Override
            public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                        List<net.minecraft.network.chat.Component> tooltip,
                                        net.minecraft.world.item.TooltipFlag flag) {
                tooltip.addAll(behavior.tooltipLines(stack));
            }
        };
    }


    @Override
    public net.minecraft.world.food.FoodProperties food(int nutrition, float saturationModifier,
                                                        boolean alwaysEdible) {
        net.minecraft.world.food.FoodProperties.Builder builder = new net.minecraft.world.food.FoodProperties.Builder()
                .nutrition(nutrition).saturationModifier(saturationModifier);
        if (alwaysEdible) {
            builder = builder.alwaysEdible();
        }
        return builder.build();
    }

    @Override
    public Block crop(BlockBehaviour.Properties properties, int maxAge) {
        return new dev.tc4port.thaumadditions.blocks.TaBlocks.CropBlock(properties, maxAge) {
            @Override
            public boolean isValidBonemealTarget(net.minecraft.world.level.LevelReader level,
                                                 BlockPos pos, BlockState state) {
                return false;
            }

            @Override
            public boolean isBonemealSuccess(Level level, net.minecraft.util.RandomSource random,
                                             BlockPos pos, BlockState state) {
                return false;
            }
        };
    }

    @Override
    public Block puriflower(BlockBehaviour.Properties properties) {
        return new NeoPuriflower(properties);
    }

    /**
     * NeoForge 1.21.1 registers item client extensions from a client event instead of from the
     * item, so a plain block item is all this node needs; the icon is attached in
     * {@code ThaumAddsNeoForgeClient}.
     */
    @Override
    public net.minecraft.world.item.Item machineItem(Block block, dev.tc4port.thaumadditions.platform.MachineIcon icon) {
        return new net.minecraft.world.item.BlockItem(block, new net.minecraft.world.item.Item.Properties());
    }

    /** 1.21.1 spells these cues as {@code Holder<SoundEvent>}. */
    @Override
    public void playSound(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
                          dev.tc4port.thaumadditions.platform.TaSound sound, float volume, float pitch) {
        net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> event = switch (sound) {
            case EXPLODE -> net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE;
            case CRAFT_FAIL -> dev.tc4port.thaumcraft.registry.TCSounds.CRAFT_FAIL;
            case CRAFT_START -> dev.tc4port.thaumcraft.registry.TCSounds.CRAFT_START;
            case BRAIN -> dev.tc4port.thaumcraft.registry.TCSounds.BRAIN;
            // 1.12's SoundsTC.poof has no TC4R entry; UPGRADE is TC4R's "something finished" cue.
            case CRAFT_DONE -> dev.tc4port.thaumcraft.registry.TCSounds.UPGRADE;
            // TC4R has no grind either; the vanilla grindstone is the same noise. 1.21.1 spells the
            // vanilla constant as a bare SoundEvent here, so it is wrapped to match the other arms.
            case GRIND -> net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT
                    .wrapAsHolder(net.minecraft.sounds.SoundEvents.GRINDSTONE_USE);
        };
        level.playSound(null, pos, event.value(), net.minecraft.sounds.SoundSource.BLOCKS, volume, pitch);
    }

    /** Concrete 1.21 shape: the shared base is abstract because {@code BushBlock.codec()} is. */
    private static final class NeoPuriflower extends dev.tc4port.thaumadditions.blocks.TaBlocks.PuriflowerBlock {
        private NeoPuriflower(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BushBlock> codec() {
            return simpleCodec(NeoPuriflower::new);
        }
    }

    @Override
    public void ignite(LivingEntity target, float seconds) {
        target.igniteForSeconds(seconds);
    }

    @Override
    public Item sword(Tier tier, float attackDamage, float attackSpeed, Item.Properties properties,
                      SwordBehavior behavior) {
        return new SwordItem(tier, properties.attributes(SwordItem.createAttributes(tier, attackDamage, attackSpeed))) {
            @Override
            public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
                return behavior.hurtEnemy(stack, target, attacker);
            }

            @Override
            public boolean isEnchantable(ItemStack stack) {
                return true;
            }

            @Override
            public net.minecraft.world.InteractionResultHolder<ItemStack> use(
                    net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player,
                    net.minecraft.world.InteractionHand hand) {
                var handled = behavior.use(level, player, hand);
                return handled == null ? super.use(level, player, hand) : handled;
            }
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
        return new Material(id, holder, visDiscounts, gogglesOverlay);
    }

    @Override
    public Item armor(TaArmorMaterial material, ArmorItem.Type type, int durabilityMultiplier, Item.Properties properties,
                      TaArmorTick behavior) {
        Material handle = (Material) material;
        return new Piece(handle, type, properties.durability(type.getDurability(durabilityMultiplier)), behavior);
    }

    @Override
    public void applyEffect(LivingEntity target, TaEffect effect, int durationTicks, int amplifier) {
        target.addEffect(new MobEffectInstance(nativeEffect(effect), durationTicks, amplifier, true, false));
    }

    @Override
    public boolean hasEffect(LivingEntity target, TaEffect effect) {
        return target.hasEffect(nativeEffect(effect));
    }

    @Override
    public int effectRemaining(LivingEntity target, TaEffect effect) {
        MobEffectInstance active = target.getEffect(nativeEffect(effect));
        return active == null ? -1 : active.getDuration();
    }

    @Override
    public void removeEffect(LivingEntity target, TaEffect effect) {
        target.removeEffect(nativeEffect(effect));
    }

    @Override
    public void clearHarmfulEffects(LivingEntity target) {
        for (MobEffectInstance active : new ArrayList<>(target.getActiveEffects()))
            if (!active.getEffect().value().isBeneficial())
                target.removeEffect(active.getEffect());
    }

    // ------------------------------------------------------------------ enchantments

    /**
     * 1.21.1 enchantments are data-driven, so the handle is a {@code Holder<Enchantment>} fetched
     * from the level's own registry view rather than from a static registry.
     */
    @Override
    public dev.tc4port.thaumadditions.platform.TaEnchantment enchantment(
            net.minecraft.world.level.Level level, ResourceLocation id) {
        Holder<Enchantment> holder = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .get(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT, id))
                .orElse(null);
        return holder == null ? null : new EnchantmentHandle(holder);
    }

    @Override
    public ResourceLocation enchantmentId(dev.tc4port.thaumadditions.platform.TaEnchantment enchantment) {
        return ((EnchantmentHandle) enchantment).value.unwrapKey()
                .map(net.minecraft.resources.ResourceKey::location).orElse(null);
    }

    @Override
    public boolean enchantmentsCompatible(dev.tc4port.thaumadditions.platform.TaEnchantment first,
                                          dev.tc4port.thaumadditions.platform.TaEnchantment second) {
        // 1.21 moved the check off the instance and onto a static pair-wise helper.
        return Enchantment.areCompatible(((EnchantmentHandle) first).value,
                ((EnchantmentHandle) second).value);
    }

    @Override
    public boolean enchantmentAppliesTo(dev.tc4port.thaumadditions.platform.TaEnchantment enchantment,
                                        ItemStack stack) {
        return ((EnchantmentHandle) enchantment).value.value().canEnchant(stack);
    }

    @Override
    public int enchantmentMinLevel(dev.tc4port.thaumadditions.platform.TaEnchantment enchantment) {
        return ((EnchantmentHandle) enchantment).value.value().getMinLevel();
    }

    @Override
    public int enchantmentMaxLevel(dev.tc4port.thaumadditions.platform.TaEnchantment enchantment) {
        return ((EnchantmentHandle) enchantment).value.value().getMaxLevel();
    }

    @Override
    public void enchantItem(ItemStack stack, dev.tc4port.thaumadditions.platform.TaEnchantment enchantment,
                            int level) {
        stack.enchant(((EnchantmentHandle) enchantment).value, level);
    }

    private record EnchantmentHandle(Holder<Enchantment> value)
            implements dev.tc4port.thaumadditions.platform.TaEnchantment {
    }

    // ------------------------------------------------------------------ menus

    @Override
    public void saveStackList(net.minecraft.world.level.Level level, net.minecraft.nbt.CompoundTag tag,
                              net.minecraft.core.NonNullList<ItemStack> slots) {
        net.minecraft.world.ContainerHelper.saveAllItems(tag, slots, level.registryAccess());
    }

    @Override
    public void loadStackList(net.minecraft.world.level.Level level, net.minecraft.nbt.CompoundTag tag,
                              net.minecraft.core.NonNullList<ItemStack> slots) {
        net.minecraft.world.ContainerHelper.loadAllItems(tag, slots, level.registryAccess());
    }

    /** An item drawn by a renderer rather than by a model file; see the Forge twin. */
    @Override
    public Item modelledItem(Item.Properties properties,
                             dev.tc4port.thaumadditions.platform.ToolIcon icon,
                             dev.tc4port.thaumadditions.items.ItemBehavior behavior) {
        return new net.minecraft.world.item.Item(properties) {
            @Override
            public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(
                    net.minecraft.world.level.Level level,
                    net.minecraft.world.entity.player.Player player,
                    net.minecraft.world.InteractionHand hand) {
                return behavior.use(level, player, hand);
            }
        };
    }

    @Override
    public <T extends net.minecraft.world.inventory.AbstractContainerMenu> net.minecraft.world.inventory.MenuType<T> menuType(
            dev.tc4port.thaumadditions.platform.TaMenuFactory<T> factory) {
        return net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(factory::create);
    }

    @Override
    public void openMenu(net.minecraft.server.level.ServerPlayer player,
                         net.minecraft.world.MenuProvider provider,
                         java.util.function.Consumer<net.minecraft.network.FriendlyByteBuf> extraData) {
        // 1.21 hands the writer a registry-aware buffer; it is still an ordinary one underneath.
        player.openMenu(provider,
                (net.minecraft.network.RegistryFriendlyByteBuf buf) -> extraData.accept(buf));
    }

    @Override
    public dev.tc4port.thaumadditions.platform.TaFluid fluid(
            String name, ResourceLocation stillTexture, ResourceLocation flowingTexture,
            int tint, int lightLevel, int density, int viscosity) {
        net.neoforged.neoforge.registries.DeferredHolder<net.neoforged.neoforge.fluids.FluidType,
                net.neoforged.neoforge.fluids.FluidType> type =
                FLUID_TYPES.register(name, () -> new net.neoforged.neoforge.fluids.FluidType(
                        net.neoforged.neoforge.fluids.FluidType.Properties.create()
                                .descriptionId("fluid." + dev.tc4port.thaumadditions.ThaumAdditions.MOD_ID + "." + name)
                                .lightLevel(lightLevel)
                                .density(density)
                                .viscosity(viscosity)) {
                    @Override
                    public void initializeClient(java.util.function.Consumer<
                            net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions> consumer) {
                        consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions() {
                            @Override
                            public ResourceLocation getStillTexture() {
                                return stillTexture;
                            }

                            @Override
                            public ResourceLocation getFlowingTexture() {
                                return flowingTexture;
                            }

                            @Override
                            public int getTintColor() {
                                return tint;
                            }
                        });
                    }
                });

        // The fluids need the properties and the properties need the fluids; the array breaks the
        // cycle, since neither supplier is read until the first fluid is built.
        net.neoforged.neoforge.registries.DeferredHolder<?, ?>[] pair =
                new net.neoforged.neoforge.registries.DeferredHolder<?, ?>[2];
        pair[0] = FLUIDS.register(name,
                () -> new net.neoforged.neoforge.fluids.BaseFlowingFluid.Source(properties(type, pair, name)));
        pair[1] = FLUIDS.register(name + "_flowing",
                () -> new net.neoforged.neoforge.fluids.BaseFlowingFluid.Flowing(properties(type, pair, name)));

        return new dev.tc4port.thaumadditions.platform.TaFluid() {
            @Override
            public Supplier<? extends net.minecraft.world.level.material.FlowingFluid> source() {
                return () -> (net.minecraft.world.level.material.FlowingFluid) pair[0].get();
            }

            @Override
            public Supplier<? extends net.minecraft.world.level.material.FlowingFluid> flowing() {
                return () -> (net.minecraft.world.level.material.FlowingFluid) pair[1].get();
            }
        };
    }

    private static net.neoforged.neoforge.fluids.BaseFlowingFluid.Properties properties(
            net.neoforged.neoforge.registries.DeferredHolder<net.neoforged.neoforge.fluids.FluidType,
                    net.neoforged.neoforge.fluids.FluidType> type,
            net.neoforged.neoforge.registries.DeferredHolder<?, ?>[] pair, String name) {
        return new net.neoforged.neoforge.fluids.BaseFlowingFluid.Properties(type,
                () -> (net.minecraft.world.level.material.FlowingFluid) pair[0].get(),
                () -> (net.minecraft.world.level.material.FlowingFluid) pair[1].get())
                // The block and the bucket are registered after the fluid, so they are found by
                // name; neither supplier is read until the fluid is used in a world.
                .block(() -> (net.minecraft.world.level.block.LiquidBlock)
                        net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(fluidId(name)))
                .bucket(() -> net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                        fluidId(name + "_bucket")))
                // 1.12's BlockFluidClassic tickRate: crystal water creeps, it does not run.
                .tickRate(20);
    }

    private static ResourceLocation fluidId(String path) {
        return ResourceLocation.tryParse(dev.tc4port.thaumadditions.ThaumAdditions.MOD_ID + ":" + path);
    }

    private static Holder<MobEffect> nativeEffect(TaEffect effect) {
        return switch (effect) {
            case NIGHT_VISION -> MobEffects.NIGHT_VISION;
            case WATER_BREATHING -> MobEffects.WATER_BREATHING;
            case FIRE_RESISTANCE -> MobEffects.FIRE_RESISTANCE;
            case MOVEMENT_SPEED -> MobEffects.MOVEMENT_SPEED;
            case JUMP -> MobEffects.JUMP;
            case SLOW_FALLING -> MobEffects.SLOW_FALLING;
            case WARP_WARD -> dev.tc4port.thaumcraft.registry.TCEffects.WARP_WARD;
            case SLOWNESS -> MobEffects.MOVEMENT_SLOWDOWN;
            case BLINDNESS -> MobEffects.BLINDNESS;
            case NAUSEA -> MobEffects.CONFUSION;
            case STRENGTH -> MobEffects.DAMAGE_BOOST;
            case HASTE -> MobEffects.DIG_SPEED;
            case HUNGER -> MobEffects.HUNGER;
            case RESISTANCE -> MobEffects.DAMAGE_RESISTANCE;
            case FLUX_TAINT -> dev.tc4port.thaumcraft.registry.TCEffects.FLUX_TAINT;
            case VIS_EXHAUST -> dev.tc4port.thaumcraft.registry.TCEffects.VIS_EXHAUST;
        };
    }

    /** Opaque handle: the registered material holder plus the gear hooks it hands to each piece. */
    private record Material(String setId, Holder<ArmorMaterial> material, Map<ArmorItem.Type, Integer> visDiscounts,
                            boolean gogglesOverlay) implements TaArmorMaterial {
    }

    private static final class Piece extends ArmorItem
            implements VisDiscountGear, GogglesOverlayGear, TaArmorSetMember {
        private final String setId;
        private final Map<ArmorItem.Type, Integer> visDiscounts;
        private final boolean gogglesOverlay;
        private final TaArmorTick behavior;

        private Piece(Material material, ArmorItem.Type type, Item.Properties properties, TaArmorTick behavior) {
            super(material.material(), type, properties);
            this.setId = material.setId();
            this.visDiscounts = material.visDiscounts();
            this.gogglesOverlay = material.gogglesOverlay();
            this.behavior = behavior;
        }

        @Override
        public String armorSetId() {
            return setId;
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            super.inventoryTick(stack, level, entity, slot, selected);
            if (behavior != null && entity instanceof Player player && player.getItemBySlot(getType().getSlot()) == stack)
                behavior.onWornTick(stack, level, player, getType());
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
