package dev.tc4port.thaumadditions.entity;

import dev.tc4port.thaumadditions.TaFx;
import dev.tc4port.thaumadditions.aspect.EdibleAspect;
import dev.tc4port.thaumadditions.aspect.TaAspect;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.aspect.AspectList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1.12 {@code EntityEssentiaShot}: the projectile the essentia pistol fires, carrying whatever the
 * pistol was loaded with.
 *
 * <p>The concrete class is per node, because {@code Entity#defineSynchedData} takes a builder on
 * 1.21.1 and nothing on 1.20.1, and there is no way to write one override that satisfies both.</p>
 *
 * <p>It does exactly two things: it draws a trail in the colour of its aspect, and when it lands on
 * a living thing it feeds that thing half again as much essentia as it was carrying. The 1.12
 * particle was HammerCore's own; the port uses the shared dust helper, which is the same colour and
 * one fewer class.</p>
 */
public abstract class EssentiaShotBase extends ThrowableProjectile {
    /** 1.12 ignored the first two ticks, so a shot fired at point-blank did not hit its thrower. */
    private static final int ARMING_TICKS = 2;
    /** The payload is worth half again as much as what was loaded. */
    private static final float PAYLOAD_MULTIPLIER = 1.5F;

    private AspectList payload = new AspectList();

    protected EssentiaShotBase(EntityType<? extends EssentiaShotBase> type, Level level) {
        super(type, level);
    }

    protected EssentiaShotBase(EntityType<? extends EssentiaShotBase> type, Level level,
                               LivingEntity thrower, AspectList payload) {
        super(type, thrower, level);
        this.payload = payload == null ? new AspectList() : payload;
    }

    /** What the shot is carrying. */
    public AspectList payload() {
        return payload;
    }

    /** The colour of the first aspect it carries, or a neutral grey when it carries none. */
    public int payloadColour() {
        return TaAspect.color(firstAspect());
    }

    private AspectId firstAspect() {
        for (AspectId aspect : payload.aspectsSortedByAmount()) {
            if (payload.amount(aspect) > 0) {
                return aspect;
            }
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            // 1.12 drew four dots along the step it just took.
            Vec3 here = position();
            Vec3 back = here.subtract(getDeltaMovement());
            for (int step = 0; step <= 4; step++) {
                double slide = step / 4.0D;
                TaFx.aspectDust(level(),
                        back.x + (here.x - back.x) * slide,
                        back.y + (here.y - back.y) * slide,
                        back.z + (here.z - back.z) * slide,
                        firstAspect(), 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        apply(hit.getEntity());
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        apply(null);
    }

    /** 1.12 {@code onImpact}: feed the victim, splash, and go. */
    private void apply(net.minecraft.world.entity.Entity target) {
        if (tickCount < ARMING_TICKS) {
            return;
        }
        if (!level().isClientSide) {
            if (target instanceof LivingEntity living && payload.visSize() > 0) {
                AspectList scaled = new AspectList();
                for (AspectId aspect : payload.aspectsSortedByAmount()) {
                    scaled.add(aspect, Math.round(payload.amount(aspect) * PAYLOAD_MULTIPLIER));
                }
                EdibleAspect.execute(living, scaled);
            }
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.SLIME_ATTACK,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        java.util.List<AspectId> ids = new java.util.ArrayList<>();
        java.util.List<Integer> amounts = new java.util.ArrayList<>();
        for (AspectId aspect : payload.aspectsSortedByAmount()) {
            ids.add(aspect);
            amounts.add(payload.amount(aspect));
        }
        TaAspect.writeAspects(tag, "Aspects", ids, amounts);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        AspectList read = new AspectList();
        java.util.List<AspectId> ids = TaAspect.readAspects(tag, "Aspects");
        java.util.List<Integer> amounts = TaAspect.readAspectAmounts(tag, "Aspects");
        for (int index = 0; index < ids.size() && index < amounts.size(); index++) {
            read.add(ids.get(index), amounts.get(index));
        }
        this.payload = read;
    }
}
