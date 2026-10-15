package dev.tc4port.thaumadditions.items;

import java.util.List;

import dev.tc4port.thaumadditions.aspect.TaAspect;
import dev.tc4port.thaumadditions.platform.TaPlatform;
import dev.tc4port.hclite.platform.HCLitePlatform;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The 1.12 weapon items.
 *
 * <p>{@code the_beheader} keeps its 20% head-drop rule, {@code mithminite_scythe}
 * keeps its 12-damage armour-piercing swing with a 2.5-block sweep,
 * {@code shadow_beam_staff} keeps the reflective beam, and
 * {@code essentia_pistol} keeps the essentia-fed hitscan shot. Every one of
 * these is self-contained: no packets, no custom entities, no GUIs.</p>
 */
public final class WeaponItems {
    private WeaponItems() {
    }

    /** 1.12 {@code ItemTheBeheader}: +10 damage, -2.8 speed, 4096 uses, 20% head drops. */
    public static Item theBeheader(dev.tc4port.thaumadditions.platform.TaPlatform platform,
                                   net.minecraft.world.item.Tier tier) {
        return platform.sword(tier, 3.0F, -2.8F, new Item.Properties().rarity(Rarity.RARE),
                (stack, target, attacker) -> {
                    if (!target.level().isClientSide && target.isDeadOrDying() && attacker instanceof Player player
                            && target.getRandom().nextInt(100) < 20) {
                        ItemStack head = headFor(target);
                        if (!head.isEmpty()) {
                            target.spawnAtLocation(head);
                        }
                    }
                    return true;
                });
    }

    private static ItemStack headFor(LivingEntity entity) {
        if (entity instanceof Zombie) {
            return new ItemStack(Items.ZOMBIE_HEAD);
        }
        if (entity instanceof Skeleton) {
            return new ItemStack(Items.SKELETON_SKULL);
        }
        if (entity instanceof Creeper) {
            return new ItemStack(Items.CREEPER_HEAD);
        }
        if (entity instanceof Player) {
            return new ItemStack(Items.PLAYER_HEAD);
        }
        return ItemStack.EMPTY;
    }

    /** 1.12 {@code ItemMithminiteScythe}: 12 armour-piercing damage plus a 2.5-block sweep. */
    public static Item mithminiteScythe(dev.tc4port.thaumadditions.platform.TaPlatform platform,
                                        net.minecraft.world.item.Tier tier) {
        return platform.sword(tier, 3.0F, -2.4F, new Item.Properties().rarity(Rarity.RARE),
                (stack, target, attacker) -> {
                    if (!target.level().isClientSide && attacker instanceof Player player) {
                        AABB area = target.getBoundingBox().inflate(2.5D);
                        List<LivingEntity> nearby = target.level().getEntitiesOfClass(LivingEntity.class, area,
                                other -> other != player && other != target && other.isAlive());
                        for (LivingEntity other : nearby) {
                            other.hurt(target.damageSources().playerAttack(player), 6.0F);
                        }
                    }
                    return true;
                });
    }

    /**
     * 1.12 {@code ItemShadowBeamStaff}: 500-point ELDRITCH reservoir, reflective beam
     * dealing 5 damage per entity per segment, 20-tick cooldown.
     */
    public static Item shadowBeamStaff(TaPlatform platform) {
        return platform.behaviorItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), new ItemBehavior() {
            @Override
            public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                ItemStack stack = player.getItemInHand(hand);
                if (!level.isClientSide) {
                    CompoundTag tag = HCLitePlatform.Holder.get().itemTag(stack);
                    int charge = tag == null ? 0 : tag.getInt("Essentia");
                    if (charge > 0) {
                        fireBeam((ServerLevel) level, player, player.getEyePosition(1.0F),
                                player.getViewVector(1.0F), 0, 80.0D);
                        HCLitePlatform.Holder.get().updateItemTag(stack, nbt -> nbt.putInt("Essentia", charge - 1));
                        player.getCooldowns().addCooldown(stack.getItem(), 20);
                        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                                SoundSource.PLAYERS, 5.0F, 1.4F);
                    }
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }

