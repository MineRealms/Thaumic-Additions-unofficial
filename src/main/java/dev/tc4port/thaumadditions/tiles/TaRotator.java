package dev.tc4port.thaumadditions.tiles;

/**
 * 1.12 HammerCore's {@code FrictionRotator}: an angle that spins up while a machine works and coasts
 * down when it stops, with the smooth interpolation a renderer needs.
 *
 * <p>Upstream kept a degree, a speed and a friction, and the renderer asked for
 * {@code getActualRotation(partialTicks)} so that a machine running at twenty ticks a second still
 * looked like it was turning continuously between ticks. The port's machines used to hold a bare
 * float and add to it, which reads fine at sixty frames a second and stutters at twenty, so every
 * machine with a moving part now holds one of these instead.</p>
 *
 * <p>{@link #update()} is the whole tick: the previous angle is remembered for interpolation, the
 * angle advances by the current speed, and the speed decays by the friction. {@link #speedup} is
 * what a machine calls while it has work, and nothing else touches the angle, so a stopped machine
 * winds down on its own exactly as 1.12's did.</p>
 */
public final class TaRotator {
    /** 1.12's default friction for most machines, and what the bore and the charger used. */
    public static final float DEFAULT_FRICTION = 0.25F;

    private float friction = DEFAULT_FRICTION;
    private float degree;
    private float previousDegree;
    private float speed;

    /** A rotator that starts at a random angle, so two machines never spin in lockstep. */
    public static TaRotator randomised(java.util.Random random) {
        TaRotator rotator = new TaRotator();
        rotator.degree = random.nextFloat() * 360.0F;
        rotator.previousDegree = rotator.degree;
        return rotator;
    }

    public void setFriction(float friction) {
        this.friction = friction;
    }

    /** Advances one tick. */
    public void update() {
        previousDegree = degree;
        degree += speed;
        speed *= 1.0F - friction;
        if (Math.abs(speed) < 0.001F) {
            speed = 0.0F;
        }
    }

    /** Adds spin, the way 1.12's machines did on every tick they had work to do. */
    public void speedup(float amount) {
        speed += amount;
    }

    /** Slows the coast-down, used by the charged machine's client FX. */
    public void slowdown(float amount) {
        speed = Math.max(0.0F, speed - amount);
    }

    /** The angle to draw at, interpolated between the last two ticks. */
    public float actualRotation(float partialTicks) {
        return previousDegree + (degree - previousDegree) * partialTicks;
    }

    public float degree() {
        return degree;
    }

    public float speed() {
        return speed;
    }
}
