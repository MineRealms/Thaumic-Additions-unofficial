package dev.tc4port.thaumadditions.tiles;

import java.util.ArrayList;
import java.util.List;

import dev.tc4port.hclite.platform.HCLitePlatform;
import dev.tc4port.hclite.tile.SyncedBlockEntity;
import dev.tc4port.thaumadditions.aspect.ShadowEnchantment;
import dev.tc4port.thaumadditions.aspect.TaAspect;
import dev.tc4port.thaumadditions.platform.TaEnchantment;
import dev.tc4port.thaumadditions.platform.TaPlatform;
import dev.tc4port.thaumadditions.platform.TaSound;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.aspect.AspectList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.12 {@code TileShadowEnchanter}: pick enchantments from a list, pay for them in essentia, and
 * write them onto whatever tool sits in the input slot.
 *
 * <p>A craft runs on two clocks. The output appears the moment the last unit of essentia lands, and
 * essentia is only asked for every sixth tick, so most of the twenty-odd seconds a typical craft
 * takes is waiting on the tubes rather than on the machine. A failed drain silences the machine's
 * idle ticking for a long while — 1.12 set the cooldown to ten thousand ticks — which is what makes
 * an unconnected enchanter go quiet instead of clicking forever.</p>
 *
 * <p>The chosen enchantments are held as ids and resolved into {@link TaEnchantment} handles
 * through the platform whenever one is needed, because 1.20.1 and 1.21.1 disagree about what an
 * enchantment value is. Two upstream pieces are absent rather than approximated: the
 * aspect-coloured FX packet the enchant was announced with (the port has no client packet for it
 * yet) and the whitelist that checked a player's research first, because every 1.12 entry left its
 * research field {@code null} and so allowed everyone.</p>
 */
