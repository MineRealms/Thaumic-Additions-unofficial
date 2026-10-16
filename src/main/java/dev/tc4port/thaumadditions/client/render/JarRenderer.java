package dev.tc4port.thaumadditions.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.tc4port.thaumadditions.aspect.TaAspect;
import dev.tc4port.thaumadditions.tiles.TileJar;
import dev.tc4port.thaumcraft.api.aspect.AspectApi;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * The liquid inside a jar.
 *
 * <p>1.12 drew it as a box from a quarter to three quarters across, from a sixteenth of the way up
 * to wherever the fill reached, textured with Thaumcraft's animated glow and tinted with the
 * aspect's colour. The port draws the same box at the same place and height — the fill reaches
 * five eighths of the way up a full jar, exactly as upstream measured it — but textures it with the
 * aspect's own picture instead of tinting a grey glow. The aspect texture already carries the
 * aspect's colour, so a jar of ignis reads as orange without anything having to tint it.</p>
 *
 * <p>The six jars share one block entity type, so this is registered once for all of them.</p>
 */
public class JarRenderer implements BlockEntityRenderer<TileJar> {
    /** 1.12's {@code 0.625}: how far up a full jar's liquid reaches. */
    private static final float FULL_HEIGHT = 0.625F;
    /** 1.12's {@code 0.0625}: where the liquid starts, one sixteenth above the jar's floor. */
    private static final float FLOOR = 0.0625F;

    private static final ModelPart LIQUID = layer().bakeRoot();

    /** 1.12's {@code thaumcraft:blocks/animatedglow}, the sprite every jar's liquid is drawn with. */
    private static final net.minecraft.resources.ResourceLocation BRINE =
            net.minecraft.resources.ResourceLocation.tryParse("thaumcraft:textures/block/animatedglow.png");

    /** 1.20.1 builds block entity renderers from a context, even when one needs nothing from it. */
    public JarRenderer(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context) {
    }

    /** One unit tall and eight across: the height is scaled to the fill at draw time. */
    public static LayerDefinition layer() {
        MeshDefinition mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("liquid",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 8, 1, 8),
                PartPose.offset(-4.0F, 0.0F, -4.0F));
        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void render(TileJar jar, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        int amount = jar.getAmount();
        AspectId aspect = jar.getAspect();
        int capacity = jar.getCapacity();
        if (amount <= 0 || aspect == null || capacity <= 0) {
            return;
        }
        if (AspectApi.get(aspect) == null) {
            return;
        }
        float level = amount / (float) capacity * FULL_HEIGHT;

        pose.pushPose();
        pose.translate(0.5D, FLOOR, 0.5D);
        // The model is one sixteenth tall, so scaling by the fill in sixteenths gives the height.
        pose.scale(1.0F, level * 16.0F, 1.0F);
        // Upstream bound a plain glow sprite and tinted it with the aspect's colour; what is in the
        // jar is that colour, and the texture is only there to have something to tint. Binding the
        // aspect's own icon instead — which is what this did — paints the jar with the aspect's
        // *picture*, stretched over the inside of the glass.
        ClientDraw.Holder.get().coloured(LIQUID, pose,
                buffers.getBuffer(RenderType.entityCutoutNoCull(BRINE)),
                light, overlay, TaAspect.color(aspect));
        pose.popPose();
    }
}
