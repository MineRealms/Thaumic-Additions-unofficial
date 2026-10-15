package dev.tc4port.thaumadditions;

import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumadditions.aspect.TaAspect;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/**
 * The coloured dust the 1.12 machines threw while they worked.
 *
 * <p>1.12 spawned these through HammerCore's {@code FXDispatcher}, which had a particle class per
 * effect. The port uses the vanilla dust particle tinted with the same colour instead, which is a
 * smaller machine — no particle class, no client-side FX registry, and the same look.</p>
 *
 * <p>The helper is written so a block entity can call it from either side: on the server the
 * particle is broadcast, on the client it is spawned locally. That is what lets the machine code
 * stay free of {@code Dist} checks.</p>
 */
public final class TaFx {
    private TaFx() {
    }

    /** One dust mote of an aspect's own colour, drifting the given way. */
    public static void aspectDust(Level level, double x, double y, double z, AspectId aspect,
                                  double dx, double dy, double dz) {
        dust(level, x, y, z, TaAspect.color(aspect), dx, dy, dz);
    }

    /** One dust mote of a raw 0xRRGGBB colour. */
    public static void dust(Level level, double x, double y, double z, int rgb,
                            double dx, double dy, double dz) {
        Vector3f colour = new Vector3f(
                ((rgb >> 16) & 0xFF) / 255.0F,
                ((rgb >> 8) & 0xFF) / 255.0F,
                (rgb & 0xFF) / 255.0F);
        DustParticleOptions particle = new DustParticleOptions(colour, 1.0F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(particle, x, y, z, 1, dx, dy, dz, 0.0D);
        } else {
            level.addParticle(particle, x, y, z, dx, dy, dz);
        }
    }
}
