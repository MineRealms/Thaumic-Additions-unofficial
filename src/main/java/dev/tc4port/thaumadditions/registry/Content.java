package dev.tc4port.thaumadditions.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.world.item.Item;

/**
 * The mod's content declarations. Node adapters call {@link #init(Registrar, Registrar)}
 * from their mod constructor, before registry events fire. Registration order here is the
 * order entries land in the creative tab.
 */
public final class Content {
    /** Ordered view of every registered item, for creative-tab population. */
    private static final List<Supplier<Item>> ITEM_ORDER = new ArrayList<>();

    public static Supplier<Item> BLUE_BONE;
    public static Supplier<Item> ZEITH_FUR;

    private Content() {
    }

    public static void init(Registrar items, Registrar blocks) {
        BLUE_BONE = track(items.register("blue_bone", () -> new Item(new Item.Properties())));
        ZEITH_FUR = track(items.register("zeith_fur", () -> new Item(new Item.Properties())));
    }

    private static Supplier<Item> track(Supplier<Item> holder) {
        ITEM_ORDER.add(holder);
        return holder;
    }

    /** Hands every registered item to the creative tab builder, in declaration order. */
    public static void forEachItem(Consumer<Item> out) {
        for (Supplier<Item> item : ITEM_ORDER)
            out.accept(item.get());
    }
}
