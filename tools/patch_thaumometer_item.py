# Rebuilds the void thaumometer: same scan behaviour as before, but on an item whose model comes
# from a renderer, which is the only way its OBJ can be drawn any more.
import io

PATH = "src/main/java/dev/tc4port/thaumadditions/items/ToolItems.java"

REPORT = '''    /**
     * 1.12 {@code ItemVoidThaumometer}: reports the aspects of whatever the player is looking at —
     * an entity within nine blocks, else a block within sixteen, else whatever is in the other
     * hand. Uses TC4R's scan catalogs where 1.12 used TC6's {@code ScanningManager}.
     */
    private static void report(Level level, Player player, Vec3 eye, Vec3 look) {
        EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil
                .getEntityHitResult(level, player, eye, eye.add(look.scale(9.0D)),
                        new AABB(eye, eye.add(look.scale(9.0D))).inflate(1.0D),
                        entity -> entity instanceof LivingEntity && entity != player);
        AspectAmounts aspects;
        String label;
        if (entityHit != null) {
            aspects = AspectQueryApi.entity(entityHit.getEntity());
            label = entityHit.getEntity().getName().getString();
        } else {
            BlockHitResult blockHit = level.clip(new net.minecraft.world.level.ClipContext(eye,
                    eye.add(look.scale(16.0D)), net.minecraft.world.level.ClipContext.Block.OUTLINE,
                    net.minecraft.world.level.ClipContext.Fluid.NONE, player));
            if (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = blockHit.getBlockPos();
                aspects = AspectQueryApi.block(level.getBlockState(pos));
                label = level.getBlockState(pos).getBlock().getName().getString();
            } else {
                ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
                if (held.isEmpty()) {
                    return;
                }
                aspects = AspectQueryApi.item(held);
                label = held.getHoverName().getString();
            }
        }
        player.sendSystemMessage(Component.translatable("chat.thaumadds.thaumometer.target", label)
                .withStyle(ChatFormatting.GRAY));
        if (aspects.amounts().isEmpty()) {
            player.sendSystemMessage(Component.translatable("chat.thaumadds.thaumometer.empty")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        aspects.amounts().forEach((aspect, amount) -> player.sendSystemMessage(
                Component.literal("  " + TaAspect.displayName(aspect) + " x" + amount)
                        .withStyle(ChatFormatting.GRAY)));
    }

    /**
     * 1.12 {@code ItemVoidThaumometer}: a scanner with a model of its own.
     *
     * <p>Its 1.12 form was a Wavefront OBJ placed by a transform per context, which is how it sat
     * correctly in a hand. Neither modern loader reads an OBJ, so the item is built through
     * {@code modelledItem} and a renderer draws it; the scan below is the same behaviour it
     * always had.</p>
     */
    public static Item voidThaumometer(TaPlatform platform) {
        return platform.modelledItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE),
                dev.tc4port.thaumadditions.platform.ToolIcon.VOID_THAUMOMETER,
                new ItemBehavior() {
                    @Override
                    public InteractionResultHolder<ItemStack> use(Level level, Player player,
                                                                 InteractionHand hand) {
                        ItemStack stack = player.getItemInHand(hand);
                        if (!level.isClientSide) {
                            report(level, player, player.getEyePosition(1.0F),
                                    player.getViewVector(1.0F));
                            level.playSound(null, player.blockPosition(),
                                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
                                    0.6F, 1.6F);
                        }
                        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
                    }
                });
    }
'''

source = io.open(PATH, encoding="utf-8").read()

# Replace everything from the doc comment above voidThaumometer through the end of its body, and
# drop the stub split the last edit left behind.
start = source.index("    /**\n     * 1.12 {@code ItemVoidThaumometer}: reports the aspects")
end = source.index("    /**\n     * 1.12 {@code ItemWormholeMirror}")
io.open(PATH, "w", encoding="utf-8", newline="\n").write(source[:start] + REPORT + "\n" + source[end:])
print("void thaumometer rebuilt")
