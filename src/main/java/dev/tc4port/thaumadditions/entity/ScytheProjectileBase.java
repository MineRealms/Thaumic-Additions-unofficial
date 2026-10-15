package dev.tc4port.thaumadditions.entity;

import dev.tc4port.thaumadditions.TaFx;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * 1.12 {@code EntityMithminiteScythe}: the blade the mithminite scythe throws, which cuts whatever
 * it reaches and then spends a moment fading out.
 *
 * <p>It flies flat — 1.12 gave it no gravity at all — passes through its thrower for the first two
 * ticks, cuts one living thing for fourteen points that ignore armour, and then retracts. The
 * concrete class is per node because {@code Entity#defineSynchedData} changed shape between the two
 * versions, exactly as it did for the essentia shot.</p>
 */
public abstract class ScytheProjectileBase extends ThrowableProjectile {
    /** 1.12 let the blade ignore its thrower for two ticks. */
    private static final int ARMING_TICKS = 2;
    /** 1.12's damage: fourteen, bypassing armour. */
    private static final float DAMAGE = 14.0F;
    /** After it cuts, it lives fifteen more ticks and no longer than sixty in total. */
    private static final int LINGER = 15;
    private static final int MAX_LIFETIME = 60;

    /** When the blade gives up; server-side only, since the removal itself is what the client sees. */
    private int disposeTime = MAX_LIFETIME;
    private boolean hasHit;

    protected ScytheProjectileBase(EntityType<? extends ScytheProjectileBase> type, Level level) {
        super(type, level);
    }

    protected ScytheProjectileBase(EntityType<? extends ScytheProjectileBase> type, Level level,
                                   LivingEntity thrower) {
        super(type, thrower, level);
    }

    /** 1.12 {@code getGravityVelocity}: none. */
    @Override
    public boolean isNoGravity() {
        return true;
    }

    /** How faded the blade is, {@code 1} while it is whole. */
    public float alpha() {
        return Math.max(0.0F, Math.min(1.0F, (disposeTime - tickCount) / (float) LINGER));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            // 1.12 drew one mote every other tick, and four while the blade was fading.
            boolean fading = alpha() < 1.0F;
            if (fading || tickCount % 2 == 0) {
                int motes = fading ? 4 : 1;
                for (int index = 0; index < motes; index++) {
                    TaFx.dust(level(),
                            getX() + (random.nextFloat() - random.nextFloat()),
                            getY() + (random.nextFloat() - random.nextFloat()),
                            getZ() + (random.nextFloat() - random.nextFloat()),
                            0xFF80FF, 0.0D, 0.0D, 0.0D);
                }
            }
        } else if (tickCount > disposeTime) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (level().isClientSide || hasHit) {
            return;
        }
        if (hit.getEntity() instanceof LivingEntity victim && victim != getOwner()
                && victim.isAlive()) {
            victim.hurt(level().damageSources().indirectMagic(this, getOwner()), DAMAGE);
            hasHit = true;
            retract();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!level().isClientSide) {
            hasHit = true;
            retract();
        }
    }

    /** 1.12 pulled the blade back fifteen ticks after it cut, and no later than tick sixty. */
    private void retract() {
        disposeTime = Math.min(disposeTime, Math.min(tickCount + LINGER, MAX_LIFETIME));
    }

    /** Whether the blade has already cut something, so it only ever cuts once. */
    protected boolean hasHit() {
        return hasHit;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("DisposeTime", disposeTime);
        tag.putBoolean("HasHit", hasHit);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.disposeTime = tag.contains("DisposeTime") ? tag.getInt("DisposeTime") : MAX_LIFETIME;
        this.hasHit = tag.getBoolean("HasHit");
    }

    /** Kept so a subclass can name the arming rule without repeating the constant. */
    protected static boolean armed(int tickCount) {
        return tickCount >= ARMING_TICKS;
    }

    /** The owner, or {@code null}. */
    @Override
    public Entity getOwner() {
        return super.getOwner();
    }
}
