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
        SealManager.register(new SealCombination(aspect("praecantatio"), aspect("iter"), aspect("alienis"),
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
     * 1.12 {@code SealPortal}: pairs with another portal seal and moves items between them.
     *
     * <p>Upstream kept the pairing in the seal's own saved data. The port does too, but moves only
     * items — the chunk-loading half of the original is a separate seal and is not ported, because
     * 1.20.1's forced chunks are a server setting rather than a block's business.</p>
     */
    public static final class Portal extends SealInstance {
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
            if (level == null || level.isClientSide || !atTickRate(level, 20) || powered(level)) {
                return;
            }
            BlockPos target = seal.portalTarget();
            if (target == null || target.equals(seal.getBlockPos())) {
                return;
            }
            List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class,
                    new net.minecraft.world.phys.AABB(seal.getBlockPos()).inflate(1.5D));
            for (ItemEntity item : items) {
                item.teleportTo(target.getX() + 0.5D, target.getY() + 1.0D, target.getZ() + 0.5D);
                return;
            }
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
