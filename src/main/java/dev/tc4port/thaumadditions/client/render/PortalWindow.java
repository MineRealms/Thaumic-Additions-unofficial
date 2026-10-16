package dev.tc4port.thaumadditions.client.render;

import java.util.List;

import com.mojang.blaze3d.pipeline.RenderTarget;

import dev.tc4port.thaumadditions.TaClientConfig;
import dev.tc4port.thaumadditions.tiles.TileSeal;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * What a portal shows: the far side of the world, rendered a second time from where the far seal
 * stands.
 *
 * <p>1.12 rendered the far side into a framebuffer, fenced it with a stencil, and drew that as the
 * portal's surface, so a bound portal was a hole rather than a picture of one. It could do that
 * because it drew in immediate mode from inside the seal's renderer. Modern Minecraft cannot: a
 * block entity renderer runs inside the level's own render pass, and re-entering that pass from
 * inside it would leave the dispatchers, the frustum and the buffers set up for the wrong camera.
 * The same pass is therefore re-entered from the frame's <em>stage</em> events instead, which fire
 * between passes rather than inside one, and what was disturbed is put back afterwards.</p>
 *
 * <p>This is the expensive thing the mod does — a second render of the world every frame — so it
 * is behind {@link TaClientConfig#PORTAL_WINDOW} and off by a word in a config file. It also
 * follows upstream's own habit of giving up quietly: 1.12 wrapped its framebuffer setup in a catch
 * and disabled the feature for the session when it failed, and so does this.</p>
 */
public final class PortalWindow {
    /** Upstream rendered into a 512-square buffer, and so does this. */
    private static final int SIZE = 512;
    /** How far away a portal can be and still get a window; beyond this it is a speck. */
    private static final double RANGE = 32.0D;

    /** The node's half: making the buffer, entering the level's pass, and restoring afterwards. */
    public interface WorldRenderer {
        /** An offscreen buffer of the given size. Vanilla's own is abstract, and each node has one. */
        RenderTarget newTarget(int width, int height);

        /** Renders the level from {@code camera} into {@code into}. */
        void renderLevelInto(Camera camera, float partialTick, RenderTarget into);
    }

    private static RenderTarget target;
    private static WorldRenderer renderer;
    private static boolean broken;
    /**
     * Whether a second pass is already being run.
     *
     * <p>The pass being entered dispatches the same frame events that called this, so without the
     * flag the second pass asks for a third, and a fourth, until the stack ends. This is the whole
     * of the re-entrancy problem, and it is one boolean wide.</p>
     */
    private static boolean inside;
    private static int texture = -1;
    private static long currentFrame = -1L;

    private PortalWindow() {
    }

    /** Called once by the node during client setup. */
    public static void install(WorldRenderer worldRenderer) {
        renderer = worldRenderer;
    }

    /**
     * The portal's own view's texture id, or -1 when there is nothing to show.
     *
     * <p>Only the portal nearest the camera gets a window, and only while its hole is open: a
     * portal with nothing about to fall through it is a shut plate, and rendering a world for it
     * would be a frame's work for something nobody can see.</p>
     */
    public static int texture() {
        return texture;
    }

    /** True while the setting is on and nothing has gone wrong. */
    public static boolean enabled() {
        return !broken && renderer != null && TaClientConfig.portalWindow();
    }

    /** Called once per frame, before the level's block entities are drawn. */
    public static void renderFrame(float partialTick) {
        texture = -1;
        if (inside || !enabled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return;
        }
        // Once a frame, not once a portal: two portals in one frame would be two extra renders.
        currentFrame++;
        inside = true;
        try {
            render(minecraft, level, partialTick);
        } catch (Throwable failure) {
            // Upstream's own answer to this: say so once and stop doing it.
            broken = true;
            texture = -1;
            dev.tc4port.thaumadditions.ThaumAdditions.LOGGER.warn(
                    "the portal window failed and has been turned off for this session", failure);
        } finally {
            inside = false;
        }
    }

    private static void render(Minecraft minecraft, Level level, float partialTick) {
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 eye = camera.getPosition();

        TileSeal near = nearestBoundPortal(level, eye);
        if (near == null) {
            return;
        }
        Level farLevel = minecraft.level;
        BlockPos farPos = near.portalBound();
        if (farPos == null || !farLevel.isLoaded(farPos)
                || farLevel.getBlockEntity(farPos) == null) {
            return;
        }
        Direction farFace = farLevel.getBlockState(farPos)
                .getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);

        var look = camera.getLookVector();
        Camera farCamera = mirroredCamera(minecraft, level, eye,
                new Vec3(look.x(), look.y(), look.z()),
                near.getBlockPos(), near.orientation(), farPos, farFace, partialTick);
        if (farCamera == null) {
            return;
        }

        RenderTarget into = target(minecraft);
        into.clear(Minecraft.ON_OSX);
        renderer.renderLevelInto(farCamera, partialTick, into);
        texture = into.getColorTextureId();
    }

    /** The closest seal near the camera that is bound and open, or null. */
    private static TileSeal nearestBoundPortal(Level level, Vec3 eye) {
        TileSeal best = null;
        double bestDistance = RANGE * RANGE;
        for (TileSeal seal : TileSeal.loaded()) {
            if (seal.getLevel() != level || seal.isRemoved()) {
                continue;
            }
            if (seal.portalBound() == null || seal.holeTarget() <= 0) {
                continue;
            }
            double distance = seal.getBlockPos().distToCenterSqr(eye);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = seal;
            }
        }
        return best;
    }

    /**
     * The camera on the far side: the viewer's, mirrored through the near portal's plane and then
     * carried to the far portal's own frame.
     *
     * <p>A reflection through a plane, composed with the rotation that takes one portal's outward
     * face to the other's, is the whole of it. What makes it right rather than nearly right is that
     * it is applied to the camera's <em>position and direction together</em>, so the image the far
     * camera sees, drawn on the near portal's face, is what the viewer would see if the two blocks
     * of world were joined.</p>
     */
    private static Camera mirroredCamera(Minecraft minecraft, Level level, Vec3 eye, Vec3 look,
                                         BlockPos nearPos, Direction nearFace,
                                         BlockPos farPos, Direction farFace, float partialTick) {
        Vec3 near = Vec3.atCenterOf(nearPos);
        Vec3 far = Vec3.atCenterOf(farPos);
        Vec3 normal = new Vec3(nearFace.getStepX(), nearFace.getStepY(), nearFace.getStepZ());

        Vec3 offset = eye.subtract(near);
        // Reflection through the near portal's plane.
        Vec3 mirrored = offset.subtract(normal.scale(2.0D * offset.dot(normal)));
        Vec3 mirroredLook = look.subtract(normal.scale(2.0D * look.dot(normal)));

        // The rotation carrying the near portal's outward face onto the far portal's.
        Vec3 from = normal;
        Vec3 to = new Vec3(farFace.getStepX(), farFace.getStepY(), farFace.getStepZ());
        Vec3 position = far.add(rotate(mirrored, from, to));
        Vec3 direction = rotate(mirroredLook, from, to);

        // A camera is placed by an entity, and Entity is abstract: a dropped item is the cheapest
        // concrete one, and it is never added to the level, so nothing can see it.
        Entity stand = new net.minecraft.world.entity.item.ItemEntity(level,
                position.x, position.y, position.z, net.minecraft.world.item.ItemStack.EMPTY);
        stand.setPos(position.x, position.y - stand.getEyeHeight(), position.z);
        stand.setYRot((float) (Mth.atan2(-direction.x, direction.z) * 180.0F / (float) Math.PI));
        stand.setXRot((float) (Mth.atan2(-direction.y, Math.sqrt(direction.x * direction.x
                + direction.z * direction.z)) * 180.0F / (float) Math.PI));
        stand.setOldPosAndRot();

        Camera camera = new Camera();
        camera.setup(level, stand, false, false, partialTick);
        return camera;
    }

    /** Rotates {@code vector} by the shortest rotation taking {@code from} to {@code to}. */
    private static Vec3 rotate(Vec3 vector, Vec3 from, Vec3 to) {
        Vec3 a = from.normalize();
        Vec3 b = to.normalize();
        Vec3 axis = a.cross(b);
        double sin = axis.length();
        double cos = a.dot(b);
        if (sin < 1.0E-6D) {
            // Already aligned, or exactly opposed: the first needs nothing, the second is a flip
            // about any axis perpendicular to it, and the sign then does not matter.
            return cos > 0.0D ? vector : vector.scale(-1.0D).add(a.scale(2.0D * vector.dot(a)));
        }
        Vec3 unit = axis.scale(1.0D / sin);
        double angle = Math.atan2(sin, cos);
        return rotateAbout(vector, unit, angle);
    }

    /** Rodrigues' rotation formula. */
    private static Vec3 rotateAbout(Vec3 vector, Vec3 axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return vector.scale(cos)
                .add(axis.cross(vector).scale(sin))
                .add(axis.scale(axis.dot(vector) * (1.0D - cos)));
    }

    /** The shared offscreen buffer, sized to the window and made on first use. */
    private static RenderTarget target(Minecraft minecraft) {
        if (target == null) {
            target = renderer.newTarget(SIZE, SIZE);
            target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        }
        return target;
    }

    /** For the frame gate; a portal is only worth a second render once per frame. */
    public static long frame() {
        return currentFrame;
    }

    /** Every loaded seal, for the caller that wants to know which are worth looking at. */
    public static List<TileSeal> loadedSeals() {
        return List.copyOf(TileSeal.loaded());
    }
}
