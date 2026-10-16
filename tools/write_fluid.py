# Writes the crystal water fluid into both nodes.
#
# 1.12 registered one object, a Fluid subclass with a colour and two textures. Modern Minecraft
# splits a fluid in two: a FluidType carrying the look and the physics, and the Fluid itself
# carrying the states. Both are loader objects on Forge and on NeoForge, so both are built here,
# on the node that owns them, and handed back as one handle.
#
# Three details worth knowing before touching this:
#
#   * The textures belong to the fluid's *type*, through a client extension, not to the fluid
#     object. Passing locations to the fluid's properties is a compile error, which is the compiler
#     saying the same thing.
#
#   * The properties need the two fluid objects, and the two fluid objects need the properties.
#     The pair is held in an array and read lazily, so the cycle resolves when the first fluid is
#     constructed.
#
#   * The block and the bucket are looked up by name when the fluid needs them, rather than handed
#     over: they are registered by the shared content class, out of this method's return value, so
#     they cannot exist yet. Nothing calls those suppliers until the fluid is used in a world.
#
# The two DeferredRegisters land in the platform class and are wired from the mod class's
# constructor: a loader registry cannot be registered on the bus from shared code.
import io

ID = "dev.tc4port.thaumadditions.ThaumAdditions.MOD_ID"

REG_FORGE = '''
    /** Modern Minecraft keeps a fluid's type in a registry of its own, beside the fluid itself. */
    private static final net.minecraftforge.registries.DeferredRegister<
            net.minecraftforge.fluids.FluidType> FLUID_TYPES =
            net.minecraftforge.registries.DeferredRegister.create(
                    net.minecraftforge.registries.ForgeRegistries.Keys.FLUID_TYPES,
                    %(ID)s);
    private static final net.minecraftforge.registries.DeferredRegister<
            net.minecraft.world.level.material.Fluid> FLUIDS =
            net.minecraftforge.registries.DeferredRegister.create(
                    net.minecraft.core.registries.Registries.FLUID, %(ID)s);

    /** Wires the fluid registries; call from the mod constructor. */
    public static void register(net.minecraftforge.eventbus.api.IEventBus modBus) {
        FLUIDS.register(modBus);
        FLUID_TYPES.register(modBus);
    }
'''

REG_NEO = '''
    /** Modern Minecraft keeps a fluid's type in a registry of its own, beside the fluid itself. */
    private static final net.neoforged.neoforge.registries.DeferredRegister<
            net.neoforged.neoforge.fluids.FluidType> FLUID_TYPES =
            net.neoforged.neoforge.registries.DeferredRegister.create(
                    net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.FLUID_TYPES,
                    %(ID)s);
    private static final net.neoforged.neoforge.registries.DeferredRegister<
            net.minecraft.world.level.material.Fluid> FLUIDS =
            net.neoforged.neoforge.registries.DeferredRegister.create(
                    net.minecraft.core.registries.Registries.FLUID, %(ID)s);
'''

