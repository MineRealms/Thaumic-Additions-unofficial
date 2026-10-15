package dev.tc4port.thaumadditions.neoforge;

import java.util.function.Supplier;

import dev.tc4port.thaumadditions.ThaumAdditions;
import dev.tc4port.thaumadditions.registry.Content;
import dev.tc4port.thaumadditions.registry.Registrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(ThaumAdditions.MOD_ID)
public final class ThaumAdditionsNeoForge {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ThaumAdditions.MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, ThaumAdditions.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ThaumAdditions.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThaumAdditions.MOD_ID);
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ThaumAdditions.MOD_ID);

    public ThaumAdditionsNeoForge(IEventBus modBus) {
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
        TaPlatformNeoForge platform = new TaPlatformNeoForge();
        dev.tc4port.thaumadditions.platform.TaPlatform.Active.set(platform);
        Registrar menus = new Registrar() {
            @SuppressWarnings({"unchecked", "rawtypes"})
            @Override
            public <T> Supplier<T> register(String name, Supplier<T> factory) {
                return (Supplier<T>) (Supplier) MENUS.register(name, (Supplier) factory);
            }
        };
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
        // The right-click hook lives on the game bus, which the mod bus does not carry.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(ThaumAdditionsNeoForge.class);
        // 1.21.1 armor materials live in their own registry and must be registered before use.
        TaPlatformNeoForge.register(modBus);

        modBus.addListener(this::commonSetup);
        ThaumAdditions.init("neoforge", "1.21.1");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
    }

    /**
     * 1.12's {@code Block#onBlockActivated} hook, moved to the loader's own event because the block
     * method was renamed in 1.20.1 and again in 1.21.
     */
    @net.neoforged.bus.api.SubscribeEvent
    public static void onRightClick(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
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
