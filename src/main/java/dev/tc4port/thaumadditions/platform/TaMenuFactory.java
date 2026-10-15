package dev.tc4port.thaumadditions.platform;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Builds a machine's menu on either side.
 *
 * <p>{@code extraData} is how the client learns which block it is looking at: the server writes a
 * position into it when the menu is opened and the client reads it back when the menu is built. Both
 * loaders have their own factory interface for that — this is the neutral shape, so the shared menu
 * classes never name one.</p>
 */
@FunctionalInterface
public interface TaMenuFactory<T extends AbstractContainerMenu> {
    T create(int windowId, Inventory inventory, FriendlyByteBuf extraData);
}
