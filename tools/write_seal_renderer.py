# Writes the seal's block entity renderer into both nodes.
#
# Two things are drawn on a seal's face, and 1.12 drew them from two places:
#
#   * The pictograms (1.12 TESRSeal): three of them, one per symbol slot, each tinted with the
#     colour of the aspect in that slot, each turned a third of a turn from the last, the third
#     turning slowly. They are the mod's own three textures, not the aspects' pictures, which is
#     why the tint is what tells one seal from another.
#
#   * The hole (1.12 TARSealRenders.renderPortal): six sheets of the mod's portal art stacked on
#     the face, each a step smaller than the last and turned further, opening to five times its
#     closed size as something walks in. One of the six is the aspect's colour and one is the
#     art's own; see the constants below.
#
# The tint is why this is per node: a model part cannot be coloured, so the quads are written out
# by hand, and the two versions renamed the vertex builder's methods. The placement and the maths
# are the same on both, and are the numbers 1.12 used.
#
# All five vertex elements have to be filled, the overlay beside the light included: a vertex that
# skips it throws "Not filled all elements of the vertex" when the buffer is drawn.
import io

FORGE_VERTEX = '''            consumer.vertex(pose.last().pose(), x, y, z)
                    .color(red, green, blue, alpha)
                    .uv(u, v)
                    .overlayCoords(overlay)
                    .uv2(light)
                    .normal(pose.last().normal(), 0.0F, 0.0F, 1.0F)
                    .endVertex();'''

NEO_VERTEX = '''            consumer.addVertex(pose.last(), x, y, z)
                    .setColor(red, green, blue, alpha)
                    .setUv(u, v)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(pose.last(), 0.0F, 0.0F, 1.0F);'''

