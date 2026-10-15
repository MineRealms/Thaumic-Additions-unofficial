package dev.tc4port.thaumadditions.entity;

import dev.tc4port.thaumcraft.api.aspect.AspectList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Builds one shot.
 *
 * <p>The concrete entity class is per node, so shared code cannot name it. This is the one thing
 * that has to cross back: a pistol in shared code needs to make one, and the node is the only side
 * that knows the class.</p>
 */
@FunctionalInterface
public interface EssentiaShotFactory {
    EssentiaShotBase create(Level level, LivingEntity thrower, AspectList payload);
}
