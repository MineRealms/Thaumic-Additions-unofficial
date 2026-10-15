package dev.tc4port.thaumadditions.tiles;

import dev.tc4port.hclite.tile.SyncedBlockEntity;
import dev.tc4port.thaumadditions.items.ThaumcraftItems;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.aspect.VisChannel;
import dev.tc4port.thaumcraft.api.aspect.VisAction;
import dev.tc4port.thaumcraft.api.node.VisNetworkApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * 1.12 {@code TileCrystalBore}: face a crystal cluster, drink vis out of the ground, and after a
 * hundred ticks of boring hand a crystallised essence back to whatever is behind the machine.
 *
 * <p>Two upstream details do not survive the move to TC4R, and both are on the cluster's side
 * rather than the machine's:</p>
 *
 * <ul>
 *   <li>1.12's cluster carried a {@code SIZE} count and the bore removed one unit from it per
 *       cycle, refusing to touch a cluster below a configurable minimum. TC4R's cluster is a single
 *       indivisible block with no count at all — its block entity says so in as many words — so a
 *       cycle now harvests the whole cluster. The {@code minCrystals} floor has nothing left to
 *       measure and is gone with it.</li>
 *   <li>1.12 drained the chunk's aura without caring what the cluster was made of. TC4R's vis
 *       network is six channels wide, and a cluster's {@code mixed} and {@code eldritch} kinds map
 *       to none of them, so those two kinds cannot be bored.</li>
 * </ul>
 *
 * <p>The drill's spin is kept for the renderer: the angle is client-only, unpersisted, and speeds
 * up while the machine is working, exactly as 1.12's {@code FrictionRotator} did.</p>
 */
public class TileCrystalBore extends SyncedBlockEntity implements TaTickables.Ticking {
    private static final String KEY_MIN_CRYSTALS = "MinCrystals";

    /** 1.12 {@code drainVis(world, pos, .02F, false)}: two centivis a tick. */
    private static final int DRAIN_PER_TICK = 2;
    /** 1.12 {@code boring >= 100}: a hundred ticks of drilling per crystal. */
    private static final int TICKS_PER_CRYSTAL = 100;
    /** How fast the drill winds up while it is boring, and how fast it coasts back down. */
    private static final float SPIN_UP = 0.5F;
    private static final float SPIN_FRICTION = 0.25F;

    /** 1.12's {@code minCrystals}; kept for parity with existing saves even though nothing reads it. */
    private int minCrystals = 1;

    /** Ticks spent boring the cluster in front; resets on every harvest. */
    private int boring;

    /** Renderer state, client-only and unpersisted. */
    private float rotation;
    private float spin;

    /** Ticks waiting on the ground before this machine may draw vis again. */
    private int drainCooldown;

    public TileCrystalBore(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.minCrystals = getPersistentData().getInt(KEY_MIN_CRYSTALS);
        this.rotation = new java.util.Random().nextFloat() * 360.0F;
    }

    public int minCrystals() {
        return minCrystals;
    }

    public int boring() {
        return boring;
    }

    public float rotation() {
        return rotation;
    }

    /** The face the block was placed with, or north when it has none. */
    public Direction face() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockStateProperties.FACING)
                ? state.getValue(BlockStateProperties.FACING) : Direction.NORTH;
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        // The drill always turns; it just turns faster while it has something to eat.
        spin = Math.max(0.0F, spin - SPIN_FRICTION);
        rotation += spin;

        Direction ahead = face();

        if (level.isClientSide) {
            return;
        }

        if (!canBore(level, pos, ahead)) {
            boring = 0;
            return;
        }

        if (drainCooldown > 0) {
            --drainCooldown;
            return;
        }

        if (!drainVis(level, pos, ahead)) {
            // Nothing in the ground to drink: pause instead of retrying every tick.
            drainCooldown = 20;
            return;
        }

        spin = Math.min(spin + SPIN_UP, 24.0F);
        ++boring;

        if (boring >= TICKS_PER_CRYSTAL) {
            boring = 0;
            harvest(level, pos, ahead);
        }
    }

    /** 1.12 {@code canBore}: unpowered, and a cluster in front. */
    private boolean canBore(Level level, BlockPos pos, Direction ahead) {
        if (level.getBestNeighborSignal(pos) != 0) {
            return false;
        }
        return level.getBlockState(pos.relative(ahead)).getBlock()
                instanceof dev.tc4port.thaumcraft.block.CrystalClusterBlock;
    }

    /**
     * 1.12 {@code drainVis(world, pos, .02F, false)} on the channel the cluster's kind belongs to.
     */
    private boolean drainVis(Level level, BlockPos pos, Direction ahead) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        VisChannel channel = channelOf(level.getBlockState(pos.relative(ahead)));
        if (channel == null) {
            return false;
        }
        return VisNetworkApi.drain(server, pos, channel, DRAIN_PER_TICK, VisAction.EXECUTE) >= DRAIN_PER_TICK;
    }

    /**
     * 1.12's {@code bore()}: take a crystallised essence of the cluster's aspect, hand it to the
     * container behind the machine, and drop it on the floor when there is nowhere to put it.
     */
    private void harvest(Level level, BlockPos pos, Direction ahead) {
        BlockPos target = pos.relative(ahead);
        AspectId aspect = aspectOf(level.getBlockState(target));
        ItemStack essence = ThaumcraftItems.crystalEssence(aspect, 1);

        // The cluster is one indivisible block in TC4R, so the whole of it goes.
        level.destroyBlock(target, true);

        if (essence.isEmpty()) {
            return;
        }
        Direction behind = ahead.getOpposite();
        BlockEntity container = level.getBlockEntity(pos.relative(behind));
        if (container instanceof Container sink) {
            for (int slot = 0; slot < sink.getContainerSize(); slot++) {
                ItemStack existing = sink.getItem(slot);
                if (existing.isEmpty()) {
                    sink.setItem(slot, essence);
                    sink.setChanged();
                    return;
                }
                if (existing.is(essence.getItem()) && existing.getCount() < existing.getMaxStackSize()) {
                    existing.grow(1);
                    sink.setChanged();
                    return;
                }
            }
        }
        Block.popResource(level, pos, essence);
    }

    /** The cluster's own aspect, or {@code null} when it has none to give. */
    private static AspectId aspectOf(BlockState state) {
        if (state.getBlock() instanceof dev.tc4port.thaumcraft.block.CrystalClusterBlock cluster) {
            return aspectOfKind(cluster.kind());
        }
        return null;
    }

    /** The cluster's kind, as the aspect the bore's produce carries. */
    private static AspectId aspectOfKind(dev.tc4port.thaumcraft.block.CrystalClusterBlock.Kind kind) {
        return switch (kind) {
            case AIR -> AspectId.parse("aer");
            case FIRE -> AspectId.parse("ignis");
            case WATER -> AspectId.parse("aqua");
            case EARTH -> AspectId.parse("terra");
            case ORDER -> AspectId.parse("ordo");
            case ENTROPY -> AspectId.parse("perditio");
            case ELDRITCH -> AspectId.parse("alienis");
            // A mixed cluster carries no single aspect; the produce takes the vis channel's own.
            case MIXED -> null;
        };
    }

    /** The vis channel a cluster draws from; {@code null} when its kind has no primal. */
    private static VisChannel channelOf(BlockState state) {
        AspectId aspect = aspectOf(state);
        return aspect == null ? null : VisChannel.fromAspectId(aspect).orElse(null);
    }
}
