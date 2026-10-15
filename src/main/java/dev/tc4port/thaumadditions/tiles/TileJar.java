package dev.tc4port.thaumadditions.tiles;

import dev.tc4port.thaumadditions.blocks.TaMachineBlocks;
import dev.tc4port.thaumadditions.platform.TaPlatform;
import dev.tc4port.thaumadditions.platform.TaSound;
import dev.tc4port.thaumcraft.api.aspect.AspectApi;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.essentia.EssentiaApi;
import dev.tc4port.thaumcraft.api.essentia.EssentiaTransferMode;
import dev.tc4port.thaumcraft.api.essentia.EssentiaTransport;
import dev.tc4port.thaumcraft.api.flux.FluxApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.12 {@code TileAbstractJarFillable} and its six variants: a single-aspect essentia container.
 *
 * <p>TC6's jar extended its own {@code TileJarFillable} with the whole phial / label / brace
 * interaction surface. This port keeps what the block state, the essentia transport and the two
 * variant rules need. The aspect label is not ported yet, so every jar behaves as an unlabelled
 * one: suction 32 while it has room, and it accepts whatever aspect reaches it first.</p>
 */
public class TileJar extends TileEssentia {
    /** 1.12 {@code TileAbstractJarFillable.absCount}; counts ticks, not persisted there either. */
    private int fillTick;

    /** 1.12 {@code TileAdaminiteJar.time}; also unpersisted upstream, so a reload resets the fuse. */
    private int detonationTick;

    /** 1.12 {@code TileMithminiteJar} spills past this, below its own 4005 capacity. */
    private static final int SPILL_THRESHOLD = 4000;

    /** 1.12 {@code TileAdaminiteJar}: a jar at or above this fraction of capacity can blow. */
    private static final float DETONATION_FILL = 0.9F;

    private final TaMachineBlocks.JarKind kind;

    public TileJar(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, kindOf(state).capacity(), 32);
        this.kind = kindOf(state);
    }

    private static TaMachineBlocks.JarKind kindOf(BlockState state) {
        return state.getBlock() instanceof TaMachineBlocks.JarBlock jar
                ? jar.kind()
                : TaMachineBlocks.JarKind.BRASS;
    }

    @Override
    public int getCapacity() {
        return kind.capacity();
    }

    /**
     * 1.12 {@code TileAbstractJarFillable.getSuctionAmount}: nothing pulls on a full jar, and an
     * unlabelled jar pulls 32 on every face (the label would raise it to 64).
     */
    @Override
    public int suctionAmount(Direction face) {
        return getAmount() >= getCapacity() ? 0 : 32;
    }

    @Override
    public int minimumSuction() {
        return 32;
    }

    // ------------------------------------------------------------------ ticking

    @Override
    protected void machineTick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) {
            return;
        }
        // 1.12 TileAbstractJarFillable.update: one unit every five ticks, while there is room.
        if (++fillTick % 5 == 0 && getAmount() < getCapacity()) {
            fillFromAbove(level, pos);
        }
        if (kind.detonatesOnFlux()) {
            checkDetonation(level, pos);
        }
    }

    /**
     * 1.12 {@code fillAbstractJar}: pull one unit from the connectable tile above, which has to
     * accept the pull (we must out-suck it) and be able to output downwards.
     */
    private void fillFromAbove(Level level, BlockPos pos) {
        EssentiaTransport above = EssentiaApi.transport(level, pos.above(), Direction.DOWN);
        if (above == null || !above.canOutputTo(Direction.DOWN)) {
            return;
        }
        int theirSuction = above.suctionAmount(Direction.DOWN);
        int mine = suctionAmount(Direction.UP);
        if (theirSuction >= mine) {
            return;
        }
        // Inherit the jar's own aspect; otherwise take whatever the neighbour is offering, but only
        // if it has some and we out-suck it by enough to beat its own minimum.
        AspectId wanted = getAspect();
        if (wanted == null || getAmount() == 0) {
            if (above.essentiaAmount(Direction.DOWN) <= 0 || mine < above.minimumSuction()) {
                return;
            }
            wanted = above.essentiaType(Direction.DOWN);
        }
        if (wanted == null) {
            return;
        }
        int taken = EssentiaApi.take(level, above, wanted, 1, Direction.DOWN, EssentiaTransferMode.EXECUTE);
        if (taken > 0) {
            insert(wanted, taken);
        }
    }

    /**
     * 1.12 {@code TileMithminiteJar.addToContainer}: the jar voids whatever it cannot hold, and
     * the more it is overfilled the likelier it is to pollute the aura while doing so. Upstream
     * computed {@code rand.nextInt(250 - overfill)}, which throws once the overfill reaches 250;
     * the bound is clamped here so a large insertion pollutes instead of crashing.
     */
    @Override
    public int insert(AspectId id, int value) {
        if (!kind.spillsIntoAura() || id == null || value <= 0) {
            return super.insert(id, value);
        }
        AspectId current = getAspect();
        if (current != null && !current.equals(id)) {
            return 0;
        }
        int stored = Math.min(value, Math.max(0, SPILL_THRESHOLD - getAmount()));
        if (stored > 0) {
            super.insert(id, stored);
        }
        int overfill = value - stored;
        Level level = getLevel();
        if (overfill > 0 && level instanceof ServerLevel server) {
            if (level.random.nextInt(Math.max(1, 250 - overfill)) == 0) {
                FluxApi.add(server, getBlockPos(), 1);
            }
        }
        // 1.12 consumed the excess rather than handing it back, so the caller sees it all taken.
        return value;
    }

    /**
     * 1.12 {@code TileAdaminiteJar.update}: a nearly full jar of flux has a one-in-two chance to
     * go off every thousand ticks. The detonation drops the jar, removes the block and pushes the
     * stored essentia into the aura.
     */
    private void checkDetonation(Level level, BlockPos pos) {
        if (++detonationTick % 1000 != 0 || !level.random.nextBoolean()) {
            return;
        }
        AspectId flux = fluxAspect();
        if (flux == null || !flux.equals(getAspect()) || getAmount() < getCapacity() * DETONATION_FILL) {
            return;
        }
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        float pitch = (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F;
        TaPlatform.Active.get().playSound(level, pos, TaSound.EXPLODE, 4.0F, pitch);
        server.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                1, 1.0, 0.0, 0.0, 0.0);
        int released = getAmount();
        Block.popResource(level, pos, new ItemStack(getBlockState().getBlock()));
        level.destroyBlock(pos, false);
        FluxApi.add(server, pos, released);
    }

    /** The flux aspect, or {@code null} when this build's TC4R does not define one. */
    private static AspectId fluxAspect() {
        try {
            AspectId flux = AspectId.parse("vitium");
            return AspectApi.contains(flux) ? flux : null;
        } catch (RuntimeException unknown) {
            return null;
        }
    }
}
