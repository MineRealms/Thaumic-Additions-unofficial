package dev.tc4port.thaumadditions.forge;

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
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(ThaumAdditions.MOD_ID)
public final class ThaumAdditionsForge {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ThaumAdditions.MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ThaumAdditions.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ThaumAdditions.MOD_ID);

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
        Content.init(items, blocks, new TaPlatformForge());

        TABS.register("main", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.thaumadds"))
                .icon(() -> new ItemStack(Content.BLUE_BONE.get()))
                .displayItems((parameters, output) -> Content.forEachItem(output::accept))
                .build());

        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        TABS.register(modBus);

        modBus.addListener(this::commonSetup);
        ThaumAdditions.init("forge", "1.20.1");
    }

    private void commonSetup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
    }
}