            /** Fires one beam segment, reflecting off the first solid face, up to 3 segments. */
            private void fireBeam(ServerLevel level, Player player, Vec3 start, Vec3 direction,
                                  int depth, double budget) {
                if (depth >= 3 || budget <= 0.0D) {
                    return;
                }
                double reach = Math.min(budget, 32.0D);
                Vec3 end = start.add(direction.scale(reach));

                EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, start, end,
                        new AABB(start, end).inflate(1.0D),
                        entity -> entity instanceof LivingEntity && entity != player);
                BlockHitResult blockHit = level.clip(new net.minecraft.world.level.ClipContext(start, end,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE, player));
                if (entityHit != null && (blockHit == null || blockHit.getType() == HitResult.Type.MISS
                        || entityHit.getLocation().distanceTo(start) < blockHit.getLocation().distanceTo(start))) {
                    double travelled = entityHit.getLocation().distanceTo(start);
                    if (entityHit.getEntity() instanceof LivingEntity victim) {
                        victim.hurt(level.damageSources().playerAttack(player), 5.0F * (depth + 1));
                    }
                    spawnSegment(level, start, entityHit.getLocation());
                    fireBeam(level, player, entityHit.getLocation(), direction, depth + 1, budget - travelled);
                    return;
                }
                if (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK) {
                    double travelled = blockHit.getLocation().distanceTo(start);
                    spawnSegment(level, start, blockHit.getLocation());
                    Vec3 normal = Vec3.atLowerCornerOf(blockHit.getDirection().getNormal());
                    Vec3 reflected = direction.subtract(normal.scale(2.0D * direction.dot(normal))).normalize();
                    fireBeam(level, player, blockHit.getLocation().add(reflected.scale(0.05D)), reflected,
                            depth + 1, budget - travelled);
                    return;
                }
                spawnSegment(level, start, end);
            }

            private void spawnSegment(ServerLevel level, Vec3 from, Vec3 to) {
                int steps = Math.max(2, (int) (from.distanceTo(to) * 4));
                for (int i = 0; i <= steps; i++) {
                    Vec3 point = from.lerp(to, i / (double) steps);
                    level.sendParticles(ParticleTypes.DRAGON_BREATH, point.x, point.y, point.z, 1,
                            0.0D, 0.0D, 0.0D, 0.0D);
                }
            }

            @Override
            public List<Component> tooltipLines(ItemStack stack) {
                CompoundTag tag = HCLitePlatform.Holder.get().itemTag(stack);
                return List.of(Component.translatable("tooltip.thaumadds.essentia",
                        tag == null ? 0 : tag.getInt("Essentia")).withStyle(ChatFormatting.GRAY));
            }

            @Override
            public boolean isFoil(ItemStack stack) {
                CompoundTag tag = HCLitePlatform.Holder.get().itemTag(stack);
                return tag != null && tag.getInt("Essentia") > 0;
            }
        });
    }

    /**
     * 1.12 {@code ItemEssentiaPistol}: consumes one stored essentia point and hits the
     * first living entity within 32 blocks. The 1.12 GUI that picked the aspect and the
     * shot count is replaced by "whatever aspect the loaded jar holds".
     */
    public static Item essentiaPistol(TaPlatform platform) {
        return platform.behaviorItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), new ItemBehavior() {
            @Override
            public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                ItemStack stack = player.getItemInHand(hand);
                if (!level.isClientSide) {
                    CompoundTag tag = HCLitePlatform.Holder.get().itemTag(stack);
                    AspectId aspect = TaAspect.readAspect(tag, "Aspect");
                    int amount = tag == null ? 0 : tag.getInt("Essentia");
                    if (aspect != null && amount > 0) {
                        Vec3 start = player.getEyePosition(1.0F);
                        Vec3 end = start.add(player.getViewVector(1.0F).scale(32.0D));
                        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, start, end,
                                new AABB(start, end).inflate(1.0D),
                                entity -> entity instanceof LivingEntity && entity != player);
                        if (hit != null && hit.getEntity() instanceof LivingEntity victim) {
                            victim.hurt(level.damageSources().playerAttack(player), 6.0F);
                            applyAspectEffect(victim, aspect);
                            victim.push(player.getViewVector(1.0F).x * 0.6D, 0.25D,
                                    player.getViewVector(1.0F).z * 0.6D);
                        }
                        HCLitePlatform.Holder.get().updateItemTag(stack, nbt -> nbt.putInt("Essentia", amount - 1));
                        player.getCooldowns().addCooldown(stack.getItem(), 10);
                        level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST,
                                SoundSource.PLAYERS, 1.0F, 1.6F);
                    }
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }

            private void applyAspectEffect(LivingEntity victim, AspectId aspect) {
                switch (aspect.value().getPath()) {
                    case "ignis", "infernum" -> platform.ignite(victim, 5.0F);
                    case "aqua", "gelum", "aer", "victus" ->
                            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0));
                    case "terra", "ordo" -> victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                    case "perditio", "exitium" -> victim.hurt(victim.damageSources().magic(), 2.0F);
                    default -> {
                    }
                }
            }

            @Override
            public List<Component> tooltipLines(ItemStack stack) {
                AspectId aspect = TaAspect.readAspect(HCLitePlatform.Holder.get().itemTag(stack), "Aspect");
                return List.of(Component.translatable("tooltip.thaumadds.essentia_pistol.aspect",
                        aspect == null ? "-" : TaAspect.displayName(aspect)).withStyle(ChatFormatting.GRAY));
            }
        });
    }
}
