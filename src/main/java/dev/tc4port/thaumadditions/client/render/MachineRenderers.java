package dev.tc4port.thaumadditions.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.tc4port.thaumadditions.client.model.MachineModels;
import dev.tc4port.thaumadditions.tiles.TaTiles;
import dev.tc4port.thaumadditions.tiles.TileAspectCombiner;
import dev.tc4port.thaumadditions.tiles.TileGrowthChamber;
import dev.tc4port.thaumadditions.tiles.TileCrystalCrusher;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The five machines the 1.12 mod drew with a TESR rather than with blockstate models.
 *
 * <p>Each mirrors its upstream renderer: the pose is moved to the block's centre and rotated so
 * the model's own "up" points along the block's facing, the model is drawn, and the extra
 * per-machine offset that the 1.12 code applied with {@code GL11.glTranslatef} is applied here.
 * The rotation angles the 1.12 renderers fed in (the bore's drill, the concentrator's valve)
 * follow once the block entities carry that state.</p>
 */
public final class MachineRenderers {
    private MachineRenderers() {
    }

    /**
     * HammerCore's {@code TESR.translateFromOrientation}: moves the origin to the middle of the
     * face the block faces and rotates the model so its own +Y runs along that facing.
     *
     * <p>1.20.1's {@link Direction} keeps 1.12's {@code EnumFacing} ordinal order
     * (down, up, north, south, west, east), so the cases carry over unchanged.</p>
     */
    public static void orient(PoseStack pose, Direction facing) {
        switch (facing) {
            case DOWN -> {
                pose.translate(0.5F, 1.0F, 0.5F);
                pose.mulPose(Axis.XP.rotationDegrees(180.0F));
            }
            case UP -> pose.translate(0.5F, 0.0F, 0.5F);
            case NORTH -> {
                pose.translate(0.5F, 0.5F, 1.0F);
                pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
            }
            case SOUTH -> {
                pose.translate(0.5F, 0.5F, 0.0F);
                pose.mulPose(Axis.XP.rotationDegrees(90.0F));
            }
            case WEST -> {
                pose.translate(1.0F, 0.5F, 0.5F);
                pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
            }
            case EAST -> {
                pose.translate(0.0F, 0.5F, 0.5F);
                pose.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
        }
    }

    /** The facing the block was placed with. */
    public static Direction facing(BlockState state) {
        return state.hasProperty(BlockStateProperties.FACING)
                ? state.getValue(BlockStateProperties.FACING)
                : Direction.NORTH;
    }

    /**
     * Draws one model in the block's orientation. The 1.12 renderers all rotated away from the
     * block's own facing ({@code te.face.getOpposite()}), so the same flip is applied here.
     */
    private static void draw(PoseStack pose, MultiBufferSource buffers, ResourceLocation texture,
                             BlockState state, int light, int overlay, ModelPart model,
                             float dropY) {
        pose.pushPose();
        orient(pose, facing(state).getOpposite());
        if (dropY != 0.0F) {
            pose.translate(0.0F, dropY, 0.0F);
        }
        model.render(pose, buffers.getBuffer(RenderType.entityCutout(texture)), light, overlay);
        pose.popPose();
    }

    /** 1.12 {@code TESRCrystalBore}. */
    public static final class CrystalBore implements BlockEntityRenderer<TaTiles.Bore> {
        private static final ModelPart ROOT = MachineModels.CrystalBore.layer().bakeRoot();

        @Override
        public void render(TaTiles.Bore be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                           int light, int overlay) {
            draw(pose, buffers, MachineModels.CrystalBore.TEXTURE, be.getBlockState(), light, overlay,
                    ROOT, -0.5F);
        }
    }

    /** 1.12 {@code TESRAspectCombiner}. */
    public static final class AspectCombiner implements BlockEntityRenderer<TileAspectCombiner> {
        private static final ModelPart ROOT = MachineModels.AspectCombiner.layer().bakeRoot();

        @Override
        public void render(TileAspectCombiner be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                           int light, int overlay) {
            draw(pose, buffers, MachineModels.AspectCombiner.TEXTURE, be.getBlockState(), light, overlay,
                    ROOT, 0.0F);
        }
    }

    /** 1.12 {@code TESRAuraCharger}. */
    public static final class AuraCharger implements BlockEntityRenderer<TaTiles.Charger> {
        private static final ModelPart ROOT = MachineModels.AuraCharger.layer().bakeRoot();

        @Override
        public void render(TaTiles.Charger be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                           int light, int overlay) {
            draw(pose, buffers, MachineModels.AuraCharger.TEXTURE, be.getBlockState(), light, overlay,
                    ROOT, 0.0F);
        }
    }

    /** 1.12 {@code TESRCrystalCrusher}. */
    public static final class CrystalCrusher implements BlockEntityRenderer<TileCrystalCrusher> {
        private static final ModelPart ROOT = MachineModels.CrystalCrusher.layer().bakeRoot();

        @Override
        public void render(TileCrystalCrusher be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                           int light, int overlay) {
            draw(pose, buffers, MachineModels.CrystalCrusher.TEXTURE, be.getBlockState(), light, overlay,
                    ROOT, 0.0F);
        }
    }

    /** 1.12 {@code TESRFluxConcentrator}. */
    public static final class FluxConcentrator implements BlockEntityRenderer<TaTiles.Concentrator> {
        private static final MachineModels.FluxConcentrator MODEL =
                new MachineModels.FluxConcentrator(MachineModels.FluxConcentrator.layer().bakeRoot());

        @Override
        public void render(TaTiles.Concentrator be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                           int light, int overlay) {
            draw(pose, buffers, MachineModels.FluxConcentrator.TEXTURE, be.getBlockState(), light, overlay,
                    MODEL.root(), 0.0F);
        }
    }
}
