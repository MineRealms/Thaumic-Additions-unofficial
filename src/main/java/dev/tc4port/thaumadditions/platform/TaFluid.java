package dev.tc4port.thaumadditions.platform;

import java.util.function.Supplier;

import net.minecraft.world.level.material.FlowingFluid;

/**
 * Handles to one fluid's two objects — the one that stands still and the one that flows.
 *
 * <p>A fluid is four registrations that have to agree: the type, the fluid, the block that holds it
 * in the world, and the bucket that carries it. Only these two cannot be named from shared code —
 * {@link FlowingFluid} is vanilla, and so are the block and the bucket — so these two are what
 * {@link TaPlatform#fluid} hands back. The block and the bucket are built from {@link #source()} by
 * the shared content class.</p>
 *
 * <p>Both are suppliers rather than values because neither exists yet when the caller asks: they are
 * filled in when the loader opens its fluid registry, which happens before the block and the item
 * registries that read them.</p>
 */
public interface TaFluid {
    /** The still fluid. {@code FlowingFluid} because that is what a block and a bucket take. */
    Supplier<? extends FlowingFluid> source();

    /** The flowing fluid: what a source turns into as it spreads. */
    Supplier<? extends FlowingFluid> flowing();
}
