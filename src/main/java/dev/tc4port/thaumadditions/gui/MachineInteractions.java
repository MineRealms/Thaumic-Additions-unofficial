package dev.tc4port.thaumadditions.gui;

import dev.tc4port.thaumadditions.blocks.TaMachineBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Right-clicking a machine opens its GUI.
 *
 * <p>1.12 did this by overriding {@code Block#onBlockActivated}. 1.20.1 renamed that method and then
 * 1.21 renamed it again with a different signature, so the port does not override it at all — each
 * node hooks its own {@code PlayerInteractEvent.RightClickBlock} and calls in here. The event is
 * spelled the same way on both loaders, and the shared side stays free of block-method drift.</p>
 */
public final class MachineInteractions {
    private MachineInteractions() {
    }

    /**
     * Opens the machine's menu if the block at {@code pos} has one.
     *
     * @return whether a menu was opened, which is what the event listener cancels the interaction on.
     */
    public static boolean openMachine(Player player, Level level, BlockPos pos) {
        if (level.isClientSide) {
            // The seal's globe is a screen rather than a menu: its symbols are already on this
            // side with the block entity, so there is nothing for the server to send.
            if (level.getBlockState(pos).getBlock()
                    instanceof dev.tc4port.thaumadditions.blocks.TaMachineBlocks.SealBlock) {
                dev.tc4port.thaumadditions.client.gui.SealGlobeScreen.tryOpen(level, pos);
                return true;
            }
            return false;
        }
        if (!(player instanceof ServerPlayer server)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        // A seal binds on right-click — the portal walks its network — and its globe is the
        // client's half of the same click.
        if (state.getBlock() instanceof TaMachineBlocks.SealBlock
                && level.getBlockEntity(pos) instanceof dev.tc4port.thaumadditions.tiles.TileSeal seal) {
            var behaviour = seal.instance();
            return behaviour != null && behaviour.onSealActivated(level, pos, player);
        }
        if (!(state.getBlock() instanceof TaMachineBlocks.BeBlock machine)) {
            return false;
        }
        TaMachineBlocks.MachineGui gui = machine.gui();
        if (gui == null) {
            return false;
        }
        MachineMenuProvider.of(pos, Component.translatable(state.getBlock().getDescriptionId()),
                gui.builder()).open(server);
        return true;
    }
}