TEMPLATE = '''package dev.tc4port.thaumadditions.%(pkg)s.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import dev.tc4port.thaumadditions.aspect.TaAspect;
import dev.tc4port.thaumadditions.client.render.MachineRenderers;
import dev.tc4port.thaumadditions.seals.SealInstance;
import dev.tc4port.thaumadditions.tiles.TileSeal;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1.12 {@code TESRSeal} and {@code TARSealRenders}: what stands on a seal's face.
 *
 * <p>Two layers of art, both drawn in the face's own plane. The pictograms are static — one per
 * symbol slot, tinted with that slot's aspect and turned a third of a turn from the slot before
 * it, with the last one turning slowly. The hole is the portal seal's, and opens when something is
 * standing in the seal: six sheets of the mod's portal art, each reaching a step further than the
 * last and turned further, the whole stack growing from nothing as the hole widens.</p>
 *
 * <p>What is <em>not</em> here is 1.12's window: when a portal had a viewer on the far side,
 * upstream rendered the far side of the world into a framebuffer, fenced it with a stencil and
 * drew that as the portal's surface, so a portal was a true hole in the world. That is a second
 * pass of the whole level through another camera, and no public API in either target offers one;
 * it also needed an entity the port does not have ({@code EntitySealViewer}). The hole drawn here
 * is the depth-less sheet 1.12 fell back to whenever the window was off — which is what its own
 * {@code ConfigsTAR.portalGfx} switch existed to turn off.</p>
 */
public class SealRenderer implements BlockEntityRenderer<TileSeal> {
    /** One pictogram per slot, in the order the slots are filled. */
    private static final ResourceLocation[] SYMBOLS = {
            ResourceLocation.tryParse("thaumadds:textures/misc/seal_0.png"),
            ResourceLocation.tryParse("thaumadds:textures/misc/seal_1.png"),
            ResourceLocation.tryParse("thaumadds:textures/misc/seal_2.png"),
    };

    /** The portal art, and the six sheets it makes: 1.12's own order, with one sheet tinted. */
    private static final ResourceLocation PORTAL = ResourceLocation.tryParse("thaumadds:textures/misc/portal.png");
    private static final ResourceLocation PORTAL_OWN = ResourceLocation.tryParse("thaumadds:textures/misc/portal2.png");
    private static final ResourceLocation PORTAL_GREY = ResourceLocation.tryParse("thaumadds:textures/misc/portal2_nc.png");
    private static final ResourceLocation[] SHEETS = {
            PORTAL, PORTAL, PORTAL, PORTAL_GREY, PORTAL_OWN, PORTAL};
    /** The one sheet that takes the sealing aspect's colour; the rest are drawn as they come. */
    private static final int TINTED_SHEET = 3;

    /** 1.12's {@code 2 / 16}: the pictograms sit this far off the face. */
    private static final float OFFSET = 2.0F / 16.0F;
    /** 1.12's {@code 0.125}: the whole group sits this far up the face. */
    private static final float LIFT = 0.125F;
    /** 1.12's {@code speed}: the third pictogram's turn, in degrees a tick. */
    private static final float SPIN = 0.25F;

    /** 1.12's {@code 0.13} and {@code -0.04}: where the sheets stand on the face. */
    private static final double PORTAL_LIFT = 0.13D;
    private static final double PORTAL_DEPTH = -0.04D;
    /** 1.12 turned each sheet faster than the last by this much of the hole's opening. */
    private static final float PORTAL_SPIN = 14.0F;
    /**
     * How far the widest sheet reaches at full open, as a fraction of the face.
     *
     * <p>1.12's own numbers put the outermost sheet at twice the face's half-width, which overflows
     * the block on every side. Nothing here can check what that looked like, so the stack is scaled
     * down so that the widest sheet at full open exactly fills the face and no more.</p>
     */
    private static final float FACE = 0.5F;
    private static final float REACH_IN_1_12 = 2.0F;
    private static final float PORTAL_SCALE = FACE / REACH_IN_1_12;

    public SealRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TileSeal seal, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        pose.pushPose();
        MachineRenderers.orient(pose, seal.orientation().getOpposite());
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.translate(0.0D, -OFFSET, 0.0D);

        // The window onto the far side is not drawn here. It belongs to the frame rather than to
        // this block's renderer — see PortalWindow.corners — and drawing it from here put it
        // somewhere else entirely, because a block entity renderer is handed a pose and inherits
        // whatever transform is current.

        portal(seal, partialTick, pose, buffers, light, overlay);
        pictograms(seal, partialTick, pose, buffers, light, overlay);

        pose.popPose();
    }

    /** 1.12 {@code TARSealRenders.renderPortal}: the hole, as six sheets of the portal art. */
    private static void portal(TileSeal seal, float partialTick, PoseStack pose,
                               MultiBufferSource buffers, int light, int overlay) {
        SealInstance behaviour = seal.instance();
        float open = behaviour == null ? 0.0F : behaviour.holeOpenness(partialTick);
        if (open <= 0.0F) {
            return;
        }

        float ticks = seal.getLevel() == null ? 0.0F : seal.getLevel().getGameTime() + partialTick;
        AspectId sealing = seal.symbol(TileSeal.SLOTS - 1);
        int tint = sealing == null ? 0xFFFFFF : TaAspect.color(sealing);

        for (int sheet = 0; sheet < SHEETS.length; sheet++) {
            int colour = sheet == TINTED_SHEET ? tint : 0xFFFFFF;
            // A sheet reaches as far as the hole is open, plus its own step of the stack.
            float reach = open * (open + sheet / (float) (SHEETS.length - 1)) * PORTAL_SCALE;
            float angle = (ticks + sheet * reach * PORTAL_SPIN) % 360.0F;
            if (sheet % 2 == 0) {
                angle = 360.0F - angle;
            }

            pose.pushPose();
            pose.translate(0.0D, PORTAL_LIFT, PORTAL_DEPTH - sheet / 100.0D);
            // 1.12 alternated between adding the sheet to what was behind it and blending it in.
            // A modern render type fixes its blend state, so the emissive one stands in for the
            // additive sheets; the difference is how much of the sheet the frame behind shows.
            boolean additive = sheet % 2 == 0;
            VertexConsumer consumer = buffers.getBuffer(additive
                    ? RenderType.entityTranslucentEmissive(SHEETS[sheet])
                    : RenderType.entityTranslucent(SHEETS[sheet]));
            quad(consumer, pose, ring(reach, angle), colour, light, overlay);
            pose.popPose();
        }
    }

    /** The pictograms: one per filled slot, each tinted and turned from the one before it. */
    private static void pictograms(TileSeal seal, float partialTick, PoseStack pose,
                                   MultiBufferSource buffers, int light, int overlay) {
        for (int slot = 0; slot < TileSeal.SLOTS; slot++) {
            AspectId symbol = seal.symbol(slot);
            if (symbol == null) {
                continue;
            }
            int colour = TaAspect.color(symbol);

            pose.pushPose();
            pose.translate(0.0D, LIFT, -0.01D * (slot + 1));
            // The third slot turns the other way, and turns.
            int direction = slot == 2 ? -1 : 1;
            pose.mulPose(Axis.ZP.rotationDegrees(slot * 120.0F * direction));
            if (slot == 2) {
                float ticks = seal.getLevel() == null ? 0.0F : seal.getLevel().getGameTime() + partialTick;
                float wave = (float) ((Math.sin(Math.toRadians(ticks * SPIN * 20.0F % 360.0D)) + 1.0D) / 2.0D);
                pose.translate(0.0D, 0.0D, wave * -0.01D);
                pose.mulPose(Axis.ZP.rotationDegrees(ticks * SPIN % 360.0F * direction));
            }

            VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(SYMBOLS[slot]));
            quad(consumer, pose, unit(), colour, light, overlay);
            pose.popPose();
        }
    }

    /** The pictogram: one unit square centred on the slot's own origin. */
    private static float[][] unit() {
        return new float[][] {{-0.5F, -0.5F, 0.0F, 0.0F, 0.0F}, {0.5F, -0.5F, 0.0F, 1.0F, 0.0F},
                {0.5F, 0.5F, 0.0F, 1.0F, 1.0F}, {-0.5F, 0.5F, 0.0F, 0.0F, 1.0F}};
    }

    /**
     * One portal sheet: the four corners of a square of half-width {@code reach} whose corners are
     * turned to {@code angle} — 1.12 put its corners on the circle rather than squaring the sheet
     * up with the face, which is what makes the stack look like it is turning.
     */
    private static float[][] ring(float reach, float angle) {
        float[][] corners = new float[4][5];
        for (int corner = 0; corner < 4; corner++) {
            double radians = Math.toRadians(angle + corner * 90.0D);
            corners[corner][0] = (float) (-Math.cos(radians) * reach);
            corners[corner][1] = (float) (Math.sin(radians) * reach);
            corners[corner][2] = 0.0F;
            corners[corner][3] = corner == 0 || corner == 3 ? 0.0F : 1.0F;
            corners[corner][4] = corner < 2 ? 0.0F : 1.0F;
        }
        return corners;
    }

    /** One quad, as two triangles, in the colour it was given. */
    private static void quad(VertexConsumer consumer, PoseStack pose, float[][] corners, int colour,
                             int light, int overlay) {
        int red = (colour >> 16) & 0xFF;
        int green = (colour >> 8) & 0xFF;
        int blue = colour & 0xFF;
        int alpha = (colour >>> 24) == 0 ? 255 : (colour >>> 24) & 0xFF;
        int[] order = {0, 1, 2, 2, 3, 0};
        for (int index : order) {
            float x = corners[index][0];
            float y = corners[index][1];
            float z = corners[index][2];
            float u = corners[index][3];
            float v = corners[index][4];
%(VERTEX)s
        }
    }
}
'''

for node, pkg in [("1.20.1-forge", "forge"), ("1.21.1-neoforge", "neoforge")]:
    # replace rather than %: the template contains the modulo operator, which % formatting eats.
    body = TEMPLATE.replace("%(pkg)s", pkg).replace(
        "%(VERTEX)s", FORGE_VERTEX if pkg == "forge" else NEO_VERTEX)
    path = "versions/%s/src/main/java/dev/tc4port/thaumadditions/%s/client/SealRenderer.java" % (node, pkg)
    io.open(path, "w", encoding="utf-8", newline="\n").write(body)
    print("wrote", path)
