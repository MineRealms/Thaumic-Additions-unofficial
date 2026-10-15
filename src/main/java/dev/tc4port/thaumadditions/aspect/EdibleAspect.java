package dev.tc4port.thaumadditions.aspect;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.IntUnaryOperator;

import dev.tc4port.thaumadditions.platform.TaEffect;
import dev.tc4port.thaumadditions.platform.TaPlatform;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.aspect.AspectList;
import dev.tc4port.thaumcraft.api.research.ResearchApi;
import dev.tc4port.thaumcraft.api.research.ResearchDiscoveryResult;
import dev.tc4port.thaumcraft.api.research.ResearchKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * 1.12 {@code EdibleAspect}: feed a living entity a bundle of essentia and let each aspect do
 * whatever that aspect does to a body.
 *
 * <p>Upstream was a table of {@code Aspect -> (entity, amount) -> used?} handlers built by
 * {@code addEatCall}, plus a second table of "complex" handlers keyed on whole aspect lists. The
 * complex table was never populated by the mod itself — every registration went through
 * {@code addEatCall} — so only the eaten table is ported.</p>
 *
 * <p>Three upstream handlers could not come across whole, and each is called out where it lives:
 * the {@code SONUS} entry was a Thaumic Additions aspect that TC4R does not have; {@code MIND}
 * granted partial progress in TC4's research categories, which TC4R replaced with per-entry
 * discoveries, so the port reveals one undiscovered entry instead; and {@code FLUX} also added
 * Warp, which TC4R's {@code PlayerWarpApi} only exposes read-only.</p>
 *
 * <p>Durations, amplifiers and the "don't overwrite a running effect" rule are 1.12's exactly. The
 * callers are the aura disperser, the arcane cake and anything else that hands essentia to a mob;
 * every one of them cares about the return value, which is the aspect list that was actually used
 * up.</p>
 */
public final class EdibleAspect {
    /** 1.12 {@code EdibleAspect.MAX_ESSENTIA}: the denominator in the scaled amplifiers. */
    public static final int MAX_ESSENTIA = 32;

    /** One aspect's handler. Returns whether the aspect was consumed. */
    private interface Eater {
        boolean eat(LivingEntity entity, int amount);
    }

    private static final Map<AspectId, Eater> EATERS = new LinkedHashMap<>();

    static {
        // Aspect -> effect, with 1.12's exact duration and amplifier expressions.
        effect("ignis", TaEffect.FIRE_RESISTANCE, c -> 10 + c * c, c -> 0);
        effect("aqua", TaEffect.WATER_BREATHING, c -> 10 + c * c, c -> 0);
        effect("vinculum", TaEffect.SLOWNESS, c -> 20 + c * c * 20, c -> 1);
        effect("sensus", TaEffect.NIGHT_VISION, c -> 400 + c * c, c -> 0);
        effect("tenebrae", TaEffect.BLINDNESS, c -> 10 + c * c, c -> 0);
        effect("permutatio", TaEffect.NAUSEA, c -> 10 + c * c, c -> 0);
        effect("potentia", TaEffect.STRENGTH, c -> 20 + c * c, c -> (int) Math.sqrt(c));
        effect("instrumentum", TaEffect.HASTE, c -> 20 + c * c, c -> (int) Math.sqrt(c));
        effect("motus", TaEffect.MOVEMENT_SPEED, c -> 10 + c * c, c -> c * 3 / MAX_ESSENTIA);
        effect("fames", TaEffect.HUNGER, c -> 100 + c * c, c -> c * 5 / MAX_ESSENTIA);
        effect("tutamen", TaEffect.RESISTANCE, c -> 200 + c * c * 2, c -> c * 10 / MAX_ESSENTIA);
        effect("auram", TaEffect.WARP_WARD, c -> 20 * 60 + c * 200, c -> 0);
        effect("vitium", TaEffect.FLUX_TAINT, c -> 10 + c * c, c -> 0);

        // 1.12's Aspect.DEATH: plain magic damage, no effect.
        register("mortuus", (entity, amount) -> {
            entity.hurt(entity.damageSources().magic(), 1.0F + (float) Math.sqrt(amount));
            return true;
        });

        // 1.12's Aspect.LIFE: heal, but only someone who is actually hurt.
        register("victus", (entity, amount) -> {
            boolean healthy = entity.getMaxHealth() - entity.getHealth() < 0.5F;
            if (!healthy) {
                entity.heal(1.0F + (float) Math.sqrt(amount));
            }
            return !healthy;
        });

        // 1.12 granted research-category progress here. TC4R has discoveries rather than
        // categories, so the port reveals one entry the player has not seen yet.
        register("cognitio", (entity, amount) -> {
            if (!(entity instanceof ServerPlayer player)) {
                return false;
            }
            return revealOneEntry(player);
        });
    }

    private EdibleAspect() {
    }

    private static void effect(String aspect, TaEffect nativeEffect, IntUnaryOperator duration,
                               IntUnaryOperator amplifier) {
        register(aspect, (entity, amount) -> addPotionEffect(entity, nativeEffect,
                duration.applyAsInt(amount), amplifier.applyAsInt(amount)));
    }

    /** Registers a handler, ignoring ids this TC4R build does not define. */
    private static void register(String aspect, Eater eater) {
        AspectId id;
        try {
            id = AspectId.parse(aspect);
        } catch (RuntimeException unknown) {
            return;
        }
        EATERS.put(id, eater);
    }

    /**
     * 1.12's {@code addPotionEffect}: apply, unless the effect is already running — and even then
     * night vision under five seconds and anything under two seconds are refreshed anyway, so the
     * caller does not lose the aspect for nothing.
     */
    public static boolean addPotionEffect(LivingEntity entity, TaEffect effect, int ticks, int amplifier) {
        int remaining = TaPlatform.Active.get().effectRemaining(entity, effect);
        boolean refreshing = effect == TaEffect.NIGHT_VISION ? remaining < 300 : remaining <= 40;
        if (remaining >= 0 && !refreshing) {
            return false;
        }
        TaPlatform.Active.get().applyEffect(entity, effect, ticks, amplifier);
        return true;
    }

    /**
     * 1.12's {@code execute}: run every handler whose aspect is in the bundle, remove what was used
     * from the working copy, and hand back what was consumed.
     *
     * <p>1.12 iterated the handlers in map order over a copy of the input; the port walks the
     * input's aspects in the upstream sort order instead, which is the same set in a stable order
     * and keeps the consumed list deterministic.</p>
     */
    public static AspectList execute(LivingEntity entity, AspectList aspects) {
        AspectList used = new AspectList();
        if (entity == null || aspects == null || aspects.visSize() == 0) {
            return used;
        }
        for (AspectId aspect : aspects.aspectsSortedByAmount()) {
            Eater eater = EATERS.get(aspect);
            if (eater == null) {
                continue;
            }
            int amount = aspects.amount(aspect);
            if (amount > 0 && eater.eat(entity, amount)) {
                used.add(aspect, amount);
            }
        }
        return used;
    }

    /** Whether any handler is registered for this aspect at all. */
    public static boolean isEdible(AspectId aspect) {
        return aspect != null && EATERS.containsKey(aspect);
    }

    /** Reveals one research entry the player has not seen yet, first in catalog order. */
    private static boolean revealOneEntry(ServerPlayer player) {
        for (ResearchKey key : ResearchApi.registry().entries().keySet()) {
            if (!ResearchApi.isDiscovered(player, key) && !ResearchApi.isComplete(player, key)) {
                return ResearchApi.discover(player, key) == ResearchDiscoveryResult.DISCOVERED;
            }
        }
        return false;
    }
}
