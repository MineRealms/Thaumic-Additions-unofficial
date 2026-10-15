package dev.tc4port.thaumadditions.seals;

import dev.tc4port.thaumadditions.tiles.TileSeal;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * The seven seals, and the symbols that make each one.
 *
 * <p>1.12's symbols map onto TC4R's aspects by meaning rather than by name: its {@code PLANT} is
 * {@code herba}, its {@code VOID} is {@code vacuos}, its {@code TOOL} is {@code instrumentum}. The
 * combinations and their descriptions are upstream's exactly, including the wildcards.</p>
 *
 * <p>Each seal searches the same box — four blocks out (two when it points up), five either side,
 * and a handful of tries — and each does one thing to whatever it finds. The 1.12 FX are gone; they
 * were HammerCore's own particles and had no state behind them.</p>
 */
public final class TaSeals {
    private TaSeals() {
    }

    /** Registers all seven. Called once, from content setup. */
    public static void register() {
        SealManager.register(new SealCombination(aspect("aqua"), null, null,
                "seal.thaumadds.fertilesoil"), WaterHydrate::new);
        SealManager.register(new SealCombination(aspect("terra"), aspect("terra"), aspect("terra"),
                "seal.thaumadds.createsoil"), TillSoil::new);
        SealManager.register(new SealCombination(aspect("victus"), aspect("herba"), null,
                "seal.thaumadds.bonemealcrops"), FertilizeCrops::new);
        SealManager.register(new SealCombination(aspect("instrumentum"), aspect("herba"), null,
                "seal.thaumadds.harvestcrops1"), HarvestCrops::new);
        SealManager.register(new SealCombination(aspect("instrumentum"), aspect("herba"), aspect("ordo"),
                "seal.thaumadds.harvestcrops2"), ReplantCrops::new);
        SealManager.register(new SealCombination(aspect("vacuos"), aspect("vacuos"), aspect("vacuos"),
                "seal.thaumadds.pickupitems"), PickupItems::new);
        // 1.12's portal overrode the match rule rather than naming three symbols: magic, then air,
        // then anything — and the anything is the network key that decides who pairs with whom.
        SealManager.register(new SealCombination(aspect("praecantatio"), aspect("aer"), null,
                "seal.thaumadds.portal"), Portal::new);
    }

    private static AspectId aspect(String id) {
        try {
            AspectId parsed = AspectId.parse(id);
            return dev.tc4port.thaumcraft.api.aspect.AspectApi.contains(parsed) ? parsed : null;
        } catch (RuntimeException unknown) {
            return null;
        }
    }

    // ------------------------------------------------------------------ the seals

    /** 1.12 {@code SealWaterHydrate}: wets farmland in front of the seal. Every tick, ten at a time. */
    public static final class WaterHydrate extends SealInstance {
        /** 1.12 hydrates up to ten blocks, keeping the driest. */
        private static final int TRIES = 10;

        public WaterHydrate(TileSeal seal) {
            super(seal);
        }

        @Override
        public String descriptionKey() {
            return "seal.thaumadds.fertilesoil";
        }

        @Override
        public void tick() {
            Level level = seal.getLevel();
            if (level == null || level.isClientSide || !atTickRate(level, 20) || powered(level)) {
                return;
            }
            for (int attempt = 0; attempt < TRIES; attempt++) {
                BlockPos spot = randomSpot(level, 3, -1);
                BlockState state = level.getBlockState(spot);
                if (!state.is(Blocks.FARMLAND)) {
                    continue;
                }
                int moisture = state.getValue(net.minecraft.world.level.block.FarmBlock.MOISTURE);
                if (moisture >= 7) {
                    continue;
                }
                level.setBlock(spot, state.setValue(
                        net.minecraft.world.level.block.FarmBlock.MOISTURE, 7), 2);
                maybePollute(level);
                return;
            }
        }
    }

    /** 1.12 {@code SealTillSoil}: turns dirt in front of the seal into farmland. */
    public static final class TillSoil extends SealInstance {
        private static final int TRIES = 40;

        public TillSoil(TileSeal seal) {
            super(seal);
        }

        @Override
        public String descriptionKey() {
            return "seal.thaumadds.createsoil";
        }

