package dev.tc4port.thaumadditions.items;

import dev.tc4port.hclite.platform.HCLitePlatform;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What a crystal bag is holding.
 *
 * <p>1.12 kept the bag's contents in the stack's own NBT through the same aspect-list helper its
 * chargeable items used, and the bag's GUI read them back out. The port had the absorbing half but
 * no way out, which made the bag a one-way trip; this is the other half.</p>
 *
 * <p>The shape is the port's own: a list of {@code {k: aspect, v: amount}} entries, capped at the
 * same thirty-two thousand and change per aspect that 1.12 capped at.</p>
 */
public final class CrystalBagStore {
    /** 1.12's per-aspect ceiling. */
    public static final int CAPACITY_PER_ASPECT = 32768;

    private static final String KEY = "Essentia";

    private CrystalBagStore() {
    }

    /** Everything the bag holds, keyed by aspect, in the order the aspects were first stored. */
    public static Map<AspectId, Integer> contents(ItemStack bag) {
        Map<AspectId, Integer> out = new LinkedHashMap<>();
        CompoundTag tag = HCLitePlatform.Holder.get().itemTag(bag);
        if (tag == null || !tag.contains(KEY, Tag.TAG_LIST)) {
            return out;
        }
        ListTag list = tag.getList(KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entry = list.getCompound(index);
            AspectId aspect = parse(entry.getString("k"));
            int amount = entry.getInt("v");
            if (aspect != null && amount > 0) {
                out.merge(aspect, amount, Integer::sum);
            }
        }
        return out;
    }

    /** Adds up to the ceiling, returning how much was actually taken in. */
    public static int add(ItemStack bag, AspectId aspect, int amount) {
        if (aspect == null || amount <= 0) {
            return 0;
        }
        int room = CAPACITY_PER_ASPECT - contents(bag).getOrDefault(aspect, 0);
        int accepted = Math.min(room, amount);
        if (accepted <= 0) {
            return 0;
        }
        write(bag, aspect, contents(bag).getOrDefault(aspect, 0) + accepted);
        return accepted;
    }

    /** Removes up to {@code amount}, returning how much came out. */
    public static int remove(ItemStack bag, AspectId aspect, int amount) {
        int held = contents(bag).getOrDefault(aspect, 0);
        int taken = Math.min(held, Math.max(0, amount));
        if (taken > 0) {
            write(bag, aspect, held - taken);
        }
        return taken;
    }

    /** Sets one aspect's count, dropping the entry when it reaches zero. */
    private static void write(ItemStack bag, AspectId aspect, int count) {
        HCLitePlatform.Holder.get().updateItemTag(bag, tag -> {
            ListTag list = tag.getList(KEY, Tag.TAG_COMPOUND);
            ListTag kept = new ListTag();
            for (int index = 0; index < list.size(); index++) {
                CompoundTag entry = list.getCompound(index);
                if (!entry.getString("k").equals(aspect.value().toString())) {
                    kept.add(entry);
                }
            }
            if (count > 0) {
                CompoundTag entry = new CompoundTag();
                entry.putString("k", aspect.value().toString());
                entry.putInt("v", Math.min(CAPACITY_PER_ASPECT, count));
                kept.add(entry);
            }
            tag.put(KEY, kept);
        });
    }

    private static AspectId parse(String raw) {
        try {
            AspectId id = AspectId.parse(raw);
            return dev.tc4port.thaumcraft.api.aspect.AspectApi.contains(id) ? id : null;
        } catch (RuntimeException unknown) {
            return null;
        }
    }
}
