package dev.tc4port.thaumadditions.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.tc4port.thaumadditions.client.model.MachineModels;
import dev.tc4port.thaumadditions.tiles.TaRotator;
import dev.tc4port.thaumadditions.tiles.TaTiles;
import dev.tc4port.thaumadditions.tiles.TileAspectCombiner;
import dev.tc4port.thaumadditions.tiles.TileCrystalBore;
import dev.tc4port.thaumadditions.tiles.TileCrystalCrusher;
import dev.tc4port.thaumadditions.tiles.TileFluxConcentrator;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The machines the 1.12 mod drew with a TESR rather than with blockstate models.
 *
 * <p>Each mirrors its upstream renderer: the pose is moved to the block's centre and rotated so the
 * model's own "up" points along the block's facing, the model is drawn, and the extra per-machine
 * offset the 1.12 code applied with {@code GL11.glTranslatef} is applied here. Where 1.12 handed a
 * {@code FrictionRotator} to the model, the machine's {@link TaRotator} is asked for its
 * {@code actualRotation(partialTicks)} and the same part is turned about the same axis.</p>
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
                : state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                        ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                        : Direction.NORTH;
    }

    /** Draws one model subtree in the block's orientation. */
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

    /** 1.12 {@code TESRCrystalBore}: the drill head turns about Y while the machine bores. */
    public static final class CrystalBore implements BlockEntityRenderer<TileCrystalBore> {
        private static final MachineModels.CrystalBore MODEL =
                new MachineModels.CrystalBore(MachineModels.CrystalBore.layer().bakeRoot());

        @Override
        public void render(TileCrystalBore be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                           int light, int overlay) {
            draw(pose, buffers, MachineModels.CrystalBore.TEXTURE, be.getBlockState(), light, overlay,
                    MODEL.staticParts(), -0.5F);
            pose.pushPose();
            orient(pose, facing(be.getBlockState()).getOpposite());
            pose.translate(0.0F, -0.5F, 0.0F);
            pose.mulPose(Axis.YP.rotationDegrees(be.rotator().actualRotation(partialTick)));
            MODEL.rotator().render(pose, buffers.getBuffer(
                    RenderType.entityCutout(MachineModels.CrystalBore.TEXTURE)), light, overlay);
            pose.popPose();
        }
    }

    /**
     * 1.12 {@code TESRAspectCombiner}: the rotor is drawn twice, the second time a further 45°
     * around, which is what gives the knob its four-fold look.
     */
    public static final class AspectCombiner implements BlockEntityRenderer<TileAspectCombiner> {
        private static final MachineModels.AspectCombiner MODEL =
                new MachineModels.AspectCombiner(MachineModels.AspectCombiner.layer().bakeRoot());

        @Override
        public void render(TileAspectCombiner be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                           int light, int overlay) {
            draw(pose, buffers, MachineModels.AspectCombiner.TEXTURE, be.getBlockState(), light, overlay,
                    MODEL.staticParts(), 0.0F);
            pose.pushPose();
            orient(pose, facing(be.getBlockState()).getOpposite());
            var buffer = buffers.getBuffer(RenderType.entityCutout(MachineModels.AspectCombiner.TEXTURE));
            pose.mulPose(Axis.YP.rotationDegrees(be.rotator().actualRotation(partialTick)));
            MODEL.rotor().render(pose, buffer, light, overlay);
            pose.mulPose(Axis.YP.rotationDegrees(45.0F));
            MODEL.rotor().render(pose, buffer, light, overlay);
            pose.popPose();
        }
    }

    /** 1.12 {@code TESRAuraCharger}: the sprinkler arms turn about Y while the machine is charged. */
    public static final class AuraCharger implements BlockEntityRenderer<TaTiles.Charger> {
        private static final MachineModels.AuraCharger MODEL =
                new MachineModels.AuraCharger(MachineModels.AuraCharger.layer().bakeRoot());

        @Override
        public void render(TaTiles.Charger be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                           int light, int overlay) {
            draw(pose, buffers, MachineModels.AuraCharger.TEXTURE, be.getBlockState(), light, overlay,
                    MODEL.staticParts(), 0.0F);
            pose.pushPose();
            orient(pose, facing(be.getBlockState()).getOpposite());
            pose.mulPose(Axis.YP.rotationDegrees(be.rotator().actualRotation(partialTick)));
            MODEL.rotor().render(pose, buffers.getBuffer(
                    RenderType.entityCutout(MachineModels.AuraCharger.TEXTURE)), light, overlay);
            pose.popPose();
        }
    }

    /** 1.12 {@code TESRCrystalCrusher}: the crushing head turns about Y while it works. */
    public static final class CrystalCrusher implements BlockEntityRenderer<TileCrystalCrusher> {
        private static final MachineModels.CrystalCrusher MODEL =
                new MachineModels.CrystalCrusher(MachineModels.CrystalCrusher.layer().bakeRoot());

        @Override
        public void render(TileCrystalCrusher be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                           int light, int overlay) {
            draw(pose, buffers, MachineModels.CrystalCrusher.TEXTURE, be.getBlockState(), light, overlay,
                    MODEL.staticParts(), 0.0F);
            pose.pushPose();
            orient(pose, facing(be.getBlockState()).getOpposite());
            pose.mulPose(Axis.YP.rotationDegrees(be.rotator().actualRotation(partialTick)));
            MODEL.head().render(pose, buffers.getBuffer(
                    RenderType.entityCutout(MachineModels.CrystalCrusher.TEXTURE)), light, overlay);
            pose.popPose();
        }
    }

    /**
     * 1.12 {@code TESRFluxConcentrator}: the valve wheel turns about Z. Upstream re-pivoted the part
     * by hand before rotating it — translate to the part's own rotation point, offset by one unit,
     * rotate, offset back — which is what the pose dance below reproduces.
     */
    public static final class FluxConcentrator implements BlockEntityRenderer<TileFluxConcentrator> {
        private static final MachineModels.FluxConcentrator MODEL =
                new MachineModels.FluxConcentrator(MachineModels.FluxConcentrator.layer().bakeRoot());

        @Override
        public void render(TileFluxConcentrator be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                           int light, int overlay) {
            draw(pose, buffers, MachineModels.FluxConcentrator.TEXTURE, be.getBlockState(), light, overlay,
                    MODEL.staticParts(), 0.0F);
            pose.pushPose();
            orient(pose, facing(be.getBlockState()).getOpposite());
            pose.translate(1.0F / 16.0F, 1.0F / 16.0F, 0.0F);
            pose.mulPose(Axis.ZP.rotationDegrees(be.rotator().actualRotation(partialTick)));
            pose.translate(-1.0F / 16.0F, -1.0F / 16.0F, 0.0F);
            MODEL.valve().render(pose, buffers.getBuffer(
                    RenderType.entityCutout(MachineModels.FluxConcentrator.TEXTURE)), light, overlay);
            pose.popPose();
        }
    }
}
