package dev.tc4port.thaumadditions.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.tc4port.thaumadditions.client.model.MachineModels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * The inventory icon for the five machines that have no blockstate geometry.
 *
 * <p>The 1.12 items drew themselves through the same TESR as the placed block
 * ({@code TESRCrystalBore.renderItem} and friends); 1.20.1's equivalent is a
 * {@link BlockEntityWithoutLevelRenderer} attached to the item. Each machine's item model points
 * at its (element-less) block model, so everything visible here comes from the shared
 * {@link MachineModels} shape.</p>
 */
public final class MachineItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static MachineItemRenderer bore;
    private static MachineItemRenderer combiner;
    private static MachineItemRenderer charger;
    private static MachineItemRenderer crusher;
    private static MachineItemRenderer concentrator;

    private final ModelPart model;
    private final ResourceLocation texture;

    private MachineItemRenderer(ModelPart model, ResourceLocation texture) {
        // Base is inert: nothing here delegates to the dispatcher or the model set.
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.model = model;
        this.texture = texture;
    }

    /**
     * The shared renderer for one machine. Built on first use rather than at class-init because
     * {@link Minecraft#getInstance()} has to exist by then; 1.12's {@code renderItem} put the model
     * a block and a half up and flipped it, which is what the pose below reproduces.
     */
    public static MachineItemRenderer bore() {
        if (bore == null) {
            bore = new MachineItemRenderer(MachineModels.CrystalBore.layer().bakeRoot(),
                    MachineModels.CrystalBore.TEXTURE);
        }
        return bore;
    }

    public static MachineItemRenderer combiner() {
        if (combiner == null) {
            combiner = new MachineItemRenderer(MachineModels.AspectCombiner.layer().bakeRoot(),
                    MachineModels.AspectCombiner.TEXTURE);
        }
        return combiner;
    }

    public static MachineItemRenderer charger() {
        if (charger == null) {
            charger = new MachineItemRenderer(MachineModels.AuraCharger.layer().bakeRoot(),
                    MachineModels.AuraCharger.TEXTURE);
        }
        return charger;
    }

    public static MachineItemRenderer crusher() {
        if (crusher == null) {
            crusher = new MachineItemRenderer(MachineModels.CrystalCrusher.layer().bakeRoot(),
                    MachineModels.CrystalCrusher.TEXTURE);
        }
        return crusher;
    }

    public static MachineItemRenderer concentrator() {
        if (concentrator == null) {
            concentrator = new MachineItemRenderer(MachineModels.FluxConcentrator.layer().bakeRoot(),
                    MachineModels.FluxConcentrator.TEXTURE);
        }
        return concentrator;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5F, 1.5F, 0.5F);
        pose.mulPose(Axis.XP.rotationDegrees(180.0F));
        model.render(pose, buffers.getBuffer(RenderType.entityCutout(texture)), light, overlay);
        pose.popPose();
    }
}
