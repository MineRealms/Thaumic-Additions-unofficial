package dev.tc4port.thaumadditions.forge.entity;

import dev.tc4port.thaumadditions.entity.EssentiaShotBase;
import dev.tc4port.thaumcraft.api.aspect.AspectList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** The 1.20.1 shot: {@code defineSynchedData} takes no arguments here. */
public class EssentiaShot extends EssentiaShotBase {
    public EssentiaShot(EntityType<? extends EssentiaShotBase> type, Level level) {
        super(type, level);
    }

    public EssentiaShot(EntityType<? extends EssentiaShotBase> type, Level level, LivingEntity thrower,
                        AspectList payload) {
        super(type, level, thrower, payload);
    }

    @Override
    protected void defineSynchedData() {
    }
}
