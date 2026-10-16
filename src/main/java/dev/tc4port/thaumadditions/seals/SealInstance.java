package dev.tc4port.thaumadditions.seals;

import dev.tc4port.thaumadditions.tiles.TileSeal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * 1.12 {@code SealInstance}: one seal's behaviour, built from the three symbols in the seal block.
 *
 * <p>Every seal works the same way — pick a spot in the box in front of the block, do something to
 * it, occasionally dirty the aura — so the base class carries the box, the tick cadence and the
 * save state, and each seal only writes what it does to the spot it picked.</p>
 */
public abstract class SealInstance {
    /** How far in front of the block the seal reaches, as 1.12 measured it. */
    public static final int REACH = 4;
    /** The box it searches, and how many times it tries to find something in it. */
    public static final int RADIUS = 5;

    protected final TileSeal seal;
    /** This seal's own tick count, which is what 1.12 measured its cadences against. */
    private long ticks;

    protected SealInstance(TileSeal seal) {
        this.seal = seal;
    }

    /** One tick, counted. The block entity calls this rather than {@link #tick()}. */
    public final void step() {
        ++ticks;
        tick();
    }

    /** The description key this seal shows, e.g. {@code seal.thaumadds:createsoil}. */
    public String descriptionKey() {
        return "";
    }

    /** Called every tick; each seal gates itself with {@link #atTickRate(int)}. */
    public void tick() {
    }

    /** 1.12 {@code onEntityCollisionWithSeal}; only the portal seal used it. */
    public void onEntityCollisionWithSeal(Level level, BlockPos pos, Entity entity) {
    }

    /** 1.12's {@code onSealActivated}: right-clicking the seal. Only the portal binds on it. */
    public boolean onSealActivated(Level level, BlockPos pos,
                                   net.minecraft.world.entity.player.Player player) {
        return false;
    }

    /** 1.12 {@code onSealBreak}, for a seal that had to clean up after itself. */
    public void onSealBreak() {
    }

    public void readFromNbt(CompoundTag tag) {
    }

    public CompoundTag writeToNbt(CompoundTag tag) {
        return tag;
    }

    // ------------------------------------------------------------------ shared plumbing

    /**
     * 1.12's {@code atTickRate}: true on every n-th tick of <em>this</em> seal's life.
     *
     * <p>Keyed on the seal's own count rather than the world clock, which is what upstream did and
     * what keeps two seals' cadences from marching in step with each other.</p>
     */
    protected boolean atTickRate(int rate) {
        return rate <= 1 || ticks % rate == 0;
    }

    /** The centre of the box the seal searches: four blocks out, or two when it points up. */
    protected BlockPos searchCentre() {
        Direction to = seal.orientation();
        int distance = to.getAxis().isVertical() ? (to == Direction.UP ? 2 : REACH) : REACH;
        return seal.getBlockPos().relative(to, distance);
    }

    /** A random position inside that box. */
    protected BlockPos randomSpot(Level level, int verticalSpread, int verticalOffset) {
        BlockPos centre = searchCentre();
        var random = level.getRandom();
        return centre.offset(random.nextInt(RADIUS) - random.nextInt(RADIUS),
                verticalOffset + random.nextInt(verticalSpread) - random.nextInt(verticalSpread),
                random.nextInt(RADIUS) - random.nextInt(RADIUS));
    }

    /** 1.12's five-percent chance to dirty the aura for a tenth of a unit. */
    protected void maybePollute(Level level) {
        if (level.getRandom().nextInt(100) < 5 && level instanceof net.minecraft.server.level.ServerLevel server) {
            dev.tc4port.thaumcraft.api.flux.FluxApi.add(server, seal.getBlockPos(), 1);
        }
    }

    /** 1.12's {@code getLocation().getRedstone()}: every seal stops when the block is powered. */
    protected boolean powered(Level level) {
        return level.getBestNeighborSignal(seal.getBlockPos()) > 0;
    }
}
