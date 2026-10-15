package dev.tc4port.thaumadditions.entity;

import dev.tc4port.thaumadditions.TaFx;
import dev.tc4port.thaumadditions.registry.Content;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;

/**
 * 1.12 {@code EntityBlueWolf}: an ordinary wolf that has been turned blue by a blue bone.
 *
 * <p>Upstream copied the whole wolf — its AI, its taming, its sitting — because that was the
 * cheapest way to get a wolf in 1.12. The port subclasses the vanilla one instead, which is the
 * same animal with three numbers changed and no code duplicated. Its model is the vanilla wolf's,
 * so it needs no model of its own either; only its textures differ.</p>
 *
 * <p>The transformation is upstream's: only an untamed, still-thinking wolf turns, and it is
 * replaced in place rather than tamed. 1.12 played a forty-tick animation first, which the port
 * does not have, so the swap is immediate and announces itself with a burst instead.</p>
 */
public class BlueWolf extends Wolf {
    /** 1.12 {@code applyEntityAttributes}: health 20, speed 0.30, attack 5. */
    private static final double SPEED = 0.30D;
    private static final double MAX_HEALTH = 20.0D;
    private static final double ATTACK_DAMAGE = 5.0D;

    public BlueWolf(EntityType<? extends BlueWolf> type, Level level) {
        super(type, level);
    }

    /** The wolf's own attributes, with the three numbers 1.12 changed. */
    public static AttributeSupplier.Builder createBlueWolfAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE);
    }

    /** Whether this wolf can be turned; only an untamed one that is thinking can. */
    public static boolean canTransform(Wolf wolf) {
        return wolf.isAlive() && !wolf.isTame() && !wolf.isNoAi();
    }

    /**
     * 1.12 {@code trasfurmate}: replace a wolf with its blue counterpart.
     *
     * @return whether the wolf was turned, which is what the blue bone consumes itself on.
     */
    public static boolean transform(Wolf wolf) {
        if (!(wolf.level() instanceof ServerLevel level) || !canTransform(wolf)) {
            return false;
        }
        BlueWolf blue = Content.BLUE_WOLF.get().create(level);
        if (blue == null) {
            return false;
        }
        blue.moveTo(wolf.getX(), wolf.getY(), wolf.getZ(), wolf.getYRot(), wolf.getXRot());
        blue.setHealth(blue.getMaxHealth());
        level.addFreshEntity(blue);

        // The burst is what 1.12's animation packet drew, minus the forty ticks of it.
        for (int index = 0; index < 24; index++) {
            TaFx.dust(level,
                    wolf.getX() + (level.getRandom().nextDouble() - 0.5D) * 1.2D,
                    wolf.getY() + level.getRandom().nextDouble() * 1.2D,
                    wolf.getZ() + (level.getRandom().nextDouble() - 0.5D) * 1.2D,
                    0x3A6BFF, 0.0D, 0.05D, 0.0D);
        }
        level.sendParticles(ParticleTypes.ENCHANT, wolf.getX(), wolf.getY() + 0.6D, wolf.getZ(),
                12, 0.4D, 0.5D, 0.4D, 0.05D);
        level.playSound(null, wolf.blockPosition(), SoundEvents.WOLF_AMBIENT, SoundSource.NEUTRAL,
                1.0F, 0.7F);
        wolf.discard();
        return true;
    }
}
