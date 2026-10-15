#!/usr/bin/env python3
"""Third round: the Content registrations, TaBlocks drift, GearItems/ToolItems cleanups."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
WILD = 'java.util.function.Supplier<? extends BlockEntityType<?>>'


def patch(path, old, new, count=1):
    p = ROOT / path
    s = p.read_text(encoding='utf-8')
    if old not in s:
        if new in s:
            print('already', path)
            return
        raise AssertionError(f'anchor missing in {path}: {old[:60]!r}')
    p.write_text(s.replace(old, new, count), encoding='utf-8', newline='\n')
    print('patched', path)


# ---------------- Content: type witnesses + wildcard helpers ----------------
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
        ('''        ESSENTIA_SINK_AUX = track(blocks.register("essentia_sink_aux", TaMachineBlocks.SinkAuxBlock::new));''',
         '''        ESSENTIA_SINK_AUX = track(blocks.<Block>register("essentia_sink_aux",
                TaMachineBlocks.SinkAuxBlock::new));'''),
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
):
    patch('src/main/java/dev/tc4port/thaumadditions/registry/Content.java', old, new)

# ---------------- TaBlocks: bonemeal lives on the platform now --------------
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
      '''        /** {@code Block} declares no {@code newBlockEntity}; the cake is not an {@code EntityBlock}. */
        public net.minecraft.world.level.block.entity.BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
            return type.get().create(pos, state);
        }''')

# ---------------- GearItems: drop the unused guards -------------------------
p = ROOT / 'src/main/java/dev/tc4port/thaumadditions/items/GearItems.java'
s = p.read_text(encoding='utf-8')
for marker in ('    /** Unused guard so the MobEffects import stays meaningful. */',
               '    /** Unused guard so the aspect import stays meaningful. */'):
    i = s.find(marker)
    if i >= 0:
        s = s[:i] + '}\n'
for imp in ('import net.minecraft.world.effect.MobEffects;\n',
            'import dev.tc4port.thaumadditions.aspect.TaAspect;\n',
            'import dev.tc4port.thaumcraft.api.aspect.AspectId;\n',
            'import dev.tc4port.thaumadditions.aspect.TaAspect;\n'):
    s = s.replace(imp, '')
p.write_text(s, encoding='utf-8', newline='\n')
print('GearItems cleaned')

# ---------------- ToolItems: missing import + pick() cast -------------------
patch('src/main/java/dev/tc4port/thaumadditions/items/ToolItems.java',
      'import net.minecraft.world.InteractionHand;\n',
      'import net.minecraft.world.InteractionHand;\nimport net.minecraft.world.InteractionResult;\n')
patch('src/main/java/dev/tc4port/thaumadditions/items/ToolItems.java',
      'BlockHitResult hit = player.pick(6.0D, 1.0F, false);',
      'BlockHitResult hit = (BlockHitResult) player.pick(6.0D, 1.0F, false);')
