# Writes the portal window's two loader halves into both nodes.
#
# The shared half (PortalWindow) does the deciding: which portal is near enough and open, where the
# far camera stands, and when the whole thing is off. What is left is the two things the two
# versions disagree about, and they disagree about both of them:
#
#   * Entering the level's render pass. 1.20.1 takes a PoseStack and a partial tick among its
#     arguments; 1.21.1 dropped the PoseStack and takes a DeltaTracker instead. Same method,
#     different shape, so the call cannot be shared.
#
#   * Drawing the result. The buffer holds a raw GL texture, not a file, so there is no
#     ResourceLocation to name it to a render type — and the render types' own shards are not
#     reachable from a mod anyway. Both halves therefore bind the shader and the texture by hand and
#     draw the quad themselves, which is where the vertex builder's renames bite.
#
# The frame's event is per node for the same reason it always is: two different packages, one name.
import io

BODY_FORGE = '''
    @Override
    public void renderLevelInto(net.minecraft.client.Camera camera, float partialTick,
                                com.mojang.blaze3d.pipeline.RenderTarget into) {
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        var levelRenderer = minecraft.levelRenderer;
        var gameRenderer = minecraft.gameRenderer;

        // Everything the nested pass is about to disturb, kept so it can be put back.
        com.mojang.blaze3d.vertex.PoseStack stack = new com.mojang.blaze3d.vertex.PoseStack();
        org.joml.Matrix4f projection = com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix();
        double fov = minecraft.options.fov().get();
        int width = into.width;
        int height = into.height;

        into.bindWrite(true);
        com.mojang.blaze3d.systems.RenderSystem.clear(16640, net.minecraft.client.Minecraft.ON_OSX);
        levelRenderer.renderLevel(stack, partialTick, net.minecraft.Util.getNanos(), false, camera,
                gameRenderer, gameRenderer.lightTexture(), gameRenderer.getProjectionMatrix(fov));
        into.unbindWrite();

        // The dispatchers were prepared for the far camera by the nested pass, and the rest of this
        // frame is drawn with the real one. Without this the entities and block entities from here
        // on are placed as though the player were standing at the far portal.
        minecraft.getBlockEntityRenderDispatcher().prepare(minecraft.level,
                minecraft.gameRenderer.getMainCamera(), minecraft.hitResult);
        minecraft.getEntityRenderDispatcher().prepare(minecraft.level,
                minecraft.gameRenderer.getMainCamera(), minecraft.crosshairPickEntity);
        com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(projection);
        com.mojang.blaze3d.systems.RenderSystem.setShaderGameTime(
                minecraft.level.getGameTime(), partialTick);
        com.mojang.blaze3d.systems.RenderSystem.viewport(0, 0,
                minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
    }
'''

BODY_NEO = '''
    @Override
    public void renderLevelInto(net.minecraft.client.Camera camera, float partialTick,
                                com.mojang.blaze3d.pipeline.RenderTarget into) {
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        var levelRenderer = minecraft.levelRenderer;
        var gameRenderer = minecraft.gameRenderer;

        // Everything the nested pass is about to disturb, kept so it can be put back.
        org.joml.Matrix4f projection = com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix();
        double fov = minecraft.options.fov().get();
        var deltaTracker = minecraft.getDeltaTracker();

        into.bindWrite(true);
        com.mojang.blaze3d.systems.RenderSystem.clear(16640, net.minecraft.client.Minecraft.ON_OSX);
        levelRenderer.renderLevel(deltaTracker, false, camera, gameRenderer,
                gameRenderer.lightTexture(), gameRenderer.getProjectionMatrix(fov),
                com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix());
        into.unbindWrite();

        // The dispatchers were prepared for the far camera by the nested pass, and the rest of this
        // frame is drawn with the real one. Without this the entities and block entities from here
        // on are placed as though the player were standing at the far portal.
        minecraft.getBlockEntityRenderDispatcher().prepare(minecraft.level,
                minecraft.gameRenderer.getMainCamera(), minecraft.hitResult);
        minecraft.getEntityRenderDispatcher().prepare(minecraft.level,
                minecraft.gameRenderer.getMainCamera(), minecraft.crosshairPickEntity);
        com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(projection);
        com.mojang.blaze3d.systems.RenderSystem.setShaderGameTime(
                minecraft.level.getGameTime(), partialTick);
        com.mojang.blaze3d.systems.RenderSystem.viewport(0, 0,
                minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
    }
'''

