package dev.tc4port.thaumadditions.tiles;

import dev.tc4port.thaumadditions.aspect.TaAspect;
import dev.tc4port.thaumadditions.blocks.TaBlocks;
import dev.tc4port.thaumcraft.api.aspect.AspectApi;
import dev.tc4port.thaumcraft.api.aspect.AspectDefinition;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.essentia.EssentiaApi;
import dev.tc4port.thaumcraft.api.essentia.EssentiaTransferMode;
import dev.tc4port.thaumcraft.api.essentia.EssentiaTransport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * 1.12 {@code TileAspectCombiner}: pull one aspect in from each side, and after a hundred ticks of
 * unpowered work hand the aspect those two components make to whatever sits above.
 *
 * <p>The combination rule is 1.12's exactly: the answer is any non-primal aspect with exactly two
 * components, both of which are the two inputs. TC4R keeps that shape in
 * {@code AspectDefinition.components()}.</p>
 *
 * <p>1.12 only ever pushed the result into a TC jar. The port pushes into whatever accepts it
 * through the transport interface, which covers its own jars, TC4R's alembics and tubes alike;
 * {@code canInputFrom} still gates it, so nothing takes essentia it would refuse.</p>
 */
public class TileAspectCombiner extends TileEssentia {
    private static final String KEY_IN_A = "InA";
    private static final String KEY_IN_B = "InB";
    private static final String KEY_OUTPUT = "Output";

    /** 1.12 {@code getMaxCraftTime()}. */
    private static final int MAX_CRAFT_TIME = 100;

    /** The two things waiting to be combined. */
    private AspectId inA;
    private AspectId inB;
    /** What the last craft produced, waiting for somewhere to go. */
    private AspectId output;

    private int craftingTime;
    private int prevCraftingTime;

    /** 1.12's animation state, all of it client-only and unpersisted. */
    private final TaRotator rotator = new TaRotator();
    private int toggle;
    private int vis;
    private int prevVis;
    private boolean crafting;
    private boolean prevCrafting;

