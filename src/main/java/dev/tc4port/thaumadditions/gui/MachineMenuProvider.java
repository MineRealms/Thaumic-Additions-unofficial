package dev.tc4port.thaumadditions.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;


/**
 * Opens a machine's menu and tells the client which block it belongs to.
 *
 * <p>1.12's containers were built straight from the tile entity on both sides, so neither side had
 * to be told anything. A 1.20.1 menu is built from a window id and the player's inventory alone, so
 * the block's position rides along in the opening data — vanilla's two-argument
 * {@code openMenu} carries it, which is why the menus have a buffer-taking constructor.</p>
 */
public final class MachineMenuProvider implements MenuProvider {
    private final BlockPos pos;
    private final Component title;
    /** Server-side menu builder: window id, player inventory, and the machine's position. */
    private final ServerMenuFactory builder;

    private MachineMenuProvider(BlockPos pos, Component title, ServerMenuFactory builder) {
        this.pos = pos;
        this.title = title;
        this.builder = builder;
    }

    /** Builds a provider for one machine. */
    public static MachineMenuProvider of(BlockPos pos, Component title, ServerMenuFactory builder) {
        return new MachineMenuProvider(pos, title, builder);
    }

    @Override
    public Component getDisplayName() {
        return title;
    }

    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return builder.create(windowId, inventory, pos);
    }

    /**
     * Opens the menu, writing the position into the opening data the client's factory reads back.
     *
     * <p>Vanilla's two-argument {@code openMenu} is the loader-neutral way to do this; each loader
     * also has its own extra-data interface, and they disagree on the method's name.</p>
     */
    public void open(ServerPlayer player) {
        dev.tc4port.thaumadditions.platform.TaPlatform.Active.get()
                .openMenu(player, this, buf -> buf.writeBlockPos(pos));
    }

    /** A machine's server-side menu constructor, without a window id. */
    @FunctionalInterface
    public interface ServerMenuFactory {
        AbstractContainerMenu create(int windowId, Inventory inventory, BlockPos pos);
    }
}
