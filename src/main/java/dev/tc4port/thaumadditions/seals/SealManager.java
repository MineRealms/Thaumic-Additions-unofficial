package dev.tc4port.thaumadditions.seals;

import dev.tc4port.thaumadditions.tiles.TileSeal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 1.12 {@code SealManager}: turns the three symbols in a seal block into the behaviour they mean.
 *
 * <p>Upstream stored combinations in a list and took the first that matched. The port takes the
 * most specific match instead, so that the harvest seal with an order symbol in its third slot wins
 * over the one that leaves that slot open — upstream got the same result by registering them in the
 * right order, which is a fragile thing to depend on.</p>
 */
public final class SealManager {
    private record Entry(SealCombination combination, Function<TileSeal, SealInstance> factory) {
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private SealManager() {
    }

    /** Registers one seal. Called once, from content setup. */
    public static void register(SealCombination combination, Function<TileSeal, SealInstance> factory) {
        ENTRIES.add(new Entry(combination, factory));
    }

    /** The behaviour a seal block's symbols mean, or {@code null} when they mean nothing yet. */
    public static SealInstance makeInstance(TileSeal seal) {
        if (!seal.isComplete()) {
            return null;
        }
        Entry best = null;
        for (Entry entry : ENTRIES) {
            if (!entry.combination().matches(seal)) {
                continue;
            }
            if (best == null || entry.combination().specificity() > best.combination().specificity()) {
                best = entry;
            }
        }
        return best == null ? null : best.factory().apply(seal);
    }

    /** The description key of the seal these symbols mean, or {@code null}. */
    public static String descriptionKey(TileSeal seal) {
        for (Entry entry : ENTRIES) {
            if (entry.combination().matches(seal)) {
                return entry.combination().descriptionKey();
            }
        }
        return null;
    }

    /** How many seals are registered; the content pass logs it. */
    public static int count() {
        return ENTRIES.size();
    }
}
