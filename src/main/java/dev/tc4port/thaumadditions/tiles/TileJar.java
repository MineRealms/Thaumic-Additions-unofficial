package dev.tc4port.thaumadditions.tiles;

import dev.tc4port.thaumadditions.blocks.TaMachineBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.12 {@code TileAbstractJarFillable}: a single-aspect essentia container.
 *
 * <p>TC6's jar extended its own {@code TileJarFillable} with the whole phial /
 * label / brace interaction surface. This port keeps what the block state and the
 * essentia transport need — one aspect, a capacity — and leaves the phial/label
 * interactions for the container pass. Capacity comes from the block, because
 * 1.12 registered six jars that differed only in capacity.</p>
 */
public class TileJar extends TileEssentia {
    private final int capacity;

    public TileJar(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, capacityOf(state), 32);
        this.capacity = capacityOf(state);
    }

    private static int capacityOf(BlockState state) {
        return state.getBlock() instanceof TaMachineBlocks.JarBlock jar ? jar.capacity() : 275;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public int suctionAmount(Direction face) {
        // 1.12 abstract jars pulled harder from the block directly above.
        return face == Direction.UP ? 64 : 32;
    }
}
