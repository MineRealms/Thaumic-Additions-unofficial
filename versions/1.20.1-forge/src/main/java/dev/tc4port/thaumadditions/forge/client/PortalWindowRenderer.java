package dev.tc4port.thaumadditions.forge.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

import dev.tc4port.thaumadditions.client.render.PortalWindow;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;

/**
 * The 1.20.1 node's half of the portal window.
 *
 * <p>{@link PortalWindow} decides what to show; this enters the level's render pass to get it and
 * draws the frame that comes back. Both halves of that are per node — the pass grew a parameter
 * between the versions, and the vertex builder was renamed — and both are written out here rather
 * than behind a template, because there are only two of them.</p>
 */
public final class PortalWindowRenderer implements PortalWindow.WorldRenderer {
    @Override
    public RenderTarget newTarget(int width, int height) {
        return new com.mojang.blaze3d.pipeline.MainTarget(width, height);
    }

    @Override
    public void renderLevelInto(Camera camera, float partialTick, RenderTarget into) {
        Minecraft minecraft = Minecraft.getInstance();
        LevelRenderer levelRenderer = minecraft.levelRenderer;
        GameRenderer gameRenderer = minecraft.gameRenderer;
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        // Everything the nested pass is about to disturb, kept so it can be put back afterwards.
        Matrix4f projectionBefore = RenderSystem.getProjectionMatrix();
        Matrix4f projection = gameRenderer.getProjectionMatrix(minecraft.options.fov().get());

        PoseStack far = new PoseStack();
        far.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        far.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
        levelRenderer.prepareCullFrustum(far, camera.getPosition(), projection);

        into.bindWrite(true);
        RenderSystem.clear(16640, Minecraft.ON_OSX);
        levelRenderer.renderLevel(far, partialTick, Util.getNanos(), false, camera, gameRenderer,
                gameRenderer.lightTexture(), projection);
        into.unbindWrite();

        restore(minecraft, levelRenderer, gameRenderer, projectionBefore, partialTick);
    }

    /**
     * Puts the frame back the way the outer pass expects to find it.
     *
     * <p>The nested pass prepared both dispatchers for the far camera and left the culling frustum
     * on the far portal's view. The rest of this frame is drawn from the player's, so every one of
     * those has to be set again — without this the entities and block entities after this point are
     * placed as though the player were standing at the far portal, which is the failure this whole
     * approach is arranged around.</p>
     */
    public static void restore(Minecraft minecraft, LevelRenderer levelRenderer,
                               GameRenderer gameRenderer, Matrix4f projection, float partialTick) {
        Camera real = gameRenderer.getMainCamera();
        minecraft.getBlockEntityRenderDispatcher().prepare(minecraft.level, real, minecraft.hitResult);
        minecraft.getEntityRenderDispatcher().prepare(minecraft.level, real,
                minecraft.crosshairPickEntity);
        RenderSystem.setProjectionMatrix(projection,
                com.mojang.blaze3d.vertex.VertexSorting.DISTANCE_TO_ORIGIN);
        RenderSystem.setShaderGameTime(minecraft.level.getGameTime(), partialTick);

        PoseStack back = new PoseStack();
        back.mulPose(Axis.XP.rotationDegrees(real.getXRot()));
        back.mulPose(Axis.YP.rotationDegrees(real.getYRot() + 180.0F));
        levelRenderer.prepareCullFrustum(back, real.getPosition(), projection);
        RenderSystem.viewport(0, 0, minecraft.getWindow().getWidth(),
                minecraft.getWindow().getHeight());
    }

    /**
     * Draws the frame on the portal's face, in the world.
     *
     * <p>What came back is a raw GL texture, not a file, so there is no name to give a render type
     * — and a render type's own state shards are not reachable from a mod's package anyway. The
     * shader and the texture are therefore bound by hand and the quad drawn directly, which is
     * what a second render target is normally drawn with.</p>
     *
     * <p>The corners are the world's, and this runs in the frame's own stage: same transform the
     * far side was rendered with, so the window and its picture agree by construction.</p>
     */
    public static void draw() {
        int texture = PortalWindow.texture();
        float[][] corners = PortalWindow.corners();
        if (texture < 0 || corners == null) {
            return;
        }
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        for (int[] order : new int[][] {{0, 1, 2, 3}}) {
            for (int index : order) {
                builder.vertex(corners[index][0], corners[index][1], corners[index][2])
                        .uv(corners[index][3], corners[index][4]).endVertex();
            }
        }
        BufferUploader.drawWithShader(builder.end());

        RenderSystem.enableBlend();
    }
}
