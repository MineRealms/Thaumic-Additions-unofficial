package dev.tc4port.thaumadditions.forge;

import dev.tc4port.thaumadditions.ThaumAdditions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ThaumAdditions.MOD_ID)
public final class ThaumAdditionsForge {
    public ThaumAdditionsForge(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        modBus.addListener(this::commonSetup);
        ThaumAdditions.init("forge", "1.20.1");
    }

    private void commonSetup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
    }
}
