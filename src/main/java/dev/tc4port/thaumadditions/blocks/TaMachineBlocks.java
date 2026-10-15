package dev.tc4port.thaumadditions.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/**
 * The machine blocks, i.e. every block that carries a block entity.
 *
 * <p>Each one only wires {@code newBlockEntity} to a registered type; the
 * behaviour lives in {@code dev.tc4port.thaumadditions.tiles}.</p>
 */
public final class TaMachineBlocks {
    private TaMachineBlocks() {
    }

    /** A block that creates a block entity from a registered type. */
    public interface BeBlock extends EntityBlock {
        java.util.function.Supplier<? extends BlockEntityType<?>> type();

        /**
         * 1.20.1 asks the block which ticker its block entity uses. 1.21 moved that onto
         * {@code BlockEntityType.Builder#build}, which {@code TaPlatform.blockEntityType} supplies,
         * so this override may simply go unused there.
         */
        @Override
        default <T extends net.minecraft.world.level.block.entity.BlockEntity>
        net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
                net.minecraft.world.level.Level level, BlockState state, BlockEntityType<T> type) {
            return dev.tc4port.thaumadditions.tiles.TaTickables.ticker();
        }
    }

    /** Shared plumbing so each machine block only names its type. */
    public abstract static class AbstractBeBlock extends TaBlocks.HorizontalFacingBlock implements BeBlock {
        private final java.util.function.Supplier<? extends BlockEntityType<?>> type;

        protected AbstractBeBlock(Properties properties,
                                    java.util.function.Supplier<? extends BlockEntityType<?>> type) {
            super(properties);
            this.type = type;
        }

        @Override
        public java.util.function.Supplier<? extends BlockEntityType<?>> type() {
            return type;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return type.get().create(pos, state);
        }
    }

    /**
     * The six 1.12 jars. They were six {@code TileAbstractJarFillable} subclasses that differed only
     * in capacity, plus two rules: the mithminite jar spills anything past its cap into the aura,
     * and a nearly full adaminite jar of flux detonates.
     */
    public enum JarKind {
        BRASS(275),
        THAUMIUM(350),
        ELDRITCH(500),
        MITHRILLIUM(1000),
        ADAMINITE(2000),
        MITHMINITE(4000);

        private final int capacity;

        JarKind(int capacity) {
            this.capacity = capacity;
        }

        public int capacity() {
            return capacity;
        }

        /** 1.12 {@code TileMithminiteJar.addToContainer}. */
        public boolean spillsIntoAura() {
            return this == MITHMINITE;
        }

        /** 1.12 {@code TileAdaminiteJar.update}. */
        public boolean detonatesOnFlux() {
            return this == ADAMINITE;
        }
    }

    /** 1.12 {@code BlockAbstractEssentiaJar}: glass jar, 3..13/16 box, capacity per variant. */
    public static class JarBlock extends AbstractBeBlock {
        private static final VoxelShape SHAPE = box(3, 0, 3, 13, 14, 13);
        private final JarKind kind;

        public JarBlock(java.util.function.Supplier<? extends BlockEntityType<?>> type, JarKind kind) {
            super(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE).strength(0.3F).sound(SoundType.GLASS).noOcclusion(), type);
            this.kind = kind;
        }

        public JarKind kind() {
            return kind;
        }

        public int capacity() {
            return kind.capacity();
        }

        @Override
        public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
                                   net.minecraft.world.phys.shapes.CollisionContext context) {
            return SHAPE;
        }
    }

    /** 1.12 {@code BlockAbstractSmelter}: facing + enabled, per-variant efficiency/speed/capacity. */
    public static class SmelterBlock extends TaBlocks.EnableableBlock implements BeBlock {
        private final java.util.function.Supplier<? extends BlockEntityType<?>> type;
        private final float efficiency;
        private final int speed;
        private final int capacity;

        public SmelterBlock(java.util.function.Supplier<? extends BlockEntityType<?>> type, float efficiency,
                            int speed, int capacity) {
            super(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().lightLevel(state -> state.getValue(ENABLED) ? 13 : 0));
            this.type = type;
            this.efficiency = efficiency;
            this.speed = speed;
            this.capacity = capacity;
        }

        /** The 1.12 {@code BlockAbstractSmelter(efficiency, speed, capacity)} constructor args. */
        public float efficiency() {
            return efficiency;
        }

        public int speed() {
            return speed;
        }

        public int capacity() {
            return capacity;
        }

        @Override
        public java.util.function.Supplier<? extends BlockEntityType<?>> type() {
            return type;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return type.get().create(pos, state);
        }
    }

    /** 1.12 {@code BlockEssentiaSink}: facing, plus the aux block it keeps alive above itself. */
    public static class SinkBlock extends AbstractBeBlock {
        public SinkBlock(java.util.function.Supplier<? extends BlockEntityType<?>> type) {
            super(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE).strength(1.5F).sound(SoundType.STONE).noOcclusion(), type);
        }
    }

    /** 1.12 {@code BlockSeal}: 6-way facing, entity-block animated, no item. */
    public static class SealBlock extends TaBlocks.OrientableBlock implements BeBlock {
        private final java.util.function.Supplier<? extends BlockEntityType<?>> type;

        public SealBlock(java.util.function.Supplier<? extends BlockEntityType<?>> type) {
            super(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE).strength(1.0F).sound(SoundType.STONE).noOcclusion());
            this.type = type;
        }

        @Override
        public java.util.function.Supplier<? extends BlockEntityType<?>> type() {
            return type;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return type.get().create(pos, state);
        }
    }

    /** 1.12 {@code BlockCrystalBlock}: 6-way facing, one stored aspect, no item form. */
    public static class CrystalBlock extends TaBlocks.OrientableBlock implements BeBlock {
        private final java.util.function.Supplier<? extends BlockEntityType<?>> type;

        public CrystalBlock(java.util.function.Supplier<? extends BlockEntityType<?>> type) {
            super(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ).strength(1.0F).sound(SoundType.GLASS).noOcclusion());
            this.type = type;
        }

        @Override
        public java.util.function.Supplier<? extends BlockEntityType<?>> type() {
            return type;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return type.get().create(pos, state);
        }
    }

    /** 1.12 {@code BlockCrystalLamp}: facing + enabled, one stored aspect. */
    public static class CrystalLampBlock extends TaBlocks.EnableableBlock implements BeBlock {
        private final java.util.function.Supplier<? extends BlockEntityType<?>> type;

        public CrystalLampBlock(java.util.function.Supplier<? extends BlockEntityType<?>> type) {
            super(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.WOOD)
                    .lightLevel(state -> state.getValue(ENABLED) ? 12 : 0));
            this.type = type;
        }

        @Override
        public java.util.function.Supplier<? extends BlockEntityType<?>> type() {
            return type;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return type.get().create(pos, state);
        }
    }

    /** 1.12 {@code BlockEntitySummoner}: iron frame, no GUI, spawns from a stored entity cell. */
    public static class SummonerBlock extends AbstractBeBlock {
        public SummonerBlock(java.util.function.Supplier<? extends BlockEntityType<?>> type) {
            super(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.METAL).noOcclusion(), type);
        }
    }

    /** 1.12 {@code BlockShadowEnchanter}: low stone pedestal with an enchant GUI. */
    public static class EnchanterBlock extends TaBlocks.HorizontalFacingBlock implements BeBlock {
        private final java.util.function.Supplier<? extends BlockEntityType<?>> type;

        public EnchanterBlock(java.util.function.Supplier<? extends BlockEntityType<?>> type) {
            super(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE).strength(1.5F).sound(SoundType.STONE).noOcclusion());
            this.type = type;
        }

        @Override
        public java.util.function.Supplier<? extends BlockEntityType<?>> type() {
            return type;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return type.get().create(pos, state);
        }
    }

    /** 1.12 {@code BlockGrowthChamber}: two-slot converter, facing. */
    public static class GrowthChamberBlock extends AbstractBeBlock {
        public GrowthChamberBlock(java.util.function.Supplier<? extends BlockEntityType<?>> type) {
            super(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops(), type);
        }
    }

    /** The six remaining machines, all of which are plain 6-way-facing essentia endpoints. */
    public static class MachineBlock extends TaBlocks.OrientableBlock implements BeBlock {
        private final java.util.function.Supplier<? extends BlockEntityType<?>> type;

        public MachineBlock(java.util.function.Supplier<? extends BlockEntityType<?>> type) {
            super(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.METAL).noOcclusion());
            this.type = type;
        }

        @Override
        public java.util.function.Supplier<? extends BlockEntityType<?>> type() {
            return type;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return type.get().create(pos, state);
        }
    }

    /** 1.12 {@code BlockEssentiaSinkAux}: invisible structural block, no item, no state. */
    public static class SinkAuxBlock extends net.minecraft.world.level.block.Block {
        public SinkAuxBlock() {
            super(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE).strength(-1.0F, 3600000.0F).noLootTable().noOcclusion());
        }
    }

}
