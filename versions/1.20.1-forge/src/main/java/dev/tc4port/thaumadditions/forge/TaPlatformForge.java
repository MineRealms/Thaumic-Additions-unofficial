package dev.tc4port.thaumadditions.forge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 1.20.1 / Forge implementation of {@link TaPlatform}.
 *
 * <p>1.20.1 keeps the pre-1.21 contracts: {@code Tier} is a hand-written
 * interface with {@code getLevel()}, tools take attack damage and speed as
 * constructor arguments, {@code ArmorMaterial} is an unregistered object whose
 * {@code getName()} is the armor texture base, and potion effects are plain
 * {@code MobEffect} values rather than {@code Holder}s.</p>
 */
public final class TaPlatformForge implements TaPlatform {
    /** Modern Minecraft keeps a fluid's type in a registry of its own, beside the fluid itself. */
    private static final net.minecraftforge.registries.DeferredRegister<
            net.minecraftforge.fluids.FluidType> FLUID_TYPES =
            net.minecraftforge.registries.DeferredRegister.create(
                    net.minecraftforge.registries.ForgeRegistries.Keys.FLUID_TYPES,
                    dev.tc4port.thaumadditions.ThaumAdditions.MOD_ID);
    private static final net.minecraftforge.registries.DeferredRegister<
            net.minecraft.world.level.material.Fluid> FLUIDS =
            net.minecraftforge.registries.DeferredRegister.create(
                    net.minecraft.core.registries.Registries.FLUID, dev.tc4port.thaumadditions.ThaumAdditions.MOD_ID);

    /** Wires the fluid registries; call from the mod constructor. */
    public static void register(net.minecraftforge.eventbus.api.IEventBus modBus) {
        FLUIDS.register(modBus);
        FLUID_TYPES.register(modBus);
    }


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
            public void appendHoverText(ItemStack stack, Level level,
                                        List<net.minecraft.network.chat.Component> tooltip,
                                        net.minecraft.world.item.TooltipFlag flag) {
                tooltip.addAll(behavior.tooltipLines(stack));
            }

