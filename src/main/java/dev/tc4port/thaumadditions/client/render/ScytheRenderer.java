package dev.tc4port.thaumadditions.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.tc4port.thaumadditions.entity.ScytheProjectileBase;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 1.12 {@code RenderMithminiteScythe}: the thrown blade, drawn as the scythe item itself, spinning.
 *
 * <p>Upstream was a {@code RenderSnowball} that bypassed the billboard its parent set up and drew
 * the scythe's own item model instead, with a fixed pose around it: down a block, pivoted by an
 * eighth of a block, laid on its side, turned to the angle it was thrown at, and then spun two
 * thirds of a turn per tick. Those numbers are 1.12's and are reproduced here in the same order,
 * because each of them is applied in a different frame.</p>
 *
 * <p>The blade does not fade out the way 1.12's did. Upstream tinted the item model with the
 * entity's alpha, which a modern item quad cannot carry — its render type is chosen by the model
 * and ignores vertex alpha — and faking it through the shader colour would tint whatever else is
 * already queued in the same buffer. It stays whole for the fifteen ticks it lingers, which is the
 * only part of that fade that was visible at all.</p>
 *
 * <p>{@code ItemRenderer#renderStatic} takes the same eight arguments on both nodes, so unlike the
 * block renderers this one needs no per-node twin.</p>
 */
public class ScytheRenderer extends EntityRenderer<ScytheProjectileBase> {
    /** Only ever returned: the item renderer binds the scythe's own sheet. */
    private static final ResourceLocation SPRITE =
            ResourceLocation.tryParse("thaumadds:textures/item/mithminite_scythe.png");
    /** 1.12 drew the blade one block below the entity's own position. */
    private static final double DROP = 1.0D;
    /** The spin is taken about a point an eighth of a block into the sprite, not its corner. */
    private static final double PIVOT = 0.125D;
    /** Two thirds of a turn a tick. */
    private static final float SPIN_PER_TICK = 40.0F;
    /** 1.12's last two transforms, which double the item model's quarter-size ground scale. */
    private static final float SCALE = 2.0F;

    private final ItemRenderer items;
    private final ItemStack blade;

    public ScytheRenderer(EntityRendererProvider.Context context, Item blade) {
        super(context);
        this.items = context.getItemRenderer();
        this.blade = new ItemStack(blade);
    }

    @Override
    public ResourceLocation getTextureLocation(ScytheProjectileBase scythe) {
        return SPRITE;
    }

    @Override
    public void render(ScytheProjectileBase scythe, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        super.render(scythe, yaw, partialTick, pose, buffers, light);
        pose.pushPose();
        pose.translate(0.0D, -DROP, 0.0D);
        pose.translate(PIVOT, PIVOT, PIVOT);
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        // 1.12 carried the throw's own yaw and pitch in synced data; the projectile is thrown along
        // its motion and never curves, so the rotation it already has is the same pair.
        pose.mulPose(Axis.ZP.rotationDegrees(
                Mth.rotLerp(partialTick, scythe.yRotO, scythe.getYRot())));
        pose.mulPose(Axis.XP.rotationDegrees(
                Mth.rotLerp(partialTick, scythe.xRotO, scythe.getXRot())));
        pose.mulPose(Axis.ZP.rotationDegrees((scythe.tickCount + partialTick) * SPIN_PER_TICK));
        pose.translate(-PIVOT, -PIVOT, -PIVOT);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        pose.scale(SCALE, SCALE, SCALE);
        items.renderStatic(blade, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY,
                pose, buffers, scythe.level(), 0);
        pose.popPose();
    }
}