    public TileAspectCombiner(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1, 1);
        this.inA = TaAspect.readAspect(getPersistentData(), KEY_IN_A);
        this.inB = TaAspect.readAspect(getPersistentData(), KEY_IN_B);
        this.output = TaAspect.readAspect(getPersistentData(), KEY_OUTPUT);
        this.craftingTime = getPersistentData().getInt("CraftingTime");
    }

    /**
     * The 1.12 combination rule: a non-primal aspect of exactly these two components.
     *
     * <p>The two inputs must differ. TC4 registered its combinations as ordered pairs, so an aspect
     * was never its own partner; without the check, feeding the same primal to both input faces
     * would answer with whichever two-component aspect happens to contain it.</p>
     */
    public static AspectId combine(AspectId a, AspectId b) {
        if (a == null || b == null || a.equals(b)) {
            return null;
        }
        for (java.util.Map.Entry<AspectId, AspectDefinition> entry : AspectApi.definitions().entrySet()) {
            AspectDefinition definition = entry.getValue();
            if (definition == null || definition.primal()) {
                continue;
            }
            java.util.List<AspectId> components = definition.components();
            if (components == null || components.size() != 2) {
                continue;
            }
            if (components.contains(a) && components.contains(b)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** Whether the aspect can take part in any two-component combination at all. */
    public static boolean isUsable(AspectId aspect) {
        if (aspect == null) {
            return false;
        }
        for (AspectDefinition definition : AspectApi.definitions().values()) {
            if (definition == null || definition.primal()) {
                continue;
            }
            java.util.List<AspectId> components = definition.components();
            if (components != null && components.size() == 2 && components.contains(aspect)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ transport

    private Direction front() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
    }

    /** 1.12 {@code front.rotateY()}: the left input face. */
    private Direction left() {
        return front().getClockWise();
    }

    /** 1.12 {@code front.rotateYCCW()}: the right input face. */
    private Direction right() {
        return front().getCounterClockWise();
    }

    @Override
    protected boolean accepts(Direction face) {
        return face == left() || face == right();
    }

    @Override
    protected boolean emits(Direction face) {
        return face == Direction.UP;
    }

    /** The only essentia this machine offers is the finished result. */
    @Override
    public AspectId getAspect() {
        return output;
    }

    @Override
    public int getAmount() {
        return output == null ? 0 : 1;
    }

    @Override
    public int insert(AspectId id, int value) {
        // Nothing is pushed into a combiner; it pulls from its two input faces.
        return 0;
    }

    @Override
    public int extract(AspectId id, int value) {
        if (output == null || id == null || !output.equals(id) || value <= 0) {
            return 0;
        }
        int taken = Math.min(1, value);
        output = null;
        store();
        return taken;
    }

    @Override
    public AspectId essentiaType(Direction face) {
        return face == Direction.UP ? output : null;
    }

    @Override
    public int essentiaAmount(Direction face) {
        return face == Direction.UP ? getAmount() : 0;
    }

    @Override
    public AspectId suctionType(Direction face) {
        // 1.12 pulled actively from its neighbours rather than advertising suction.
        return null;
    }

    @Override
    public int suctionAmount(Direction face) {
        return 0;
    }

    // ------------------------------------------------------------------ ticking

    @Override
    protected void machineTick(Level level, BlockPos pos, BlockState state) {
        prevCraftingTime = craftingTime;
        prevVis = vis;
        rotator.update();

        vis = (inA != null ? 1 : 0) + (inB != null ? 1 : 0) + (output != null ? 1 : 0);

        boolean powered = level.hasNeighborSignal(pos);
        boolean wasCrafting = crafting;
        crafting = false;

        AspectId combined = combine(inA, inB);
        if (inA != null && inB != null && output == null && combined != null) {
            if (craftingTime < MAX_CRAFT_TIME && !powered) {
                ++craftingTime;
                crafting = true;
            }
            if (craftingTime >= MAX_CRAFT_TIME) {
                output = combined;
                craftingTime = 0;
                if (!level.isClientSide) {
                    inA = null;
                    inB = null;
                    store();
                }
            }
        }

        // The knob winds up while it works and unwinds when it stops, exactly as in 1.12.
        if (crafting != prevCrafting) {
            toggle = 36;
        }
        float speed = (float) Math.sqrt((!crafting ? toggle / 36.0F : (36 - toggle) / 36.0F) * 81.0F) / 9.0F * 8.0F;
        rotator.speedup(speed);
        if (toggle != 0) {
            toggle += toggle < 0 ? 1 : -1;
        }
        prevCrafting = crafting;

        if (level.isClientSide) {
            return;
        }

        boolean dirty = false;
        if (pullOne(level, pos, left(), true)) {
            dirty = true;
        }
        if (pullOne(level, pos, right(), false)) {
            dirty = true;
        }
        if (pushOutput(level, pos)) {
            dirty = true;
        }
        if (dirty) {
            store();
        }
    }

    /** 1.12's per-face pull: one unit into the slot that face owns, if the aspect is usable there. */
    private boolean pullOne(Level level, BlockPos pos, Direction face, boolean intoA) {
        if (intoA ? inA != null : inB != null) {
            return false;
        }
        EssentiaTransport neighbour = EssentiaApi.transport(level, pos.relative(face), face.getOpposite());
        if (neighbour == null || !neighbour.canOutputTo(face.getOpposite())) {
            return false;
        }
        AspectId offered = neighbour.essentiaType(face.getOpposite());
        if (!canAccept(offered)) {
            return false;
        }
        int taken = EssentiaApi.take(level, neighbour, offered, 1, face.getOpposite(),
                EssentiaTransferMode.EXECUTE);
        if (taken != 1) {
            return false;
        }
        if (intoA) {
            inA = offered;
        } else {
            inB = offered;
        }
        return true;
    }

    /** 1.12 {@code canAccept}. */
    public boolean canAccept(AspectId type) {
        if (type == null) {
            return false;
        }
        if (inA == null && inB == null) {
            return isUsable(type);
        }
        if (inA == null) {
            return combine(inB, type) != null;
        }
        if (inB == null) {
            return combine(inA, type) != null;
        }
        return false;
    }

    /** 1.12 pushed the result upward; anything that accepts it through the transport will take it. */
    private boolean pushOutput(Level level, BlockPos pos) {
        if (output == null) {
            return false;
        }
        EssentiaTransport above = EssentiaApi.transport(level, pos.above(), Direction.DOWN);
        if (above == null || !above.canInputFrom(Direction.DOWN)) {
            return false;
        }
        int accepted = EssentiaApi.add(level, above, output, 1, Direction.DOWN, EssentiaTransferMode.EXECUTE);
        if (accepted <= 0) {
            return false;
        }
        output = null;
        return true;
    }

    private void store() {
        TaAspect.writeAspect(getPersistentData(), KEY_IN_A, inA);
        TaAspect.writeAspect(getPersistentData(), KEY_IN_B, inB);
        TaAspect.writeAspect(getPersistentData(), KEY_OUTPUT, output);
        getPersistentData().putInt("CraftingTime", craftingTime);
        setChanged();
        sync();
    }

    // ------------------------------------------------------------------ for the renderer

    public int craftingTime() {
        return craftingTime;
    }

    public TaRotator rotator() {
        return rotator;
    }

    public int vis() {
        return vis;
    }

    public AspectId inputA() {
        return inA;
    }

    public AspectId inputB() {
        return inB;
    }

    public AspectId outputAspect() {
        return output;
    }
}
