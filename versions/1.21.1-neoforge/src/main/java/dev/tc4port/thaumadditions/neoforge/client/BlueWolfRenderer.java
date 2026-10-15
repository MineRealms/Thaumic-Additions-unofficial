package dev.tc4port.thaumadditions.neoforge.client;

import dev.tc4port.thaumadditions.entity.BlueWolf;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * The blue wolf, drawn with the vanilla wolf's model and its own two textures.
 *
 * <p>1.12 gave the blue wolf its own {@code ModelBlueWolf}, but that model is the vanilla wolf's
 * shape to the box — same head, body, mane, four legs and tail, same 64-square sheet. Only the
 * texture differs, and so the port borrows the vanilla model rather than copying it.</p>
 */
public class BlueWolfRenderer extends MobRenderer<BlueWolf, WolfModel<BlueWolf>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.tryParse("thaumadds:textures/entity/blue_wolf.png");
    private static final ResourceLocation TEXTURE_ANGRY =
            ResourceLocation.tryParse("thaumadds:textures/entity/blue_wolf_angry.png");

    public BlueWolfRenderer(EntityRendererProvider.Context context) {
        super(context, new WolfModel<>(context.bakeLayer(ModelLayers.WOLF)), 0.5F);
        // No collar layer: a blue wolf is only ever made from an untamed wolf, so it never wears one.
    }

    @Override
    public ResourceLocation getTextureLocation(BlueWolf wolf) {
        return wolf.isAngry() ? TEXTURE_ANGRY : TEXTURE;
    }
}
