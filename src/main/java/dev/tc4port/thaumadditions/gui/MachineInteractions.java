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
        if (!(player instanceof ServerPlayer server)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
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