FORGE = '''
    @Override
    public dev.tc4port.thaumadditions.platform.TaFluid fluid(
            String name, ResourceLocation stillTexture, ResourceLocation flowingTexture,
            int tint, int lightLevel, int density, int viscosity) {
        net.minecraftforge.registries.RegistryObject<net.minecraftforge.fluids.FluidType> type =
                FLUID_TYPES.register(name, () -> new net.minecraftforge.fluids.FluidType(
                        net.minecraftforge.fluids.FluidType.Properties.create()
                                .descriptionId("fluid." + %(ID)s + "." + name)
                                .lightLevel(lightLevel)
                                .density(density)
                                .viscosity(viscosity)) {
                    @Override
                    public void initializeClient(java.util.function.Consumer<
                            net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions> consumer) {
                        consumer.accept(new net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions() {
                            @Override
                            public ResourceLocation getStillTexture() {
                                return stillTexture;
                            }

                            @Override
                            public ResourceLocation getFlowingTexture() {
                                return flowingTexture;
                            }

                            @Override
                            public int getTintColor() {
                                return tint;
                            }
                        });
                    }
                });

        // The fluids need the properties and the properties need the fluids; the array breaks the
        // cycle, since neither supplier is read until the first fluid is built.
        net.minecraftforge.registries.RegistryObject<?>[] pair =
                new net.minecraftforge.registries.RegistryObject<?>[2];
        pair[0] = FLUIDS.register(name,
                () -> new net.minecraftforge.fluids.ForgeFlowingFluid.Source(properties(type, pair, name)));
        pair[1] = FLUIDS.register(name + "_flowing",
                () -> new net.minecraftforge.fluids.ForgeFlowingFluid.Flowing(properties(type, pair, name)));

        return new dev.tc4port.thaumadditions.platform.TaFluid() {
            @Override
            public Supplier<? extends net.minecraft.world.level.material.FlowingFluid> source() {
                return () -> (net.minecraft.world.level.material.FlowingFluid) pair[0].get();
            }

            @Override
            public Supplier<? extends net.minecraft.world.level.material.FlowingFluid> flowing() {
                return () -> (net.minecraft.world.level.material.FlowingFluid) pair[1].get();
            }
        };
    }

    private static net.minecraftforge.fluids.ForgeFlowingFluid.Properties properties(
            net.minecraftforge.registries.RegistryObject<net.minecraftforge.fluids.FluidType> type,
            net.minecraftforge.registries.RegistryObject<?>[] pair, String name) {
        return new net.minecraftforge.fluids.ForgeFlowingFluid.Properties(type,
                () -> (net.minecraft.world.level.material.FlowingFluid) pair[0].get(),
                () -> (net.minecraft.world.level.material.FlowingFluid) pair[1].get())
                // The block and the bucket are registered after the fluid, so they are found by
                // name; neither supplier is read until the fluid is used in a world.
                .block(() -> (net.minecraft.world.level.block.LiquidBlock)
                        net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(fluidId(name)))
                .bucket(() -> net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                        fluidId(name + "_bucket")))
                // 1.12's BlockFluidClassic tickRate: crystal water creeps, it does not run.
                .tickRate(20);
    }

    private static ResourceLocation fluidId(String path) {
        return ResourceLocation.tryParse(%(ID)s + ":" + path);
    }
'''

NEO = '''
    @Override
    public dev.tc4port.thaumadditions.platform.TaFluid fluid(
            String name, ResourceLocation stillTexture, ResourceLocation flowingTexture,
            int tint, int lightLevel, int density, int viscosity) {
        net.neoforged.neoforge.registries.DeferredHolder<net.neoforged.neoforge.fluids.FluidType,
                net.neoforged.neoforge.fluids.FluidType> type =
                FLUID_TYPES.register(name, () -> new net.neoforged.neoforge.fluids.FluidType(
                        net.neoforged.neoforge.fluids.FluidType.Properties.create()
                                .descriptionId("fluid." + %(ID)s + "." + name)
                                .lightLevel(lightLevel)
                                .density(density)
                                .viscosity(viscosity)) {
                    @Override
                    public void initializeClient(java.util.function.Consumer<
                            net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions> consumer) {
                        consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions() {
                            @Override
                            public ResourceLocation getStillTexture() {
                                return stillTexture;
                            }

                            @Override
                            public ResourceLocation getFlowingTexture() {
                                return flowingTexture;
                            }

                            @Override
                            public int getTintColor() {
                                return tint;
                            }
                        });
                    }
                });

        // The fluids need the properties and the properties need the fluids; the array breaks the
        // cycle, since neither supplier is read until the first fluid is built.
        net.neoforged.neoforge.registries.DeferredHolder<?, ?>[] pair =
                new net.neoforged.neoforge.registries.DeferredHolder<?, ?>[2];
        pair[0] = FLUIDS.register(name,
                () -> new net.neoforged.neoforge.fluids.BaseFlowingFluid.Source(properties(type, pair, name)));
        pair[1] = FLUIDS.register(name + "_flowing",
                () -> new net.neoforged.neoforge.fluids.BaseFlowingFluid.Flowing(properties(type, pair, name)));

        return new dev.tc4port.thaumadditions.platform.TaFluid() {
            @Override
            public Supplier<? extends net.minecraft.world.level.material.FlowingFluid> source() {
                return () -> (net.minecraft.world.level.material.FlowingFluid) pair[0].get();
            }

            @Override
            public Supplier<? extends net.minecraft.world.level.material.FlowingFluid> flowing() {
                return () -> (net.minecraft.world.level.material.FlowingFluid) pair[1].get();
            }
        };
    }

    private static net.neoforged.neoforge.fluids.BaseFlowingFluid.Properties properties(
            net.neoforged.neoforge.registries.DeferredHolder<net.neoforged.neoforge.fluids.FluidType,
                    net.neoforged.neoforge.fluids.FluidType> type,
            net.neoforged.neoforge.registries.DeferredHolder<?, ?>[] pair, String name) {
        return new net.neoforged.neoforge.fluids.BaseFlowingFluid.Properties(type,
                () -> (net.minecraft.world.level.material.FlowingFluid) pair[0].get(),
                () -> (net.minecraft.world.level.material.FlowingFluid) pair[1].get())
                // The block and the bucket are registered after the fluid, so they are found by
                // name; neither supplier is read until the fluid is used in a world.
                .block(() -> (net.minecraft.world.level.block.LiquidBlock)
                        net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(fluidId(name)))
                .bucket(() -> net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                        fluidId(name + "_bucket")))
                // 1.12's BlockFluidClassic tickRate: crystal water creeps, it does not run.
                .tickRate(20);
    }

    private static ResourceLocation fluidId(String path) {
        return ResourceLocation.tryParse(%(ID)s + ":" + path);
    }
'''

