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
import org.joml.Matrix3f;
import org.joml.Vector3f;

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
    /**
     * How far away a portal can be and still get a window.
     *
     * <p>Close, because this is when a portal is a thing being looked through rather than a thing
     * seen. Measured against the seal's own 0.4-block teleport reach, twelve blocks is a wide
     * margin: it is the range at which the far side is still legible on a one-block face.</p>
     */
    private static final double RANGE = 12.0D;

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
    /** Which seal the frame rendered for, and which way its face points. */
    private static BlockPos chosen;
    private static Direction chosenFace;
    private static long currentFrame = -1L;
    /** Why the last frame did or did not produce a window; for the probes, and cheap. */
    private static String lastReason = "not run";

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

    /**
     * The window's four corners in the world, ready to draw, or null when there is no window.
     *
     * <p>In world space and drawn from the frame's own stage, not from the seal's renderer. The
     * first attempt drew it there, in the seal's own pose, and it came out somewhere else: a block
     * entity renderer is handed a pose and inherits whatever transform is current, and a quad
     * drawn by hand in that moment does not land where the batched geometry beside it does. The
     * stage has the level's own transform, which is the one the far side was rendered with, so the
     * two agree by construction rather than by luck.</p>
     *
     * <p>Corners run bottom-left, bottom-right, top-right, top-left as seen by someone standing in
     * front of the face; the image's own origin is its bottom-left, so the two line up.</p>
     */
    public static float[][] corners() {
        if (texture < 0 || chosen == null || chosenFace == null) {
            return null;
        }
        Vec3 out = axis(chosenFace);
        Vec3 up = chosenFace.getAxis().isVertical()
                ? new Vec3(0.0D, 0.0D, -1.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = up.cross(out);
        // On the face, not in the middle of the block. Half a block out from the centre is the
        // plane the seal's own art is drawn on, and a hundredth further so the window is not
        // fighting the block for the same depth.
        Vec3 centre = chosen.getCenter().add(out.scale(0.5D + 0.005D));
        Vec3 halfRight = right.scale(0.5D);
        Vec3 halfUp = up.scale(0.5D);
        Vec3 lowNear = centre.subtract(halfRight).subtract(halfUp);
        Vec3 lowFar = centre.add(halfRight).subtract(halfUp);
        Vec3 highFar = centre.add(halfRight).add(halfUp);
        Vec3 highNear = centre.subtract(halfRight).add(halfUp);
        return new float[][] {
                {(float) lowNear.x, (float) lowNear.y, (float) lowNear.z, 0.0F, 1.0F},
                {(float) lowFar.x, (float) lowFar.y, (float) lowFar.z, 1.0F, 1.0F},
                {(float) highFar.x, (float) highFar.y, (float) highFar.z, 1.0F, 0.0F},
                {(float) highNear.x, (float) highNear.y, (float) highNear.z, 0.0F, 0.0F},
        };
    }

    /** True while the setting is on and nothing has gone wrong. */
    public static boolean enabled() {
        return !broken && renderer != null && TaClientConfig.portalWindow();
    }

    /** Called once per frame, before the level's block entities are drawn. */
    public static void renderFrame(float partialTick) {
        texture = -1;
        chosen = null;
        chosenFace = null;
        if (inside || !enabled()) {
            lastReason = inside ? "already inside" : "disabled";
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || minecraft.player == null) {
            lastReason = "no level or player";
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

        TileSeal near = watchedPortal(level, eye, camera.getLookVector());
        if (near == null) {
            lastReason = "no portal watched";
            return;
        }
        Level farLevel = minecraft.level;
        BlockPos farPos = near.portalBound();
        if (farPos == null || !farLevel.isLoaded(farPos)
                || farLevel.getBlockEntity(farPos) == null) {
            lastReason = "far end not there: " + farPos;
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
        chosen = near.getBlockPos();
        chosenFace = near.orientation();
        lastReason = "rendered for " + chosen + " -> " + farPos + " texture " + texture;
    }

    /**
     * The portal the camera is looking at, or null.
     *
     * <p>Looking at, not nearest to: there are always two portals in a pair, they are usually a few
     * blocks apart, and picking whichever is nearer makes the image hop from one to the other as
     * the player turns — which reads as the picture sliding around on its own. The one in front of
     * the camera, and best lined up with where it is pointing, is the one being looked through.</p>
     */
    private static TileSeal watchedPortal(Level level, Vec3 eye, org.joml.Vector3f look) {
        Vec3 gaze = new Vec3(look.x(), look.y(), look.z()).normalize();
        TileSeal best = null;
        double bestAngle = -1.0D;
        for (TileSeal seal : TileSeal.loaded()) {
            if (seal.getLevel() != level || seal.isRemoved()) {
                continue;
            }
            // Bound, and that is all. It is tempting to ask that the hole be open — there is
            // something pleasingly literal about a window only where there is a hole — but the
            // open hole is the state that carries things through, and the two compete: whatever
            // stands in the seal to hold it open is the thing it takes away. 1.12 drew the window
            // for a *viewer* rather than for an open hole, and a viewer is what a player looking at
            // one from a few blocks away is.
            if (seal.portalBound() == null) {
                continue;
            }
            Vec3 toPortal = seal.getBlockPos().getCenter().subtract(eye);
            double distance = toPortal.length();
            if (distance > RANGE || distance < 0.05D) {
                continue;
            }
            double alignment = toPortal.normalize().dot(gaze);
            if (alignment <= 0.0D) {
                continue;
            }
            if (alignment > bestAngle) {
                bestAngle = alignment;
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
        Vec3 normal = axis(nearFace);

        // The reflection through the near portal's plane. Applied to the camera's position and to
        // its direction together: a portal is a mirror, and half a mirror is not a picture of
        // anything.
        Vec3 offset = eye.subtract(near);
        Vec3 mirrored = offset.subtract(normal.scale(2.0D * offset.dot(normal)));
        Vec3 mirroredLook = look.subtract(normal.scale(2.0D * look.dot(normal)));

        // And then carried from the near portal's own frame to the far portal's. This is the part
        // that is easy to get wrong: any rotation at all takes one face's normal to the other's,
        // and they are not the same picture. What is wanted is the one that takes the near
        // portal's *right and up* to the far portal's, which is this, and only this.
        Matrix3f carry = frame(farFace).mul(frame(nearFace).transpose(), new Matrix3f());

        Vector3f position = carry.transform(mirrored.toVector3f(), new Vector3f())
                .add(far.toVector3f());
        Vector3f direction = carry.transform(mirroredLook.toVector3f(), new Vector3f());

        // A camera is placed by an entity, and Entity is abstract: a dropped item is the cheapest
        // concrete one, and it is never added to the level, so nothing can see it.
        Entity stand = new net.minecraft.world.entity.item.ItemEntity(level,
                position.x(), position.y(), position.z(), net.minecraft.world.item.ItemStack.EMPTY);
        stand.setPos(position.x(), position.y() - stand.getEyeHeight(), position.z());
        stand.setYRot((float) (Mth.atan2(-direction.x(), direction.z()) * 180.0F / (float) Math.PI));
        stand.setXRot((float) (Mth.atan2(-direction.y(), Math.sqrt(direction.x() * direction.x()
                + direction.z() * direction.z())) * 180.0F / (float) Math.PI));
        stand.setOldPosAndRot();

        Camera camera = new Camera();
        camera.setup(level, stand, false, false, partialTick);
        return camera;
    }

    /** One face's outward normal, as a vector. */
    private static Vec3 axis(Direction face) {
        return new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
    }

    /**
     * A portal's own frame: which way is right, which way is up, which way is out.
     *
     * <p>Out is the face's normal. Up is up — except for a portal in a floor or a ceiling, where
     * there is no up in the face and north is used instead, which is the same choice the model
     * rotator makes. Right is the one the other two imply.</p>
     */
    private static Matrix3f frame(Direction face) {
        Vec3 out = axis(face);
        Vec3 up = face.getAxis().isVertical()
                ? new Vec3(0.0D, 0.0D, -1.0D)
                : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = up.cross(out);
        // Columns: a local point (x, y, z) becomes x·right + y·up + z·out.
        return new Matrix3f(
                (float) right.x, (float) up.x, (float) out.x,
                (float) right.y, (float) up.y, (float) out.y,
                (float) right.z, (float) up.z, (float) out.z);
    }

    /** The shared offscreen buffer, sized to the window and made on first use. */
    private static RenderTarget target(Minecraft minecraft) {
        if (target == null) {
            target = renderer.newTarget(SIZE, SIZE);
            target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        }
        return target;
    }

    /** What the last frame did, for the probes. */
    public static String lastReason() {
        return lastReason;
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
