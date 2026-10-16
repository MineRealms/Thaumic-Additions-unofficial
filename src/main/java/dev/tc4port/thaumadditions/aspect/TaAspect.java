package dev.tc4port.thaumadditions.aspect;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dev.tc4port.thaumcraft.api.aspect.AspectApi;
import dev.tc4port.thaumcraft.api.aspect.AspectDefinition;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;

/**
 * Aspect helpers shared by the ported essentia items and block entities.
 *
 * <p>The 1.12 code stored aspects as plain strings in NBT ("Aspect" for one,
 * an "Aspects" map for many). This port keeps the same NBT shape but writes
 * the modern {@link AspectId} spelling, and never assumes the 1.12 TA-only
 * aspect set exists — every lookup goes through the live TC4R registry.</p>
 */
public final class TaAspect {
    private TaAspect() {
    }

    /** Writes one aspect id as a string, or removes the tag when {@code null}. */
    public static void writeAspect(CompoundTag tag, String key, AspectId id) {
        if (id == null) {
            tag.remove(key);
        } else {
            tag.putString(key, id.value().toString());
        }
    }

    /** Reads an aspect id, or {@code null} when absent/unresolvable in the live registry. */
    public static AspectId readAspect(CompoundTag tag, String key) {
        if (tag == null || !tag.contains(key, Tag.TAG_STRING)) {
            return null;
        }
        String raw = tag.getString(key);
        AspectId id;
        try {
            id = AspectId.parse(raw);
        } catch (RuntimeException invalid) {
            return null;
        }
        return AspectApi.contains(id) ? id : null;
    }

    /** Writes an {@code AspectList}-shaped map: {@code "Aspects":[{"k":id,"v":amount}]}. */
    public static void writeAspects(CompoundTag tag, String key, List<AspectId> ids, List<Integer> amounts) {
        ListTag list = new ListTag();
        for (int i = 0; i < ids.size(); i++) {
            CompoundTag entry = new CompoundTag();
            entry.putString("k", ids.get(i).value().toString());
            entry.putInt("v", amounts.get(i));
            list.add(entry);
        }
        tag.put(key, list);
    }

    /** Reads back the {@code writeAspects} shape. */
    public static List<AspectId> readAspects(CompoundTag tag, String key) {
        List<AspectId> out = new ArrayList<>();
        if (tag == null || !tag.contains(key, Tag.TAG_LIST)) {
            return out;
        }
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            AspectId id = readAspect(entry, "k");
            if (id != null) {
                out.add(id);
            }
        }
        return out;
    }

    public static List<Integer> readAspectAmounts(CompoundTag tag, String key) {
        List<Integer> out = new ArrayList<>();
        if (tag == null || !tag.contains(key, Tag.TAG_LIST)) {
            return out;
        }
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            out.add(list.getCompound(i).getInt("v"));
        }
        return out;
    }

    /** 24-bit RGB of the aspect, or a neutral grey when the definition is unknown. */
    public static int color(AspectId id) {
        AspectDefinition definition = id == null ? null : AspectApi.get(id);
        return definition == null ? 0x888888 : definition.color();
    }

    /**
     * The aspect's name as the reader's language has it.
     *
     * <p>Thaumcraft carries a line per aspect under {@code tc.aspect.&lt;id&gt;}, which is what
     * 1.12's {@code Aspect#getName} returned and what every tooltip in that mod printed. The port
     * printed the registry path instead, so a tooltip read {@code ignis} where a translated game
     * says {@code Fire} — and the id is lower case, which no name is. Everything that shows an
     * aspect's name comes through here, the item names and the JEI subtype labels included.</p>
     */
    public static String displayName(AspectId id) {
        if (id == null) {
            return "?";
        }
        if (AspectApi.get(id) == null) {
            return id.value().toString();
        }
        return net.minecraft.network.chat.Component
                .translatable("tc.aspect." + id.value().getPath()).getString();
    }

    /** Uniformly random aspect from the live registry; {@code null} when none are registered. */
    public static AspectId random(RandomSource random) {
        List<AspectId> all = new ArrayList<>(AspectApi.definitions().keySet());
        return all.isEmpty() ? null : all.get(random.nextInt(all.size()));
    }

    /** Uniformly random aspect, for code that has no {@link RandomSource} handy. */
    public static AspectId random(Random random) {
        List<AspectId> all = new ArrayList<>(AspectApi.definitions().keySet());
        return all.isEmpty() ? null : all.get(random.nextInt(all.size()));
    }

    public static List<AspectId> all() {
        return new ArrayList<>(AspectApi.definitions().keySet());
    }
}
