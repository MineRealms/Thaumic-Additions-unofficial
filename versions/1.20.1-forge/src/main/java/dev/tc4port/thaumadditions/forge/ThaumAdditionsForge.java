package dev.tc4port.thaumadditions.forge;

import java.util.function.Supplier;

import dev.tc4port.thaumadditions.ThaumAdditions;
import dev.tc4port.thaumadditions.platform.TaPlatform;
import dev.tc4port.thaumadditions.registry.Content;
import dev.tc4port.thaumadditions.registry.Registrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

@Mod(ThaumAdditions.MOD_ID)
public final class ThaumAdditionsForge {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ThaumAdditions.MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ThaumAdditions.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ThaumAdditions.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ThaumAdditions.MOD_ID);
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ThaumAdditions.MOD_ID);

    public ThaumAdditionsForge(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();

        Registrar items = new Registrar() {
            @SuppressWarnings({"unchecked", "rawtypes"})
            @Override
            public <T> Supplier<T> register(String name, Supplier<T> factory) {
                return (Supplier<T>) (Supplier) ITEMS.register(name, (Supplier) factory);
            }
        };
        Registrar blocks = new Registrar() {
            @SuppressWarnings({"unchecked", "rawtypes"})
            @Override
            public <T> Supplier<T> register(String name, Supplier<T> factory) {
                return (Supplier<T>) (Supplier) BLOCKS.register(name, (Supplier) factory);
            }
        };
        Registrar blockEntities = new Registrar() {
            @SuppressWarnings({"unchecked", "rawtypes"})
            @Override
            public <T> Supplier<T> register(String name, Supplier<T> factory) {
                return (Supplier<T>) (Supplier) BLOCK_ENTITIES.register(name, (Supplier) factory);
            }
        };
        Registrar menus = new Registrar() {
            @SuppressWarnings({"unchecked", "rawtypes"})
            @Override
            public <T> Supplier<T> register(String name, Supplier<T> factory) {
                return (Supplier<T>) (Supplier) MENUS.register(name, (Supplier) factory);
            }
        };
        TaPlatformForge platform = new TaPlatformForge();
        TaPlatform.Active.set(platform);
        Content.init(items, blocks, blockEntities, menus, platform);

        TABS.register("main", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.thaumadds"))
                .icon(() -> new ItemStack(Content.BLUE_BONE.get()))
                .displayItems((parameters, output) -> Content.forEachItem(output::accept))
                .build());

        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        TABS.register(modBus);

        modBus.addListener(this::commonSetup);
        // The right-click hook lives on the game bus, which the mod bus does not carry.
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(ThaumAdditionsForge.class);
        ThaumAdditions.init("forge", "1.20.1");
    }

    private void commonSetup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
    }

    /**
     * 1.12's {@code Block#onBlockActivated} hook, moved to the loader's own event because the block
     * method was renamed in 1.20.1 and again in 1.21.
     */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onRightClick(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return;
        }
        if (dev.tc4port.thaumadditions.gui.MachineInteractions.openMachine(
                event.getEntity(), event.getLevel(), event.getPos())) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        }
    }
}
