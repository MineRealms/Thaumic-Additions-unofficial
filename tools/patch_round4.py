#!/usr/bin/env python3
"""Fourth round: platform-only food builder, crop imports, the block track overload."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def patch(path, old, new):
    p = ROOT / path
    s = p.read_text(encoding='utf-8')
    if old not in s:
        if new in s:
            print('already', path)
            return
        raise AssertionError(f'anchor missing in {path}: {old[:70]!r}')
    p.write_text(s.replace(old, new, 1), encoding='utf-8', newline='\n')
    print('patched', path)


# 1. Content: restore the block track overload
patch('src/main/java/dev/tc4port/thaumadditions/registry/Content.java',
      '''    private static Supplier<Item> track(Supplier<Item> holder) {
        ITEM_ORDER.add(holder);
        return holder;
    }''',
      '''    private static Supplier<Item> track(Supplier<Item> holder) {
        ITEM_ORDER.add(holder);
        return holder;
    }

    /** Blocks are tracked by their own registration order; nothing to collect here. */
    private static Supplier<Block> track(Supplier<Block> holder) {
        return holder;
    }''')

# 2. Food: alwaysEat/alwaysEdible is another drift -> move it onto the port
patch('src/main/java/dev/tc4port/thaumadditions/platform/TaPlatform.java',
      '''    /**
     * Creates a crop block with vanilla bonemeal disabled, as both 1.12 crops were.''',
      '''    /**
     * Builds the 1.12 void-fruit food properties. {@code FoodProperties.Builder}
     * spells the always-edible flag {@code alwaysEat} on 1.20.1 and
     * {@code alwaysEdible} on 1.21.1, so it lives on the port too.
     */
    net.minecraft.world.food.FoodProperties food(int nutrition, float saturationModifier, boolean alwaysEdible);

    /**
     * Creates a crop block with vanilla bonemeal disabled, as both 1.12 crops were.''')

patch('src/main/java/dev/tc4port/thaumadditions/items/ToolItems.java',
      '''    /** 1.12 {@code ItemVoidFruit}: 5 hunger / 10 saturation, always edible. */
    public static Item voidFruit() {
        return new Item(new Item.Properties().food(new FoodProperties.Builder()
                .nutrition(5).saturationMod(10.0F).alwaysEdible().build())) {
        };
    }''',
      '''    /** 1.12 {@code ItemVoidFruit}: 5 hunger / 10 saturation, always edible. */
    public static Item voidFruit(dev.tc4port.thaumadditions.platform.TaPlatform platform) {
        return new Item(new Item.Properties()
                .food(platform.food(5, 10.0F, true))) {
        };
    }''')

patch('src/main/java/dev/tc4port/thaumadditions/registry/Content.java',
      'VOID_FRUIT = track(items.register("void_fruit", ToolItems::voidFruit));',
      'VOID_FRUIT = track(items.register("void_fruit", () -> ToolItems.voidFruit(platform)));')

# 3. forge crop(): missing imports
p = ROOT / 'versions/1.20.1-forge/src/main/java/dev/tc4port/thaumadditions/forge/TaPlatformForge.java'
s = p.read_text(encoding='utf-8')
for imp in ('import net.minecraft.core.BlockPos;\n', 'import net.minecraft.world.level.block.state.BlockState;\n'):
    if imp not in s:
        s = s.replace('import net.minecraft.world.level.block.Block;\n',
                      'import net.minecraft.world.level.block.Block;\n' + imp, 1)
p.write_text(s, encoding='utf-8', newline='\n')
print('forge crop imports')

# 4. node food() implementations
FORGE_FOOD = '''
    @Override
    public net.minecraft.world.food.FoodProperties food(int nutrition, float saturationModifier,
                                                        boolean alwaysEdible) {
        net.minecraft.world.food.FoodProperties.Builder builder = new net.minecraft.world.food.FoodProperties.Builder()
                .nutrition(nutrition).saturationMod(saturationModifier);
        if (alwaysEdible) {
            builder = builder.alwaysEat();
        }
        return builder.build();
    }
'''
NEO_FOOD = FORGE_FOOD.replace('.alwaysEat();', '.alwaysEdible();')

for path, impl in (
        ('versions/1.20.1-forge/src/main/java/dev/tc4port/thaumadditions/forge/TaPlatformForge.java', FORGE_FOOD),
        ('versions/1.21.1-neoforge/src/main/java/dev/tc4port/thaumadditions/neoforge/TaPlatformNeoForge.java', NEO_FOOD),
):
    p = ROOT / path
    s = p.read_text(encoding='utf-8')
    if 'public net.minecraft.world.food.FoodProperties food(' in s:
        print('already', path)
        continue
    anchor = '    @Override\n    public Block crop('
    assert anchor in s, path
    s = s.replace(anchor, impl + '\n' + anchor, 1)
    p.write_text(s, encoding='utf-8', newline='\n')
    print('patched', path)
