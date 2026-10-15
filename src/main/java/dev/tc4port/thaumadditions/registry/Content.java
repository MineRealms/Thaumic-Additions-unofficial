package dev.tc4port.thaumadditions.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * The mod's content declarations. Node adapters call {@link #init(Registrar, Registrar)}
 * from their mod constructor, before registry events fire. Registration order here is the
 * order entries land in the creative tab.
 *
 * <p>Ids match the 1.12 {@code setTranslationKey} names so the original lang keys and
 * assets stay 1:1 with upstream. Items that carry behaviour in 1.12 (flight device,
 * seal globe, resonator, phantom ink phial) are intentionally absent until their
 * behaviour stage — registering them as inert items would ship dead content.</p>
 */
public final class Content {
    /** Ordered view of every registered item, for creative-tab population. */
    private static final List<Supplier<Item>> ITEM_ORDER = new ArrayList<>();

    // --- drops ---
    public static Supplier<Item> BLUE_BONE;
    public static Supplier<Item> ZEITH_FUR;

    // --- material family (1.12 ItemsTAR "MATERIALS") ---
    public static Supplier<Item> MITHRILLIUM_NUGGET;
    public static Supplier<Item> ADAMINITE_NUGGET;
    public static Supplier<Item> MITHMINITE_NUGGET;
    public static Supplier<Item> MITHRILLIUM_INGOT;
    public static Supplier<Item> ADAMINITE_INGOT;
    public static Supplier<Item> MITHMINITE_INGOT;
    public static Supplier<Item> MITHRILLIUM_PLATE;
    public static Supplier<Item> ADAMINITE_PLATE;
    public static Supplier<Item> MITHMINITE_PLATE;
    public static Supplier<Item> ADAMINITE_FABRIC;
    public static Supplier<Item> MITHMINITE_FABRIC;
    public static Supplier<Item> MITHMINITE_BLADE;
    public static Supplier<Item> MITHMINITE_HANDLE;
    public static Supplier<Item> ODOUR_POWDER;

    // --- plain blocks (1.12 BlocksTAR: BlockTARStorage family) ---
    public static Supplier<Block> MITHRILLIUM_BLOCK;
    public static Supplier<Block> ADAMINITE_BLOCK;
    public static Supplier<Block> MITHMINITE_BLOCK;
    public static Supplier<Block> TAINTWOOD_PLANKS;
    public static Supplier<Block> CHISELED_AMBER_BLOCK;

    private Content() {
    }

    public static void init(Registrar items, Registrar blocks) {
        BLUE_BONE = track(items.register("blue_bone", Content::simple));
        ZEITH_FUR = track(items.register("zeith_fur", Content::simple));

        MITHRILLIUM_NUGGET = track(items.register("mithrillium_nugget", Content::simple));
        ADAMINITE_NUGGET = track(items.register("adaminite_nugget", Content::simple));
        MITHMINITE_NUGGET = track(items.register("mithminite_nugget", Content::simple));
        MITHRILLIUM_INGOT = track(items.register("mithrillium_ingot", Content::simple));
        ADAMINITE_INGOT = track(items.register("adaminite_ingot", Content::simple));
        MITHMINITE_INGOT = track(items.register("mithminite_ingot", Content::simple));
        MITHRILLIUM_PLATE = track(items.register("mithrillium_plate", Content::simple));
        ADAMINITE_PLATE = track(items.register("adaminite_plate", Content::simple));
        MITHMINITE_PLATE = track(items.register("mithminite_plate", Content::simple));
        ADAMINITE_FABRIC = track(items.register("adaminite_fabric", Content::simple));
        MITHMINITE_FABRIC = track(items.register("mithminite_fabric", Content::simple));
        MITHMINITE_BLADE = track(items.register("mithminite_blade", Content::simple));
        MITHMINITE_HANDLE = track(items.register("mithminite_handle", Content::simple));
        ODOUR_POWDER = track(items.register("odour_powder", Content::simple));

        // 1.12 BlockTARStorage(metal): hardness 3, metal sound, harvest level 3 (diamond pickaxe).
        MITHRILLIUM_BLOCK = block(items, blocks, "mithrillium_block", metalStorage());
        ADAMINITE_BLOCK = block(items, blocks, "adaminite_block", metalStorage());
        MITHMINITE_BLOCK = block(items, blocks, "mithminite_block", metalStorage());
        // BlockTARStorage(Material.WOOD, WOOD, 1F, "axe", 0) and (Material.ROCK, STONE, 1F, "pickaxe", 0).
        TAINTWOOD_PLANKS = block(items, blocks, "taintwood_planks",
                Block.Properties.of().mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD));
        CHISELED_AMBER_BLOCK = block(items, blocks, "chiseled_amber_block",
                Block.Properties.of().mapColor(MapColor.STONE).strength(1.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());
    }

    /** Registers a plain block plus the matching block item, in tab order. */
    private static Supplier<Block> block(Registrar items, Registrar blocks, String name, Block.Properties properties) {
        Supplier<Block> holder = blocks.register(name, () -> new Block(properties));
        track(items.register(name, () -> new BlockItem(holder.get(), new Item.Properties())));
        return holder;
    }

    private static Block.Properties metalStorage() {
        return Block.Properties.of().mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    private static Item simple() {
        return new Item(new Item.Properties());
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
