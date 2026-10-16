# Writes the tesseract that turns inside the entity summoner, into both nodes.
#
# 1.12 drew a four-dimensional hypercube inside that machine — twenty-four square faces in x, y, z
# and w, rotated in the yw plane as the machine turns, then projected back down to three dimensions
# by dividing by w + 1. It is the one effect of upstream's that has no substitute in the port: the
# mob preview is there, and the cage around it is not.
#
# The geometry is upstream's, transcribed face by face. Only the drawing is per node, because the
# two versions renamed the vertex builder's methods; the faces are lines, which both of them can
# draw, and the projection is arithmetic that belongs to neither.
import io

FORGE_VERTEX = '''                consumer.vertex(pose.last().pose(), x, y, z)
                        .color(red, green, blue, alpha)
                        .normal(pose.last().normal(), 0.0F, 0.0F, 1.0F)
                        .endVertex();'''

NEO_VERTEX = '''                consumer.addVertex(pose.last(), x, y, z)
                        .setColor(red, green, blue, alpha)
                        .setNormal(pose.last(), 0.0F, 0.0F, 1.0F);'''

TEMPLATE = '''package dev.tc4port.thaumadditions.%(pkg)s.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import dev.tc4port.thaumadditions.client.render.Tesseract;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * The hypercube inside the entity summoner, drawn as twenty-four squares.
 *
 * <p>1.12 drew it with {@code GL_LINE_LOOP} and the thaumium texture, so each face came out as its
 * own wireframe outline. The port draws the same four edges with the line render type, which is the
 * same picture without the texture: a line has no surface to sample one from.</p>
 */
public final class TesseractLines implements dev.tc4port.thaumadditions.client.render.TesseractRenderer {
    /** Upstream's colour, {@code 0xFFFFFFFF}, and its size, three tenths. */
    private static final int COLOUR = 0xFFFFFFFF;
    private static final float SCALE = 0.3F;

    public TesseractLines() {
    }

    /**
     * Draws the cage, turned by {@code radians}.
     *
     * <p>The turn is upstream's: the machine's own rotation halved, and passed in radians.</p>
     */
    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, double radians, int light) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.lines());
        int red = (COLOUR >> 16) & 0xFF;
        int green = (COLOUR >> 8) & 0xFF;
        int blue = COLOUR & 0xFF;
        int alpha = COLOUR >>> 24;

        float[][] projected = Tesseract.project(radians);
        // Each face is four points and closes on itself, so its outline is four edges: p0-p1,
        // p1-p2, p2-p3, p3-p0, written as eight vertices because a line has two ends.
        for (int face = 0; face < Tesseract.FACES; face++) {
            int base = face * 4;
            for (int edge = 0; edge < 4; edge++) {
                int from = base + edge;
                int to = base + (edge + 1) % 4;
                for (int end = 0; end < 2; end++) {
                    float[] point = projected[end == 0 ? from : to];
                    float x = point[0] * SCALE;
                    float y = point[1] * SCALE;
                    float z = point[2] * SCALE;
%(VERTEX)s
                }
            }
        }
    }
}
'''

for node, pkg in [("1.20.1-forge", "forge"), ("1.21.1-neoforge", "neoforge")]:
    body = TEMPLATE.replace("%(pkg)s", pkg).replace(
        "%(VERTEX)s", FORGE_VERTEX if pkg == "forge" else NEO_VERTEX)
    path = "versions/%s/src/main/java/dev/tc4port/thaumadditions/%s/client/TesseractLines.java" % (
        node, pkg)
    io.open(path, "w", encoding="utf-8", newline="\n").write(body)
    print("wrote", path)
