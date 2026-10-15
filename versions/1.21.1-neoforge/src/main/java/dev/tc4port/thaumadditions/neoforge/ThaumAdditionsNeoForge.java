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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(ThaumAdditions.MOD_ID)
public final class ThaumAdditionsNeoForge {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ThaumAdditions.MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, ThaumAdditions.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ThaumAdditions.MOD_ID);

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
        Content.init(items, blocks);

        TABS.register("main", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.thaumadds"))
                .icon(() -> new ItemStack(Content.BLUE_BONE.get()))
                .displayItems((parameters, output) -> Content.forEachItem(output::accept))
                .build());

        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        TABS.register(modBus);

        modBus.addListener(this::commonSetup);
        ThaumAdditions.init("neoforge", "1.21.1");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
    }
}
