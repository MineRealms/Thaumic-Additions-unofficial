package dev.tc4port.thaumadditions.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.tc4port.thaumadditions.ThaumAdditions;
import dev.tc4port.thaumadditions.aspect.TaAspect;
import dev.tc4port.thaumadditions.platform.TaPlatform;
import dev.tc4port.hclite.platform.HCLitePlatform;
import dev.tc4port.thaumcraft.api.aspect.AspectAmounts;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.aspect.AspectQueryApi;
import dev.tc4port.thaumcraft.api.player.PlayerKnowledgeApi;
import dev.tc4port.thaumcraft.api.research.ResearchApi;
import dev.tc4port.thaumcraft.api.research.ResearchCompletionContext;
import dev.tc4port.thaumcraft.api.research.ResearchCompletionReason;
import dev.tc4port.thaumcraft.api.research.ResearchCompletionResult;
import dev.tc4port.thaumcraft.api.research.ResearchKey;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The 1.12 tool and misc items, written against {@link ItemBehavior} so the
 * version-drifted {@code Item} hooks stay on the platform port.
 *
 * <p>Everything here is self-contained: no packets, no GUIs, no custom entities.
 * Where 1.12 leaned on a subsystem that is not ported yet (the Chester entity,
 * the seal-globe editor) the item still registers and is fully functional as an
 * object; the missing interaction is called out per item.</p>
 */
public final class ToolItems {
    private ToolItems() {
    }

