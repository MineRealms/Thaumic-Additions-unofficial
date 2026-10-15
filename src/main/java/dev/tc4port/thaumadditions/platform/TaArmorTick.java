package dev.tc4port.thaumadditions.platform;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Per-piece behaviour run while the armor piece is actually worn.
 *
 * <p>Modern Minecraft has no {@code onArmorTick}; the platform's armor item
 * forwards {@code inventoryTick} here once it has confirmed the stack sits in
 * its own equipment slot.</p>
 */
@FunctionalInterface
public interface TaArmorTick {
    void onWornTick(ItemStack stack, Level level, Player wearer, ArmorItem.Type type);
}