public class TileShadowEnchanter extends SyncedBlockEntity
        implements TaTickables.Ticking, Container {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    private static final int SLOTS = 2;

    private static final String KEY_ITEMS = "Items";
    private static final String KEY_ENCHANTS = "Enchantments";
    private static final String KEY_ASPECTS = "Aspects";
    private static final String KEY_INFUSING = "Infusing";
    private static final String KEY_TIMER = "Timer";

    /** 1.12 {@code drainEssentia(this, a, null, 12, 1)}: suction 12, one unit at a time. */
    private static final int DRAIN_SUCTION = 12;
    /** 1.12 {@code atTickRate(6)}: essentia is asked for every sixth tick. */
    private static final int DRAIN_INTERVAL = 6;
    /** 1.12 refused a recipe costing more than this many points of essentia. */
    private static final int MAX_RECIPE_SIZE = 32;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    /** The enchantments the player has chosen for this craft, by registry id. */
    private final List<Choice> enchants = new ArrayList<>();

    /** Whether a craft is running. */
    private boolean infusing;
    private int craftTimer;
    /** What the running craft still owes, or {@code null} when idle. */
    private AspectList pending;

    /** Ticks until the machine may hum again; also parked high while it cannot reach essentia. */
    private int brainCooldown;
    /** Counts to {@link #DRAIN_INTERVAL}; 1.12's {@code atTickRate}. */
    private int tickCounter;

    public TileShadowEnchanter(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        CompoundTag tag = getPersistentData();
        this.infusing = tag.getBoolean(KEY_INFUSING);
        this.craftTimer = tag.getInt(KEY_TIMER);
        this.pending = readAspects(tag);
        HCLitePlatform.Holder.get().loadItemStacks(this, tag, KEY_ITEMS, inventory);
        readEnchants(tag);
    }

    /** One chosen enchantment and the level it will be written at. */
    public record Choice(ResourceLocation enchantment, int level) {
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        // An enchantment stops being applicable the moment the item under it changes.
        enchants.removeIf(choice -> !isApplicable(level, choice.enchantment()));

        if (!infusing || !inventory.get(SLOT_OUTPUT).isEmpty()) {
            return;
        }

        if (pending == null || inventory.get(SLOT_INPUT).isEmpty()) {
            if (!level.isClientSide) {
                TaPlatform.Active.get().playSound(level, pos, TaSound.CRAFT_FAIL, 1.0F, 1.0F);
            }
            infusing = false;
            craftTimer = 0;
            enchants.clear();
            pending = null;
            store();
            return;
        }

        if (level.isClientSide) {
            return;
        }

        ++craftTimer;

        // The idle hum: once the cooldown runs out the machine ticks, then waits a short random
        // while. A failed drain parks it at ten thousand ticks, which is the "no essentia" silence.
        if (brainCooldown > 0) {
            --brainCooldown;
        }
        if (brainCooldown <= 0) {
            TaPlatform.Active.get().playSound(level, pos, TaSound.BRAIN, 0.2F, 1.0F);
            brainCooldown += 35 + level.getRandom().nextInt(50);
        }

        if (pending.visSize() == 0) {
            finish(level, pos);
            return;
        }

        if (++tickCounter < DRAIN_INTERVAL) {
            return;
        }
        tickCounter = 0;

        AspectId wanted = firstPending();
        if (wanted == null) {
            return;
        }
        if (!drain(level, pos, wanted)) {
            brainCooldown = 10000;
            store();
            return;
        }

        pending.remove(wanted, 1);
        brainCooldown = Math.min(brainCooldown, 50);
        store();
    }

    /** 1.12 drained the alphabetically first aspect still owed. */
    private AspectId firstPending() {
        for (AspectId id : pending.aspectsSorted()) {
            if (pending.amount(id) > 0) {
                return id;
            }
        }
        return null;
    }

    /** 1.12 {@code EssentiaHandler.drainEssentia(this, a, null, 12, 1)}. */
    private boolean drain(Level level, BlockPos pos, AspectId aspect) {
        return TileEssentia.pullFromNeighbours(level, pos, aspect, 1) == 1;
    }

    /** 1.12's finishing branch: the enchanted copy lands in the output and the input is spent. */
    private void finish(Level level, BlockPos pos) {
        ItemStack input = inventory.get(SLOT_INPUT);
        ItemStack result = input.copy();
        for (Choice choice : enchants) {
            TaEnchantment enchantment = TaPlatform.Active.get()
                    .enchantment(level, choice.enchantment());
            if (enchantment != null) {
                TaPlatform.Active.get().enchantItem(result, enchantment, choice.level());
            }
        }
        inventory.set(SLOT_OUTPUT, result);
        input.shrink(1);
        if (input.isEmpty()) {
            inventory.set(SLOT_INPUT, ItemStack.EMPTY);
        }
        TaPlatform.Active.get().playSound(level, pos, TaSound.CRAFT_DONE, 1.0F, 1.0F);
        enchants.clear();
        infusing = false;
        craftTimer = 0;
        pending = null;
        saveInventory();
        store();
    }

    // ------------------------------------------------------------------ the recipe

    /** 1.12 {@code calculateAspects}: what the chosen enchantments cost together. */
    public AspectList calculateAspects() {
        AspectList total = new AspectList();
        for (Choice choice : enchants) {
            AspectList cost = ShadowEnchantment.costOf(choice.enchantment(), choice.level());
            if (cost == null) {
                return null;
            }
            total.add(cost);
        }
        return total;
    }

    /** 1.12 {@code startCraft}. */
    public void startCraft(Level level, BlockPos pos) {
        if (infusing || enchants.isEmpty() || !inventory.get(SLOT_OUTPUT).isEmpty()) {
            return;
        }
        AspectList cost = calculateAspects();
        if (cost == null || cost.size() > MAX_RECIPE_SIZE) {
            return;
        }
        infusing = true;
        craftTimer = 0;
        pending = cost;
        TaPlatform.Active.get().playSound(level, pos, TaSound.CRAFT_START, 0.2F, 1.0F);
        store();
    }

    /**
     * 1.12 {@code isAplicable}: the item must take enchantments, the candidate must apply to it, and
     * no already-chosen enchantment may be incompatible with the candidate.
     */
    public boolean isApplicable(Level level, ResourceLocation enchantment) {
        TaEnchantment handle = TaPlatform.Active.get().enchantment(level, enchantment);
        if (handle == null) {
            return false;
        }
        ItemStack input = inventory.get(SLOT_INPUT);
        if (input.isEmpty() || !input.isEnchantable()
                || !TaPlatform.Active.get().enchantmentAppliesTo(handle, input)) {
            return false;
        }
        for (Choice other : enchants) {
            if (other.enchantment().equals(enchantment)) {
                continue;
            }
            TaEnchantment otherHandle = TaPlatform.Active.get()
                    .enchantment(level, other.enchantment());
            if (otherHandle != null
                    && !TaPlatform.Active.get().enchantmentsCompatible(otherHandle, handle)) {
                return false;
            }
        }
        return true;
    }

    /** 1.12 {@code upLvl}: add the enchantment, or raise it until it hits its own maximum. */
    public void upLevel(Level level, ResourceLocation enchantment) {
        if (infusing || !isApplicable(level, enchantment)) {
            return;
        }
        TaEnchantment handle = TaPlatform.Active.get().enchantment(level, enchantment);
        if (handle == null) {
            return;
        }
        for (int index = 0; index < enchants.size(); index++) {
            Choice choice = enchants.get(index);
            if (choice.enchantment().equals(enchantment)) {
                int next = Math.min(choice.level() + 1,
                        TaPlatform.Active.get().enchantmentMaxLevel(handle));
                enchants.set(index, new Choice(enchantment, next));
                store();
                return;
            }
        }
        enchants.add(new Choice(enchantment, TaPlatform.Active.get().enchantmentMinLevel(handle)));
        store();
    }

    /** 1.12 {@code downLvl}: lower the enchantment, dropping it once it is at its minimum. */
    public void downLevel(Level level, ResourceLocation enchantment) {
        if (infusing) {
            return;
        }
        TaEnchantment handle = TaPlatform.Active.get().enchantment(level, enchantment);
        int minLevel = handle == null ? 1 : TaPlatform.Active.get().enchantmentMinLevel(handle);
        for (int index = 0; index < enchants.size(); index++) {
            Choice choice = enchants.get(index);
            if (!choice.enchantment().equals(enchantment)) {
                continue;
            }
            if (choice.level() <= minLevel) {
                enchants.remove(index);
            } else {
                enchants.set(index, new Choice(enchantment, choice.level() - 1));
            }
            store();
            return;
        }
    }

    // ------------------------------------------------------------------ state

    public List<Choice> enchants() {
        return enchants;
    }

    public boolean infusing() {
        return infusing;
    }

    public int craftTimer() {
        return craftTimer;
    }

    public AspectList pending() {
        return pending;
    }

    private void store() {
        CompoundTag tag = getPersistentData();
        tag.putBoolean(KEY_INFUSING, infusing);
        tag.putInt(KEY_TIMER, craftTimer);
        writeAspects(tag, pending);
        writeEnchants(tag);
        setChanged();
        sync();
    }

    private void saveInventory() {
        HCLitePlatform.Holder.get().saveItemStacks(this, getPersistentData(), KEY_ITEMS, inventory);
    }

    private void writeEnchants(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Choice choice : enchants) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", choice.enchantment().toString());
            entry.putInt("lvl", choice.level());
            list.add(entry);
        }
        tag.put(KEY_ENCHANTS, list);
    }

    private void readEnchants(CompoundTag tag) {
        ListTag list = tag.getList(KEY_ENCHANTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("id"));
            // An enchantment a data pack removed simply drops out.
            if (id != null && ShadowEnchantment.canWrite(id)) {
                enchants.add(new Choice(id, Math.max(1, entry.getInt("lvl"))));
            }
        }
    }

    private void writeAspects(CompoundTag tag, AspectList list) {
        if (list == null) {
            tag.remove(KEY_ASPECTS);
            return;
        }
        List<AspectId> ids = new ArrayList<>();
        List<Integer> amounts = new ArrayList<>();
        for (AspectId id : list.aspectsSortedByAmount()) {
            int amount = list.amount(id);
            if (amount > 0) {
                ids.add(id);
                amounts.add(amount);
            }
        }
        TaAspect.writeAspects(tag, KEY_ASPECTS, ids, amounts);
    }

    private AspectList readAspects(CompoundTag tag) {
        if (!tag.contains(KEY_ASPECTS)) {
            return null;
        }
        List<AspectId> ids = TaAspect.readAspects(tag, KEY_ASPECTS);
        List<Integer> amounts = TaAspect.readAspectAmounts(tag, KEY_ASPECTS);
        if (ids.isEmpty()) {
            return null;
        }
        AspectList list = new AspectList();
        for (int i = 0; i < ids.size() && i < amounts.size(); i++) {
            list.add(ids.get(i), amounts.get(i));
        }
        return list.visSize() > 0 ? list : null;
    }

    // ------------------------------------------------------------------ Container

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return inventory.stream().allMatch(ItemStack::isEmpty);
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

    public NonNullList<ItemStack> inventory() {
        return inventory;
    }
}
