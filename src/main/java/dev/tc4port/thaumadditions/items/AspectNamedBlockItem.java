package dev.tc4port.thaumadditions.items;

import dev.tc4port.thaumadditions.aspect.TaAspect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * A block whose item carries the aspect of the specific block being held.
 *
 * <p>Only the crystal block is one: nine crystals of one aspect pressed into a block, so the block
 * in hand is a crystal block *of that aspect* and says so. There is no viewer-facing method on
 * {@link BlockItem} that drifted between the two targets, so this class is shared — the aspect
 * substitution it delegates to is the same one the carried items use.</p>
 *
 * <p>Upstream built one recipe per aspect, each producing a block that already carried it; that
 * loop is not ported (see {@code PORT_STATE.md}), so a crafted block arrives without one and picks
 * its own on creation, exactly as the carried items do.</p>
 */
public final class AspectNamedBlockItem extends BlockItem {
    public AspectNamedBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        if (AspectItems.aspectOf(stack) == null) {
            AspectItems.setAspect(stack, TaAspect.random(level.getRandom()));
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        return AspectNames.substitute(super.getName(stack), stack);
    }
}
