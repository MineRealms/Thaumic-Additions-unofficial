package dev.tc4port.thaumadditions.items;

import java.util.LinkedHashMap;
import java.util.Map;

import dev.tc4port.hclite.platform.HCLitePlatform;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 1.12 {@code IAspectChargableItem}: items that drink essentia and spend it later.
 *
 * <p>Upstream made this an interface on the item, with a storage helper that wrote a whole
 * {@code AspectList} into the stack's root NBT. The port has one chargeable item — the shadow beam
 * staff — and it holds a single aspect in an integer tag, so the interface collapses into a table
 * of "which items take which aspect, up to how much". The essentia sink, which is the only thing
 * that fills these items, needs nothing more than this.</p>
 *
 * <p>A stack is chargeable exactly when its item's registry id is registered here. Entries are
 * keyed by id rather than by {@code Item} because the registry holds no item instances at the time
 * content is declared.</p>
 */
public final class AspectCharges {
    /** The NBT key 1.12 stored a charge under, kept so existing staffs keep their count. */
    private static final String TAG_CHARGE = "Essentia";

    /** Item id -> the aspect it accepts, with its capacity. */
    private static final Map<ResourceLocation, Entry> REGISTRY = new LinkedHashMap<>();

    private record Entry(AspectId aspect, int capacity) {
    }

    private AspectCharges() {
    }

    /** Marks an item as taking essentia of one aspect, up to the given capacity. */
    public static void register(String itemId, AspectId aspect, int capacity) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id != null && aspect != null && capacity > 0) {
            REGISTRY.put(id, new Entry(aspect, capacity));
        }
    }

    private static Entry entryOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? null : REGISTRY.get(id);
    }

    /** The aspect this stack drinks, or {@code null} when it is not chargeable at all. */
    public static AspectId acceptedAspect(ItemStack stack) {
        Entry entry = entryOf(stack);
        return entry == null ? null : entry.aspect();
    }

    /** How much of the given aspect the stack can still take. */
    public static int remainingCapacity(ItemStack stack, AspectId aspect) {
        Entry entry = entryOf(stack);
        if (entry == null || !entry.aspect().equals(aspect)) {
            return 0;
        }
        return Math.max(0, entry.capacity() - charge(stack));
    }

    /** The stack's current charge; {@code 0} for anything not chargeable. */
    public static int charge(ItemStack stack) {
        CompoundTag tag = HCLitePlatform.Holder.get().itemTag(stack);
        return tag == null ? 0 : tag.getInt(TAG_CHARGE);
    }

    /** Whether the stack is chargeable with this aspect and has room for any of it. */
    public static boolean canAccept(ItemStack stack, AspectId aspect) {
        return remainingCapacity(stack, aspect) > 0;
    }

    /** Adds up to {@code amount}, returning how much was actually taken. */
    public static int addCharge(ItemStack stack, AspectId aspect, int amount) {
        int space = Math.min(remainingCapacity(stack, aspect), Math.max(0, amount));
        if (space <= 0) {
            return 0;
        }
        int total = charge(stack) + space;
        HCLitePlatform.Holder.get().updateItemTag(stack, tag -> tag.putInt(TAG_CHARGE, total));
        return space;
    }

    /** Removes up to {@code amount}, returning how much was actually removed. */
    public static int takeCharge(ItemStack stack, int amount) {
        int held = charge(stack);
        int taken = Math.min(held, Math.max(0, amount));
        if (taken > 0) {
            int left = held - taken;
            HCLitePlatform.Holder.get().updateItemTag(stack, tag -> tag.putInt(TAG_CHARGE, left));
        }
        return taken;
    }
}