        @Override
        public void tick() {
            Level level = seal.getLevel();
            if (level == null || level.isClientSide || !atTickRate(level, 10) || powered(level)) {
                return;
            }
            for (int attempt = 0; attempt < TRIES; attempt++) {
                BlockPos spot = randomSpot(level, 3, 0);
                BlockState state = level.getBlockState(spot);
                boolean bare = state.is(Blocks.DIRT)
                        || (state.is(Blocks.GRASS_BLOCK) && level.getBlockState(spot.above()).isAir());
                if (!bare) {
                    continue;
                }
                maybePollute(level);
                level.setBlock(spot, Blocks.FARMLAND.defaultBlockState(), 2);
                return;
            }
        }
    }

    /** 1.12 {@code SealFertilizeCrops}: bonemeals whatever in front of the seal can be grown. */
    public static final class FertilizeCrops extends SealInstance {
        private static final int TRIES = 20;

        public FertilizeCrops(TileSeal seal) {
            super(seal);
        }

        @Override
        public String descriptionKey() {
            return "seal.thaumadds.bonemealcrops";
        }

        @Override
        public void tick() {
            Level level = seal.getLevel();
            if (level == null || level.isClientSide || !atTickRate(level, 50) || powered(level)) {
                return;
            }
            if (!(level instanceof ServerLevel)) {
                return;
            }
            ItemStack meal = new ItemStack(Items.BONE_MEAL);
            for (int attempt = 0; attempt < TRIES; attempt++) {
                BlockPos spot = randomSpot(level, 2, -1);
                // applyBonemeal answers whether the spot took it, which is the check 1.12 made
                // separately; the two versions disagree about that check's signature.
                if (BoneMealItem.applyBonemeal(meal.copy(), level, spot, null)) {
                    return;
                }
            }
        }
    }

    /** 1.12 {@code SealHarvestCrops1}: breaks whatever in front of the seal has stopped growing. */
    public static final class HarvestCrops extends SealInstance {
        private static final int TRIES = 50;

        public HarvestCrops(TileSeal seal) {
            super(seal);
        }

        @Override
        public String descriptionKey() {
            return "seal.thaumadds.harvestcrops1";
        }

        @Override
        public void tick() {
            Level level = seal.getLevel();
            if (level == null || level.isClientSide || !atTickRate(level, 20) || powered(level)) {
                return;
            }
            for (int attempt = 0; attempt < TRIES; attempt++) {
                BlockPos spot = randomSpot(level, 3, 0);
                BlockState state = level.getBlockState(spot);
                // 1.12 harvested whatever could no longer grow, which is a ripe crop.
                if (!isRipe(state)) {
                    continue;
                }
                maybePollute(level);
                level.destroyBlock(spot, true);
                return;
            }
        }
    }

    /** 1.12 {@code SealHarvestCrops2}: the same, and puts the seed back. */
    public static final class ReplantCrops extends SealInstance {
        private static final int TRIES = 50;

        public ReplantCrops(TileSeal seal) {
            super(seal);
        }

        @Override
        public String descriptionKey() {
            return "seal.thaumadds.harvestcrops2";
        }

        @Override
        public void tick() {
            Level level = seal.getLevel();
            if (level == null || level.isClientSide || !atTickRate(level, 20) || powered(level)) {
                return;
            }
            for (int attempt = 0; attempt < TRIES; attempt++) {
                BlockPos spot = randomSpot(level, 3, 0);
                BlockState state = level.getBlockState(spot);
                if (!(state.getBlock() instanceof CropBlock crop)
                        || !crop.isMaxAge(state)) {
                    continue;
                }
                ItemStack seed = seedFor(crop);
                maybePollute(level);
                level.destroyBlock(spot, true);
                if (!seed.isEmpty() && level.getBlockState(spot).isAir()) {
                    // 1.12 replanted with a fake player's right-click; the port plants directly.
                    level.setBlock(spot, crop.getStateForAge(0), 2);
                }
                return;
            }
        }

        /** The item this crop grows from, which is what goes back in the ground. */
        private static ItemStack seedFor(CropBlock crop) {
            if (crop == Blocks.WHEAT.defaultBlockState().getBlock() || crop.asItem() == Items.WHEAT_SEEDS) {
                return new ItemStack(Items.WHEAT_SEEDS);
            }
            return crop.asItem() == Items.AIR ? ItemStack.EMPTY : new ItemStack(crop.asItem());
        }
    }

    /** 1.12 {@code SealPickup}: drags loose items in front of the seal into it. */
    public static final class PickupItems extends SealInstance {
        /** 1.12 pulled anything within two blocks of the seal straight through the block. */
        private static final double REACH_SQUARED = 4.0D;