TEMPLATE = '''package dev.tc4port.thaumadditions.%(pkg)s.client;

import java.util.List;

import com.mojang.blaze3d.pipeline.RenderTarget;

import dev.tc4port.thaumadditions.client.render.PortalWindow;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
%(IMPORTS)s
/**
 * The node's half of the portal window: entering the level's render pass, and drawing the frame
 * that came back out of it.
 *
 * <p>See {@link PortalWindow} for what the window is and why the second pass is not run from the
 * seal's own renderer.</p>
 */
public final class PortalWindowRenderer implements PortalWindow.WorldRenderer {
    /** The far side is drawn into a square buffer, so its texture is stretched over the face. */
    private static final ResourceLocation BLANK =
            ResourceLocation.tryParse("thaumadds:textures/misc/portal.png");
%(BODY)s
    /** Draws the window on the seal's face, or nothing when there is no window this frame. */
    public static void draw(PoseStackType pose, MultiBufferSource buffers, float width,
                            float height, int light, float depth) {
        int texture = PortalWindow.texture();
        if (texture < 0) {
            return;
        }
        org.joml.Matrix4f matrix = pose.last().pose();
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRendererType::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, texture);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();

        com.mojang.blaze3d.vertex.BufferBuilder builder = TessellatorType.getInstance()%(BEGIN)s;
        float half = width / 2.0F;
        float tall = height / 2.0F;
        // The buffer's own origin is its bottom-left, so the image is turned on its way over.
        vertex(builder, pose, matrix, -half, tall, depth, 0.0F, 0.0F);
        vertex(builder, pose, matrix, half, tall, depth, 1.0F, 0.0F);
        vertex(builder, pose, matrix, half, -tall, depth, 1.0F, 1.0F);
        vertex(builder, pose, matrix, -half, -tall, depth, 0.0F, 1.0F);
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(builder%(END)s);

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
    }

    /** One corner of the window. */
    private static void vertex(com.mojang.blaze3d.vertex.BufferBuilder builder, VertexPoseType pose,
                               org.joml.Matrix4f matrix, float x, float y, float z, float u, float v) {
%(VERTEX)s
    }
}
'''

FORGE_IMPORTS = '''import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.renderer.GameRenderer;'''

FORGE_ALIASES = {
    'PoseStackType': 'com.mojang.blaze3d.vertex.PoseStack',
    'VertexPoseType': 'com.mojang.blaze3d.vertex.PoseStack',
    'GameRendererType': 'net.minecraft.client.renderer.GameRenderer',
    'TessellatorType': 'com.mojang.blaze3d.vertex.Tesselator',
}

FORGE_BEGIN = '.getBuilder()\n                .begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,\n                        com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX)'
FORGE_END = '.end()'

FORGE_VERTEX = '''        builder.vertex(matrix, x, y, z).uv(u, v).endVertex();'''

NEO_BEGIN = '.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,\n                        com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX)'
NEO_END = ''
NEO_VERTEX = '''        builder.addVertex(matrix, x, y, z).setUv(u, v);'''


def write(node, pkg, imports, body, aliases, begin, end, vertex, pose_type, vertex_pose_type):
    text = TEMPLATE % {
        'pkg': pkg, 'IMPORTS': imports, 'BODY': body, 'BEGIN': begin, 'END': end,
        'VERTEX': vertex,
    }
    for alias, real in aliases.items():
        text = text.replace(alias, real)
    path = "versions/%s/src/main/java/dev/tc4port/thaumadditions/%s/client/PortalWindowRenderer.java" % (
        node, pkg)
    io.open(path, "w", encoding="utf-8", newline="\n").write(text)
    print("wrote", path)


write("1.20.1-forge", "forge", FORGE_IMPORTS, BODY_FORGE, FORGE_ALIASES,
      FORGE_BEGIN, FORGE_END, FORGE_VERTEX, None, None)
write("1.21.1-neoforge", "neoforge", FORGE_IMPORTS, BODY_NEO, FORGE_ALIASES,
      NEO_BEGIN, NEO_END, NEO_VERTEX, None, None)
