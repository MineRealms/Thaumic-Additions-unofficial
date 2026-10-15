package dev.tc4port.thaumadditions.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import dev.tc4port.thaumadditions.armor.MithminiteArmorBehavior;
import dev.tc4port.thaumadditions.blocks.TaBlocks;
import dev.tc4port.thaumadditions.blocks.TaMachineBlocks;
import dev.tc4port.thaumadditions.items.AspectCharges;
import dev.tc4port.thaumadditions.items.GearItems;
import dev.tc4port.thaumadditions.items.ToolItems;
import dev.tc4port.thaumadditions.items.WeaponItems;
import dev.tc4port.thaumadditions.platform.MachineIcon;
import dev.tc4port.thaumadditions.platform.TaArmorMaterial;
import dev.tc4port.thaumadditions.platform.TaArmorSound;
import dev.tc4port.thaumadditions.platform.TaArmorTick;
import dev.tc4port.thaumadditions.platform.TaPlatform;
import dev.tc4port.thaumadditions.platform.ToolKind;
import dev.tc4port.thaumadditions.tiles.TileAspectCombiner;
import dev.tc4port.thaumadditions.tiles.TileAuraDisperser;
import dev.tc4port.thaumadditions.tiles.TileCrystal;
import dev.tc4port.thaumadditions.tiles.TileCrystalLamp;
import dev.tc4port.thaumadditions.tiles.TileCrystalBore;
import dev.tc4port.thaumadditions.tiles.TileCrystalCrusher;
import dev.tc4port.thaumadditions.tiles.TileEntitySummoner;
import dev.tc4port.thaumadditions.tiles.TileEssentiaSink;
import dev.tc4port.thaumadditions.tiles.TileFluxConcentrator;
import dev.tc4port.thaumadditions.tiles.TileGrowthChamber;
import dev.tc4port.thaumadditions.tiles.TileJar;
import dev.tc4port.thaumadditions.tiles.TileSeal;
import dev.tc4port.thaumadditions.tiles.TileShadowEnchanter;
import dev.tc4port.thaumadditions.tiles.TileSmelter;
import dev.tc4port.thaumadditions.tiles.TaTiles;
import dev.tc4port.thaumcraft.api.ThaumcraftTags;
import dev.tc4port.thaumcraft.api.item.ArmorTextureApi;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * The mod's content declarations. Node adapters call
 * {@link #init(Registrar, Registrar, Registrar, TaPlatform)} from their mod
 * constructor, before registry events fire. Registration order here is the order
 * entries land in the creative tab.
 *
 * <p>Ids match the 1.12 {@code setTranslationKey} names so the original lang keys
 * and assets stay 1:1 with upstream — the bulk-normalized assets in
 * {@code assets/thaumadds} were produced by {@code tools/legacy_assets.py} from
 * the 1.12 tree, so a blockstate file already exists for every block declared
 * here (and its property names are the ones declared below).</p>
 *
 * <p>Everything whose vanilla API drifted between 1.20.1 and 1.21.1 (tool tiers,
 * armor materials, tool/armor construction) goes through {@link TaPlatform}; the
 * declarations below stay version-agnostic.</p>
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

    // --- essentia materials ---
    public static Supplier<Item> SALT_ESSENCE;
    public static Supplier<Item> VIS_POD;
    public static Supplier<Item> SEAL_SYMBOL;
    public static Supplier<Item> VIS_SEEDS;
    public static Supplier<Item> VOID_SEED;
    public static Supplier<Item> VOID_FRUIT;

    // --- plain materials and self-contained tools ---
    public static Supplier<Item> MITHRILLIUM_RESONATOR;
    public static Supplier<Item> LEVITATION_DEVICE;
    public static Supplier<Item> BONE_EYE;
    public static Supplier<Item> PHANTOM_INK_PHIAL;
    public static Supplier<Item> KNOWLEDGE_TOME;
    public static Supplier<Item> CRYSTAL_BAG;
    public static Supplier<Item> DISENCHANT_FABRIC;
    public static Supplier<Item> ENTITY_CELL;
    public static Supplier<Item> CHESTER;
    public static Supplier<Item> SEAL_GLOBE;

    // --- tools (1.12 ItemsTAR "TOOLS": void elemental family) ---
    public static Supplier<Item> VOID_ELEMENTAL_PICKAXE;
    public static Supplier<Item> VOID_ELEMENTAL_AXE;
    public static Supplier<Item> VOID_ELEMENTAL_SHOVEL;
    public static Supplier<Item> VOID_ELEMENTAL_HOE;
    public static Supplier<Item> VIS_SCRIBING_TOOLS;
    public static Supplier<Item> VOID_THAUMOMETER;
    public static Supplier<Item> WORMHOLE_MIRROR;

    // --- weapons (1.12 ItemsTAR "WEAPONS") ---
    public static Supplier<Item> ADAMINITE_SWORD;
    public static Supplier<Item> THE_BEHEADER;
    public static Supplier<Item> MITHMINITE_SCYTHE;
    public static Supplier<Item> SHADOW_BEAM_STAFF;
    public static Supplier<Item> ESSENTIA_PISTOL;

    // --- baubles (held in this port: Curios is runtime-only) ---
    public static Supplier<Item> TRAVELLER_BELT;
    public static Supplier<Item> STRIDING_BELT;
    public static Supplier<Item> METEOR_BELT;
    public static Supplier<Item> FRAGNANT_PENDANT;
    public static Supplier<Item> RECHARGE_CHARM;

    // --- armor (1.12 ItemsTAR "ARMOR") ---
    public static Supplier<Item> ADAMINITE_HOOD;
    public static Supplier<Item> ADAMINITE_ROBE;
    public static Supplier<Item> ADAMINITE_BELT;
    public static Supplier<Item> ADAMINITE_BOOTS;

    public static Supplier<Item> MITHMINITE_HOOD;
    public static Supplier<Item> MITHMINITE_ROBE;
    public static Supplier<Item> MITHMINITE_BELT;
    public static Supplier<Item> MITHMINITE_BOOTS;

    public static Supplier<Item> BLUE_WOLF_MASK;
    public static Supplier<Item> BLUE_WOLF_BODY;
    public static Supplier<Item> BLUE_WOLF_LEGS;
    public static Supplier<Item> BLUE_WOLF_FEETPAWS;

    // --- blocks (1.12 BlocksTAR) ---
    public static Supplier<Block> MITHRILLIUM_BLOCK;
    public static Supplier<Block> ADAMINITE_BLOCK;
    public static Supplier<Block> MITHMINITE_BLOCK;
    public static Supplier<Block> TAINTWOOD_PLANKS;
    public static Supplier<Block> CHISELED_AMBER_BLOCK;
    public static Supplier<Block> CHISELED_GREATWOOD;
    public static Supplier<Block> IRON_FRAMED_GREATWOOD;
    public static Supplier<Block> BRASS_PLATED_SILVERWOOD;
    public static Supplier<Block> AMBER_LAMP;
    public static Supplier<Block> PURIFLOWER;
    public static Supplier<Block> VOID_CROP;
    public static Supplier<Block> VIS_CROP;
    public static Supplier<Block> CAKE;
    public static Supplier<Block> DAWN_TOTEM;
    public static Supplier<Block> TWILIGHT_TOTEM;
    public static Supplier<Block> TAINTKIN;
    public static Supplier<Block> TAINTKIN_LIT;
    public static Supplier<Block> VOID_ANVIL;
    public static Supplier<Block> THAUMIC_LECTERN;

    public static Supplier<Block> JAR_BRASS;
    public static Supplier<Block> JAR_THAUMIUM;
    public static Supplier<Block> JAR_ELDRITCH;
    public static Supplier<Block> JAR_MITHRILLIUM;
    public static Supplier<Block> JAR_ADAMINITE;
    public static Supplier<Block> JAR_MITHMINITE;

    public static Supplier<Block> MITHRILLIUM_SMELTER;
    public static Supplier<Block> ADAMINITE_SMELTER;
    public static Supplier<Block> MITHMINITE_SMELTER;
    public static Supplier<Block> GROWTH_CHAMBER;
    public static Supplier<Block> CRYSTAL_BORE;
    public static Supplier<Block> CRYSTAL_CRUSHER;
    public static Supplier<Block> ASPECT_COMBINER;
    public static Supplier<Block> AURA_CHARGER;
    public static Supplier<Block> AURA_DISPERSER;
    public static Supplier<Block> FLUX_CONCENTRATOR;

    /**
     * The item forms of the five machines above. They are named rather than anonymous because the
     * NeoForge node attaches their inventory icon from a client event and needs the item to hand
     * it; on Forge the item carries the hook itself.
     */
    public static Supplier<Item> CRYSTAL_BORE_ITEM;
    public static Supplier<Item> CRYSTAL_CRUSHER_ITEM;
    public static Supplier<Item> ASPECT_COMBINER_ITEM;
    public static Supplier<Item> AURA_CHARGER_ITEM;
    public static Supplier<Item> FLUX_CONCENTRATOR_ITEM;

    public static Supplier<Block> ESSENTIA_SINK;
    public static Supplier<Block> ESSENTIA_SINK_AUX;
    public static Supplier<Block> SHADOW_ENCHANTER;
    public static Supplier<Block> ENTITY_SUMMONER;
    public static Supplier<Block> SEAL;
    public static Supplier<Block> CRYSTAL_BLOCK;
    public static Supplier<Block> CRYSTAL_LAMP;

    /** The sixteen block entity types. */
    public static Supplier<BlockEntityType<TileJar>> JAR_BE;
    public static Supplier<BlockEntityType<TileSmelter>> SMELTER_BE;
    public static Supplier<BlockEntityType<TileCrystal>> CRYSTAL_BE;
    public static Supplier<BlockEntityType<TileCrystalLamp>> CRYSTAL_LAMP_BE;
    public static Supplier<BlockEntityType<TileCrystal.Cake>> CAKE_BE;
    public static Supplier<BlockEntityType<TileSeal>> SEAL_BE;
    public static Supplier<BlockEntityType<TileEssentiaSink>> SINK_BE;
    public static Supplier<BlockEntityType<TileEntitySummoner>> SUMMONER_BE;
    public static Supplier<BlockEntityType<TileShadowEnchanter>> ENCHANTER_BE;
    public static Supplier<BlockEntityType<TileGrowthChamber>> GROWTH_BE;
    public static Supplier<BlockEntityType<TileAspectCombiner>> COMBINER_BE;

    // The five machine menus. 1.12 had a container and a GUI per machine; the port has one menu
    // each, carrying the same slots, and the screens read the machine's own synced state.
    public static Supplier<net.minecraft.world.inventory.MenuType<dev.tc4port.thaumadditions.gui.MachineMenus.Smelter>> SMELTER_MENU;
    public static Supplier<net.minecraft.world.inventory.MenuType<dev.tc4port.thaumadditions.gui.MachineMenus.CrystalCrusher>> CRUSHER_MENU;
    public static Supplier<net.minecraft.world.inventory.MenuType<dev.tc4port.thaumadditions.gui.MachineMenus.GrowthChamber>> GROWTH_MENU;
    public static Supplier<net.minecraft.world.inventory.MenuType<dev.tc4port.thaumadditions.gui.MachineMenus.AuraDisperser>> DISPERSER_MENU;
    public static Supplier<net.minecraft.world.inventory.MenuType<dev.tc4port.thaumadditions.gui.MachineMenus.ShadowEnchanter>> ENCHANTER_MENU;

    /**
     * The essentia pistol's shot. The blue wolf, Chester and the scythe are registered in
     * {@link #init}, because their classes are shared rather than per node.
     *
     * <p>Filled by the node rather than by {@link #init}, because the entity's concrete class has to
     * be per node: {@code defineSynchedData} takes a builder on one version and nothing on the
     * other.</p>
     */
    public static Supplier<net.minecraft.world.entity.EntityType<? extends dev.tc4port.thaumadditions.entity.EssentiaShotBase>> ESSENTIA_SHOT;

    /** How shared code builds one; the node fills this alongside the type. */
    public static dev.tc4port.thaumadditions.entity.EssentiaShotFactory ESSENTIA_SHOT_FACTORY;

    /** The blue wolf; its class is shared, so this one registers through {@link #init}. */
    public static Supplier<net.minecraft.world.entity.EntityType<dev.tc4port.thaumadditions.entity.BlueWolf>> BLUE_WOLF;

    /** The essentia pistol's one loading slot. */
    public static Supplier<net.minecraft.world.inventory.MenuType<dev.tc4port.thaumadditions.gui.PistolMenu>> PISTOL_MENU;

    /** The crystal bag's nine virtual slots. */
    public static Supplier<net.minecraft.world.inventory.MenuType<dev.tc4port.thaumadditions.gui.CrystalBagMenu>> CRYSTAL_BAG_MENU;

    /** Chester, the walking chest, and the menu that shows its thirty-six slots. */
    public static Supplier<net.minecraft.world.entity.EntityType<dev.tc4port.thaumadditions.entity.Chester>> CHESTER_ENTITY;
    public static Supplier<net.minecraft.world.inventory.MenuType<dev.tc4port.thaumadditions.gui.ChesterMenu>> CHESTER_MENU;

    /** The thrown scythe blade; its class is per node for the same reason the shot's is. */
    public static Supplier<net.minecraft.world.entity.EntityType<? extends dev.tc4port.thaumadditions.entity.ScytheProjectileBase>> MITHMINITE_SCYTHE_ENTITY;
    /** Builds one, for the scythe item in shared code. */
    public static java.util.function.BiFunction<net.minecraft.world.level.Level, net.minecraft.world.entity.LivingEntity,
            dev.tc4port.thaumadditions.entity.ScytheProjectileBase> SCYTHE_FACTORY;

    /** Called by the node once it has registered the blade. */
    public static void setScythe(
            Supplier<net.minecraft.world.entity.EntityType<? extends dev.tc4port.thaumadditions.entity.ScytheProjectileBase>> type,
            java.util.function.BiFunction<net.minecraft.world.level.Level, net.minecraft.world.entity.LivingEntity,
                    dev.tc4port.thaumadditions.entity.ScytheProjectileBase> factory) {
        MITHMINITE_SCYTHE_ENTITY = type;
        SCYTHE_FACTORY = factory;
    }

    /** Called by the node once it has registered the entity type. */
    public static void setEssentiaShot(
            Supplier<net.minecraft.world.entity.EntityType<? extends dev.tc4port.thaumadditions.entity.EssentiaShotBase>> type,
            dev.tc4port.thaumadditions.entity.EssentiaShotFactory factory) {
        ESSENTIA_SHOT = type;
        ESSENTIA_SHOT_FACTORY = factory;
    }
    public static Supplier<BlockEntityType<TaTiles.Charger>> CHARGER_BE;
    public static Supplier<BlockEntityType<TileAuraDisperser>> DISPERSER_BE;
    public static Supplier<BlockEntityType<TileCrystalBore>> BORE_BE;
    public static Supplier<BlockEntityType<TileCrystalCrusher>> CRUSHER_BE;
    public static Supplier<BlockEntityType<TileFluxConcentrator>> CONCENTRATOR_BE;

    private Content() {
    }

    public static void init(Registrar items, Registrar blocks, Registrar blockEntities, Registrar menus,
                            Registrar entityTypes, TaPlatform platform) {
        materials(items, platform);
        essentiaMaterials(items, platform);
        plainItems(items, platform);
        tools(items, platform);
        weapons(items, platform);
        baubles(items, platform);
        armor(items, platform);

        plainBlocks(items, blocks, platform);
        machines(items, blocks, platform);
        blockEntities(blockEntities);
        menus(menus, platform);
        // The seals are behaviour rather than content: the block is registered above, and this
        // teaches it what its three symbols mean.
        dev.tc4port.thaumadditions.seals.TaSeals.register();
        entities(entityTypes);
        blockItems(items);
        crops(items);
    }

    /** A machine's GUI: the registered menu type and the server-side menu constructor. */
    private static TaMachineBlocks.MachineGui gui(
            Supplier<? extends net.minecraft.world.inventory.MenuType<?>> type,
            dev.tc4port.thaumadditions.gui.MachineMenuProvider.ServerMenuFactory builder) {
        return new TaMachineBlocks.MachineGui(type, builder);
    }

    /** The entities whose class is shared, and so can be registered here. */
    private static void entities(Registrar entityTypes) {
        BLUE_WOLF = entityTypes.register("blue_wolf",
                () -> net.minecraft.world.entity.EntityType.Builder
                        .<dev.tc4port.thaumadditions.entity.BlueWolf>of(
                                dev.tc4port.thaumadditions.entity.BlueWolf::new,
                                net.minecraft.world.entity.MobCategory.CREATURE)
                        .sized(0.6F, 0.85F)
                        .clientTrackingRange(10)
                        .build("thaumadds:blue_wolf"));
        CHESTER_ENTITY = entityTypes.register("chester",
                () -> net.minecraft.world.entity.EntityType.Builder
                        .<dev.tc4port.thaumadditions.entity.Chester>of(
                                dev.tc4port.thaumadditions.entity.Chester::new,
                                net.minecraft.world.entity.MobCategory.CREATURE)
                        .sized(0.9F, 0.9F)
                        .clientTrackingRange(10)
                        .build("thaumadds:chester"));
    }

    /** One menu per machine that had a container in 1.12. */
    private static void menus(Registrar menus, TaPlatform platform) {
        SMELTER_MENU = menus.register("smelter",
                () -> platform.menuType(dev.tc4port.thaumadditions.gui.MachineMenus.Smelter::new));
        CRUSHER_MENU = menus.register("crystal_crusher",
                () -> platform.menuType(dev.tc4port.thaumadditions.gui.MachineMenus.CrystalCrusher::new));
        GROWTH_MENU = menus.register("growth_chamber",
                () -> platform.menuType(dev.tc4port.thaumadditions.gui.MachineMenus.GrowthChamber::new));
        DISPERSER_MENU = menus.register("aura_disperser",
                () -> platform.menuType(dev.tc4port.thaumadditions.gui.MachineMenus.AuraDisperser::new));
        ENCHANTER_MENU = menus.register("shadow_enchanter",
                () -> platform.menuType(dev.tc4port.thaumadditions.gui.MachineMenus.ShadowEnchanter::new));
        CHESTER_MENU = menus.register("chester",
                () -> platform.menuType(dev.tc4port.thaumadditions.gui.ChesterMenu::new));
        CRYSTAL_BAG_MENU = menus.register("crystal_bag",
                () -> platform.menuType(dev.tc4port.thaumadditions.gui.CrystalBagMenu::new));
        PISTOL_MENU = menus.register("essentia_pistol",
                () -> platform.menuType(dev.tc4port.thaumadditions.gui.PistolMenu::new));
    }

    // ------------------------------------------------------------------ items

    private static void materials(Registrar items, TaPlatform platform) {
        BLUE_BONE = track(items.register("blue_bone", () -> ToolItems.blueBone(platform)));
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
    }

    private static void essentiaMaterials(Registrar items, TaPlatform platform) {
        SALT_ESSENCE = track(items.register("salt_essence", () -> GearItems.saltEssence(platform)));
        VIS_POD = track(items.register("vis_pod", () -> GearItems.visPod(platform)));
        SEAL_SYMBOL = track(items.register("seal_symbol", () -> GearItems.sealSymbol(platform)));
        VOID_FRUIT = track(items.register("void_fruit", () -> ToolItems.voidFruit(platform)));
    }

    private static void plainItems(Registrar items, TaPlatform platform) {
        MITHRILLIUM_RESONATOR = track(items.register("mithrillium_resonator", ToolItems::plain));
        LEVITATION_DEVICE = track(items.register("levitation_device", ToolItems::plain));
        BONE_EYE = track(items.register("bone_eye", ToolItems::plain));
        PHANTOM_INK_PHIAL = track(items.register("phantom_ink_phial", ToolItems::plain));
        KNOWLEDGE_TOME = track(items.register("knowledge_tome", () -> ToolItems.knowledgeTome(platform)));
        CRYSTAL_BAG = track(items.register("crystal_bag", () -> ToolItems.crystalBag(platform)));
        DISENCHANT_FABRIC = track(items.register("disenchant_fabric", GearItems::disenchantFabric));
        ENTITY_CELL = track(items.register("dna_sample", () -> ToolItems.entityCell(platform)));
        CHESTER = track(items.register("chester", () -> ToolItems.chester(platform)));
        SEAL_GLOBE = track(items.register("seal_globe", () -> ToolItems.sealGlobe(platform)));
    }

    private static void tools(Registrar items, TaPlatform platform) {
        Tier voidElemental = platform.toolTier(3, 1500, 9.0F, 3.0F, 18, INCORRECT_FOR_DIAMOND_TOOL,
                Content::thaumiumRepair);

        VOID_ELEMENTAL_PICKAXE = track(items.register("void_elemental_pickaxe",
                () -> platform.tool(ToolKind.PICKAXE, voidElemental, 1.0F, -2.8F, new Item.Properties())));
        VOID_ELEMENTAL_AXE = track(items.register("void_elemental_axe",
                () -> platform.tool(ToolKind.AXE, voidElemental, 5.0F, -3.0F, new Item.Properties())));
        VOID_ELEMENTAL_SHOVEL = track(items.register("void_elemental_shovel",
                () -> platform.tool(ToolKind.SHOVEL, voidElemental, 1.5F, -3.0F, new Item.Properties())));
        VOID_ELEMENTAL_HOE = track(items.register("void_elemental_hoe",
                () -> platform.tool(ToolKind.HOE, voidElemental, 0.0F, -3.0F, new Item.Properties())));

        VIS_SCRIBING_TOOLS = track(items.register("vis_scribing_tools", GearItems::visScribingTools));
        VOID_THAUMOMETER = track(items.register("void_thaumometer", ToolItems::voidThaumometer));
        WORMHOLE_MIRROR = track(items.register("wormhole_mirror", ToolItems::wormholeMirror));
    }

    private static void weapons(Registrar items, TaPlatform platform) {
        // 1.12 ItemAdaminiteSword: material (harvest 0, 0 uses, speed 5, +12 damage, enchant 60).
        // maxUses 0 makes the 1.12 item unbreakable; that survives as a 0-durability tier.
        Tier adaminiteSword = platform.toolTier(0, 0, 5.0F, 12.0F, 60, INCORRECT_FOR_DIAMOND_TOOL,
                Content::thaumiumRepair);
        ADAMINITE_SWORD = track(items.register("adaminite_sword",
                () -> platform.tool(ToolKind.SWORD, adaminiteSword, 3.0F, -2.4F, new Item.Properties())));

        // 1.12 ItemMithminiteScythe: harvest 3, 4096 uses, +12 damage, enchant 25, thaumium repair.
        Tier mithminiteWeapon = platform.toolTier(3, 4096, 12.0F, 12.0F, 25, INCORRECT_FOR_DIAMOND_TOOL,
                Content::thaumiumRepair);
        THE_BEHEADER = track(items.register("the_beheader",
                () -> WeaponItems.theBeheader(platform, mithminiteWeapon)));
        MITHMINITE_SCYTHE = track(items.register("mithminite_scythe",
                () -> WeaponItems.mithminiteScythe(platform, mithminiteWeapon)));
        SHADOW_BEAM_STAFF = track(items.register("shadow_beam_staff", () -> WeaponItems.shadowBeamStaff(platform)));
        // 1.12 ItemShadowBeamStaff implemented IAspectChargableItem: ELDRITCH only, 500 points.
        AspectCharges.register("thaumadds:shadow_beam_staff",
                dev.tc4port.thaumcraft.api.aspect.AspectId.parse("alienis"), 500);
        ESSENTIA_PISTOL = track(items.register("essentia_pistol", () -> WeaponItems.essentiaPistol(platform)));
    }

    private static void baubles(Registrar items, TaPlatform platform) {
        TRAVELLER_BELT = track(items.register("traveller_belt", () -> GearItems.travellerBelt(platform)));
        STRIDING_BELT = track(items.register("striding_belt", () -> GearItems.stridingBelt(platform)));
        METEOR_BELT = track(items.register("meteor_belt", () -> GearItems.meteorBelt(platform)));
        FRAGNANT_PENDANT = track(items.register("fragnant_pendant",
                () -> GearItems.fragnantPendant(platform, ODOUR_POWDER.get())));
        RECHARGE_CHARM = track(items.register("recharge_charm", GearItems::rechargeCharm));
    }

    // ------------------------------------------------------------------ blocks

    private static void plainBlocks(Registrar items, Registrar blocks, TaPlatform platform) {
        // 1.12 BlockTARStorage(metal): hardness 3, metal sound, harvest level 3 (diamond pickaxe).
        MITHRILLIUM_BLOCK = block(items, blocks, "mithrillium_block", metalStorage());
        ADAMINITE_BLOCK = block(items, blocks, "adaminite_block", metalStorage());
        MITHMINITE_BLOCK = block(items, blocks, "mithminite_block", metalStorage());
        // BlockTARStorage(Material.WOOD, WOOD, 1F, "axe", 0) and (Material.ROCK, STONE, 1F, "pickaxe", 0).
        TAINTWOOD_PLANKS = block(items, blocks, "taintwood_planks",
                wood(1.0F, false));
        CHISELED_AMBER_BLOCK = block(items, blocks, "chiseled_amber_block",
                stone(1.0F));
        CHISELED_GREATWOOD = block(items, blocks, "chiseled_greatwood", wood(2.0F, false));
        IRON_FRAMED_GREATWOOD = block(items, blocks, "iron_framed_greatwood", wood(2.0F, false));
        BRASS_PLATED_SILVERWOOD = block(items, blocks, "brass_plated_silverwood", wood(2.0F, false));
        // Amber lamp has no switch in 1.12: it is simply always lit.
        AMBER_LAMP = block(items, blocks, "amber_lamp",
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL)
                        .lightLevel(state -> 12).requiresCorrectToolForDrops());
        PURIFLOWER = block(items, blocks, "puriflower", () -> platform.puriflower(
                BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.2F).sound(SoundType.GRASS)
                        .lightLevel(state -> 8).noCollission()));
        DAWN_TOTEM = block(items, blocks, "dawn_totem", wood(1.5F, false));
        TWILIGHT_TOTEM = block(items, blocks, "twilight_totem", wood(1.5F, false));
        // These four carry a 1.12 horizontal facing (BlockTaintkin extends BlockPumpkin;
        // BlockThaumicLectern and BlockVoidAnvil implement IBlockHorizontal), which their
        // blockstates declare as facing=north/south/west/east. A plain Block has no properties at
        // all, so nothing but the empty variant matched and the models fell back to missingno.
        TAINTKIN = block(items, blocks, "taintkin", () -> new TaBlocks.HorizontalFacingBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.0F).sound(SoundType.WOOD)));
        TAINTKIN_LIT = block(items, blocks, "taintkin_lit", () -> new TaBlocks.HorizontalFacingBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.0F).sound(SoundType.WOOD)
                        .lightLevel(state -> 9)));
        VOID_ANVIL = block(items, blocks, "void_anvil", () -> new TaBlocks.HorizontalFacingBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.ANVIL)
                        .requiresCorrectToolForDrops()));
        THAUMIC_LECTERN = block(items, blocks, "thaumic_lectern", () -> new TaBlocks.HorizontalFacingBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD)
                        .noOcclusion()));
    }

    /** Every block that carries a block entity, wired to its type supplier. */
    private static void machines(Registrar items, Registrar blocks, TaPlatform platform) {
        JAR_BRASS = jar(blocks, "jar_brass", TaMachineBlocks.JarKind.BRASS);
        JAR_THAUMIUM = jar(blocks, "jar_thaumium", TaMachineBlocks.JarKind.THAUMIUM);
        JAR_ELDRITCH = jar(blocks, "jar_eldritch", TaMachineBlocks.JarKind.ELDRITCH);
        JAR_MITHRILLIUM = jar(blocks, "jar_mithrillium", TaMachineBlocks.JarKind.MITHRILLIUM);
        JAR_ADAMINITE = jar(blocks, "jar_adaminite", TaMachineBlocks.JarKind.ADAMINITE);
        JAR_MITHMINITE = jar(blocks, "jar_mithminite", TaMachineBlocks.JarKind.MITHMINITE);

        // 1.12 BlockAbstractSmelter(efficiency, speed, capacity) per variant.
        MITHRILLIUM_SMELTER = smelter(blocks, "mithrillium_smelter", 1.0F, 20, 1000);
        ADAMINITE_SMELTER = smelter(blocks, "adaminite_smelter", 1.2F, 15, 2000);
        MITHMINITE_SMELTER = smelter(blocks, "mithminite_smelter", 1.5F, 10, 4000);

        GROWTH_CHAMBER = trackBlock(blocks.<Block>register("growth_chamber",
                () -> new TaMachineBlocks.GrowthChamberBlock(GROWTH_BE, gui(GROWTH_MENU,
                        dev.tc4port.thaumadditions.gui.MachineMenus.GrowthChamber::new))));
        // Each lambda reads its *_BE static field when the block is instantiated, not when this
        // method runs: blockEntities() assigns them further down, so binding the value here (as a
        // helper taking the supplier as a parameter would) hands the block a null type.
        CRYSTAL_BORE = trackBlock(blocks.<Block>register("crystal_bore",
                () -> new TaMachineBlocks.MachineBlock(BORE_BE)));
        CRYSTAL_BORE_ITEM = track(items.register("crystal_bore",
                () -> platform.machineItem(CRYSTAL_BORE.get(), MachineIcon.CRYSTAL_BORE)));
        CRYSTAL_CRUSHER = trackBlock(blocks.<Block>register("crystal_crusher",
                () -> new TaMachineBlocks.MachineBlock(CRUSHER_BE, gui(CRUSHER_MENU,
                        dev.tc4port.thaumadditions.gui.MachineMenus.CrystalCrusher::new))));
        CRYSTAL_CRUSHER_ITEM = track(items.register("crystal_crusher",
                () -> platform.machineItem(CRYSTAL_CRUSHER.get(), MachineIcon.CRYSTAL_CRUSHER)));
        ASPECT_COMBINER = trackBlock(blocks.<Block>register("aspect_combiner",
                () -> new TaMachineBlocks.MachineBlock(COMBINER_BE)));
        ASPECT_COMBINER_ITEM = track(items.register("aspect_combiner",
                () -> platform.machineItem(ASPECT_COMBINER.get(), MachineIcon.ASPECT_COMBINER)));
        AURA_CHARGER = trackBlock(blocks.<Block>register("aura_charger",
                () -> new TaMachineBlocks.MachineBlock(CHARGER_BE)));
        AURA_CHARGER_ITEM = track(items.register("aura_charger",
                () -> platform.machineItem(AURA_CHARGER.get(), MachineIcon.AURA_CHARGER)));
        AURA_DISPERSER = trackBlock(blocks.<Block>register("aura_disperser",
                () -> new TaMachineBlocks.MachineBlock(DISPERSER_BE, gui(DISPERSER_MENU,
                        dev.tc4port.thaumadditions.gui.MachineMenus.AuraDisperser::new))));
        FLUX_CONCENTRATOR = trackBlock(blocks.<Block>register("flux_concentrator",
                () -> new TaMachineBlocks.MachineBlock(CONCENTRATOR_BE)));
        FLUX_CONCENTRATOR_ITEM = track(items.register("flux_concentrator",
                () -> platform.machineItem(FLUX_CONCENTRATOR.get(), MachineIcon.FLUX_CONCENTRATOR)));
        ESSENTIA_SINK = trackBlock(blocks.<Block>register("essentia_sink",
                () -> new TaMachineBlocks.SinkBlock(SINK_BE)));
        ESSENTIA_SINK_AUX = trackBlock(blocks.<Block>register("essentia_sink_aux",
                TaMachineBlocks.SinkAuxBlock::new));
        SHADOW_ENCHANTER = trackBlock(blocks.<Block>register("shadow_enchanter",
                () -> new TaMachineBlocks.EnchanterBlock(ENCHANTER_BE, gui(ENCHANTER_MENU,
                        dev.tc4port.thaumadditions.gui.MachineMenus.ShadowEnchanter::new))));
        ENTITY_SUMMONER = trackBlock(blocks.<Block>register("entity_summoner",
                () -> new TaMachineBlocks.SummonerBlock(SUMMONER_BE)));
        SEAL = trackBlock(blocks.<Block>register("seal", () -> new TaMachineBlocks.SealBlock(SEAL_BE)));
        CRYSTAL_LAMP = trackBlock(blocks.<Block>register("crystal_lamp",
                () -> new TaMachineBlocks.CrystalLampBlock(CRYSTAL_LAMP_BE)));
        CRYSTAL_BLOCK = trackBlock(blocks.<Block>register("crystal_block",
                () -> new TaMachineBlocks.CrystalBlock(CRYSTAL_BE)));

        // The crops keep their own block classes because the age bound differs.
        VOID_CROP = trackBlock(blocks.<Block>register("void_crop", () -> platform.crop(crop(0.0F), 3)));
        VIS_CROP = trackBlock(blocks.<Block>register("vis_crop", () -> platform.crop(crop(0.0F), 4)));
        CAKE = trackBlock(blocks.<Block>register("cake",
                () -> new TaBlocks.ArcaneCakeBlock(cake(), CAKE_BE)));
    }

    /**
     * The sixteen block entity types. One type covers all six jars and one covers all
     * three smelters, because 1.12 varied them only through the block's own fields.
     */
    private static void blockEntities(Registrar registrar) {
        JAR_BE = registrar.register("jar", () -> BlockEntityType.Builder.<TileJar>of(
                (pos, state) -> new TileJar(JAR_BE.get(), pos, state), JAR_BRASS.get(), JAR_THAUMIUM.get(), JAR_ELDRITCH.get(),
                JAR_MITHRILLIUM.get(), JAR_ADAMINITE.get(), JAR_MITHMINITE.get()).build(null));
        SMELTER_BE = registrar.register("smelter", () -> BlockEntityType.Builder.<TileSmelter>of(
                (pos, state) -> new TileSmelter(SMELTER_BE.get(), pos, state), MITHRILLIUM_SMELTER.get(), ADAMINITE_SMELTER.get(),
                MITHMINITE_SMELTER.get()).build(null));
        CRYSTAL_BE = registrar.register("crystal_block", () -> BlockEntityType.Builder.<TileCrystal>of(
                (pos, state) -> new TileCrystal(CRYSTAL_BE.get(), pos, state), CRYSTAL_BLOCK.get()).build(null));
        CRYSTAL_LAMP_BE = registrar.register("crystal_lamp", () -> BlockEntityType.Builder.<TileCrystalLamp>of(
                (pos, state) -> new TileCrystalLamp(CRYSTAL_LAMP_BE.get(), pos, state), CRYSTAL_LAMP.get()).build(null));
        CAKE_BE = registrar.register("cake", () -> BlockEntityType.Builder.<TileCrystal.Cake>of(
                (pos, state) -> new TileCrystal.Cake(CAKE_BE.get(), pos, state), CAKE.get()).build(null));
        SEAL_BE = registrar.register("seal", () -> BlockEntityType.Builder.<TileSeal>of(
                (pos, state) -> new TileSeal(SEAL_BE.get(), pos, state), SEAL.get()).build(null));
        SINK_BE = registrar.register("essentia_sink", () -> BlockEntityType.Builder.<TileEssentiaSink>of(
                (pos, state) -> new TileEssentiaSink(SINK_BE.get(), pos, state), ESSENTIA_SINK.get()).build(null));
        SUMMONER_BE = registrar.register("entity_summoner", () -> BlockEntityType.Builder.<TileEntitySummoner>of(
                (pos, state) -> new TileEntitySummoner(SUMMONER_BE.get(), pos, state), ENTITY_SUMMONER.get()).build(null));
        ENCHANTER_BE = registrar.register("shadow_enchanter", () -> BlockEntityType.Builder.<TileShadowEnchanter>of(
                (pos, state) -> new TileShadowEnchanter(ENCHANTER_BE.get(), pos, state), SHADOW_ENCHANTER.get()).build(null));
        GROWTH_BE = registrar.register("growth_chamber", () -> BlockEntityType.Builder.<TileGrowthChamber>of(
                (pos, state) -> new TileGrowthChamber(GROWTH_BE.get(), pos, state), GROWTH_CHAMBER.get()).build(null));
        COMBINER_BE = registrar.register("aspect_combiner", () -> BlockEntityType.Builder.<TileAspectCombiner>of(
                (pos, state) -> new TileAspectCombiner(COMBINER_BE.get(), pos, state), ASPECT_COMBINER.get()).build(null));
        CHARGER_BE = registrar.register("aura_charger", () -> BlockEntityType.Builder.<TaTiles.Charger>of(
                (pos, state) -> new TaTiles.Charger(CHARGER_BE.get(), pos, state), AURA_CHARGER.get()).build(null));
        DISPERSER_BE = registrar.register("aura_disperser", () -> BlockEntityType.Builder.<TileAuraDisperser>of(
                (pos, state) -> new TileAuraDisperser(DISPERSER_BE.get(), pos, state), AURA_DISPERSER.get()).build(null));
        BORE_BE = registrar.register("crystal_bore", () -> BlockEntityType.Builder.<TileCrystalBore>of(
                (pos, state) -> new TileCrystalBore(BORE_BE.get(), pos, state), CRYSTAL_BORE.get()).build(null));
        CRUSHER_BE = registrar.register("crystal_crusher", () -> BlockEntityType.Builder.<TileCrystalCrusher>of(
                (pos, state) -> new TileCrystalCrusher(CRUSHER_BE.get(), pos, state), CRYSTAL_CRUSHER.get()).build(null));
        CONCENTRATOR_BE = registrar.register("flux_concentrator", () -> BlockEntityType.Builder.<TileFluxConcentrator>of(
                (pos, state) -> new TileFluxConcentrator(CONCENTRATOR_BE.get(), pos, state), FLUX_CONCENTRATOR.get()).build(null));
    }

    /**
     * Block items for every block whose registration did not already create one, in tab order.
     * The plain blocks ({@link #plainBlocks}) come through {@link #block}, which registers the
     * block and its item together — listing them here too would double-register.
     */
    private static void blockItems(Registrar items) {
        blockItem(items, "jar_brass", JAR_BRASS);
        blockItem(items, "jar_thaumium", JAR_THAUMIUM);
        blockItem(items, "jar_eldritch", JAR_ELDRITCH);
        blockItem(items, "jar_mithrillium", JAR_MITHRILLIUM);
        blockItem(items, "jar_adaminite", JAR_ADAMINITE);
        blockItem(items, "jar_mithminite", JAR_MITHMINITE);
        blockItem(items, "mithrillium_smelter", MITHRILLIUM_SMELTER);
        blockItem(items, "adaminite_smelter", ADAMINITE_SMELTER);
        blockItem(items, "mithminite_smelter", MITHMINITE_SMELTER);
        blockItem(items, "growth_chamber", GROWTH_CHAMBER);
        blockItem(items, "aura_disperser", AURA_DISPERSER);
        blockItem(items, "essentia_sink", ESSENTIA_SINK);
        blockItem(items, "shadow_enchanter", SHADOW_ENCHANTER);
        blockItem(items, "entity_summoner", ENTITY_SUMMONER);
        blockItem(items, "crystal_block", CRYSTAL_BLOCK);
        blockItem(items, "crystal_lamp", CRYSTAL_LAMP);
        blockItem(items, "cake", CAKE);
    }

    private static void blockItem(Registrar items, String name, Supplier<Block> holder) {
        track(items.register(name, () -> new BlockItem(holder.get(), new Item.Properties())));
    }

    /** The two seeds, after their crops exist. */
    private static void crops(Registrar items) {
        VIS_SEEDS = track(items.register("vis_seeds", () -> ToolItems.visSeeds(VIS_CROP)));
        VOID_SEED = track(items.register("void_seed", () -> ToolItems.voidSeed(VOID_CROP)));
    }

    private static Supplier<Block> jar(Registrar blocks, String name, TaMachineBlocks.JarKind kind) {
        return blocks.<Block>register(name, () -> new TaMachineBlocks.JarBlock(JAR_BE, kind));
    }

    private static Supplier<Block> smelter(Registrar blocks, String name, float efficiency, int speed,
                                          int capacity) {
        return blocks.<Block>register(name, () -> new TaMachineBlocks.SmelterBlock(SMELTER_BE,
                efficiency, speed, capacity, gui(SMELTER_MENU,
                        dev.tc4port.thaumadditions.gui.MachineMenus.Smelter::new)));
    }

    // ------------------------------------------------------------------ armor

    /**
     * 1.12 {@code ItemAdaminiteDress.ADAMINITE}: durability factor 40, protection
     * {feet 4, legs 6, chest 8, head 5}, enchantability 40, toughness 8, leather equip sound.
     * Vis discounts (percent) and the head-only goggles overlay come from the 1.12 item.
     */
    private static void armor(Registrar items, TaPlatform platform) {
        TaArmorMaterial adaminite = platform.armorMaterial("adaminite",
                ResourceLocation.tryParse("thaumadds:adaminite"), defense(5, 8, 6, 4), 40, 40,
                TaArmorSound.LEATHER, Content::thaumiumRepair, 8.0F, 0.0F, discounts(8, 4), true);
        ADAMINITE_HOOD = piece(items, platform, adaminite, ArmorItem.Type.HELMET, 40, null, "adaminite_hood");
        ADAMINITE_ROBE = piece(items, platform, adaminite, ArmorItem.Type.CHESTPLATE, 40, null, "adaminite_robe");
        ADAMINITE_BELT = piece(items, platform, adaminite, ArmorItem.Type.LEGGINGS, 40, null, "adaminite_belt");
        ADAMINITE_BOOTS = piece(items, platform, adaminite, ArmorItem.Type.BOOTS, 40, null, "adaminite_boots");

        // 1.12 ItemMithminiteDress.MITHMINITE: protection {feet 6, legs 10, chest 15, head 8},
        // same durability factor/enchantability/toughness, discounts {head 15, chest 10}, goggles.
        MithminiteArmorBehavior mithminiteBehavior = new MithminiteArmorBehavior(platform);
        TaArmorMaterial mithminite = platform.armorMaterial("mithminite",
                ResourceLocation.tryParse("thaumadds:mithminite"), defense(8, 15, 10, 6), 40, 40,
                TaArmorSound.LEATHER, Content::thaumiumRepair, 8.0F, 0.0F, discounts(15, 10), true);
        MITHMINITE_HOOD = piece(items, platform, mithminite, ArmorItem.Type.HELMET, 40, mithminiteBehavior, "mithminite_hood");
        MITHMINITE_ROBE = piece(items, platform, mithminite, ArmorItem.Type.CHESTPLATE, 40, mithminiteBehavior, "mithminite_robe");
        MITHMINITE_BELT = piece(items, platform, mithminite, ArmorItem.Type.LEGGINGS, 40, mithminiteBehavior, "mithminite_belt");
        MITHMINITE_BOOTS = piece(items, platform, mithminite, ArmorItem.Type.BOOTS, 40, mithminiteBehavior, "mithminite_boots");

        // 1.12 ItemBlueWolfSuit.BLUE_WOLF: protection {feet 6, legs 12, chest 16, head 8}, same
        // discounts as mithminite, but no goggles. The 1.12 registrations were commented out
        // upstream and the suit's appearance is a custom wavefront (OBJ) biped model with 256px
        // entity textures, none of which is ported yet — so the material points at TC4R's
        // invisible layer and the worn model is a follow-up. The items are functional.
        TaArmorMaterial blueWolf = platform.armorMaterial("blue_wolf",
                ArmorTextureApi.INVISIBLE_MATERIAL, defense(8, 16, 12, 6), 40, 40,
                TaArmorSound.LEATHER, Content::thaumiumRepair, 8.0F, 0.0F, discounts(15, 10), false);
        BLUE_WOLF_MASK = piece(items, platform, blueWolf, ArmorItem.Type.HELMET, 40, null, "blue_wolf_mask");
        BLUE_WOLF_BODY = piece(items, platform, blueWolf, ArmorItem.Type.CHESTPLATE, 40, null, "blue_wolf_body");
        BLUE_WOLF_LEGS = piece(items, platform, blueWolf, ArmorItem.Type.LEGGINGS, 40, null, "blue_wolf_legs");
        BLUE_WOLF_FEETPAWS = piece(items, platform, blueWolf, ArmorItem.Type.BOOTS, 40, null, "blue_wolf_feetpaws");
    }

    /** Registers one armor piece in tab order. */
    private static Supplier<Item> piece(Registrar items, TaPlatform platform, TaArmorMaterial material,
                                        ArmorItem.Type type, int durabilityMultiplier, TaArmorTick behavior,
                                        String name) {
        return track(items.register(name, () -> platform.armor(
                material, type, durabilityMultiplier, new Item.Properties(), behavior)));
    }

    /** 1.12 armor protection, given as head/chest/legs/feet. */
    private static Map<ArmorItem.Type, Integer> defense(int helmet, int chest, int legs, int boots) {
        return Map.of(ArmorItem.Type.HELMET, helmet, ArmorItem.Type.CHESTPLATE, chest,
                ArmorItem.Type.LEGGINGS, legs, ArmorItem.Type.BOOTS, boots);
    }

    /** 1.12 vis discounts (percent), given as head/chest; the other slots give none. */
    private static Map<ArmorItem.Type, Integer> discounts(int helmet, int chest) {
        return Map.of(ArmorItem.Type.HELMET, helmet, ArmorItem.Type.CHESTPLATE, chest);
    }

    private static Ingredient thaumiumRepair() {
        return Ingredient.of(ThaumcraftTags.THAUMIUM_INGOTS);
    }

    // ------------------------------------------------------------------ helpers

    private static Block.Properties metalStorage() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }

    private static Block.Properties wood(float strength, boolean ignored) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(strength).sound(SoundType.WOOD);
    }

    private static Block.Properties stone(float strength) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(strength).sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
    }

    private static Block.Properties crop(float strength) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().randomTicks()
                .sound(SoundType.CROP).strength(strength);
    }

    private static Block.Properties cake() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.5F).sound(SoundType.WOOL)
                .noOcclusion();
    }

    /** Registers a plain block plus the matching block item, in tab order. */
    private static Supplier<Block> block(Registrar items, Registrar blocks, String name,
                                         BlockBehaviour.Properties properties) {
        Supplier<Block> holder = blocks.register(name, () -> new Block(properties));
        track(items.register(name, () -> new BlockItem(holder.get(), new Item.Properties())));
        return holder;
    }

    /** Registers a block whose concrete class comes from a per-node factory, plus its block item. */
    private static Supplier<Block> block(Registrar items, Registrar blocks, String name,
                                         java.util.function.Supplier<Block> factory) {
        Supplier<Block> holder = blocks.register(name, factory);
        track(items.register(name, () -> new BlockItem(holder.get(), new Item.Properties())));
        return holder;
    }

    private static Item simple() {
        return new Item(new Item.Properties());
    }

    private static Supplier<Item> track(Supplier<Item> holder) {
        ITEM_ORDER.add(holder);
        return holder;
    }

    /** Blocks have no tab collection of their own; the holder is returned unchanged. */
    private static Supplier<Block> trackBlock(Supplier<Block> holder) {
        return holder;
    }

    /** Hands every registered item to the creative tab builder, in declaration order. */
    public static void forEachItem(Consumer<Item> out) {
        for (Supplier<Item> item : ITEM_ORDER)
            out.accept(item.get());
    }
}
