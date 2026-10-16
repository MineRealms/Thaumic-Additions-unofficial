package dev.tc4port.thaumadditions.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import dev.tc4port.thaumadditions.client.render.ClientDraw;
import net.minecraft.client.model.geom.ModelPart;

/**
 * 1.21.1 tints a model part with one packed colour; see {@link ClientDraw}.
 */
public final class ClientDrawImpl implements ClientDraw {
    @Override
    public void coloured(ModelPart part, PoseStack pose, VertexConsumer consumer, int light,
                         int overlay, int rgb) {
        part.render(pose, consumer, light, overlay, 0xFF000000 | rgb);
    }
}
