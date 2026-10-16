# Writes the void thaumometer's renderer into both nodes, reading its OBJ at runtime.
#
# The two differ only in how a vertex is written: 1.20.1 says vertex/color/uv/overlay/uv2/normal
# and ends each vertex, 1.21 says addVertex/setColor/setUv/setOverlay/setLight/setNormal and
# flushes on the next one.
#
# All five elements have to be filled. The entity render types carry an overlay coordinate beside
# the light, and a vertex that skips it throws "Not filled all elements of the vertex" the moment
# the buffer is drawn — which is what this renderer did until it was first shown in the hotbar.
import io

FORGE_VERTEX = '''                    consumer.vertex(pose.last().pose(), x, y, z)
                            .color(255, 255, 255, 255)
                            .uv(u, v)
                            .overlayCoords(overlay)
                            .uv2(light)
                            .normal(pose.last().normal(), 0.0F, 1.0F, 0.0F)
                            .endVertex();'''

NEO_VERTEX = '''                    consumer.addVertex(pose.last(), x, y, z)
                            .setColor(255, 255, 255, 255)
                            .setUv(u, v)
                            .setOverlay(overlay)
                            .setLight(light)
                            .setNormal(pose.last(), 0.0F, 1.0F, 0.0F);'''

TEMPLATE = '''package dev.tc4port.thaumadditions.%(pkg)s.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import dev.tc4port.thaumadditions.client.model.ObjModel;
import dev.tc4port.thaumadditions.client.render.VoidThaumometerTransforms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * The void thaumometer, read from the OBJ its 1.12 model was.
 *
 * <p>Its 1.12 form was a Wavefront OBJ with a transform per context, which is how it sat correctly
 * in a hand. Neither modern loader reads an OBJ, so the mesh is parsed at runtime by
 * {@link ObjModel} and drawn here, placed with the same numbers its 1.12 file carried. That is also
 * what the modern Thaumcraft does for its own thaumometer: an item model of {@code builtin/entity}
 * with the drawing left to a renderer like this one.</p>
 *
 * <p>The one duplication between the two nodes is the vertex call itself, whose methods were
 * renamed between the versions.</p>
 */
public class VoidThaumometerRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation MESH =
            ResourceLocation.tryParse("thaumadds:models/item/void_thaumometer.obj");
    /** What the OBJ's two materials are textured from, by the names its MTL gives them. */
    private static final ResourceLocation BODY =
            ResourceLocation.tryParse("thaumadds:textures/item/void_thaumometer.png");
    private static final ResourceLocation PANE =
            ResourceLocation.tryParse("thaumcraft:textures/models/scanscreen.png");
    /** The mesh is about 2.8 across; a sixteenth of that sits right in a hand. */
    private static final float MODEL_SCALE = 0.0625F;

    public VoidThaumometerRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        ObjModel mesh = ObjModel.get(MESH);
        if (mesh == null) {
            return;
        }
        VoidThaumometerTransforms.Place place = VoidThaumometerTransforms.forContext(context);

        pose.pushPose();
        pose.translate(0.5D, 0.5D, 0.5D);
        pose.translate(place.tx() / 16.0F, place.ty() / 16.0F, place.tz() / 16.0F);
        if (place.rotX() != 0.0F) {
            pose.mulPose(Axis.XP.rotationDegrees(place.rotX()));
        }
        if (place.rotY() != 0.0F) {
            pose.mulPose(Axis.YP.rotationDegrees(place.rotY()));
        }
        if (place.rotZ() != 0.0F) {
            pose.mulPose(Axis.ZP.rotationDegrees(place.rotZ()));
        }
        float scale = place.sx() * MODEL_SCALE;
        pose.scale(scale, scale, scale);

        // One pass per material: the two halves of the model come from different sheets, and a
        // buffer can only carry one texture.
        for (String material : mesh.materials()) {
            ResourceLocation texture = "pane".equals(material) ? PANE : BODY;
            VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(texture));
            float centreX = mesh.centreX();
            float centreY = mesh.centreY();
            float centreZ = mesh.centreZ();
            for (ObjModel.Face face : mesh.faces()) {
                if (!face.material().equals(material)) {
                    continue;
                }
                for (int corner = 0; corner < 4; corner++) {
                    int base = corner * 5;
                    float x = face.corners()[base] - centreX;
                    float y = face.corners()[base + 1] - centreY;
                    float z = face.corners()[base + 2] - centreZ;
                    float u = face.corners()[base + 3];
                    float v = face.corners()[base + 4];
%(VERTEX)s
                }
            }
        }
        pose.popPose();
    }
}
'''

for node, pkg in [("1.20.1-forge", "forge"), ("1.21.1-neoforge", "neoforge")]:
    body = TEMPLATE % {"pkg": pkg, "VERTEX": FORGE_VERTEX if pkg == "forge" else NEO_VERTEX}
    path = "versions/%s/src/main/java/dev/tc4port/thaumadditions/%s/client/VoidThaumometerRenderer.java" % (node, pkg)
    io.open(path, "w", encoding="utf-8", newline="\n").write(body)
    print("wrote", path)
