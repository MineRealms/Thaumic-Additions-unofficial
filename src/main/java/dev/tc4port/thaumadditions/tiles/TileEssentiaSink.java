package dev.tc4port.thaumadditions.tiles;

import dev.tc4port.hclite.platform.HCLitePlatform;
import dev.tc4port.hclite.tile.SyncedBlockEntity;
import dev.tc4port.thaumadditions.items.AspectCharges;
import dev.tc4port.thaumcraft.api.aspect.AspectAmounts;
import dev.tc4port.thaumcraft.api.aspect.AspectContainerView;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.essentia.EssentiaApi;
import dev.tc4port.thaumcraft.api.essentia.EssentiaTransferMode;
import dev.tc4port.thaumcraft.api.essentia.EssentiaTransport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Map;

/**
 * 1.12 {@code TileEssentiaSink}: it does not store essentia at all — it pours it straight into
 * whatever chargeable item is sitting in its one slot.
 *
 * <p>The sink only listens on one face, the one to the left of the way the block faces, and it
 * advertises suction 120 only while the item in the slot is asking for something. A single point
 * moves per tick, which is what paces the machine: filling a shadow beam staff to its five hundred
 * takes five hundred ticks of steady supply.</p>
 *
 * <p>1.12 hard-coded ELDRITCH; the port asks the item instead, because the table in
 * {@link AspectCharges} is what says which aspect an item takes. For the staff the two agree.</p>
 */
public class TileEssentiaSink extends SyncedBlockEntity
        implements TaTickables.Ticking, Container, EssentiaTransport, AspectContainerView {
    private static final int SLOTS = 1;
    private static final String KEY_ITEMS = "Items";

    /** 1.12 {@code getSuctionAmount(face)}: 120 while the item wants something. */
    private static final int SUCTION = 120;
    /** 1.12 moved exactly one point of essentia per tick. */
    private static final int PER_TICK = 1;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public TileEssentiaSink(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        HCLitePlatform.Holder.get().loadItemStacks(this, getPersistentData(), KEY_ITEMS, inventory);
    }

    /**
     * 1.12 {@code WorldUtil.getFacing(state).rotateYCCW()}: the face the sink listens on.
     *
     * <p>The sink's block is a horizontal-only one, so the facing property it carries is
     * {@code HORIZONTAL_FACING}; the vertical fallback is here so that a six-way variant would not
     * quietly turn the machine around.</p>
     */
    public Direction essentiaFace() {
        BlockState state = getBlockState();
        Direction facing;
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        } else if (state.hasProperty(BlockStateProperties.FACING)) {
            facing = state.getValue(BlockStateProperties.FACING);
        } else {
            facing = Direction.NORTH;
        }
        return facing.getCounterClockWise();
    }

    /** The aspect the item in the slot is asking for, or {@code null} when it wants nothing. */
    private AspectId requested() {
        return AspectCharges.acceptedAspect(inventory.get(0));
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) {
            return;
        }
        AspectId wanted = requested();
        if (wanted == null) {
            return;
        }
        Direction face = essentiaFace();
        EssentiaTransport source = EssentiaApi.transport(level, pos.relative(face), face.getOpposite());
        if (source == null || !source.canOutputTo(face.getOpposite())) {
            return;
        }
        if (!wanted.equals(source.essentiaType(face.getOpposite()))) {
            return;
        }
        if (!AspectCharges.canAccept(inventory.get(0), wanted)) {
            return;
        }
        int taken = EssentiaApi.take(level, source, wanted, PER_TICK, face.getOpposite(),
                EssentiaTransferMode.EXECUTE);
        if (taken <= 0) {
            return;
        }
        int accepted = AspectCharges.addCharge(inventory.get(0), wanted, taken);
        if (accepted < taken) {
            // The item filled up between the check and the transfer; give the rest back.
            EssentiaApi.add(level, source, wanted, taken - accepted, face.getOpposite(),
                    EssentiaTransferMode.EXECUTE);
        }
        setChanged();
        sync();
    }

    // ------------------------------------------------------------------ EssentiaTransport

    /** 1.12 {@code isConnectable}: only the one face. */
    @Override
    public boolean isConnectable(Direction face) {
        return face == essentiaFace();
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return face == essentiaFace();
    }

    /** A sink never gives essentia back out. */
    @Override
    public boolean canOutputTo(Direction face) {
        return false;
    }

    @Override
    public AspectId suctionType(Direction face) {
        return face == essentiaFace() ? requested() : null;
    }

    @Override
    public int suctionAmount(Direction face) {
        return suctionType(face) == null ? 0 : SUCTION;
    }

    @Override
    public int takeEssentia(AspectId aspect, int amount, Direction face, EssentiaTransferMode mode) {
        return 0;
    }

    @Override
    public int addEssentia(AspectId aspect, int amount, Direction face, EssentiaTransferMode mode) {
        return 0;
    }

    @Override
    public AspectId essentiaType(Direction face) {
        return null;
    }

    @Override
    public int essentiaAmount(Direction face) {
        return 0;
    }

    @Override
    public int minimumSuction() {
        return 0;
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }

    // ------------------------------------------------------------------ AspectContainerView

    /** What the goggles see through the sink: whatever the chargeable item is holding. */
    @Override
    public AspectAmounts visibleAspects() {
        AspectId accepted = requested();
        if (accepted == null) {
            return AspectAmounts.EMPTY;
        }
        int held = AspectCharges.charge(inventory.get(0));
        return held > 0 ? new AspectAmounts(Map.of(accepted, held)) : AspectAmounts.EMPTY;
    }

    // ------------------------------------------------------------------ Container

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return inventory.get(0).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack taken = ContainerHelper.removeItem(inventory, slot, amount);
        if (!taken.isEmpty()) {
            saveInventory();
            setChanged();
        }
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack taken = ContainerHelper.takeItem(inventory, slot);
        saveInventory();
        return taken;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory.set(slot, stack);
        saveInventory();
        setChanged();
    }

    /** 1.12 kept one item in the slot. */
    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        inventory.clear();
        saveInventory();
        setChanged();
    }

    private void saveInventory() {
        HCLitePlatform.Holder.get().saveItemStacks(this, getPersistentData(), KEY_ITEMS, inventory);
    }

    public NonNullList<ItemStack> inventory() {
        return inventory;
    }
}