        public PickupItems(TileSeal seal) {
            super(seal);
        }

        @Override
        public String descriptionKey() {
            return "seal.thaumadds.pickupitems";
        }

        @Override
        public void tick() {
            Level level = seal.getLevel();
            if (level == null || level.isClientSide || !atTickRate(level, 10) || powered(level)) {
                return;
            }
            BlockPos centre = seal.getBlockPos();
            List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class,
                    new net.minecraft.world.phys.AABB(centre).inflate(2.0D));
            for (ItemEntity item : items) {
                if (item.distanceToSqr(centre.getX() + 0.5D, centre.getY() + 0.5D, centre.getZ() + 0.5D)
                        > REACH_SQUARED) {
                    continue;
                }
                // 1.12 destroyed the entity and dropped its stack at the seal; the port does the same.
                BlockPos at = seal.getBlockPos();
                ItemStack contents = item.getItem().copy();
                item.discard();
                Block.popResource(level, at, contents);
                return;
            }
        }
    }

    /**
     * 1.12 {@code SealPortal}: a hole in the world that anything walking into comes out of the far
     * side of, at another portal seal.
     *
     * <p>The pairing is upstream's: two portal seals belong to the same network when they share
     * their third symbol and their owner, and right-clicking one walks it through the members of
     * that network, so a player builds a pair by clicking each until they point at each other.
     * Nothing binds by item — the third symbol is the network key.</p>
     *
     * <p>The seal also holds its own chunk loaded while it is bound, which is what lets a portal
     * work over a distance longer than the view. 1.12 wrote its own chunk loader for that; the port
     * asks the server directly, which is the one place it uses a loader facility where upstream
     * built one.</p>
     */
    public static final class Portal extends SealInstance {
        /** Every portal seal whose chunk is loaded, so a pair can find each other. */
        private static final java.util.Map<net.minecraft.resources.ResourceKey<Level>,
                java.util.List<Portal>> ACTIVE = new java.util.HashMap<>();

        /** 1.12 teleported anything whose box met the seal's, grown by this. */
        private static final double CONTACT = 0.4D;
        /** 1.12 waited five ticks after a binding before it would carry anything. */
        private static final int BIND_COOLDOWN = 5;
        /** How far below the target's floor a traveller arrives, before the air check lifts them. */
        private static final double ARRIVAL_DROP = 1.0D;

        /** Where this portal leads, or {@code null} while it has not been paired. */
        private net.minecraft.core.BlockPos bound;
        private int cooldown;
        /** Whether this seal has forced its own chunk, so it only ever relaxes what it forced. */
        private boolean holding;

        public Portal(TileSeal seal) {
            super(seal);
        }

        @Override
        public String descriptionKey() {
            return "seal.thaumadds.portal";
        }

        @Override
        public void tick() {
            Level level = seal.getLevel();
            if (level == null || level.isClientSide) {
                return;
            }
            join(level);
            if (cooldown > 0) {
                --cooldown;
            }
            if (bound != null && !holding && level instanceof ServerLevel server) {
                hold(server, seal.getBlockPos(), true);
            }
        }

        /**
         * Registers this seal so its partner can find it. Called every tick, so it checks first:
         * without the check the list would grow by one entry per tick per portal.
         */
        private void join(Level level) {
            List<Portal> peers = ACTIVE.computeIfAbsent(level.dimension(),
                    key -> new java.util.ArrayList<>());
            if (!peers.contains(this)) {
                peers.add(this);
            }
        }

        /** Forces or relaxes this seal's chunk, remembering that it did. */
        private void hold(ServerLevel server, net.minecraft.core.BlockPos pos, boolean force) {
            net.minecraft.world.level.ChunkPos chunk = new net.minecraft.world.level.ChunkPos(pos);
            server.setChunkForced(chunk.x, chunk.z, force);
            holding = force;
        }

        /** The other portal seals this one may lead to: same network, same owner, not itself. */
        private List<Portal> network() {
            Level level = seal.getLevel();
            List<Portal> peers = level == null ? null : ACTIVE.get(level.dimension());
            if (peers == null) {
                return List.of();
            }
            List<Portal> out = new java.util.ArrayList<>();
            for (Portal other : new java.util.ArrayList<>(peers)) {
                if (other == this || other.seal.isRemoved() || other.seal.getLevel() == null) {
                    continue;
                }
                if (!java.util.Objects.equals(other.seal.getOwner(), seal.getOwner())) {
                    continue;
                }
                if (!java.util.Objects.equals(other.seal.symbol(2), seal.symbol(2))) {
                    continue;
                }
                out.add(other);
            }
            return out;
        }

        /** 1.12's {@code onSealActivated}: step the binding to the next member of the network. */
        @Override
        public boolean onSealActivated(Level level, net.minecraft.core.BlockPos pos,
                                       net.minecraft.world.entity.player.Player player) {
            List<Portal> peers = network();
            if (peers.isEmpty()) {
                return false;
            }
            int current = -1;
            for (int index = 0; index < peers.size(); index++) {
                net.minecraft.core.BlockPos theirBound = peers.get(index).bound;
                if (theirBound != null && theirBound.equals(seal.getBlockPos())) {
                    current = index;
                    break;
                }
            }
            Portal next = peers.get((current + 1) % peers.size());
            this.bound = next.seal.getBlockPos();
            next.bound = seal.getBlockPos();
            this.cooldown += BIND_COOLDOWN;
            next.cooldown += BIND_COOLDOWN;
            seal.sync();
            next.seal.sync();
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            return true;
        }

        /** 1.12's {@code onEntityCollisionWithSeal}: carry whatever steps into the hole. */
        @Override
        public void onEntityCollisionWithSeal(Level level, net.minecraft.core.BlockPos pos,
                                              net.minecraft.world.entity.Entity entity) {
            if (level.isClientSide || bound == null || cooldown > 0) {
                return;
            }
            var shape = seal.getBlockState().getShape(level, pos).bounds().move(pos).inflate(CONTACT);
            if (!entity.getBoundingBox().intersects(shape)) {
                return;
            }
            if (!(level.getBlockEntity(bound) instanceof TileSeal target)
                    || !(target.instance() instanceof Portal portal)) {
                // The far end has gone; forget it rather than pointing at nothing.
                this.bound = null;
                return;
            }
            net.minecraft.core.Direction out = target.orientation();
            net.minecraft.core.BlockPos landing = bound.relative(out);
            double x = landing.getX() + 0.5D;
            double y = landing.getY() - ARRIVAL_DROP;
            double z = landing.getZ() + 0.5D;
            if (!level.getBlockState(net.minecraft.core.BlockPos.containing(x, y, z)).isAir()) {
                y += 1.0D;
            }
            entity.teleportTo(x, y, z);
            // The traveller comes out facing away from the far portal, as upstream had them.
            entity.setYRot(switch (out) {
                case NORTH -> 180.0F;
                case SOUTH -> 0.0F;
                case WEST -> 90.0F;
                case EAST -> 270.0F;
                default -> entity.getYRot();
            });
            this.cooldown += BIND_COOLDOWN;
            portal.cooldown += BIND_COOLDOWN;
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.4F);
        }

        @Override
        public void onSealBreak() {
            Level level = seal.getLevel();
            if (bound != null && level instanceof ServerLevel server) {
                hold(server, seal.getBlockPos(), false);
            }
            List<Portal> peers = level == null ? null : ACTIVE.get(level.dimension());
            if (peers == null) {
                return;
            }
            peers.remove(this);
            for (Portal other : peers) {
                if (seal.getBlockPos().equals(other.bound)) {
                    other.bound = null;
                    if (other.seal.getLevel() instanceof ServerLevel otherServer) {
                        other.hold(otherServer, other.seal.getBlockPos(), false);
                    }
                }
            }
        }

        @Override
        public void readFromNbt(net.minecraft.nbt.CompoundTag tag) {
            this.bound = tag.contains("Bound")
                    ? net.minecraft.core.BlockPos.of(tag.getLong("Bound")) : null;
        }

        @Override
        public net.minecraft.nbt.CompoundTag writeToNbt(net.minecraft.nbt.CompoundTag tag) {
            if (bound != null) {
                tag.putLong("Bound", bound.asLong());
            }
            return tag;
        }
    }

    /**
     * Whether a crop has stopped growing, which is what 1.12 harvested.
     *
     * <p>Asked of the state rather than of the block, because the block interface that answered it
     * changed shape between the two versions this port targets.</p>
     */
    private static boolean isRipe(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        for (var property : state.getProperties()) {
            if (property instanceof net.minecraft.world.level.block.state.properties.IntegerProperty age
                    && "age".equals(property.getName())
                    && state.getValue(age) >= java.util.Collections.max(age.getPossibleValues())) {
                return true;
            }
        }
        return false;
    }
}
