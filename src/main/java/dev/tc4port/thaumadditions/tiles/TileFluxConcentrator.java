package dev.tc4port.thaumadditions.tiles;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.tc4port.thaumadditions.TaFx;
import dev.tc4port.thaumadditions.aspect.EdibleAspect;
import dev.tc4port.thaumadditions.platform.TaEffect;
import dev.tc4port.thaumcraft.api.aspect.AspectAmounts;
import dev.tc4port.thaumcraft.api.aspect.AspectApi;
import dev.tc4port.thaumcraft.api.aspect.AspectContainerView;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.damage.ThaumcraftDamageTypes;
import dev.tc4port.thaumcraft.api.essentia.EssentiaApi;
import dev.tc4port.thaumcraft.api.essentia.EssentiaTransferMode;
import dev.tc4port.thaumcraft.api.essentia.EssentiaTransport;
import dev.tc4port.thaumcraft.api.flux.FluxApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

/**
 * 1.12 {@code TileFluxConcentrator}: pour FLUX essentia in one face, get a creeping field of taint
 * out of it, and let that field rewrite the blocks it covers.
 *
 * <p>The machine is a small economy. One point of vitium buys ten units of fuel; spreading the field
 * over a new block costs one unit, converting a block costs ten, and any living thing standing in
 * the field costs five more. A conversion then locks the machine out for six hundred ticks, which is
 * what makes a big field crawl rather than sweep.</p>
 *
 * <p>Three upstream details are recorded here because they are behaviours rather than ports:</p>
 * <ul>
 *   <li>Powering the block stops it spreading or converting and makes it instead unmark one fluxed
 *       position every five ticks, furthest first — the field withdraws instead of growing.</li>
 *   <li>A field with any column open to the sky is abandoned outright. This is 1.12's rule and it
 *       is checked on every position in the field, both while spreading and while re-flooding.</li>
 *   <li>1.12's recipe table converted both mushrooms into TC's vishroom, the two pumpkins into the
 *       mod's own taintkin, and any log into TC's taint log. TC4R has no taint log, so that third
 *       rule has nowhere to land and is dropped. Its {@code BlockSideHelper} pair of passability
 *       helpers collapses into one predicate: a position is passable when it is a recipe input or
 *       when nothing solid is in the way.</li>
 * </ul>
 */
public class TileFluxConcentrator extends TileEssentia implements AspectContainerView {
    private static final String KEY_FUEL = "Fuel";
    private static final String KEY_COOLDOWN = "Cooldown";

    /** 1.12 {@code CAPACITY}: the vitium tank, which is also the suction face's content. */
    private static final int CAPACITY = 10;
    /** 1.12 {@code getSuctionAmount(face)}: 128 on the one face it listens on. */
    private static final int SUCTION = 128;

    /** One vitium becomes this much fuel, and only when the tank has nearly run dry. */
    private static final int FUEL_PER_VITIUM = 10;
    private static final int FUEL_REFILL_BELOW = 10;
    /** Fuel spent marking a new block, converting one, and hurting one entity. */
    private static final int FUEL_PER_SPREAD = 1;
    private static final int FUEL_PER_CONVERSION = 10;
    private static final int FUEL_PER_ENTITY = 5;

    /** The field may not reach further than this from the machine, squared. */
    private static final double RADIUS_SQUARED = 256.0D;
    /** Spreading, converting and withdrawing each run on their own cadence. */
    private static final int SPREAD_INTERVAL = 20;
    private static final int WITHDRAW_INTERVAL = 5;
    private static final int POLLUTION_INTERVAL = 600;
    private static final int ENTITY_DAMAGE_INTERVAL = 30;
    private static final int CONVERSION_COOLDOWN = 200;
    private static final int CONVERSION_LOCKOUT = 400;
    /** 1.12 put vis exhaustion on everything standing in the field, for this long. */
    private static final int EXHAUST_TICKS = 219;
    /** 1.12 drew up to five motes over the field each step. */
    private static final int FX_COUNT = 5;

    /** The blocks the field currently covers, in the order they were found. */
    private final List<BlockPos> fluxed = new ArrayList<>();
    /** What each covered position looked like when it was marked, to notice edits. */
    private final Map<BlockPos, BlockState> cache = new LinkedHashMap<>();

