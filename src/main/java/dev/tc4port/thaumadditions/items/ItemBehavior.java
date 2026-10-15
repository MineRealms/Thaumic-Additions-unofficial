package dev.tc4port.thaumadditions.items;

import java.util.List;
import java.util.function.Function;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * The item hooks ThaumicAdditions gear uses, as a plain interface.
 *
 * <p>Several {@link Item} methods drifted between 1.20.1 and 1.21.1 —
 * {@code appendHoverText} gained/lost its context parameter, the bonemeal hooks
 * changed arity — so a shared {@code Item} subclass cannot override them. Each
 * node therefore supplies one small anonymous subclass (see
 * {@code TaPlatform.behaviorItem}) that forwards to this interface, and every
 * ported item is written against the interface instead.</p>
 */
public interface ItemBehavior {
    /** 1.12 {@code onItemRightClick}. */
    default InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(ItemStack.EMPTY);
    }

    /** 1.12 {@code onItemUse}. */
    default InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS;
    }

    /** 1.12 {@code itemInteractionForEntity}. */
    default InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                   InteractionHand hand) {
        return InteractionResult.PASS;
    }

    /** 1.12 {@code onUpdate}. */
    default void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
    }

    /** 1.12 {@code onCreated} / {@code onCrafting}. */
    default void onCreated(ItemStack stack, Level level, Player player) {
    }

    /** 1.12 {@code hasEffect}. */
    default boolean isFoil(ItemStack stack) {
        return false;
    }

    /** 1.12 {@code addInformation}. */
    default List<Component> tooltipLines(ItemStack stack) {
        return List.of();
    }

    /** Convenience: an item with tooltip lines only. */
    static Function<ItemStack, List<Component>> lines(List<Component> constant) {
        return stack -> constant;
    }

    /** Convenience for the (unused) tooltip flag, kept so impls can match signatures. */
    static TooltipFlag flag() {
        return TooltipFlag.Default.NORMAL;
    }
}
