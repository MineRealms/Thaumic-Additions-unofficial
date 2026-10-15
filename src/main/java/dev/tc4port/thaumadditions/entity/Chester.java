package dev.tc4port.thaumadditions.entity;

import dev.tc4port.thaumadditions.gui.ChesterMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 1.12 {@code EntityChester}: a walking chest that follows whoever summoned it and carries
 * thirty-six slots of their things.
 *
 * <p>Upstream made it an {@code EntityAnimal} with an inventory glued on, which brought breeding
 * and ageing along for no reason — Chester is a container, not livestock. The port makes it a
 * {@code PathfinderMob} with a {@link Container} on the side, which is what it actually is, and
 * keeps the parts that matter: it owns itself to a player, it stays put when told to, it opens its
 * lid when someone is looking inside, and it spills its contents when it dies.</p>
 */
public class Chester extends PathfinderMob implements Container {
    /** 1.12 {@code new InventoryDummy(36)}: four rows of nine. */
    public static final int SLOTS = 36;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    /** Who summoned it; it follows that player and answers to nobody else. */
    private UUID owner;
    /** 1.12's {@code staying}: told to hold position. */
    private boolean staying;
    /** How far the lid is open, {@code 0..1}; driven by whether anyone is looking inside. */
    private float lid;
    private float previousLid;

    public Chester(EntityType<? extends Chester> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createChesterAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    // ------------------------------------------------------------------ ownership

    public UUID owner() {
        return owner;
    }

    public void setOwner(Player player) {
        this.owner = player.getUUID();
    }

    public boolean ownedBy(Player player) {
        return owner == null || owner.equals(player.getUUID());
    }

    public boolean staying() {
        return staying;
    }

    /** 1.12's stay/follow toggle. */
    public void setStaying(boolean staying) {
        this.staying = staying;
    }

    // ------------------------------------------------------------------ the lid

    /** Whether anyone is looking inside; the renderer sets this from the player's open menu. */
    private boolean open;

    public void setOpen(boolean open) {
        this.open = open;
    }

    public float lidAngle(float partialTick) {
        return previousLid + (lid - previousLid) * partialTick;
    }

    @Override
    public void tick() {
        super.tick();
        previousLid = lid;
        float target = open ? 1.0F : 0.0F;
        lid += (target - lid) * (open ? 0.15F : 0.1F);
        if (Math.abs(target - lid) < 0.001F) {
            lid = target;
        }
        // Whoever summoned it is who it keeps up with; told to stay, it stays.
        if (!level().isClientSide && !staying && owner != null
                && tickCount % 20 == 0 && level().getServer() != null) {
            Player found = level().getServer().getPlayerList().getPlayer(owner);
            if (found != null && distanceToSqr(found) > 144.0D) {
                getNavigation().moveTo(found, 1.0D);
            }
        }
    }

    // ------------------------------------------------------------------ interaction

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide || !ownedBy(player)) {
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (player.isShiftKeyDown()) {
            setStaying(!staying);
            playSound(SoundEvents.CHEST_OPEN, 1.0F, staying ? 1.0F : 1.2F);
            return InteractionResult.CONSUME;
        }
        ChesterMenu.open(player, this);
        return InteractionResult.CONSUME;
    }

    // ------------------------------------------------------------------ persistence

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putBoolean("Staying", staying);
        dev.tc4port.thaumadditions.platform.TaPlatform.Active.get()
                .saveStackList(level(), tag, inventory);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.owner = tag.getUUID("Owner");
        }
        this.staying = tag.getBoolean("Staying");
        inventory.clear();
        dev.tc4port.thaumadditions.platform.TaPlatform.Active.get()
                .loadStackList(level(), tag, inventory);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 1.12 let its owner whack it around without killing it by accident.
        if (source.getEntity() instanceof Player player && ownedBy(player)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    /** 1.12 spilled the chest when it died; the drop hook was renamed in 1.21, so die is used. */
    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) {
            net.minecraft.world.Containers.dropContents(level(), blockPosition(), inventory);
            inventory.clear();
        }
        super.die(source);
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
        return ContainerHelper.removeItem(inventory, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(inventory, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory.set(slot, stack);
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return isAlive() && player.distanceToSqr(this) <= 64.0D;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public void clearContent() {
        inventory.clear();
    }

    /** The slots, for the menu that wraps them. */
    public Container container() {
        return this;
    }

    /** Places one where the item was used, facing the player who summoned it. */
    public static Chester summon(Level level, Player player, Vec3 at) {
        Chester chester = dev.tc4port.thaumadditions.registry.Content.CHESTER_ENTITY.get().create(level);
        if (chester == null) {
            return null;
        }
        chester.moveTo(at.x, at.y, at.z, player.getYRot(), 0.0F);
        chester.setOwner(player);
        level.addFreshEntity(chester);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.CHEST_OPEN, SoundSource.NEUTRAL, 0.7F, 1.2F);
        return chester;
    }

    /** Kept so the item can tell an empty one from a loaded one. */
    public boolean hasAnything() {
        return !isEmpty();
    }
}

