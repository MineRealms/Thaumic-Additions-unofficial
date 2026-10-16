package dev.tc4port.thaumadditions.neoforge.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import dev.tc4port.thaumadditions.client.render.PortalWindow;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * The 1.21.1 node's half of the portal window; see the Forge twin for what this is.
 *
 * <p>1.21 changed both things this class does. The level's render pass lost its {@code PoseStack}
 * and its partial tick and took a delta tracker and a model-view matrix instead, so the view is
 * built here rather than handed over; and the vertex builder stopped ending its vertices, so the
 * quad is written with the other names and the mesh finished in one step.</p>
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
        Matrix4f modelView = view(camera);

        levelRenderer.prepareCullFrustum(camera.getPosition(), modelView, projection);

        into.bindWrite(true);
        RenderSystem.clear(16640, Minecraft.ON_OSX);
        levelRenderer.renderLevel(minecraft.getTimer(), false, camera, gameRenderer,
                gameRenderer.lightTexture(), modelView, projection);
        into.unbindWrite();

        restore(minecraft, levelRenderer, gameRenderer, projectionBefore, partialTick);
    }

    /**
     * Puts the frame back the way the outer pass expects to find it; see the Forge twin.
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
        levelRenderer.prepareCullFrustum(real.getPosition(), view(real), projection);
        RenderSystem.viewport(0, 0, minecraft.getWindow().getWidth(),
                minecraft.getWindow().getHeight());
    }

    /** The view matrix for a camera, built the way the game builds its own. */
    private static Matrix4f view(Camera camera) {
        return new Matrix4f().rotation(camera.rotation().conjugate(new Quaternionf()));
    }

    /** Draws the frame on the portal's face, in the world; see the Forge twin. */
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

        BufferBuilder builder = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        for (int index : new int[] {0, 1, 2, 3}) {
            builder.addVertex(corners[index][0], corners[index][1], corners[index][2])
                    .setUv(corners[index][3], corners[index][4]);
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());

        RenderSystem.enableBlend();
    }
}
