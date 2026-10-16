package dev.tc4port.thaumadditions.items;

import java.util.List;

import dev.tc4port.thaumadditions.aspect.TaAspect;
import dev.tc4port.thaumadditions.platform.TaPlatform;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The 1.12 essentia-carrying items ({@code ItemSaltEssence}, {@code ItemVisPod},
 * {@code ItemSealSymbol}): one aspect in the {@code "Aspect"} NBT tag, assigned on
 * creation, named with the aspect suffix.
 *
 * <p>The 1.12 {@code IEssentiaContainerItem} aspect source is dropped here: the
 * items keep their aspect identity and their tooltip, but essentia transfer is
 * handled by the block entities that consume them (crusher, combiner, jars).</p>
 */
public final class AspectItems {
    private AspectItems() {
    }

    /** One point of essentia, as carried by a salt essence. */
    public static Item salt(TaPlatform platform) {
        return create(platform, new Item.Properties().stacksTo(64), 1, false);
    }

    /** Five points per stack, as carried by a vis pod. */
    public static Item pod(TaPlatform platform) {
        return create(platform, new Item.Properties().stacksTo(64), 5, true);
    }

    /** No essentia at all; the aspect is only a label (seal symbols). */
    public static Item symbol(TaPlatform platform) {
        return create(platform, new Item.Properties().stacksTo(16), 0, false);
    }

    private static Item create(TaPlatform platform, Item.Properties properties, int essentiaPerStack,
                               boolean showsEssentia) {
        ItemBehavior behavior = new ItemBehavior() {
            @Override
            public void onCreated(ItemStack stack, net.minecraft.world.level.Level level,
                                  net.minecraft.world.entity.player.Player player) {
                if (aspectOf(stack) == null) {
                    setAspect(stack, TaAspect.random(level.getRandom()));
                }
            }

            @Override
            public boolean isFoil(ItemStack stack) {
                return aspectOf(stack) != null;
            }

            /** 1.12 {@code getItemStackDisplayName}: the language file leaves the aspect as a hole. */
            @Override
            public Component displayName(ItemStack stack, Component name) {
                return AspectNames.substitute(name, stack);
            }

            @Override
            public List<Component> tooltipLines(ItemStack stack) {
                AspectId aspect = aspectOf(stack);
                if (aspect == null) {
                    return List.of();
                }
                List<Component> lines = new java.util.ArrayList<>();
                lines.add(Component.translatable("tooltip.thaumadds.aspect",
                        TaAspect.displayName(aspect)).withStyle(ChatFormatting.GRAY));
                if (showsEssentia) {
                    lines.add(Component.translatable("tooltip.thaumadds.essentia",
                            essentiaPerStack * stack.getCount()).withStyle(ChatFormatting.GRAY));
                }
                return lines;
            }
        };
        return platform.behaviorItem(properties, behavior);
    }

    public static AspectId aspectOf(ItemStack stack) {
        return TaAspect.readAspect(dev.tc4port.hclite.platform.HCLitePlatform.Holder.get().itemTag(stack), "Aspect");
    }

    public static void setAspect(ItemStack stack, AspectId id) {
        dev.tc4port.hclite.platform.HCLitePlatform.Holder.get()
                .updateItemTag(stack, tag -> TaAspect.writeAspect(tag, "Aspect", id));
    }

    /** Total essentia this stack represents; {@code 0} for label-only items. */
    public static int essentia(ItemStack stack, int perStack) {
        return perStack * stack.getCount();
    }
}
