package dev.tc4port.thaumadditions.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.tc4port.thaumadditions.entity.Chester;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1.12 {@code RenderChester}: a chest with a lid, drawn from three boxes.
 *
 * <p>Upstream's {@code ModelChester} is the vanilla chest's shape — a lid, a knob and a body, all
 * off a 64-square sheet — and the only motion is the lid swinging up when someone looks inside.
 * The parts are rendered directly rather than through an {@code EntityModel}, because that base
 * class changed the shape of its draw call between the two versions this port targets and three
 * boxes do not need it.</p>
 */
public class ChesterRenderer extends EntityRenderer<Chester> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.tryParse("thaumadds:textures/models/chester.png");

    private final ModelPart lid;
    private final ModelPart knob;
    private final ModelPart body;

    public ChesterRenderer(EntityRendererProvider.Context context) {
        super(context);
        ModelPart root = layer().bakeRoot();
        this.lid = root.getChild("lid");
        this.knob = root.getChild("knob");
        this.body = root.getChild("body");
        this.shadowRadius = 0.5F;
    }

    /** 1.12 {@code ModelChester}: lid 14×5×14, knob 2×4×1, body 14×10×14, on a 64-square sheet. */
    public static LayerDefinition layer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("lid",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -5.0F, -14.0F, 14, 5, 14),
                PartPose.offset(1.0F, 7.0F, 15.0F));
        root.addOrReplaceChild("knob",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -2.0F, -15.0F, 2, 4, 1),
                PartPose.offset(8.0F, 7.0F, 15.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 19).addBox(0.0F, 0.0F, 0.0F, 14, 10, 14),
                PartPose.offset(1.0F, 6.0F, 1.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ResourceLocation getTextureLocation(Chester chester) {
        return TEXTURE;
    }

    @Override
    public void render(Chester chester, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        super.render(chester, yaw, partialTick, pose, buffers, light);
        pose.pushPose();
        // The model is authored with the chest's own origin at its middle front; the entity sits
        // on it the way 1.12 drew it.
        pose.translate(-0.5D, 0.0D, -0.5D);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-yaw));
        var consumer = buffers.getBuffer(RenderType.entityCutout(TEXTURE));
        // The lid swings up about the back edge, which is where its pivot already is.
        pose.pushPose();
        pose.translate(0.0F, 0.0F, 0.0F);
        lid.xRot = -chester.lidAngle(partialTick) * 1.4F;
        lid.render(pose, consumer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        pose.popPose();
        knob.render(pose, consumer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        body.render(pose, consumer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
