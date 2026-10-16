package dev.tc4port.thaumadditions.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.geom.ModelPart;

/**
 * The one drawing call the two versions spell differently, handed from the node to the shared code.
 *
 * <p>A model part can be drawn in a colour, which is how a jar shows whose essentia is inside it:
 * 1.12 bound a plain glow sprite and tinted it with the aspect's colour. The two versions take
 * that colour differently — 1.20.1 as four floats, 1.21.1 as one packed int — so the call cannot
 * be written once. Everything around it can, and is.</p>
 */
public interface ClientDraw {
    /** Draws one model part, tinted. The colour is 0xRRGGBB. */
    void coloured(ModelPart part, PoseStack pose, VertexConsumer consumer, int light, int overlay,
                  int rgb);

    /** The node's implementation, installed during client setup. */
    final class Holder {
        private static ClientDraw implementation;

        private Holder() {
        }

        public static void install(ClientDraw draw) {
            implementation = draw;
        }

        public static ClientDraw get() {
            return implementation;
        }
    }
}
