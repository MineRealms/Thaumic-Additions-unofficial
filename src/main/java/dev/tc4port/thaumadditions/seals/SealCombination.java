package dev.tc4port.thaumadditions.seals;

import dev.tc4port.thaumadditions.tiles.TileSeal;
import dev.tc4port.thaumcraft.api.aspect.AspectId;

/**
 * 1.12 {@code SealCombination}: the three symbols that make one seal, where a {@code null} slot
 * means "any symbol will do".
 *
 * <p>The lookup is by shape rather than by value, which is why the wildcard matters: the water seal
 * is "water, then two of anything", and without nulls it would need a registry entry per pair of
 * extra symbols.</p>
 */
public final class SealCombination {
    private final AspectId[] slots = new AspectId[TileSeal.SLOTS];
    private final String descriptionKey;

    public SealCombination(AspectId first, AspectId second, AspectId third, String descriptionKey) {
        this.slots[0] = first;
        this.slots[1] = second;
        this.slots[2] = third;
        this.descriptionKey = descriptionKey;
    }

    /** The key the seal's description is looked up under. */
    public String descriptionKey() {
        return descriptionKey;
    }

    /** Whether a seal's three symbols are this combination. */
    public boolean matches(TileSeal seal) {
        for (int slot = 0; slot < TileSeal.SLOTS; slot++) {
            AspectId required = slots[slot];
            if (required != null && !required.equals(seal.symbol(slot))) {
                return false;
            }
        }
        return true;
    }

    /** How many slots are pinned; a more specific combination wins over a looser one. */
    public int specificity() {
        int pinned = 0;
        for (AspectId slot : slots) {
            if (slot != null) {
                ++pinned;
            }
        }
        return pinned;
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        for (AspectId slot : slots) {
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(slot == null ? "*" : slot.value().getPath());
        }
        return out.toString();
    }
}
