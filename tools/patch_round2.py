#!/usr/bin/env python3
"""Second round of compile fixes: generics, stale helpers, drifted overrides."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def patch(path, old, new, count=1):
    p = ROOT / path
    s = p.read_text(encoding='utf-8')
    if old not in s:
        if new in s:
            print('already patched', path)
            return
        raise AssertionError(f'anchor missing in {path}')
    s = s.replace(old, new, count)
    p.write_text(s, encoding='utf-8', newline='\n')
    print('patched', path)


# --- 1. BeBlock / block classes accept wildcard suppliers -------------------
patch('src/main/java/dev/tc4port/thaumadditions/blocks/TaMachineBlocks.java',
      'Supplier<BlockEntityType<?>> type();',
      'java.util.function.Supplier<? extends BlockEntityType<?>> type();')
for old, new in (
        ('Supplier<BlockEntityType<?>> type;', 'java.util.function.Supplier<? extends BlockEntityType<?>> type;'),
        ('protected AbstractBeBlock(Properties properties, Supplier<BlockEntityType<?>> type)',
         'protected AbstractBeBlock(Properties properties,\n                                    java.util.function.Supplier<? extends BlockEntityType<?>> type)'),
        ('public JarBlock(Supplier<BlockEntityType<?>> type, int capacity)',
         'public JarBlock(java.util.function.Supplier<? extends BlockEntityType<?>> type, int capacity)'),
        ('public SmelterBlock(Supplier<BlockEntityType<?>> type, float efficiency, int speed, int capacity)',
         'public SmelterBlock(java.util.function.Supplier<? extends BlockEntityType<?>> type, float efficiency,\n                                int speed, int capacity)'),
        ('public Supplier<BlockEntityType<?>> type()', 'public java.util.function.Supplier<? extends BlockEntityType<?>> type()'),
        ('private final Supplier<BlockEntityType<?>> type;',
         'private final java.util.function.Supplier<? extends BlockEntityType<?>> type;'),
        ('public SinkBlock(Supplier<BlockEntityType<?>> type)',
         'public SinkBlock(java.util.function.Supplier<? extends BlockEntityType<?>> type)'),
        ('public SealBlock(Supplier<BlockEntityType<?>> type)',
         'public SealBlock(java.util.function.Supplier<? extends BlockEntityType<?>> type)'),
        ('public CrystalBlock(Supplier<BlockEntityType<?>> type)',
         'public CrystalBlock(java.util.function.Supplier<? extends BlockEntityType<?>> type)'),
        ('public CrystalLampBlock(Supplier<BlockEntityType<?>> type)',
         'public CrystalLampBlock(java.util.function.Supplier<? extends BlockEntityType<?>> type)'),
        ('public SummonerBlock(Supplier<BlockEntityType<?>> type)',
         'public SummonerBlock(java.util.function.Supplier<? extends BlockEntityType<?>> type)'),
        ('public EnchanterBlock(Supplier<BlockEntityType<?>> type)',
         'public EnchanterBlock(java.util.function.Supplier<? extends BlockEntityType<?>> type)'),
        ('public GrowthChamberBlock(Supplier<BlockEntityType<?>> type)',
         'public GrowthChamberBlock(java.util.function.Supplier<? extends BlockEntityType<?>> type)'),
        ('public MachineBlock(Supplier<BlockEntityType<?>> type)',
         'public MachineBlock(java.util.function.Supplier<? extends BlockEntityType<?>> type)'),
):
    patch('src/main/java/dev/tc4port/thaumadditions/blocks/TaMachineBlocks.java', old, new)

# --- 2. SmelterBlock accessors + drop the stale draft helpers ---------------
patch('src/main/java/dev/tc4port/thaumadditions/blocks/TaMachineBlocks.java',
      '''    public static class SmelterBlock extends TaBlocks.EnableableBlock {
        public SmelterBlock(Supplier<BlockEntityType<?>> type) {
            super(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().lightLevel(state -> state.getValue(ENABLED) ? 13 : 0));
            this.type = type;
        }

        @Override
        public Supplier<BlockEntityType<?>> type() {
            return type;
        }''',
      '''    public static class SmelterBlock extends TaBlocks.EnableableBlock implements BeBlock {
        private final java.util.function.Supplier<? extends BlockEntityType<?>> type;
        private final float efficiency;
        private final int speed;
        private final int capacity;

        public SmelterBlock(java.util.function.Supplier<? extends BlockEntityType<?>> type, float efficiency,
                            int speed, int capacity) {
            super(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().lightLevel(state -> state.getValue(ENABLED) ? 13 : 0));
            this.type = type;
            this.efficiency = efficiency;
            this.speed = speed;
            this.capacity = capacity;
        }

        /** 1.12 {@code BlockAbstractSmelter(efficiency, speed, capacity)} constructor args. */
        public float efficiency() {
            return efficiency;
        }

        public int speed() {
            return speed;
        }

        public int capacity() {
            return capacity;
        }

        @Override
        public java.util.function.Supplier<? extends BlockEntityType<?>> type() {
            return type;
        }''')

# Drop the stale tile-factory helpers from the first draft.
p = ROOT / 'src/main/java/dev/tc4port/thaumadditions/blocks/TaMachineBlocks.java'
s = p.read_text(encoding='utf-8')
start = s.find('    /** Unused guard so the tile imports stay meaningful for javac. */')
if start >= 0:
    s = s[:start] + '}\n'
    p.write_text(s, encoding='utf-8', newline='\n')
    print('dropped stale tile helpers')

# --- 3. TaBlocks: remove the drifted bonemeal overrides, fix cake @Override --
patch('src/main/java/dev/tc4port/thaumadditions/blocks/TaBlocks.java',
      '''        @Override
        public int getMaxAge() {
            return maxAge;
        }

        @Override
        public boolean isValidBonemealTarget(Level level, BlockPos pos, BlockState state, boolean isClient) {
            return false;
        }

        @Override
        public boolean isBonemealSuccess(Level level, net.minecraft.util.RandomSource random, BlockPos pos,
                                         BlockState state) {
            return false;
        }
    }''',
      '''        @Override
        public int getMaxAge() {
            return maxAge;
        }
    }''')
patch('src/main/java/dev/tc4port/thaumadditions/blocks/TaBlocks.java',
      '''        @Override
        public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return type.get().create(pos, state);
        }''',
      '''        /** {@code Block} has no {@code newBlockEntity}; the cake is not an {@code EntityBlock}. */
        public net.minecraft.world.level.block.entity.BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
            return type.get().create(pos, state);
        }''')

# --- 4. GearItems: drop the unused guards ----------------------------------
p = ROOT / 'src/main/java/dev/tc4port/thaumadditions/items/GearItems.java'
s = p.read_text(encoding='utf-8')
start = s.find('    /** Unused guards keeping the shared imports meaningful. */')
if start < 0:
    start = s.find('    /** Unused guard so the MobEffects import stays meaningful. */')
if start >= 0:
    s = s[:start] + '}\n'
    s = s.replace('import net.minecraft.world.effect.MobEffects;\n', '')
    s = s.replace('import dev.tc4port.thaumadditions.aspect.TaAspect;\n', '')
    s = s.replace('import dev.tc4port.thaumcraft.api.aspect.AspectId;\n', '')
    p.write_text(s, encoding='utf-8', newline='\n')
    print('GearItems cleaned')

# --- 5. ToolItems: pick() cast ---------------------------------------------
patch('src/main/java/dev/tc4port/thaumadditions/items/ToolItems.java',
      'BlockHitResult hit = player.pick(6.0D, 1.0F, false);',
      'BlockHitResult hit = (BlockHitResult) player.pick(6.0D, 1.0F, false);')

# --- 6. Content: explicit type witnesses for the registrar generics --------
patch('src/main/java/dev/tc4port/thaumadditions/registry/Content.java',
      '''    private static Supplier<Block> jar(Registrar blocks, String name, int capacity) {
        return blocks.register(name, () -> new TaMachineBlocks.JarBlock(JAR_BE, capacity));
    }

    private static Supplier<Block> smelter(Registrar blocks, String name, float efficiency, int speed,
                                          int capacity) {
        return blocks.register(name, () -> new TaMachineBlocks.SmelterBlock(SMELTER_BE, efficiency, speed, capacity));
    }

    private static Supplier<Block> machine(Registrar blocks, String name, Supplier<BlockEntityType<?>> type) {
        return blocks.register(name, () -> new TaMachineBlocks.MachineBlock(type));
    }''',
      '''    private static Supplier<Block> jar(Registrar blocks, String name, int capacity) {
        return blocks.<Block>register(name, () -> new TaMachineBlocks.JarBlock(JAR_BE, capacity));
    }

    private static Supplier<Block> smelter(Registrar blocks, String name, float efficiency, int speed,
                                          int capacity) {
        return blocks.<Block>register(name,
                () -> new TaMachineBlocks.SmelterBlock(SMELTER_BE, efficiency, speed, capacity));
    }

    private static Supplier<Block> machine(Registrar blocks, String name,
                                           java.util.function.Supplier<? extends BlockEntityType<?>> type) {
        return blocks.<Block>register(name, () -> new TaMachineBlocks.MachineBlock(type));
    }''')
for old, new in (
        ('''        GROWTH_CHAMBER = track(blocks.register("growth_chamber",
                () -> new TaMachineBlocks.GrowthChamberBlock(GROWTH_BE)));''',
         '''        GROWTH_CHAMBER = track(blocks.<Block>register("growth_chamber",
                () -> new TaMachineBlocks.GrowthChamberBlock(GROWTH_BE)));'''),
        ('''        ESSENTIA_SINK = track(blocks.register("essentia_sink", () -> new TaMachineBlocks.SinkBlock(SINK_BE)));''',
         '''        ESSENTIA_SINK = track(blocks.<Block>register("essentia_sink",
                () -> new TaMachineBlocks.SinkBlock(SINK_BE)));'''),
        ('''        SHADOW_ENCHANTER = track(blocks.register("shadow_enchanter",
                () -> new TaMachineBlocks.EnchanterBlock(ENCHANTER_BE)));''',
         '''        SHADOW_ENCHANTER = track(blocks.<Block>register("shadow_enchanter",
                () -> new TaMachineBlocks.EnchanterBlock(ENCHANTER_BE)));'''),
        ('''        ENTITY_SUMMONER = track(blocks.register("entity_summoner",
                () -> new TaMachineBlocks.SummonerBlock(SUMMONER_BE)));''',
         '''        ENTITY_SUMMONER = track(blocks.<Block>register("entity_summoner",
                () -> new TaMachineBlocks.SummonerBlock(SUMMONER_BE)));'''),
        ('''        SEAL = track(blocks.register("seal", () -> new TaMachineBlocks.SealBlock(SEAL_BE)));''',
         '''        SEAL = track(blocks.<Block>register("seal", () -> new TaMachineBlocks.SealBlock(SEAL_BE)));'''),
        ('''        CRYSTAL_LAMP = track(blocks.register("crystal_lamp",
                () -> new TaMachineBlocks.CrystalLampBlock(CRYSTAL_LAMP_BE)));''',
         '''        CRYSTAL_LAMP = track(blocks.<Block>register("crystal_lamp",
                () -> new TaMachineBlocks.CrystalLampBlock(CRYSTAL_LAMP_BE)));'''),
        ('''        CRYSTAL_BLOCK = track(blocks.register("crystal_block",
                () -> new TaMachineBlocks.CrystalBlock(CRYSTAL_BE)));''',
         '''        CRYSTAL_BLOCK = track(blocks.<Block>register("crystal_block",
                () -> new TaMachineBlocks.CrystalBlock(CRYSTAL_BE)));'''),
        ('''        VOID_CROP = track(blocks.register("void_crop", () -> platform.crop(crop(0.0F), 3)));
        VIS_CROP = track(blocks.register("vis_crop", () -> platform.crop(crop(0.0F), 4)));''',
         '''        VOID_CROP = track(blocks.<Block>register("void_crop", () -> platform.crop(crop(0.0F), 3)));
        VIS_CROP = track(blocks.<Block>register("vis_crop", () -> platform.crop(crop(0.0F), 4)));'''),
        ('''        CAKE = track(blocks.register("cake", () -> new TaBlocks.ArcaneCakeBlock(cake(), CAKE_BE)));''',
         '''        CAKE = track(blocks.<Block>register("cake",
                () -> new TaBlocks.ArcaneCakeBlock(cake(), CAKE_BE)));'''),
        ('''        ESSENTIA_SINK_AUX = track(blocks.register("essentia_sink_aux", TaMachineBlocks.SinkAuxBlock::new));''',
         '''        ESSENTIA_SINK_AUX = track(blocks.<Block>register("essentia_sink_aux",
                TaMachineBlocks.SinkAuxBlock::new));'''),
):
    patch('src/main/java/dev/tc4port/thaumadditions/registry/Content.java', old, new)
