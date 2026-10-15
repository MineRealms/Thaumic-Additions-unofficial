package dev.tc4port.thaumadditions.neoforge.entity;

import dev.tc4port.thaumadditions.entity.ScytheProjectileBase;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** The 1.21.1 blade. */
public class MithminiteScythe extends ScytheProjectileBase {
    public MithminiteScythe(EntityType<? extends ScytheProjectileBase> type, Level level) {
        super(type, level);
    }

    public MithminiteScythe(EntityType<? extends ScytheProjectileBase> type, Level level,
                            LivingEntity thrower) {
        super(type, level, thrower);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }
}
