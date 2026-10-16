package dev.tc4port.thaumadditions.blocks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * 1.12 {@code BlockCrystalWater}: the fluid's block in the world, and the one rule the fluid has.
 *
 * <p>Upstream, standing this water next to a crystal sped that crystal's growth and spent the water
 * doing it. {@code BlockCrystalWater.isCrystalGrowable} wanted a partly grown crystal, with at
 * least two of the four blocks under it held as water <em>sources</em>, and {@code growCrystal}
 * took one of those sources away half the time it grew something.</p>
 *
 * <p>There is no partly grown crystal here to grow: TC4R's crystal clusters are grown by its
 * essentia crystalliser and expose no size. So the rule points at the crystal growth this port does
 * have — {@link dev.tc4port.thaumadditions.tiles.TileGrowthChamber}. The chamber asks how much of
 * this water stands around it, grows faster for it, and spends it exactly the way upstream did: one
 * source at a time, half the time. The ring and the odds are upstream's; only the thing grown had
 * to move.</p>
 */
public class BlockCrystalWater extends LiquidBlock {
    /** The eight blocks around one, as the research counts them: "surrounded by eight buckets". */
    private static final int[][] RING = {
            {-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};

    /** 1.12 {@code growCrystal} spent a neighbouring source one time in {@code 2}. */
    private static final int SPEND_ONE_IN = 2;

    public BlockCrystalWater(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    /**
     * How many source blocks of this water stand in the eight blocks around {@code pos}.
     *
     * <p>Only sources count, as upstream counted them: the thin edge of a spread-out flow is not
     * the bucket of water the research is talking about.</p>
     */
    public static int countSources(LevelReader level, BlockPos pos) {
        int sources = 0;
        for (int[] offset : RING) {
            if (isSource(level, pos.offset(offset[0], 0, offset[1]))) {
                sources++;
            }
        }
        return sources;
    }

    /**
     * Takes one neighbouring source away, if the roll allows and there is one to take.
     *
     * <p>This is the cost side of the water: growth does not come free, and upstream made the
     * player keep spares. Returns true when a source went, so a caller can say so.</p>
     */
    public static boolean spendSource(Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(SPEND_ONE_IN) != 0) {
            return false;
        }
        List<BlockPos> sources = new ArrayList<>(RING.length);
        for (int[] offset : RING) {
            BlockPos beside = pos.offset(offset[0], 0, offset[1]);
            if (isSource(level, beside)) {
                sources.add(beside);
            }
        }
        if (sources.isEmpty()) {
            return false;
        }
        level.destroyBlock(sources.get(random.nextInt(sources.size())), false);
        return true;
    }

    /** True when {@code pos} holds a source block of this water rather than a flowing edge. */
    private static boolean isSource(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof BlockCrystalWater && state.getValue(LiquidBlock.LEVEL) == 0;
    }
}
