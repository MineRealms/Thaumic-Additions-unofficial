package dev.tc4port.thaumadditions.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.tc4port.hclite.platform.HCLitePlatform;
import dev.tc4port.thaumadditions.tiles.TileEntitySummoner;
import dev.tc4port.thaumadditions.tiles.TileShadowEnchanter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The two 1.12 machines that drew something other than a model: a hovering item, and a preview of
 * the creature about to be summoned.
 *
 * <p>1.12's {@code TESRShadowEnchanter} bobbed the item a twentieth of a block on a one-second sine
 * and turned it once a second; {@code TESRMobSummoner} drew the captured creature spinning above
 * the machine. The port keeps both motions and both cadences.</p>
 */
public final class ItemRenderers {
    private ItemRenderers() {
    }

    /**
     * 1.12 {@code TESRShadowEnchanter}: whichever slot is filled, hovering and turning above the
     * pedestal. Upstream preferred the input slot and fell back to the output.
     */
    public static final class ShadowEnchanter implements BlockEntityRenderer<TileShadowEnchanter> {
        /** One full turn a second, as upstream's {@code (ticksExisted + partial) % 360}. */
        private static final float DEGREES_PER_TICK = 6.0F;
        /** The bob's period, in ticks. */
        private static final float BOB_PERIOD = 60.0F;
        private static final double BOB_HEIGHT = 0.05D;

        @Override
        public void render(TileShadowEnchanter be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                           int light, int overlay) {
            ItemStack shown = be.getItem(TileShadowEnchanter.SLOT_INPUT);
            if (shown.isEmpty()) {
                shown = be.getItem(TileShadowEnchanter.SLOT_OUTPUT);
            }
            if (shown.isEmpty()) {
                return;
            }
            float ticks = be.getLevel() == null ? 0.0F : be.getLevel().getGameTime() + partialTick;

            pose.pushPose();
            pose.translate(0.5D, 1.0D + Math.sin(Math.PI * ((ticks / BOB_PERIOD) % BOB_PERIOD) / BOB_PERIOD)
                    * BOB_HEIGHT, 0.5D);
            pose.mulPose(Axis.YP.rotationDegrees(ticks * DEGREES_PER_TICK % 360.0F));
            pose.translate(0.08D, 0.0D, 0.0D);
            pose.mulPose(Axis.ZP.rotationDegrees(45.0F));

            ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
            renderer.renderStatic(shown, ItemDisplayContext.GROUND, light, overlay, pose, buffers,
                    be.getLevel(), 0);
            pose.popPose();
        }
    }

    /**
     * 1.12 {@code TESRMobSummoner}: the creature the cell holds, drawn small and turning above the
     * machine.
     *
     * <p>Upstream also drew a tesseract behind it. The port has no tesseract model and no texture
     * for one, so the creature is drawn on its own; the turn is the same half-angle twist.</p>
     */
    public static final class MobSummoner implements BlockEntityRenderer<TileEntitySummoner> {
        /** Upstream scaled the whole thing down to three tenths. */
        private static final float PREVIEW_SCALE = 0.3F;

        /** The preview entity, rebuilt only when the cell changes. */
        private Entity preview;
        private CompoundTag previewSource;
        /** The cage around the preview; null when the node has not been given one. */
        private final TesseractRenderer tesseract;

        public MobSummoner(TesseractRenderer tesseract) {
            this.tesseract = tesseract;
        }

        @Override
        public void render(TileEntitySummoner be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                           int light, int overlay) {
            Entity shown = preview(be);
            if (shown == null) {
                return;
            }
            float twist = be.rotator().actualRotation(partialTick) / 2.0F;
            pose.pushPose();
            pose.translate(0.5D, 0.5D, 0.5D);
            pose.mulPose(Axis.YP.rotationDegrees(twist));
            pose.scale(PREVIEW_SCALE, PREVIEW_SCALE, PREVIEW_SCALE);

            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            dispatcher.setRenderShadow(false);
            dispatcher.render(shown, 0.0D, 0.0D, 0.0D, 0.0F, partialTick, pose, buffers, light);
            dispatcher.setRenderShadow(true);
            pose.popPose();

            // 1.12 turned a hypercube around the creature it was about to summon: the mob preview
            // is only half of what that machine showed. The turn is the machine's own rotation over
            // two, in radians. The drawing itself is per node — see TesseractRenderer.
            if (tesseract != null) {
                pose.pushPose();
                pose.translate(0.5D, 0.5D, 0.5D);
                tesseract.render(pose, buffers, Math.toRadians(twist * 2.0F), light);
                pose.popPose();
            }
        }

        /** Builds and caches the preview creature from the cell in the machine's slot. */
        private Entity preview(TileEntitySummoner be) {
            Level level = be.getLevel();
            if (level == null) {
                return null;
            }
            ItemStack cell = be.getItem(0);
            CompoundTag tag = cell.isEmpty() ? null : HCLitePlatform.Holder.get().itemTag(cell);
            CompoundTag stored = tag == null ? null : tag.getCompound("Entity");
            if (stored == null || stored.isEmpty()) {
                preview = null;
                previewSource = null;
                return null;
            }
            if (preview != null && stored.equals(previewSource)) {
                return preview;
            }
            EntityType<?> type = EntityType.by(stored).orElse(null);
            if (type == null) {
                preview = null;
                previewSource = null;
                return null;
            }
            Entity built = type.create(level);
            if (built == null) {
                return null;
            }
            built.load(stored);
            preview = built;
            previewSource = stored.copy();
            return preview;
        }
    }
}
