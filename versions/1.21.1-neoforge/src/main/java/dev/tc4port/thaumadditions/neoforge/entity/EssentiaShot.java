package dev.tc4port.thaumadditions.neoforge.entity;

import dev.tc4port.thaumadditions.entity.EssentiaShotBase;
import dev.tc4port.thaumcraft.api.aspect.AspectList;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** The 1.21.1 shot: {@code defineSynchedData} takes a builder here. */
public class EssentiaShot extends EssentiaShotBase {
    public EssentiaShot(EntityType<? extends EssentiaShotBase> type, Level level) {
        super(type, level);
    }

    public EssentiaShot(EntityType<? extends EssentiaShotBase> type, Level level, LivingEntity thrower,
                        AspectList payload) {
        super(type, level, thrower, payload);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }
}
