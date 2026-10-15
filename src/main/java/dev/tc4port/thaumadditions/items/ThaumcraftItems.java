package dev.tc4port.thaumadditions.items;

import dev.tc4port.thaumcraft.api.aspect.AspectId;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Thaumcraft items the port hands out but does not own.
 *
 * <p>1.12 built these straight from {@code ItemsTC}, and its own {@code AspectUtil} did the same.
 * TC4R registers them under the same ids, so the port resolves them through the item registry and
 * uses TC4R's own factory where one exists — {@code CrystalEssenceItem.forAspect} is the modern
 * {@code ItemCrystalEssence#setAspects}.</p>
 */
public final class ThaumcraftItems {
    private ThaumcraftItems() {
    }

    /** {@code thaumcraft:crystal_essence}, or air when this TC4R build does not register it. */
    public static Item crystalEssence() {
        return BuiltInRegistries.ITEM
                .getOptional(ResourceLocation.tryParse("thaumcraft:crystal_essence"))
                .orElse(Items.AIR);
    }

    /**
     * Crystallised essence of one aspect, as 1.12's {@code AspectUtil.crystalEssence(aspect, count)}
     * produced. Empty when TC4R has no crystal essence item to fill.
     */
    public static ItemStack crystalEssence(AspectId aspect, int count) {
        Item item = crystalEssence();
        if (item == Items.AIR || aspect == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = dev.tc4port.thaumcraft.item.CrystalEssenceItem.forAspect(item, aspect);
        stack.setCount(count);
        return stack;
    }
}
