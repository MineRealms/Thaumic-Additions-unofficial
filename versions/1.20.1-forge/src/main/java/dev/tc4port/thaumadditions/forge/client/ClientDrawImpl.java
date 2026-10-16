package dev.tc4port.thaumadditions.forge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import dev.tc4port.thaumadditions.client.render.ClientDraw;
import net.minecraft.client.model.geom.ModelPart;

/**
 * 1.20.1 tints a model part with four floats; see {@link ClientDraw}.
 */
public final class ClientDrawImpl implements ClientDraw {
    @Override
    public void coloured(ModelPart part, PoseStack pose, VertexConsumer consumer, int light,
                         int overlay, int rgb) {
        part.render(pose, consumer, light, overlay,
                ((rgb >> 16) & 0xFF) / 255.0F,
                ((rgb >> 8) & 0xFF) / 255.0F,
                (rgb & 0xFF) / 255.0F,
                1.0F);
    }
}
