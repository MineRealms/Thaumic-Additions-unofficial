package dev.tc4port.thaumadditions.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import dev.tc4port.thaumadditions.platform.TaArmorMaterial;
import dev.tc4port.thaumadditions.platform.TaArmorSound;
import dev.tc4port.thaumadditions.platform.TaPlatform;
import dev.tc4port.thaumadditions.platform.ToolKind;
import dev.tc4port.thaumcraft.api.ThaumcraftTags;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * The mod's content declarations. Node adapters call
 * {@link #init(Registrar, Registrar, TaPlatform)} from their mod constructor,
 * before registry events fire. Registration order here is the order entries
 * land in the creative tab.
 *
 * <p>Ids match the 1.12 {@code setTranslationKey} names so the original lang
 * keys and assets stay 1:1 with upstream. Items that carry behaviour in 1.12
 * (flight device, seal globe, resonator, phantom ink phial) are intentionally
 * absent until their behaviour stage — registering them as inert items would
 * ship dead content.</p>
 *
 * <p>Everything whose vanilla API drifted between 1.20.1 and 1.21.1 (tool
 * tiers, armor materials, tool/armor construction) goes through
 * {@link TaPlatform}; the declarations below stay version-agnostic.</p>
 */
public final class Content {
    /** Ordered view of every registered item, for creative-tab population. */
    private static final List<Supplier<Item>> ITEM_ORDER = new ArrayList<>();

    /** Harvest level 3 (diamond-equivalent), as used by the 1.12 void tool material. */
    private static final TagKey<Block> INCORRECT_FOR_DIAMOND_TOOL =
            TagKey.create(Registries.BLOCK, ResourceLocation.tryParse("minecraft:incorrect_for_diamond_tool"));

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

    // --- tools (1.12 ItemsTAR "TOOLS": void elemental family) ---
    public static Supplier<Item> VOID_ELEMENTAL_PICKAXE;
    public static Supplier<Item> VOID_ELEMENTAL_AXE;
    public static Supplier<Item> VOID_ELEMENTAL_SHOVEL;
    public static Supplier<Item> VOID_ELEMENTAL_HOE;

    // --- weapons (1.12 ItemsTAR "WEAPONS") ---
    public static Supplier<Item> ADAMINITE_SWORD;

    // --- armor (1.12 ItemsTAR "ARMOR") ---
    public static Supplier<Item> ADAMINITE_HOOD;
    public static Supplier<Item> ADAMINITE_ROBE;
    public static Supplier<Item> ADAMINITE_BELT;
    public static Supplier<Item> ADAMINITE_BOOTS;

    private Content() {
    }

    public static void init(Registrar items, Registrar blocks, TaPlatform platform) {
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

        tools(items, platform);
        armor(items, platform);
    }

    /**
     * 1.12 {@code ItemVoidElementalHoe.TOOLMAT_VOID_ELEMENTAL}:
     * harvest 3, 1500 uses, speed 9, +3 damage, enchantability 18, repaired with thaumium.
     *
     * <p>The four tools were TC6 elemental tools that drained ambient aura to repair themselves.
     * TC4R dropped the aura pool and repairs its own void tools on a timer, so the port follows
     * that rule (see {@link dev.tc4port.thaumadditions.platform.TaGearBehavior}).</p>
     */
    private static void tools(Registrar items, TaPlatform platform) {
        Tier voidElemental = platform.toolTier(3, 1500, 9.0F, 3.0F, 18, INCORRECT_FOR_DIAMOND_TOOL,
                () -> Ingredient.of(ThaumcraftTags.THAUMIUM_INGOTS));

        VOID_ELEMENTAL_PICKAXE = track(items.register("void_elemental_pickaxe",
                () -> platform.tool(ToolKind.PICKAXE, voidElemental, 1.0F, -2.8F, new Item.Properties())));
        VOID_ELEMENTAL_AXE = track(items.register("void_elemental_axe",
                () -> platform.tool(ToolKind.AXE, voidElemental, 5.0F, -3.0F, new Item.Properties())));
        VOID_ELEMENTAL_SHOVEL = track(items.register("void_elemental_shovel",
                () -> platform.tool(ToolKind.SHOVEL, voidElemental, 1.5F, -3.0F, new Item.Properties())));
        VOID_ELEMENTAL_HOE = track(items.register("void_elemental_hoe",
                () -> platform.tool(ToolKind.HOE, voidElemental, 0.0F, -3.0F, new Item.Properties())));

        // 1.12 ItemAdaminiteSword: material (harvest 0, 0 uses, speed 5, +12 damage, enchant 60).
        // maxUses 0 makes the 1.12 item unbreakable; that survives as a 0-durability tier.
        Tier adaminiteSword = platform.toolTier(0, 0, 5.0F, 12.0F, 60, INCORRECT_FOR_DIAMOND_TOOL,
                () -> Ingredient.of(ThaumcraftTags.THAUMIUM_INGOTS));
        ADAMINITE_SWORD = track(items.register("adaminite_sword",
                () -> platform.tool(ToolKind.SWORD, adaminiteSword, 3.0F, -2.4F, new Item.Properties())));
    }

    /**
     * 1.12 {@code ItemAdaminiteDress.ADAMINITE}: durability factor 40, protection
     * {feet 4, legs 6, chest 8, head 5}, enchantability 40, toughness 8, leather equip sound.
     * Vis discounts (percent) and the head-only goggles overlay come from the 1.12 item.
     */
    private static void armor(Registrar items, TaPlatform platform) {
        Map<ArmorItem.Type, Integer> defense = Map.of(
                ArmorItem.Type.HELMET, 5,
                ArmorItem.Type.CHESTPLATE, 8,
                ArmorItem.Type.LEGGINGS, 6,
                ArmorItem.Type.BOOTS, 4);
        Map<ArmorItem.Type, Integer> discounts = Map.of(
                ArmorItem.Type.HELMET, 8,
                ArmorItem.Type.CHESTPLATE, 4);
        TaArmorMaterial adaminite = platform.armorMaterial("adaminite",
                ResourceLocation.tryParse("thaumadds:adaminite"), defense, 40, 40,
                TaArmorSound.LEATHER, () -> Ingredient.of(ThaumcraftTags.THAUMIUM_INGOTS),
                8.0F, 0.0F, discounts, true);

        ADAMINITE_HOOD = track(items.register("adaminite_hood",
                () -> platform.armor(adaminite, ArmorItem.Type.HELMET, 40, new Item.Properties())));
        ADAMINITE_ROBE = track(items.register("adaminite_robe",
                () -> platform.armor(adaminite, ArmorItem.Type.CHESTPLATE, 40, new Item.Properties())));
        ADAMINITE_BELT = track(items.register("adaminite_belt",
                () -> platform.armor(adaminite, ArmorItem.Type.LEGGINGS, 40, new Item.Properties())));
        ADAMINITE_BOOTS = track(items.register("adaminite_boots",
                () -> platform.armor(adaminite, ArmorItem.Type.BOOTS, 40, new Item.Properties())));
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
