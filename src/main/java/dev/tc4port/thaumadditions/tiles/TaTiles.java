package dev.tc4port.thaumadditions.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
     * 1.12 {@code TileAuraCharger}: pulls AURA from below and puts vis back into the world.
     *
     * <p>1.12 added ten vis to the <em>chunk</em> every hundred ticks, because in that Thaumcraft
     * the aura was a pool that belonged to a chunk. TC4R has no such pool: its vis lives in aura
     * nodes, which are blocks. So the port charges a node instead — the nearest one within eight
     * blocks — through {@code NodeApi.replaceLoadedState}, which is the compare-and-set the modern
     * Thaumcraft publishes for exactly this. Nothing in TC4R itself calls it; it exists for addons,
     * and this machine is the addon it was written for.</p>
     *
     * <p>One thing had to be chosen rather than translated. A node's vis is kept per aspect, but the
     * network only ever hands out the six primal <em>channels</em>, so adding 1.12's raw aura to a
     * node would sit there and never be drawn. The charger therefore tops up whichever primal
     * channel the node is emptiest in, which is the closest reading of "put vis back" that the
     * network can actually use.</p>
     */
    public static class Charger extends TileEssentia {
        /** 1.12 {@code getSuctionAmount(face)}: 128 from below, nowhere else. */
        private static final int SUCTION = 128;
        /** 1.12 added ten vis for each point of aura, every hundred ticks. */
        private static final int VIS_PER_CHARGE = 10;
        private static final int CHARGE_INTERVAL = 100;
        /** How far the machine looks for a node to charge. */
        private static final int NODE_RANGE = 8;
        /** 1.12's {@code int rays = 2}, and how finely the trail between the ends is drawn. */
        private static final int SPARK_RAYS = 2;
        private static final int SPARK_STEPS = 12;

        /** The sprinkler arms' spin, for the renderer. */
        private final TaRotator rotator = TaRotator.randomised(new java.util.Random());
        /** Counts to the hundred-tick charging step. */
        private int chargeTicker;

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
            boolean working = getAmount() > 0 && level.getBestNeighborSignal(pos) == 0;
            if (working) {
                rotator.speedup(0.2F);
            }
            if (!working || level.isClientSide || !(level instanceof ServerLevel server)) {
                return;
            }
            if (++chargeTicker < CHARGE_INTERVAL) {
                return;
            }
            chargeTicker = 0;
            if (chargeNearestNode(server, pos)) {
                extract(getAspect(), 1);
                chargeSparks(server, pos);
            }
        }

        /**
         * The sprinkle of vis the machine throws when it charges something.
         *
         * <p>1.12 fired this from a client event and spawned Thaumcraft's own vis sparkle particle:
         * two rays, each starting a third of a block out from the centre at the rotor's current
         * angle and flying outwards and upwards, in the colour of the aura aspect. The port sends
         * dust in the same colour, as a trail rather than a single sprite — the same picture
         * without a particle class, and the trail is what makes a ray read as a line.</p>
         *
         * <p>What is <em>not</em> here is the lightning 1.12 also wrote for this machine: its
         * calls to {@code ThunderRunnable} sit inside a comment in the 1.12 source, so the arcs
         * never ran there either and the port does not invent them back.</p>
         */
        private void chargeSparks(ServerLevel server, BlockPos pos) {
            dev.tc4port.thaumcraft.api.aspect.AspectId aura =
                    dev.tc4port.thaumcraft.api.aspect.AspectId.parse("auram");
            int colour = dev.tc4port.thaumadditions.aspect.TaAspect.color(aura);
            java.util.Random random = new java.util.Random(server.getRandom().nextLong());
            for (int ray = 0; ray < SPARK_RAYS; ray++) {
                float degrees = rotator.actualRotation(1.0F) - 5.0F + ray * (360.0F / SPARK_RAYS);
                double radians = Math.toRadians(degrees);
                double cos = Math.cos(radians);
                double sin = Math.sin(radians);
                double x = pos.getX() + 0.5D + cos * 0.3D;
                double y = pos.getY() + 0.5D;
                double z = pos.getZ() + 0.5D + sin * 0.3D;
                double farX = pos.getX() + 0.5D + cos * random.nextFloat() * 6.0D;
                double farY = pos.getY() + 1.5D + random.nextFloat() * 4.0D;
                double farZ = pos.getZ() + 0.5D + sin * random.nextFloat() * 6.0D;
                for (int step = 0; step <= SPARK_STEPS; step++) {
                    double t = step / (double) SPARK_STEPS;
                    dev.tc4port.thaumadditions.TaFx.dust(server,
                            x + (farX - x) * t, y + (farY - y) * t, z + (farZ - z) * t,
                            colour, 0.0D, 0.0D, 0.0D);
                }
            }
        }

        /** Adds one charge to the closest node in reach, returning whether one took it. */
        private boolean chargeNearestNode(ServerLevel server, BlockPos pos) {
            BlockPos nearest = null;
            double best = Double.MAX_VALUE;
            for (BlockPos candidate : BlockPos.betweenClosed(
                    pos.offset(-NODE_RANGE, -NODE_RANGE, -NODE_RANGE),
                    pos.offset(NODE_RANGE, NODE_RANGE, NODE_RANGE))) {
                if (!(server.getBlockEntity(candidate)
                        instanceof dev.tc4port.thaumcraft.api.node.AuraNodeView)) {
                    continue;
                }
                double distance = candidate.distSqr(pos);
                if (distance < best) {
                    best = distance;
                    nearest = candidate.immutable();
                }
            }
            return nearest != null && charge(server, nearest);
        }

        /** The compare-and-set one node wants: read it, add, put it back if nobody moved first. */
        private boolean charge(ServerLevel server, BlockPos at) {
            if (!(server.getBlockEntity(at)
                    instanceof dev.tc4port.thaumcraft.api.node.AuraNodeView view)) {
                return false;
            }
            var state = view.nodeState();
            var vis = new java.util.LinkedHashMap<>(
                    state.currentVis().amounts());

            // The emptiest channel is the one to fill; see the class comment for why a channel.
            dev.tc4port.thaumcraft.api.aspect.VisChannel emptiest = null;
            int least = Integer.MAX_VALUE;
            for (var channel : dev.tc4port.thaumcraft.api.aspect.VisChannel.values()) {
                int held = vis.getOrDefault(channel.aspectId(), 0);
                if (held < least) {
                    least = held;
                    emptiest = channel;
                }
            }
            if (emptiest == null) {
                return false;
            }
            vis.merge(emptiest.aspectId(), VIS_PER_CHARGE, Integer::sum);

            var replacement = new dev.tc4port.thaumcraft.api.node.AuraNodeState(
                    state.instanceId(), state.type(), state.modifier(), state.baseVis(),
                    new dev.tc4port.thaumcraft.api.node.NodeVis(vis));
            return dev.tc4port.thaumcraft.api.node.NodeApi
                    .replaceLoadedState(server, at, state, replacement,
                            dev.tc4port.thaumcraft.api.aspect.VisAction.EXECUTE)
                    .successful();
        }
    }

}
