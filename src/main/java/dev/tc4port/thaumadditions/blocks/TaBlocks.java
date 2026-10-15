package dev.tc4port.thaumadditions.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The block shapes ThaumicAdditions needs beyond vanilla.
 *
 * <p>1.12 got these from HammerCore's {@code IBlockHorizontal} /
 * {@code IBlockEnableable} / {@code IBlockOrientable} interfaces; the blockstate
 * files the converter emitted keep the same property names ({@code facing},
 * {@code enabled}), so the properties here must match them exactly.</p>
 */
public final class TaBlocks {
    private TaBlocks() {
    }

    /** HammerCore {@code IBlockHorizontal}: 4-way facing, as used by every smelter and sink. */
    public static class HorizontalFacingBlock extends Block {
        public HorizontalFacingBlock(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(BlockStateProperties.HORIZONTAL_FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,
                    context.getHorizontalDirection().getOpposite());
        }

        @Override
        public BlockState rotate(BlockState state, Rotation rotation) {
            return state.setValue(BlockStateProperties.HORIZONTAL_FACING,
                    rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
        }

        @Override
        public BlockState mirror(BlockState state, Mirror mirror) {
            return state.setValue(BlockStateProperties.HORIZONTAL_FACING,
                    mirror.mirror(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
        }
    }

    /** HammerCore {@code IBlockOrientable}: 6-way facing, as used by the crystal machines. */
    public static class OrientableBlock extends Block {
        public OrientableBlock(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(BlockStateProperties.FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return context.getPlayer() == null
                    ? defaultBlockState()
                    : defaultBlockState().setValue(BlockStateProperties.FACING,
                            Direction.orderedByNearest(context.getPlayer())[0].getOpposite());
        }

        @Override
        public BlockState rotate(BlockState state, Rotation rotation) {
            return state.setValue(BlockStateProperties.FACING, rotation.rotate(state.getValue(BlockStateProperties.FACING)));
        }

        @Override
        public BlockState mirror(BlockState state, Mirror mirror) {
            return state.setValue(BlockStateProperties.FACING, mirror.mirror(state.getValue(BlockStateProperties.FACING)));
        }
    }

    /** {@code facing} + {@code enabled}: the smelters and the crystal lamp. */
    public static class EnableableBlock extends HorizontalFacingBlock {
        public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;

        public EnableableBlock(Properties properties) {
            super(properties);
            registerDefaultState(defaultBlockState().setValue(ENABLED, Boolean.FALSE));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(ENABLED);
        }
    }

    /**
     * A crop whose age property is bounded by the caller, with bonemeal disabled —
     * both 1.12 crops refused vanilla bonemeal so only the elemental hoe and seals
     * could advance them.
     */
    public static class CropBlock extends net.minecraft.world.level.block.CropBlock {
        /**
         * The vanilla {@code CropBlock} constructor calls {@link #getAgeProperty()} and
         * {@code createBlockStateDefinition} before any field of this subclass is assigned, so the
         * age property cannot live in an instance field while the super constructor runs.
         * {@link #installAge} stashes it in a static that the constructor argument evaluates, and
         * the constructor body then freezes it into the instance field. Block registration is
         * single-threaded, so one static is enough.
         */
        private static IntegerProperty pendingAge;

        private final IntegerProperty age;
        private final int maxAge;

        public CropBlock(Properties properties, int maxAge) {
            super(installAge(properties, maxAge));
            this.age = pendingAge;
            this.maxAge = maxAge;
        }

        private static Properties installAge(Properties properties, int maxAge) {
            pendingAge = IntegerProperty.create("age", 0, maxAge);
            return properties;
        }

        @Override
        public IntegerProperty getAgeProperty() {
            return age != null ? age : pendingAge;
        }

        @Override
        public int getMaxAge() {
            return maxAge;
        }

        /** Replaces vanilla's {@code AGE_7}: the 1.12 crops stop at their own bound. */
        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(getAgeProperty());
        }
    }

    /**
     * 1.12 {@code BlockArcaneCake}: six bites, self-restoring, and it stores its
     * eaten aspects in a block entity instead of dropping them.
     */
    public static class ArcaneCakeBlock extends net.minecraft.world.level.block.CakeBlock
            implements net.minecraft.world.level.block.EntityBlock {
        private final java.util.function.Supplier<? extends net.minecraft.world.level.block.entity.BlockEntityType<?>> type;

        public ArcaneCakeBlock(Properties properties,
                               java.util.function.Supplier<? extends net.minecraft.world.level.block.entity.BlockEntityType<?>> type) {
            super(properties);
            this.type = type;
        }

        /** The engine only creates block entities through {@code EntityBlock}, not by naming. */
        @Override
        public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return type.get().create(pos, state);
        }
    }

    /**
     * 1.12 {@code BlockPuriflower}: drains flux from the chunk it grows in. TC4R
     * exposes no chunk-aura API, so this port keeps the growth cadence and the
     * light, and logs the aura interaction as pending rather than faking it.
     */
    public abstract static class PuriflowerBlock extends net.minecraft.world.level.block.BushBlock {
        protected PuriflowerBlock(Properties properties) {
            super(properties);
        }
    }

    /** 1.12 {@code BlockTotem}: dawn drains, twilight adds; both are aura-only, so both are inert here. */
    public static class TotemBlock extends Block {
        public TotemBlock(Properties properties) {
            super(properties);
        }
    }
}
