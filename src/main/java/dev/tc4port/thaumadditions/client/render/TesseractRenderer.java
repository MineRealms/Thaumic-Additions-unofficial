package dev.tc4port.thaumadditions.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Draws the entity summoner's hypercube.
 *
 * <p>The geometry and the arithmetic are in {@link Tesseract}; this is only the drawing, and the
 * drawing is the part the two versions disagree about — they renamed every method of the vertex
 * builder, so the same twelve lines of code exist twice in the two nodes. A node hands its own to
 * the shared renderer, which is the same arrangement the OBJ renderer uses.</p>
 */
public interface TesseractRenderer {
    /** Twenty-four wireframe squares, turned by {@code radians}, at the origin of {@code pose}. */
    void render(PoseStack pose, MultiBufferSource buffers, double radians, int light);
}