    /** Work units, bought ten at a time with one vitium. */
    private int fuel;
    /** Ticks left before another block may be converted. */
    private int cooldown;
    /** Counts to each of the cadences above. */
    private int spreadTicker;
    private int withdrawTicker;
    private int pollutionTicker;
    private int tearTicker;
    /** Set on the tick the field tears at what is standing in it. */
    private boolean tear;

    /** The valve's spin, for the renderer. 1.12 gave this one a lighter friction than the rest. */
    private final TaRotator rotator = new TaRotator();

    public TileFluxConcentrator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, CAPACITY, SUCTION);
        this.fuel = getPersistentData().getInt(KEY_FUEL);
        this.cooldown = getPersistentData().getInt(KEY_COOLDOWN);
    }

    /** The face the machine faces: where its essentia comes in, and the seed of the field. */
    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockStateProperties.FACING)
                ? state.getValue(BlockStateProperties.FACING) : Direction.NORTH;
    }

    /** 1.12 {@code isConnectable}: only the face it faces. */
    @Override
    protected boolean accepts(Direction face) {
        return face == facing();
    }

    @Override
    protected boolean emits(Direction face) {
        return false;
    }

    /** 1.12 {@code getSuctionType}: FLUX whenever a tube is on its face. */
    @Override
    public AspectId suctionType(Direction face) {
        return face == facing() ? fluxAspect() : null;
    }

    public int fuel() {
        return fuel;
    }

    /** The valve; the renderer turns it about Z. */
    public TaRotator rotator() {
        return rotator;
    }

    public List<BlockPos> fluxed() {
        return fluxed;
    }

    @Override
    public AspectAmounts visibleAspects() {
        int held = getAmount();
        AspectId flux = getAspect();
        return held > 0 && flux != null ? new AspectAmounts(Map.of(flux, held)) : AspectAmounts.EMPTY;
    }

    private static AspectId fluxAspect() {
        try {
            AspectId flux = AspectId.parse("vitium");
            return AspectApi.contains(flux) ? flux : null;
        } catch (RuntimeException unknown) {
            return null;
        }
    }

    private void store() {
        getPersistentData().putInt(KEY_FUEL, fuel);
        getPersistentData().putInt(KEY_COOLDOWN, cooldown);
        setChanged();
        sync();
    }

    // ------------------------------------------------------------------ ticking

    @Override
    protected void machineTick(Level level, BlockPos pos, BlockState state) {
        // Spare vitium becomes fuel, but only once the tank of work is nearly empty.
        if (fuel <= FUEL_REFILL_BELOW && getAmount() > 0) {
            extract(getAspect(), 1);
            fuel += FUEL_PER_VITIUM;
            store();
        }
        if (cooldown > 0) {
            --cooldown;
        }

        // 1.12 wound the valve up on the client whenever the machine had work and a field.
        rotator.setFriction(0.1F);
        rotator.update();
        if (level.isClientSide) {
            if (fuel > 0 && !fluxed.isEmpty()) {
                rotator.speedup(0.25F);
            }
            return;
        }

        drawFromFace(level, pos);

        if (level.getBestNeighborSignal(pos) > 0) {
            if (++withdrawTicker >= WITHDRAW_INTERVAL) {
                withdrawTicker = 0;
                withdrawFurthest(level, pos);
            }
            return;
        }

        if (!fluxed.isEmpty() && ++pollutionTicker >= POLLUTION_INTERVAL) {
            pollutionTicker = 0;
            if (level.getRandom().nextBoolean() && level instanceof ServerLevel server) {
                BlockPos target = fluxed.get(level.getRandom().nextInt(fluxed.size()));
                // 1.12 added half to one unit of flux; TC4R counts whole quanta, so it is one.
                FluxApi.add(server, target, 1);
            }
        }

        if (++tearTicker >= ENTITY_DAMAGE_INTERVAL) {
            tearTicker = 0;
            tear = true;
        }

        if (fuel <= 0) {
            return;
        }
        if (++spreadTicker < SPREAD_INTERVAL) {
            return;
        }
        spreadTicker = 0;
        spread(level, pos);
        tear = false;
    }

    /** 1.12's intake: pull FLUX out of the transport on the face the machine faces. */
    private void drawFromFace(Level level, BlockPos pos) {
        AspectId flux = fluxAspect();
        if (flux == null) {
            return;
        }
        int room = CAPACITY - getAmount();
        if (room <= 0) {
            return;
        }
        Direction face = facing();
        EssentiaTransport source = EssentiaApi.transport(level, pos.relative(face), face.getOpposite());
        if (source == null || !source.canOutputTo(face.getOpposite())) {
            return;
        }
        if (!flux.equals(source.essentiaType(face.getOpposite()))) {
            return;
        }
        int taken = EssentiaApi.take(level, source, flux, room, face.getOpposite(),
                EssentiaTransferMode.EXECUTE);
        if (taken > 0) {
            insert(flux, taken);
            store();
        }
    }

    /** 1.12's redstone behaviour: drop the furthest covered position, one every five ticks. */
    private void withdrawFurthest(Level level, BlockPos pos) {
        if (fluxed.isEmpty()) {
            return;
        }
        BlockPos furthest = null;
        double best = -1.0D;
        for (BlockPos candidate : fluxed) {
            double distance = candidate.distSqr(pos);
            if (distance > best) {
                best = distance;
                furthest = candidate;
            }
        }
        if (furthest != null) {
            fluxed.remove(furthest);
            cache.remove(furthest);
            sync();
        }
    }

    /** One spreading step: grow the field, abandon it when it is open to the sky, convert a block. */
    private void spread(Level level, BlockPos pos) {
        if (fluxed.isEmpty()) {
            BlockPos seed = pos.relative(facing().getOpposite());
            fluxed.add(seed);
            cache.put(seed, level.getBlockState(seed));
        }

        boolean changed = false;
        // Snapshot: the loop walks only the positions the field had when this step began.
        int known = fluxed.size();
        for (int index = 0; index < known; index++) {
            BlockPos current = fluxed.get(index);
            if (!columnIsSheltered(level, current)) {
                fluxed.clear();
                cache.clear();
                sync();
                return;
            }
            if (current.distSqr(pos) >= RADIUS_SQUARED) {
                continue;
            }
            BlockState recorded = cache.get(current);
            BlockState present = level.getBlockState(current);
            if (recorded != null && present != recorded) {
                changed = true;
            }
            for (Direction face : Direction.values()) {
                if (!isPassable(level, current, face)) {
                    continue;
                }
                BlockPos target = current.relative(face);
                if (fluxed.contains(target)) {
                    continue;
                }
                if (!isPassable(level, target, face.getOpposite())) {
                    continue;
                }
                fluxed.add(target);
                cache.put(target, level.getBlockState(target));
                changed = true;
                if (--fuel <= 0) {
                    store();
                    return;
                }
            }
        }

        if (changed) {
            redefine(level, pos);
        }

        damageEntities(level, pos);
        convertOne(level, pos);
        taintParticles(level);
        store();
    }

    /**
     * 1.12's field shimmer: up to five motes over random positions the field covers, in the colour
     * of flux. Upstream drew these on the client from a synced cache; the port has the field list on
     * both sides, so it draws them wherever the machine is being ticked.
     */
    private void taintParticles(Level level) {
        if (fluxed.isEmpty()) {
            return;
        }
        int count = Math.min(fluxed.size() / 2, FX_COUNT);
        for (int index = 0; index < count; index++) {
            BlockPos at = fluxed.get(level.getRandom().nextInt(fluxed.size()));
            TaFx.aspectDust(level, at.getX() + level.getRandom().nextDouble(),
                    at.getY() + level.getRandom().nextDouble(),
                    at.getZ() + level.getRandom().nextDouble(),
                    fluxAspect(),
                    (level.getRandom().nextFloat() - level.getRandom().nextFloat()) / 10.0D,
                    (level.getRandom().nextFloat() - level.getRandom().nextFloat()) / 10.0D,
                    (level.getRandom().nextFloat() - level.getRandom().nextFloat()) / 10.0D);
        }
    }

    /** 1.12 {@code redefine}: throw the field away and flood it again from the seed. */
    private void redefine(Level level, BlockPos pos) {
        fluxed.clear();
        cache.clear();
        BlockPos seed = pos.relative(facing().getOpposite());
        fluxed.add(seed);
        cache.put(seed, level.getBlockState(seed));

        for (int index = 0; index < fluxed.size(); index++) {
            BlockPos current = fluxed.get(index);
            if (!columnIsSheltered(level, current)) {
                fluxed.clear();
                cache.clear();
                return;
            }
            if (current.distSqr(pos) >= RADIUS_SQUARED) {
                continue;
            }
            for (Direction face : Direction.values()) {
                if (!isPassable(level, current, face)) {
                    continue;
                }
                BlockPos target = current.relative(face);
                if (fluxed.contains(target) || !isPassable(level, target, face.getOpposite())) {
                    continue;
                }
                fluxed.add(target);
                cache.put(target, level.getBlockState(target));
            }
        }
    }

    /**
     * 1.12's sky check: everything above must be blocked. A column that stays open all the way to
     * the build limit means the field is under the sky, and the whole of it is abandoned.
     */
    private boolean columnIsSheltered(Level level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int y = pos.getY(); y < level.getMaxBuildHeight() - 1; y++) {
            cursor.setY(y);
            if (!isPassable(level, cursor, Direction.UP)) {
                return true;
            }
        }
        return false;
    }

    /** Everything standing in the field is exhausted, and every thirty ticks torn at. */
    private void damageEntities(Level level, BlockPos pos) {
        if (fluxed.isEmpty()) {
            return;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos covered : fluxed) {
            minX = Math.min(minX, covered.getX());
            minY = Math.min(minY, covered.getY());
            minZ = Math.min(minZ, covered.getZ());
            maxX = Math.max(maxX, covered.getX());
            maxY = Math.max(maxY, covered.getY());
            maxZ = Math.max(maxZ, covered.getZ());
        }
        AABB box = new AABB(minX, minY, minZ, maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            BlockPos at = entity.blockPosition();
            if (!fluxed.contains(at) && !fluxed.contains(at.below())) {
                continue;
            }
            EdibleAspect.addPotionEffect(entity, TaEffect.VIS_EXHAUST, EXHAUST_TICKS, 0);
            if (tear && fuel >= FUEL_PER_ENTITY) {
                entity.hurt(ThaumcraftDamageTypes.dissolve(level), 1.0F);
                fuel -= FUEL_PER_ENTITY;
            }
        }
    }

    /** 1.12's conversion step: one block per cycle, and a long lockout afterwards. */
    private void convertOne(Level level, BlockPos pos) {
        if (cooldown > 0 || fuel < FUEL_PER_CONVERSION) {
            return;
        }
        cooldown = CONVERSION_COOLDOWN;
        for (BlockPos covered : fluxed) {
            BlockState replacement = replacementFor(level.getBlockState(covered));
            if (replacement == null) {
                continue;
            }
            level.setBlock(covered, replacement, 2);
            cooldown += CONVERSION_LOCKOUT;
            fuel -= FUEL_PER_CONVERSION;
            cache.put(covered, replacement);
            break;
        }
    }

    // ------------------------------------------------------------------ the table

    /**
     * 1.12's recipe table, minus the log rule. TC4R has a vishroom to turn mushrooms into, and the
     * mod has its own taintkin for the pumpkins, but TC4R has no taint log for the third rule.
     */
    private static BlockState replacementFor(BlockState state) {
        if (state.is(Blocks.BROWN_MUSHROOM) || state.is(Blocks.RED_MUSHROOM)) {
            return blockState("thaumcraft:vishroom");
        }
        if (state.is(Blocks.PUMPKIN)) {
            return blockState("thaumadds:taintkin");
        }
        if (state.is(Blocks.JACK_O_LANTERN)) {
            return blockState("thaumadds:taintkin_lit");
        }
        return null;
    }

    private static BlockState blockState(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        var block = key == null ? null : BuiltInRegistries.BLOCK.get(key);
        return block == null || block == Blocks.AIR ? null : block.defaultBlockState();
    }

    /** Whether the field may pass this position in the given direction. */
    private static boolean isPassable(Level level, BlockPos pos, Direction face) {
        BlockState state = level.getBlockState(pos);
        if (replacementFor(state) != null) {
            // Recipe inputs are what the field is made of; it flows through them.
            return true;
        }
        return state.getCollisionShape(level, pos).isEmpty()
                && state.getFluidState().isEmpty();
    }
}