            @Override
            public Component getName(ItemStack stack) {
                return behavior.displayName(stack, super.getName(stack));
            }
        };
    }


    @Override
    public net.minecraft.world.food.FoodProperties food(int nutrition, float saturationModifier,
                                                        boolean alwaysEdible) {
        net.minecraft.world.food.FoodProperties.Builder builder = new net.minecraft.world.food.FoodProperties.Builder()
                .nutrition(nutrition).saturationMod(saturationModifier);
        if (alwaysEdible) {
            builder = builder.alwaysEat();
        }
        return builder.build();
    }

    @Override
    public Block crop(BlockBehaviour.Properties properties, int maxAge) {
        return new dev.tc4port.thaumadditions.blocks.TaBlocks.CropBlock(properties, maxAge) {
            @Override
            public boolean isValidBonemealTarget(net.minecraft.world.level.LevelReader level, BlockPos pos,
                                                     BlockState state, boolean isClient) {
                return false;
            }

            @Override
            public boolean isBonemealSuccess(Level level, net.minecraft.util.RandomSource random, BlockPos pos,
                                             BlockState state) {
                return false;
            }
        };
    }

    /**
     * Forge 1.20.1 has no client extension event, so the item carries its own icon hook:
     * {@code Item#initializeClient} is only ever called on the client, and the renderer it names
     * comes from the client-only {@code MachineItemRenderer}.
     */
    /** 1.20.1 spells these cues as plain {@code SoundEvent}s. */
    @Override
    public void playSound(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
                          dev.tc4port.thaumadditions.platform.TaSound sound, float volume, float pitch) {
        net.minecraft.sounds.SoundEvent event = switch (sound) {
            case EXPLODE -> net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE;
            case CRAFT_FAIL -> dev.tc4port.thaumcraft.registry.TCSounds.CRAFT_FAIL.get();
            case CRAFT_START -> dev.tc4port.thaumcraft.registry.TCSounds.CRAFT_START.get();
            case BRAIN -> dev.tc4port.thaumcraft.registry.TCSounds.BRAIN.get();
            // 1.12's SoundsTC.poof has no TC4R entry; UPGRADE is TC4R's "something finished" cue.
            case CRAFT_DONE -> dev.tc4port.thaumcraft.registry.TCSounds.UPGRADE.get();
            // TC4R has no grind either; the vanilla grindstone is the same noise.
            case GRIND -> net.minecraft.sounds.SoundEvents.GRINDSTONE_USE;
        };
        level.playSound(null, pos, event, net.minecraft.sounds.SoundSource.BLOCKS, volume, pitch);
    }

    @Override
    public net.minecraft.world.item.Item machineItem(Block block, dev.tc4port.thaumadditions.platform.MachineIcon icon) {
        return new net.minecraft.world.item.BlockItem(block, new net.minecraft.world.item.Item.Properties()) {
            @Override
            public void initializeClient(java.util.function.Consumer<
                    net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
                consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
                    @Override
                    public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                        return switch (icon) {
                            case CRYSTAL_BORE ->
                                    dev.tc4port.thaumadditions.client.render.MachineItemRenderer.bore();
                            case ASPECT_COMBINER ->
                                    dev.tc4port.thaumadditions.client.render.MachineItemRenderer.combiner();
                            case AURA_CHARGER ->
                                    dev.tc4port.thaumadditions.client.render.MachineItemRenderer.charger();
                            case CRYSTAL_CRUSHER ->
                                    dev.tc4port.thaumadditions.client.render.MachineItemRenderer.crusher();
                            case FLUX_CONCENTRATOR ->
                                    dev.tc4port.thaumadditions.client.render.MachineItemRenderer.concentrator();
                        };
                    }
                });
            }
        };
    }

    @Override
    public Block puriflower(BlockBehaviour.Properties properties) {
        return new dev.tc4port.thaumadditions.blocks.TaBlocks.PuriflowerBlock(properties) {
        };
    }

    @Override
    public void ignite(LivingEntity target, float seconds) {
        target.setSecondsOnFire((int) seconds);
    }

    @Override
    public Item sword(Tier tier, float attackDamage, float attackSpeed, Item.Properties properties,
                      SwordBehavior behavior) {
        return new SwordItem(tier, (int) attackDamage, attackSpeed, properties) {
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
        return new Material(id, material, visDiscounts, gogglesOverlay);
    }

    @Override
    public Item armor(TaArmorMaterial material, ArmorItem.Type type, int durabilityMultiplier, Item.Properties properties,
                      TaArmorTick behavior) {
        Material handle = (Material) material;
        return new Piece(handle, type, properties, behavior);
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
            if (!active.getEffect().isBeneficial())
                target.removeEffect(active.getEffect());
    }

    // ------------------------------------------------------------------ enchantments

    /** 1.20.1 enchantments are a static registry; the handle is a holder onto an entry. */
    @Override
    public dev.tc4port.thaumadditions.platform.TaEnchantment enchantment(
            net.minecraft.world.level.Level level, ResourceLocation id) {
        Enchantment value = net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.get(id);
        return value == null ? null
                : new EnchantmentHandle(net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT
                        .wrapAsHolder(value));
    }

    @Override
    public ResourceLocation enchantmentId(dev.tc4port.thaumadditions.platform.TaEnchantment enchantment) {
        return net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT
                .getKey(((EnchantmentHandle) enchantment).value.value());
    }

    @Override
    public boolean enchantmentsCompatible(dev.tc4port.thaumadditions.platform.TaEnchantment first,
                                          dev.tc4port.thaumadditions.platform.TaEnchantment second) {
        return ((EnchantmentHandle) first).value.value()
                .isCompatibleWith(((EnchantmentHandle) second).value.value());
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
        stack.enchant(((EnchantmentHandle) enchantment).value.value(), level);
    }

    private record EnchantmentHandle(net.minecraft.core.Holder<Enchantment> value)
            implements dev.tc4port.thaumadditions.platform.TaEnchantment {
    }

    // ------------------------------------------------------------------ menus

    @Override
    public void saveStackList(net.minecraft.world.level.Level level, net.minecraft.nbt.CompoundTag tag,
                              net.minecraft.core.NonNullList<ItemStack> slots) {
        net.minecraft.world.ContainerHelper.saveAllItems(tag, slots);
    }

    @Override
    public void loadStackList(net.minecraft.world.level.Level level, net.minecraft.nbt.CompoundTag tag,
                              net.minecraft.core.NonNullList<ItemStack> slots) {
        net.minecraft.world.ContainerHelper.loadAllItems(tag, slots);
    }

    /**
     * An item drawn by a renderer rather than by a model file. Forge 1.20.1 has no client
     * extension event, so the hook goes on the item itself, exactly as the machine icons do.
     */
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

            @Override
            public void initializeClient(java.util.function.Consumer<
                    net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
                consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
                    @Override
                    public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                        return new dev.tc4port.thaumadditions.forge.client.VoidThaumometerRenderer();
                    }
                });
            }
        };
    }

    @Override
    public <T extends net.minecraft.world.inventory.AbstractContainerMenu> net.minecraft.world.inventory.MenuType<T> menuType(
            dev.tc4port.thaumadditions.platform.TaMenuFactory<T> factory) {
        return net.minecraftforge.common.extensions.IForgeMenuType.create(factory::create);
    }

    @Override
    public void openMenu(net.minecraft.server.level.ServerPlayer player,
                         net.minecraft.world.MenuProvider provider,
                         java.util.function.Consumer<net.minecraft.network.FriendlyByteBuf> extraData) {
        net.minecraftforge.network.NetworkHooks.openScreen(player, provider, extraData);
    }

    @Override
    public dev.tc4port.thaumadditions.platform.TaFluid fluid(
            String name, ResourceLocation stillTexture, ResourceLocation flowingTexture,
            int tint, int lightLevel, int density, int viscosity) {
        net.minecraftforge.registries.RegistryObject<net.minecraftforge.fluids.FluidType> type =
                FLUID_TYPES.register(name, () -> new net.minecraftforge.fluids.FluidType(
                        net.minecraftforge.fluids.FluidType.Properties.create()
                                .descriptionId("fluid." + dev.tc4port.thaumadditions.ThaumAdditions.MOD_ID + "." + name)
                                .lightLevel(lightLevel)
                                .density(density)
                                .viscosity(viscosity)) {
                    @Override
                    public void initializeClient(java.util.function.Consumer<
                            net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions> consumer) {
                        consumer.accept(new net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions() {
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
        net.minecraftforge.registries.RegistryObject<?>[] pair =
                new net.minecraftforge.registries.RegistryObject<?>[2];
        pair[0] = FLUIDS.register(name,
                () -> new net.minecraftforge.fluids.ForgeFlowingFluid.Source(properties(type, pair, name)));
        pair[1] = FLUIDS.register(name + "_flowing",
                () -> new net.minecraftforge.fluids.ForgeFlowingFluid.Flowing(properties(type, pair, name)));

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

    private static net.minecraftforge.fluids.ForgeFlowingFluid.Properties properties(
            net.minecraftforge.registries.RegistryObject<net.minecraftforge.fluids.FluidType> type,
            net.minecraftforge.registries.RegistryObject<?>[] pair, String name) {
        return new net.minecraftforge.fluids.ForgeFlowingFluid.Properties(type,
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

    private static MobEffect nativeEffect(TaEffect effect) {
        return switch (effect) {
            case NIGHT_VISION -> MobEffects.NIGHT_VISION;
            case WATER_BREATHING -> MobEffects.WATER_BREATHING;
            case FIRE_RESISTANCE -> MobEffects.FIRE_RESISTANCE;
            case MOVEMENT_SPEED -> MobEffects.MOVEMENT_SPEED;
            case JUMP -> MobEffects.JUMP;
            case SLOW_FALLING -> MobEffects.SLOW_FALLING;
            case WARP_WARD -> dev.tc4port.thaumcraft.registry.TCEffects.WARP_WARD.get();
            case SLOWNESS -> MobEffects.MOVEMENT_SLOWDOWN;
            case BLINDNESS -> MobEffects.BLINDNESS;
            case NAUSEA -> MobEffects.CONFUSION;
            case STRENGTH -> MobEffects.DAMAGE_BOOST;
            case HASTE -> MobEffects.DIG_SPEED;
            case HUNGER -> MobEffects.HUNGER;
            case RESISTANCE -> MobEffects.DAMAGE_RESISTANCE;
            case FLUX_TAINT -> dev.tc4port.thaumcraft.registry.TCEffects.FLUX_TAINT.get();
            case VIS_EXHAUST -> dev.tc4port.thaumcraft.registry.TCEffects.VIS_EXHAUST.get();
        };
    }

    /** Opaque handle: the 1.20.1 material object plus the gear hooks it hands to each piece. */
    private record Material(String setId, ArmorMaterial material, Map<ArmorItem.Type, Integer> visDiscounts,
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
