#!/usr/bin/env python3
"""One-shot patch: replace tooltipItem with the ItemBehavior facade in both nodes."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

OLD_PLATFORM = '''    /**
     * Creates an item that appends tooltip lines.
     *
     * <p>{@code Item.appendHoverText} took a {@code Level} on 1.20.1 and an
     * {@code Item.TooltipContext} on 1.21.1, so a shared override is impossible;
     * each node supplies the subclass and delegates to the shared lines.</p>
     */
    Item tooltipItem(Item.Properties properties, Function<ItemStack, List<Component>> lines);'''

NEW_PLATFORM = '''    /**
     * Creates an item whose drifted {@link Item} hooks are forwarded to
     * {@link dev.tc4port.thaumadditions.items.ItemBehavior}.
     *
     * <p>This is the single place the 1.20.1 vs 1.21.1 item-method drift is
     * absorbed: {@code appendHoverText} took a {@code Level} on 1.20.1 and an
     * {@code Item.TooltipContext} on 1.21.1. Every ported item is written against
     * {@code ItemBehavior} instead of overriding those methods directly.</p>
     */
    Item behaviorItem(Item.Properties properties, dev.tc4port.thaumadditions.items.ItemBehavior behavior);'''


def forge_impl():
    return '''    @Override
    public Item behaviorItem(Item.Properties properties,
                             dev.tc4port.thaumadditions.items.ItemBehavior behavior) {
        return new Item(properties) {
            @Override
            public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                return behavior.use(level, player, hand);
            }

            @Override
            public InteractionResult useOn(UseOnContext context) {
                return behavior.useOn(context);
            }

            @Override
            public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                          InteractionHand hand) {
                return behavior.interactLivingEntity(stack, player, target, hand);
            }

            @Override
            public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
                behavior.inventoryTick(stack, level, entity, slot, selected);
            }

            @Override
            public void onCraftedBy(ItemStack stack, Level level, Player player) {
                behavior.onCreated(stack, level, player);
            }

            @Override
            public boolean isFoil(ItemStack stack) {
                return behavior.isFoil(stack);
            }

            @Override
            public void appendHoverText(ItemStack stack, Level level,
                                        List<net.minecraft.network.chat.Component> tooltip,
                                        net.minecraft.world.item.TooltipFlag flag) {
                tooltip.addAll(behavior.tooltipLines(stack));
            }
        };
    }

    @Override
    public Block crop(BlockBehaviour.Properties properties, int maxAge) {
        return new dev.tc4port.thaumadditions.blocks.TaBlocks.CropBlock(properties, maxAge) {
            @Override
            public boolean isValidBonemealTarget(Level level, BlockPos pos, BlockState state, boolean isClient) {
                return false;
            }

            @Override
            public boolean isBonemealSuccess(Level level, net.minecraft.util.RandomSource random, BlockPos pos,
                                             BlockState state) {
                return false;
            }
        };
    }
'''


def neo_impl():
    return forge_impl().replace(
            'public void appendHoverText(ItemStack stack, Level level,',
            'public void appendHoverText(ItemStack stack, Item.TooltipContext context,')


def patch(path, old, new, extra_imports):
    p = ROOT / path
    s = p.read_text(encoding='utf-8')
    assert old in s, f'anchor missing in {path}'
    s = s.replace(old, new)
    for imp in extra_imports:
        if imp not in s:
            marker = 'import net.minecraft.world.entity.Entity;\n'
            assert marker in s, f'import anchor missing in {path}'
            s = s.replace(marker, marker + imp, 1)
    p.write_text(s, encoding='utf-8', newline='\n')
    print('patched', path)


IMPORTS = [
    'import net.minecraft.world.InteractionHand;\n',
    'import net.minecraft.world.InteractionResult;\n',
    'import net.minecraft.world.InteractionResultHolder;\n',
    'import net.minecraft.world.item.context.UseOnContext;\n',
    'import net.minecraft.world.entity.player.Player;\n',
]

# 1. shared interface
patch('src/main/java/dev/tc4port/thaumadditions/platform/TaPlatform.java',
      OLD_PLATFORM, NEW_PLATFORM,
      ['import net.minecraft.world.level.block.state.BlockBehaviour;\n'])

# 2. node implementations: replace the tooltipItem block (and the crop block) wholesale
for node, impl in (('versions/1.20.1-forge/src/main/java/dev/tc4port/thaumadditions/forge/TaPlatformForge.java', forge_impl()),
                   ('versions/1.21.1-neoforge/src/main/java/dev/tc4port/thaumadditions/neoforge/TaPlatformNeoForge.java', neo_impl())):
    p = ROOT / node
    s = p.read_text(encoding='utf-8')
    start = s.index('    @Override\n    public Item tooltipItem(')
    end = s.index('    @Override\n    public Block crop(')
    end2 = s.index('    @Override\n    public Item sword(')
    s = s[:start] + impl + '\n' + s[end2:]
    for imp in IMPORTS:
        if imp not in s:
            marker = 'import net.minecraft.world.entity.Entity;\n'
            s = s.replace(marker, marker + imp, 1)
    if 'import net.minecraft.world.level.block.state.BlockBehaviour;' not in s:
        marker = 'import net.minecraft.world.level.block.Block;\n'
        s = s.replace(marker, marker + 'import net.minecraft.world.level.block.state.BlockBehaviour;\n', 1)
    p.write_text(s, encoding='utf-8', newline='\n')
    print('patched', node)
