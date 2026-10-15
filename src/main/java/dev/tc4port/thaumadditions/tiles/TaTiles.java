package dev.tc4port.thaumadditions.tiles;

import net.minecraft.core.BlockPos;
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

    /** 1.12 {@code TileAuraCharger}: pulls AURA from below, injects vis into the chunk. */
    public static class Charger extends TileEssentia {
        public Charger(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state, 10, 128);
        }

        @Override
        protected boolean accepts(Direction face) {
            return face == Direction.DOWN;
        }

        @Override
        protected boolean emits(Direction face) {
            return false;
        }
    }

    /** 1.12 {@code TileAuraDisperser}: nine salt slots, 20s per bag, area effect. */
    public static class Disperser extends TileEssentia {
        public Disperser(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state, 64, 1);
        }

        @Override
        protected boolean accepts(Direction face) {
            return false;
        }

        @Override
        protected boolean emits(Direction face) {
            return false;
        }
    }

    /** 1.12 {@code TileCrystalBore}: drains vis while facing a crystal block, 100t per bore. */
    public static class Bore extends TileEssentia {
        public Bore(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state, 64, 1);
        }

        @Override
        protected boolean accepts(Direction face) {
            return false;
        }

        @Override
        protected boolean emits(Direction face) {
            return false;
        }
    }

    /** 1.12 {@code TileFluxConcentrator}: pulls FLUX from behind, converts blocks in a flood fill. */
    public static class Concentrator extends TileEssentia {
        public Concentrator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state, 10, 128);
        }

        @Override
        protected boolean accepts(Direction face) {
            return face == Direction.NORTH || face == Direction.SOUTH
                    || face == Direction.EAST || face == Direction.WEST;
        }

        @Override
        protected boolean emits(Direction face) {
            return false;
        }
    }

    /** 1.12 {@code TileEssentiaSink}: pulls ELDRITCH from the face left of its facing. */
    public static class Sink extends TileEssentia {
        public Sink(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state, 128, 120);
        }

        @Override
        protected boolean accepts(Direction face) {
            return face == Direction.NORTH || face == Direction.SOUTH
                    || face == Direction.EAST || face == Direction.WEST;
        }

        @Override
        protected boolean emits(Direction face) {
            return false;
        }
    }

    /** 1.12 {@code TileEntitySummoner}: accumulates entity aspects, spawns 1-4 mobs per cycle. */
    public static class Summoner extends TileEssentia {
        public Summoner(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state, 64, 8);
        }

        @Override
        protected boolean accepts(Direction face) {
            return true;
        }

        @Override
        protected boolean emits(Direction face) {
            return false;
        }
    }

    /** 1.12 {@code TileShadowEnchanter}: spends essentia to write one whitelisted enchantment. */
    public static class Enchanter extends TileEssentia {
        public Enchanter(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state, 32, 12);
        }

        @Override
        protected boolean accepts(Direction face) {
            return true;
        }

        @Override
        protected boolean emits(Direction face) {
            return false;
        }
    }

}
