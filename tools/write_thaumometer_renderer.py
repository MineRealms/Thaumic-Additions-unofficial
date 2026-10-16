# Writes the void thaumometer's renderer into both nodes.
#
# The two differ only in how a vertex is written: 1.20.1 says vertex/color/uv/uv2/overlay/normal and
# ends each vertex, 1.21 says addVertex/setColor/setUv/setLight/setOverlay/setNormal and flushes on
# the next vertex. Everything else — the mesh, the transforms, the two textures — is shared.
import io

FORGE_VERTEX = '''                consumer.vertex(pose.last(), x, y, z)
                        .color(red, green, blue, alpha)
                        .uv(u, v)
                        .overlay(overlay)
                        .uv2(light)
                        .normal(pose.last(), 0.0F, 1.0F, 0.0F)
                        .endVertex();'''

NEO_VERTEX = '''                consumer.addVertex(pose.last(), x, y, z)
                        .setColor(red, green, blue, alpha)
                        .setUv(u, v)
                        .setOverlay(overlay)
                        .setLight(light)
                        .setNormal(pose.last(), 0.0F, 1.0F, 0.0F);'''

TEMPLATE = '''package dev.tc4port.thaumadditions.%(pkg)s.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import dev.tc4port.thaumadditions.client.model.VoidThaumometerMesh;
import dev.tc4port.thaumadditions.client.render.VoidThaumometerTransforms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * The void thaumometer, drawn from the mesh its OBJ used to hold.
 *
 * <p>Its 1.12 model was a Wavefront OBJ with a transform per context, which is how it sat correctly
 * in a hand in both first and third person. Neither modern loader can load an OBJ, so the mesh is
 * baked into {@link VoidThaumometerMesh} and this draws it, placing it with the same numbers its
 * 1.12 file carried. That is also what the modern Thaumcraft does for its own thaumometer: an item
 * model of {@code builtin/entity} with the drawing left to a renderer like this one.</p>
 *
 * <p>The one duplication between the two nodes is the vertex call itself, whose methods were
 * renamed between the versions.</p>
 */
public class VoidThaumometerRenderer extends BlockEntityWithoutLevelRenderer {
    /** The scanner's own body. */
    private static final ResourceLocation BODY =
            ResourceLocation.tryParse("thaumadds:textures/item/void_thaumometer.png");
    /** The screen, which is the modern Thaumcraft's scan sheet — the same picture 1.12 used. */
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
        VoidThaumometerTransforms.Place place = VoidThaumometerTransforms.forContext(context);

        pose.pushPose();
        // The mesh is centred on its own origin, so the item's half-block offset comes first.
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

        // One pass per material, because the two halves are textured from different sheets.
        for (int material = 0; material < 2; material++) {
            ResourceLocation texture = material == VoidThaumometerMesh.MATERIAL_BODY ? BODY : PANE;
            VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(texture));
            draw(consumer, pose, material, light, overlay);
        }
        pose.popPose();
    }

    /** Emits this material's quads. */
    private static void draw(VertexConsumer consumer, PoseStack pose, int material, int light,
                             int overlay) {
        for (VoidThaumometerMesh.Quad quad : VoidThaumometerMesh.QUADS) {
            if (quad.material() != material) {
                continue;
            }
            float[] v = quad.vertices();
            for (int vertex = 0; vertex < 4; vertex++) {
                int base = vertex * 5;
                float x = v[base];
                float y = v[base + 1];
                float z = v[base + 2];
                float u = v[base + 3];
                float uv = v[base + 4];
%(VERTEX)s
            }
        }
    }
}
'''

for node, pkg in [("1.20.1-forge", "forge"), ("1.21.1-neoforge", "neoforge")]:
    body = TEMPLATE % {"pkg": pkg, "VERTEX": FORGE_VERTEX if pkg == "forge" else NEO_VERTEX}
    path = "versions/%s/src/main/java/dev/tc4port/thaumadditions/%s/client/VoidThaumometerRenderer.java" % (node, pkg)
    io.open(path, "w", encoding="utf-8", newline="\n").write(body)
    print("wrote", path)