    /**
     * 1.12 {@code ItemVoidThaumometer}: reports the aspects of whatever the player
     * is looking at (entity within 9 blocks, else block within 16, else the held
     * item), using TC4R's scan catalogs instead of TC6's ScanningManager.
     */
    public static Item voidThaumometer() {
        return new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)) {
            @Override
            public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                ItemStack stack = player.getItemInHand(hand);
                if (!level.isClientSide) {
                    Vec3 eye = player.getEyePosition(1.0F);
                    Vec3 look = player.getViewVector(1.0F);
                    report(level, player, eye, look);
                    level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                            SoundSource.PLAYERS, 0.6F, 1.6F);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }

            private void report(Level level, Player player, Vec3 eye, Vec3 look) {
                EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil
                        .getEntityHitResult(level, player, eye, eye.add(look.scale(9.0D)),
                                new AABB(eye, eye.add(look.scale(9.0D))).inflate(1.0D),
                                entity -> entity instanceof LivingEntity && entity != player);
                AspectAmounts aspects;
                String label;
                if (entityHit != null) {
                    aspects = AspectQueryApi.entity(entityHit.getEntity());
                    label = entityHit.getEntity().getName().getString();
                } else {
                    BlockHitResult blockHit = level.clip(new net.minecraft.world.level.ClipContext(eye,
                            eye.add(look.scale(16.0D)), net.minecraft.world.level.ClipContext.Block.OUTLINE,
                            net.minecraft.world.level.ClipContext.Fluid.NONE, player));
                    if (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK) {
                        BlockPos pos = blockHit.getBlockPos();
                        aspects = AspectQueryApi.block(level.getBlockState(pos));
                        label = level.getBlockState(pos).getBlock().getName().getString();
                    } else {
                        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
                        if (held.isEmpty()) {
                            return;
                        }
                        aspects = AspectQueryApi.item(held);
                        label = held.getHoverName().getString();
                    }
                }
                player.sendSystemMessage(Component.translatable("chat.thaumadds.thaumometer.target", label)
                        .withStyle(ChatFormatting.GRAY));
                if (aspects.amounts().isEmpty()) {
                    player.sendSystemMessage(Component.translatable("chat.thaumadds.thaumometer.empty")
                            .withStyle(ChatFormatting.DARK_GRAY));
                    return;
                }
                aspects.amounts().forEach((aspect, amount) -> player.sendSystemMessage(
                        Component.literal("  " + TaAspect.displayName(aspect) + " x" + amount)
                                .withStyle(ChatFormatting.GRAY)));
            }
        };
    }

    /**
     * 1.12 {@code ItemWormholeMirror}: sneak-right-click binds the targeted block,
     * right-click teleports back to it.
     */
    public static Item wormholeMirror() {
        return new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)) {
            @Override
            public InteractionResult useOn(UseOnContext context) {
                Player player = context.getPlayer();
                if (player == null || !player.isShiftKeyDown()) {
                    return net.minecraft.world.InteractionResult.PASS;
                }
                ItemStack stack = context.getItemInHand();
                BlockPos target = context.getClickedPos().relative(context.getClickedFace());
                HCLitePlatform.Holder.get().updateItemTag(stack, tag -> tag.putLong("LinkPos", target.asLong()));
                if (context.getLevel() instanceof ServerLevel serverLevel) {
                    HCLitePlatform.Holder.get().updateItemTag(stack, tag -> tag.putString("LinkDim",
                            serverLevel.dimension().location().toString()));
                }
                if (!context.getLevel().isClientSide) {
                    context.getLevel().playSound(null, target, SoundEvents.ENDER_EYE_LAUNCH,
                            SoundSource.PLAYERS, 0.8F, 1.2F);
                }
                return net.minecraft.world.InteractionResult.sidedSuccess(context.getLevel().isClientSide);
            }

            @Override
            public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                ItemStack stack = player.getItemInHand(hand);
                CompoundTag tag = HCLitePlatform.Holder.get().itemTag(stack);
                if (tag == null || !tag.contains("LinkPos", Tag.TAG_LONG)) {
                    return InteractionResultHolder.pass(stack);
                }
                if (level.isClientSide) {
                    return InteractionResultHolder.success(stack);
                }
                ServerPlayer serverPlayer = (ServerPlayer) player;
                ResourceLocation dim = ResourceLocation.tryParse(tag.getString("LinkDim"));
                ServerLevel destination = dim == null
                        ? serverPlayer.serverLevel()
                        : Optional.ofNullable(serverPlayer.server.getLevel(
                                        ResourceKey.create(Registries.DIMENSION, dim)))
                                .orElse(serverPlayer.serverLevel());
                BlockPos pos = BlockPos.of(tag.getLong("LinkPos"));
                serverPlayer.teleportTo(destination, pos.getX() + 0.5D, pos.getY() + 0.2D, pos.getZ() + 0.5D,
                        serverPlayer.getYRot(), serverPlayer.getXRot());
                serverPlayer.level().playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT,
                        SoundSource.PLAYERS, 0.8F, 1.0F);
                return InteractionResultHolder.consume(stack);
            }
        };
    }

    /**
     * 1.12 {@code ItemKnowledgeTome}: sneak-right-click snapshots the player's
     * completed research, right-click restores it through TC4R's grant API.
     */
    public static Item knowledgeTome(TaPlatform platform) {
        return platform.behaviorItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), new ItemBehavior() {
            @Override
            public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                ItemStack stack = player.getItemInHand(hand);
                CompoundTag tag = HCLitePlatform.Holder.get().itemTag(stack);
                boolean stored = tag != null && tag.contains("Research", Tag.TAG_LIST);
                if (level.isClientSide) {
                    return InteractionResultHolder.success(stack);
                }
                ServerPlayer serverPlayer = (ServerPlayer) player;
                if (player.isShiftKeyDown() && !stored) {
                    ListTag list = new ListTag();
                    for (ResearchKey key : PlayerKnowledgeApi.view(player).completedResearch()) {
                        list.add(StringTag.valueOf(key.serialized()));
                    }
                    HCLitePlatform.Holder.get().updateItemTag(stack, nbt -> nbt.put("Research", list));
                    player.sendSystemMessage(Component.translatable("gui.thaumadds.knowledge_tome.store",
                            list.size()).withStyle(ChatFormatting.GRAY));
                    level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN,
                            SoundSource.PLAYERS, 1.0F, 1.0F);
                    return InteractionResultHolder.consume(stack);
                }
                if (stored) {
                    ListTag list = tag.getList("Research", Tag.TAG_STRING);
                    int granted = 0;
                    for (int i = 0; i < list.size(); i++) {
                        ResearchKey key = ResearchKey.parse(list.getString(i));
                        if (ResearchApi.complete(serverPlayer, key, new ResearchCompletionContext(
                                ResourceLocation.fromNamespaceAndPath(ThaumAdditions.MOD_ID, "knowledge_tome"),
                                ResearchCompletionReason.SHARED_KNOWLEDGE)) == ResearchCompletionResult.COMPLETED) {
                            granted++;
                        }
                    }
                    player.sendSystemMessage(Component.translatable("gui.thaumadds.knowledge_tome.restored",
                            granted).withStyle(ChatFormatting.GRAY));
                    level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                            SoundSource.PLAYERS, 0.7F, 1.4F);
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                }
                return InteractionResultHolder.consume(stack);
            }

            @Override
            public List<Component> tooltipLines(ItemStack stack) {
                List<Component> tooltip = new ArrayList<>();
                CompoundTag tag = HCLitePlatform.Holder.get().itemTag(stack);
                if (tag != null && tag.contains("Research", Tag.TAG_LIST)) {
                    tooltip.add(Component.translatable("tooltip.thaumadds.knowledge_tome.entries",
                            tag.getList("Research", Tag.TAG_STRING).size()).withStyle(ChatFormatting.GRAY));
                }
                return tooltip;
            }
        });
    }

    /**
     * 1.12 {@code ItemCrystalBag}: absorbs nearby {@code thaumcraft:crystal_essence}
     * item entities into an NBT essentia store (32768 per aspect). The 1.12 GUI is
     * not ported yet, so the bag shows its contents in the tooltip instead.
     */
    public static Item crystalBag(TaPlatform platform) {
        return platform.behaviorItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), new ItemBehavior() {
            @Override
            public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player,
                                                                             InteractionHand hand) {
                ItemStack stack = player.getItemInHand(hand);
                if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer server) {
                    dev.tc4port.thaumadditions.gui.CrystalBagMenu.open(server, hand);
                }
                return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }

            @Override
            public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
                if (level.isClientSide || !(holder instanceof Player player) || level.getGameTime() % 10 != 0) {
                    return;
                }
                for (net.minecraft.world.entity.item.ItemEntity entity : level.getEntitiesOfClass(
                        net.minecraft.world.entity.item.ItemEntity.class,
                        new AABB(holder.blockPosition()).inflate(2.0D))) {
                    ItemStack carried = entity.getItem();
                    if (net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(carried.getItem())
                            .equals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "crystal_essence"))) {
                        absorb(stack, carried);
                        entity.discard();
                    }
                }
            }

            private void absorb(ItemStack stack, ItemStack essence) {
                AspectQueryApi.item(essence).amounts().forEach((aspect, amount) -> add(stack, aspect, amount));
            }

            private void add(ItemStack stack, AspectId aspect, int amount) {
                dev.tc4port.thaumadditions.items.CrystalBagStore.add(stack, aspect, amount);
            }

            @Override
            public List<Component> tooltipLines(ItemStack stack) {
                List<Component> tooltip = new ArrayList<>();
                CompoundTag tag = HCLitePlatform.Holder.get().itemTag(stack);
                if (tag == null || !tag.contains("Essentia", Tag.TAG_LIST)) {
                    return tooltip;
                }
                ListTag list = tag.getList("Essentia", Tag.TAG_COMPOUND);
                for (int i = 0; i < Math.min(6, list.size()); i++) {
                    CompoundTag entry = list.getCompound(i);
                    tooltip.add(Component.literal(TaAspect.displayName(TaAspect.readAspect(entry, "k"))
                            + " " + entry.getInt("v")).withStyle(ChatFormatting.GRAY));
                }
                return tooltip;
            }
        });
    }

    /**
     * 1.12 {@code ItemEntityCell}: right-click an entity captures its full NBT,
     * right-click a block releases it with a fresh identity.
     */
    /**
     * 1.12 {@code ItemBlueBone}: use it on a wolf and the wolf turns blue.
     *
     * <p>Upstream only turned untamed wolves, and the blue wolf is a wild animal rather than a pet,
     * so that restriction is kept.</p>
     */
    public static Item blueBone(TaPlatform platform) {
        return platform.behaviorItem(new Item.Properties(), new ItemBehavior() {
            @Override
            public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                          LivingEntity target, InteractionHand hand) {
                if (!(target instanceof net.minecraft.world.entity.animal.Wolf wolf)
                        || wolf.level().isClientSide) {
                    return InteractionResult.PASS;
                }
                if (!dev.tc4port.thaumadditions.entity.BlueWolf.transform(wolf)) {
                    return InteractionResult.PASS;
                }
                stack.shrink(1);
                player.swing(hand);
                return InteractionResult.CONSUME;
            }
        });
    }

    public static Item entityCell(TaPlatform platform) {
        return platform.behaviorItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), new ItemBehavior() {
            @Override
            public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                          InteractionHand hand) {
                if (player.level().isClientSide) {
                    return InteractionResult.SUCCESS;
                }
                CompoundTag tag = HCLitePlatform.Holder.get().itemTag(stack);
                if (tag != null && tag.contains("Entity", Tag.TAG_COMPOUND)) {
                    return InteractionResult.PASS;
                }
                CompoundTag data = new CompoundTag();
                target.save(data);
                target.remove(Entity.RemovalReason.DISCARDED);
                HCLitePlatform.Holder.get().updateItemTag(stack, nbt -> nbt.put("Entity", data));
                player.level().playSound(null, target.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL,
                        SoundSource.PLAYERS, 0.7F, 1.2F);
                return InteractionResult.CONSUME;
            }

            @Override
            public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                ItemStack stack = player.getItemInHand(hand);
                CompoundTag tag = HCLitePlatform.Holder.get().itemTag(stack);
                boolean hasEntity = tag != null && tag.contains("Entity", Tag.TAG_COMPOUND);
                if (player.isShiftKeyDown() && hasEntity) {
                    HCLitePlatform.Holder.get().updateItemTag(stack, nbt -> nbt.remove("Entity"));
                    return InteractionResultHolder.success(stack);
                }
                if (level.isClientSide || !hasEntity) {
                    return InteractionResultHolder.pass(stack);
                }
                BlockHitResult hit = (BlockHitResult) player.pick(6.0D, 1.0F, false);
                BlockPos pos = hit.getBlockPos().relative(hit.getDirection());
                CompoundTag data = tag.getCompound("Entity");
                Optional<Entity> spawned = EntityType.by(data).map(type -> type.create(level));
                if (spawned.isEmpty()) {
                    return InteractionResultHolder.pass(stack);
                }
                Entity entity = spawned.get();
                entity.load(data);
                entity.setPos(pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D);
                entity.setYRot(player.getYRot());
                level.addFreshEntity(entity);
                HCLitePlatform.Holder.get().updateItemTag(stack, nbt -> nbt.remove("Entity"));
                level.playSound(null, pos, SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.7F, 0.8F);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                return InteractionResultHolder.consume(stack);
            }

            @Override
            public List<Component> tooltipLines(ItemStack stack) {
                List<Component> tooltip = new ArrayList<>();
                CompoundTag tag = HCLitePlatform.Holder.get().itemTag(stack);
                if (tag != null && tag.contains("Entity", Tag.TAG_COMPOUND)) {
                    tooltip.add(Component.literal(tag.getCompound("Entity").getString("id"))
                            .withStyle(ChatFormatting.GRAY));
                }
                return tooltip;
            }
        });
    }

    /** 1.12 {@code ItemVoidFruit}: 5 hunger / 10 saturation, always edible. */
    public static Item voidFruit(dev.tc4port.thaumadditions.platform.TaPlatform platform) {
        return new Item(new Item.Properties()
                .food(platform.food(5, 10.0F, true))) {
        };
    }

    /**
     * 1.12 {@code ItemVisSeeds}: plants the vis crop on farmland. The 1.12 design
     * registered one crop per aspect; this port keeps one crop and carries the
     * aspect on the seed itself, so the crop's tint and its vis-pod drop stay
     * generic until the per-aspect registration pass.
     */
    public static Item visSeeds(java.util.function.Supplier<Block> crop) {
        return named(crop.get(), "item.thaumadds.vis_seeds");
    }

    /** 1.12 {@code ItemVoidSeed}: plants the void crop on farmland. */
    public static Item voidSeed(java.util.function.Supplier<Block> crop) {
        return named(crop.get(), "item.thaumadds.void_seed");
    }

    /**
     * A seed item named after itself rather than its block: {@code BlockItem} defaults
     * {@code getDescriptionId} to the crop's id, which would show "Void Crop" on a seed.
     */
    private static Item named(Block crop, String descriptionId) {
        return new BlockItem(crop, new Item.Properties()) {
            @Override
            public String getDescriptionId() {
                return descriptionId;
            }
        };
    }

    /**
     * 1.12 {@code ItemChester}: the item exists and is fully usable as an object,
     * but spawning the Chester needs {@code EntityChester} (the 5 entities are a
     * separate pass), so the placement interaction is disabled.
     */
    /** 1.12 {@code ItemChester}: places one where you point it, owned by whoever placed it. */
    public static Item chester(TaPlatform platform) {
        return platform.behaviorItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON),
                new ItemBehavior() {
                    @Override
                    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
                        Level level = context.getLevel();
                        if (level.isClientSide || context.getPlayer() == null) {
                            return InteractionResult.SUCCESS;
                        }
                        var at = context.getClickedPos().relative(context.getClickedFace());
                        var chester = dev.tc4port.thaumadditions.entity.Chester.summon(level,
                                context.getPlayer(), Vec3.atBottomCenterOf(at));
                        if (chester == null) {
                            return InteractionResult.PASS;
                        }
                        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                            context.getItemInHand().shrink(1);
                        }
                        return InteractionResult.CONSUME;
                    }
                });
    }

    /**
     * 1.12 {@code ItemSealGlobe}: opens the seal editor over {@code TileSeal}. The
     * editor is a GUI, and GUIs are the next pass, so the globe is inert for now.
     */
    public static Item sealGlobe(TaPlatform platform) {
        return platform.behaviorItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), new ItemBehavior() {
            @Override
            public List<Component> tooltipLines(ItemStack stack) {
                return List.of(Component.translatable("tooltip.thaumadds.seal_globe.pending")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        });
    }

    /** Plain material items with no behaviour in 1.12 either. */
    public static Item plain() {
        return new Item(new Item.Properties());
    }

    /** Unused guards keeping the shared imports meaningful. */
    static UUID unused() {
        return UUID.randomUUID();
    }

    static List<AspectId> unusedAspects() {
        return new ArrayList<>();
    }
}
