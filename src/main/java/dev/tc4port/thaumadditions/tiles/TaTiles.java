package dev.tc4port.thaumadditions.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The remaining essentia machines, in one file because they share a shape.
 *
 * <p>Each keeps its 1.12 suction, capacity and open faces; the TC-coupled halves
 * (flux conversions, aura injection, entity-tag lookups, alembic pumping) are
 * called out per class and left to the machine-behaviour pass.</p>
 */
public final class TaTiles {
    private TaTiles() {
    }

    /**
     * 1.12 {@code TileAuraCharger}: pulls AURA from below and injects vis into the chunk.
     *
     * <p>The intake is here and works; the injection is the one thing in the mod that TC4R has no
     * entry point for. 1.12 called {@code AuraHandler.addVis} to put ten vis into the chunk every
     * hundred ticks, and TC4R's vis network only publishes {@code drain} — nothing adds to a network
     * that is built out of nodes and relays. Until that changes this machine banks its essentia and
     * spins, which at least tells the player it is charged.</p>
     */
    public static class Charger extends TileEssentia {
        /** 1.12 {@code getSuctionAmount(face)}: 128 from below, nowhere else. */
        private static final int SUCTION = 128;

        /** The sprinkler arms' spin, for the renderer. */
        private final TaRotator rotator = TaRotator.randomised(new java.util.Random());

        public Charger(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state, 10, SUCTION);
        }

        /** The rotor; the renderer turns it about Y. */
        public TaRotator rotator() {
            return rotator;
        }

        @Override
        protected boolean accepts(Direction face) {
            return face == Direction.DOWN;
        }

        @Override
        protected boolean emits(Direction face) {
            return false;
        }

        @Override
        protected void machineTick(Level level, BlockPos pos, BlockState state) {
            rotator.update();
            // 1.12 only turned the arms while there was charge to spend, and stopped them outright
            // under redstone.
            if (getAmount() > 0 && level.getBestNeighborSignal(pos) == 0) {
                rotator.speedup(0.2F);
            }
        }
    }

}
