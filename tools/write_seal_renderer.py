# Writes the seal's block entity renderer into both nodes.
#
# 1.12 drew three pictograms flat on the block's face, one per symbol slot, each tinted with the
# colour of the aspect sitting in that slot, each turned a third of a turn from the last, and the
# third of them turning slowly. The pictograms are the mod's own three textures; they are not the
# aspects' own pictures, which is why the tint is what tells one seal from another.
#
# The tint is why this is per node: a model part cannot be coloured, so the quad is written out by
# hand, and the two versions renamed the vertex builder's methods. The placement and the maths are
# the same on both, and are the numbers 1.12 used.
import io

FORGE_VERTEX = '''                consumer.vertex(pose.last().pose(), x, y, z)
                        .color(red, green, blue, 255)
                        .uv(u, v)
                        .uv2(light)
                        .normal(pose.last().normal(), 0.0F, 0.0F, 1.0F)
                        .endVertex();'''

NEO_VERTEX = '''                consumer.addVertex(pose.last(), x, y, z)
                        .setColor(red, green, blue, 255)
                        .setUv(u, v)
                        .setLight(light)
                        .setNormal(pose.last(), 0.0F, 0.0F, 1.0F);'''

TEMPLATE = '''package dev.tc4port.thaumadditions.%(pkg)s.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import dev.tc4port.thaumadditions.aspect.TaAspect;
import dev.tc4port.thaumadditions.client.render.MachineRenderers;
import dev.tc4port.thaumadditions.tiles.TileSeal;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1.12 {@code TESRSeal}: the three pictograms on a seal's face.
 *
 * <p>Each of the block's three symbol slots draws the same pictogram — the mod shipped one per
 * slot, not one per aspect — tinted with the colour of the aspect in that slot and turned a third
 * of a turn from the slot before it. The third pictogram also turns, slowly, which is upstream's
 * doing and the only motion the block has.</p>
 *
 * <p>What is not here is the portal's window. 1.12 rendered the far side of a portal into a
 * framebuffer and drew that as the portal's surface; that is a render-to-texture pass with a
 * stencil, whose modern equivalent shares almost nothing with it and is a piece of work in its own
 * right. A bound portal seal shows its pictograms here and nothing more.</p>
 */
public class SealRenderer implements BlockEntityRenderer<TileSeal> {
    /** One pictogram per slot, in the order the slots are filled. */
    private static final ResourceLocation[] SYMBOLS = {
            ResourceLocation.tryParse("thaumadds:textures/misc/seal_0.png"),
            ResourceLocation.tryParse("thaumadds:textures/misc/seal_1.png"),
            ResourceLocation.tryParse("thaumadds:textures/misc/seal_2.png"),
    };
    /** 1.12's {@code 2 / 16}: the pictograms sit this far off the face. */
    private static final float OFFSET = 2.0F / 16.0F;
    /** 1.12's {@code 0.125}: the whole group sits this far up the face. */
    private static final float LIFT = 0.125F;
    /** 1.12's {@code speed}: the third pictogram's turn, in degrees a tick. */
    private static final float SPIN = 0.25F;

    public SealRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TileSeal seal, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        pose.pushPose();
        MachineRenderers.orient(pose, seal.orientation().getOpposite());
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.translate(0.0D, -OFFSET, 0.0D);

        for (int slot = 0; slot < TileSeal.SLOTS; slot++) {
            AspectId symbol = seal.symbol(slot);
            if (symbol == null) {
                continue;
            }
            int colour = TaAspect.color(symbol);
            int red = (colour >> 16) & 0xFF;
            int green = (colour >> 8) & 0xFF;
            int blue = colour & 0xFF;

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
            quad(consumer, pose, red, green, blue, light);
            pose.popPose();
        }
        pose.popPose();
    }

    /** The pictogram: one unit square centred on the slot's own origin. */
    private static void quad(VertexConsumer consumer, PoseStack pose, int red, int green, int blue,
                             int light) {
        // x, y, z, u, v: the pictogram is a unit square lying in the face's own plane.
        float[][] corners = {{-0.5F, -0.5F, 0.0F, 0.0F, 0.0F}, {0.5F, -0.5F, 0.0F, 1.0F, 0.0F},
                {0.5F, 0.5F, 0.0F, 1.0F, 1.0F}, {-0.5F, 0.5F, 0.0F, 0.0F, 1.0F}};
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
