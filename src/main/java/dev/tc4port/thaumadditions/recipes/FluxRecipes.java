package dev.tc4port.thaumadditions.recipes;

import dev.tc4port.thaumadditions.registry.Content;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * What the flux concentrator turns into what.
 *
 * <p>1.12 kept this table in {@code RecipesFluxConcentrator}, registered by the mod's own setup,
 * and the machine and the JEI plugin both read it. The port does the same, with the table here so
 * that neither the block entity nor the plugin owns it.</p>
 *
 * <p>1.12's third rule — any log becomes TC's taint log, with the axis carried over — has no
 * counterpart: TC4R ships no taint log. The other two rules are unchanged.</p>
 */
public final class FluxRecipes {
    /** One rule: the block that goes in, and the block that comes out. */
    public record Entry(ItemStack input, ItemStack output) {
    }

    private FluxRecipes() {
    }

    /** The block a covered position turns into, or {@code null} when the field leaves it alone. */
    public static BlockState outputFor(BlockState state) {
        if (state.is(Blocks.BROWN_MUSHROOM) || state.is(Blocks.RED_MUSHROOM)) {
            return stateOf("thaumcraft:vishroom");
        }
        if (state.is(Blocks.PUMPKIN)) {
            return Content.TAINTKIN.get().defaultBlockState();
        }
        if (state.is(Blocks.JACK_O_LANTERN)) {
            return Content.TAINTKIN_LIT.get().defaultBlockState();
        }
        return null;
    }

    /** Whether the field treats this position as a recipe input, and so flows through it. */
    public static boolean isInput(BlockState state) {
        return outputFor(state) != null;
    }

    /** The table as items, for JEI and for anything else that wants to show it. */
    public static List<Entry> entries() {
        List<Entry> entries = new ArrayList<>();
        for (Block input : List.of(Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM,
                Blocks.PUMPKIN, Blocks.JACK_O_LANTERN)) {
            BlockState out = outputFor(input.defaultBlockState());
            if (out == null || out.getBlock().asItem() == net.minecraft.world.item.Items.AIR) {
                continue;
            }
            entries.add(new Entry(new ItemStack(input.asItem()),
                    new ItemStack(out.getBlock().asItem())));
        }
        return entries;
    }

    private static BlockState stateOf(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        Block block = key == null ? null : BuiltInRegistries.BLOCK.get(key);
        return block == null || block == Blocks.AIR ? null : block.defaultBlockState();
    }
}