# Which method on the platform class holds the bus wiring, and how the mod class reaches it.
WIRING = {
    "1.20.1-forge": (
        "forge", "Forge",
        "public final class TaPlatformForge implements TaPlatform {",
        "        TABS.register(modBus);",
        "        TABS.register(modBus);\n        TaPlatformForge.register(modBus);",
    ),
    "1.21.1-neoforge": (
        "neoforge", "NeoForge",
        "public final class TaPlatformNeoForge implements TaPlatform {",
        "        ARMOR_MATERIALS.register(modBus);",
        "        ARMOR_MATERIALS.register(modBus);\n        FLUIDS.register(modBus);\n"
        "        FLUID_TYPES.register(modBus);",
    ),
}

for node, (pkg, suffix, declaration, bus_marker, bus_replacement) in WIRING.items():
    body = FORGE if pkg == "forge" else NEO
    reg = (REG_FORGE if pkg == "forge" else REG_NEO).replace("%(ID)s", ID).strip("\n")

    platform = "versions/%s/src/main/java/dev/tc4port/thaumadditions/%s/TaPlatform%s.java" % (
        node, pkg, suffix)
    text = io.open(platform, encoding="utf-8").read()

    marker = "    private static MobEffect nativeEffect(TaEffect effect) {"
    if marker not in text:
        marker = "    private static Holder<MobEffect> nativeEffect(TaEffect effect) {"
    assert marker in text, platform
    if "public dev.tc4port.thaumadditions.platform.TaFluid fluid(" in text:
        # Already written: running twice must not write the registers twice.
        print("skipped", platform)
    else:
        text = text.replace(marker, body.replace("%(ID)s", ID).strip("\n") + "\n\n" + marker, 1)
        at = text.index("{", text.index(declaration)) + 1
        text = text[:at] + "\n" + reg + "\n" + text[at:]
        io.open(platform, "w", encoding="utf-8", newline="\n").write(text)
        print("wrote", platform)

    if bus_marker is not None:
        # Where the bus wiring goes depends on which class already owns a wiring method: Forge's
        # platform has none, so the mod constructor calls the new one; NeoForge's platform already
        # has one for the armor materials, and its mod constructor already calls it.
        target = platform if bus_marker in text else (
            "versions/%s/src/main/java/dev/tc4port/thaumadditions/%s/ThaumAdditions%s.java" % (
                node, pkg, suffix))
        text = io.open(target, encoding="utf-8").read()
        assert bus_marker in text, target
        # The last line the replacement adds is what tells us whether it has already run.
        if bus_replacement.rsplit("\n", 1)[-1].strip() not in text:
            text = text.replace(bus_marker, bus_replacement, 1)
            io.open(target, "w", encoding="utf-8", newline="\n").write(text)
            print("wrote", target)
        else:
            print("skipped", target)
