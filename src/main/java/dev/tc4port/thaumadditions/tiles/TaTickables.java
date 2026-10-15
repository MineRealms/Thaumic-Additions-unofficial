package dev.tc4port.thaumadditions.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server tick plumbing for the machines.
 *
 * <p>The 1.12 tiles all ran their work from an {@code update()} that HammerCore called every tick;
 * 1.20.1 only ticks a block entity type that was built with a ticker, and every type here was
 * registered with {@code build(null)}. This is the single ticker they share.</p>
 */
public final class TaTickables {
    private TaTickables() {
    }

    /** A block entity with per-tick work. */
    public interface Ticking {
        void tick(Level level, BlockPos pos, BlockState state);
    }

    /**
     * The ticker handed to every {@code BlockEntityType.Builder.build}. {@code BlockEntityTicker}
     * keeps the same shape on both loaders, so this stays in the shared source.
     */
    public static <T extends BlockEntity> BlockEntityTicker<T> ticker() {
        return (level, pos, state, blockEntity) -> {
            if (blockEntity instanceof Ticking ticking) {
                ticking.tick(level, pos, state);
            }
        };
    }
}
