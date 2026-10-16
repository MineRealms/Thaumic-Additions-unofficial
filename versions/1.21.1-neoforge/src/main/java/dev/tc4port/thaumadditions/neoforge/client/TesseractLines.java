package dev.tc4port.thaumadditions.neoforge.client;

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
                consumer.addVertex(pose.last(), x, y, z)
                        .setColor(red, green, blue, alpha)
                        .setNormal(pose.last(), 0.0F, 0.0F, 1.0F);
                }
            }
        }
    }
}
