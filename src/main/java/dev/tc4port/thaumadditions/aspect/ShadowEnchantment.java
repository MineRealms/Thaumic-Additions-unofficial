package dev.tc4port.thaumadditions.aspect;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

import dev.tc4port.thaumcraft.api.aspect.AspectApi;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.aspect.AspectList;
import net.minecraft.resources.ResourceLocation;

/**
 * 1.12 {@code ShadowEnchantment}: what a shadow enchanter charges in essentia for writing an
 * enchantment onto a tool.
 *
 * <p>Upstream registered twenty-three enchantments, each with a list of aspects priced either per
 * level or flat. Six of those charges were Thaumic Additions' own Knowledge aspects — {@code
 * exitium}, {@code imperium}, {@code caeles}, {@code visum}, {@code fluctus}, {@code ventus} — which
 * TC4R does not have, so those terms are dropped and every enchantment keeps the rest of its price.
 * {@code infinity} was priced in {@code caeles} alone and so has no price left; it is not in the
 * table at all, which is what makes the enchanter refuse it, exactly as 1.12 refused an enchantment
 * it had no entry for.</p>
 *
 * <p>The table is keyed by the enchantment's registry id rather than by a Java constant, because
 * 1.20.1 and 1.21.1 disagree about what a vanilla enchantment constant even is, and because the ids
 * are what the block entity already persists. The map is fixed at class load; only the aspect
 * lookups against TC4R's registry happen per call, since a data pack may remove an aspect.</p>
 */
public final class ShadowEnchantment {
    private static final Map<ResourceLocation, AspectBuilder> COSTS = new LinkedHashMap<>();

    static {
        // 1.12 registered these against Aspect.TOOL, PROTECT, EXCHANGE, AVERSION, BEAST, DESIRE,
        // ELDRITCH, MIND, FIRE, FLIGHT, AIR, ORDER, WATER and COLD; the ids below are TC4R's names
        // for the same fourteen aspects.
        add("unbreaking", aspectBuilder().multiplyByLvl("instrumentum", 16).multiplyByLvl("tutamen", 4));
        add("thorns", aspectBuilder().multiplyByLvl("permutatio", 20).multiplyByLvl("tutamen", 6));
        add("sharpness", aspectBuilder().multiplyByLvl("telum", 20));
        add("bane_of_arthropods", aspectBuilder().multiplyByLvl("telum", 20).multiplyByLvl("bestia", 10));
        add("punch", aspectBuilder().multiplyByLvl("permutatio", 12));
        add("protection", aspectBuilder().multiplyByLvl("tutamen", 20));
        add("lure", aspectBuilder().multiplyByLvl("fames", 20));
        add("looting", aspectBuilder().multiplyByLvl("fames", 20).constant("permutatio", 10));
        add("knockback", aspectBuilder().multiplyByLvl("permutatio", 12));
        // infinity is absent: its only 1.12 charge was the Knowledge aspect caeles.
        add("fortune", aspectBuilder().multiplyByLvl("fames", 20)
                .multiplyByLvl("alienis", 10).multiplyByLvl("cognitio", 5));
        add("fire_protection", aspectBuilder().multiplyByLvl("tutamen", 20).multiplyByLvl("ignis", 10));
        add("feather_falling", aspectBuilder().multiplyByLvl("volatus", 20).multiplyByLvl("aer", 10));
        add("blast_protection", aspectBuilder().multiplyByLvl("tutamen", 20));
        add("silk_touch", aspectBuilder().constant("ordo", 100).constant("fames", 10));
        add("smite", aspectBuilder().multiplyByLvl("telum", 10));
        add("fire_aspect", aspectBuilder().multiplyByLvl("telum", 10).multiplyByLvl("ignis", 20));
        add("luck_of_the_sea", aspectBuilder().multiplyByLvl("aqua", 20).multiplyByLvl("permutatio", 15));
        add("power", aspectBuilder().multiplyByLvl("telum", 15)
                .multiplyByLvl("volatus", 10).multiplyByLvl("aer", 2));
        add("efficiency", aspectBuilder().multiplyByLvl("ordo", 5)
                .multiplyByLvl("permutatio", 10).multiplyByLvl("instrumentum", 10));
        add("frost_walker", aspectBuilder().multiplyByLvl("gelum", 20));
        add("respiration", aspectBuilder().multiplyByLvl("aqua", 20).multiplyByLvl("permutatio", 5));
        add("mending", aspectBuilder().multiplyByLvl("fames", 200).multiplyByLvl("permutatio", 500));
    }

    private ShadowEnchantment() {
    }

    /**
     * What one application of this enchantment costs at this level, or {@code null} when the
     * enchanter cannot write it.
     */
    public static AspectList costOf(ResourceLocation enchantment, int level) {
        AspectBuilder cost = enchantment == null ? null : COSTS.get(enchantment);
        return cost == null ? null : cost.apply(level);
    }

    /** Whether the enchanter can write this enchantment at all. */
    public static boolean canWrite(ResourceLocation enchantment) {
        return enchantment != null && COSTS.containsKey(enchantment);
    }

    /** Every enchantment the enchanter can write, in registration order. */
    public static List<ResourceLocation> registry() {
        return List.copyOf(COSTS.keySet());
    }

    private static void add(String enchantment, AspectBuilder builder) {
        if (builder.isEmpty()) {
            return;
        }
        COSTS.put(ResourceLocation.withDefaultNamespace(enchantment), builder);
    }

    private static AspectBuilder aspectBuilder() {
        return new AspectBuilder();
    }

    /**
     * The 1.12 {@code AspectBuilder}: aspect ids paired with how their amount grows with the
     * enchantment's level, turned into a finished list on demand.
     */
    private static final class AspectBuilder {
        private final Map<AspectId, IntUnaryOperator> parts = new LinkedHashMap<>();

        AspectBuilder multiplyByLvl(String aspect, int baseAmount) {
            return put(aspect, level -> baseAmount * level);
        }

        AspectBuilder constant(String aspect, int amount) {
            return put(aspect, level -> amount);
        }

        private AspectBuilder put(String aspect, IntUnaryOperator amount) {
            AspectId id;
            try {
                id = AspectId.parse(aspect);
            } catch (RuntimeException unknown) {
                return this;
            }
            parts.put(id, amount);
            return this;
        }

        boolean isEmpty() {
            return parts.isEmpty();
        }

        /** Builds the list; the flat parts ignore {@code level}, as in 1.12. */
        AspectList apply(int level) {
            AspectList list = new AspectList();
            for (Map.Entry<AspectId, IntUnaryOperator> part : parts.entrySet()) {
                // The registry check happens here rather than at parse time: the ids above are all
                // TC4R's, but a data pack is free to remove one.
                if (AspectApi.contains(part.getKey())) {
                    list.add(part.getKey(), part.getValue().applyAsInt(level));
                }
            }
            return list;
        }
    }
}
