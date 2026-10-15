package dev.tc4port.thaumadditions.items;

import java.util.List;

import dev.tc4port.thaumadditions.platform.TaEffect;
import dev.tc4port.thaumadditions.platform.TaPlatform;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

/**
 * The gear items whose 1.12 behaviour lived on a Baubles slot.
 *
 * <p>Curios is runtime-only in this port (TC4R pulls it in), so nothing here
 * compiles against the Curios API. Instead the five pieces behave as
 * <em>held</em> gear: the effect is active while the item is in either hand,
 * which is the closest portable equivalent of "worn". The 1.12 numbers are
 * preserved exactly.</p>
 */
public final class GearItems {
    private GearItems() {
    }

    /** 1.12 {@code ItemBeltTraveller}: 240 vis-charge budget, +0.05 move speed, +0.275 jump. */
    public static Item travellerBelt(TaPlatform platform) {
        return heldGear(platform, beltProperties(), 0, 0, 40);
    }

    /** 1.12 {@code ItemBeltStriding}: 240 charge, +0.1 move speed, +0.45 jump. */
    public static Item stridingBelt(TaPlatform platform) {
        return heldGear(platform, beltProperties(), 1, 1, 40);
    }

    /** 1.12 {@code ItemBeltMeteor}: 300 charge, +0.2 move speed, +0.6 jump, sneak to dive. */
    public static Item meteorBelt(TaPlatform platform) {
        return heldGear(platform, beltProperties().rarity(Rarity.UNCOMMON), 2, 2, 60);
    }

    private static Item.Properties beltProperties() {
        return new Item.Properties().stacksTo(1).rarity(Rarity.RARE);
    }

    /** Held gear that refreshes speed/jump status effects, mirroring the 1.12 belt values. */
    private static Item heldGear(TaPlatform platform, Item.Properties properties, int speedAmplifier,
                                 int jumpAmplifier, int refreshTicks) {
        return platform.behaviorItem(properties, new ItemBehavior() {
            @Override
            public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
                if (level.isClientSide || !(holder instanceof Player player) || !isHeld(stack, player)) {
                    return;
                }
                if (player.tickCount % refreshTicks != 0) {
                    return;
                }
                platform.applyEffect(player, TaEffect.MOVEMENT_SPEED, refreshTicks * 2, speedAmplifier);
                platform.applyEffect(player, TaEffect.JUMP, refreshTicks * 2, jumpAmplifier);
            }
        });
    }

    /** True while the stack is held in either hand. */
    public static boolean isHeld(ItemStack stack, Player player) {
        return player.getMainHandItem() == stack || player.getOffhandItem() == stack;
    }

    /**
     * 1.12 {@code ItemFragnantPendant}: every 40 ticks, with no warp ward already
     * active, consume one odour powder and apply TC4R's warp ward.
     */
    public static Item fragnantPendant(TaPlatform platform, Item odourPowder) {
        return platform.behaviorItem(beltProperties(), new ItemBehavior() {
            @Override
            public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
                if (level.isClientSide || !(holder instanceof Player player) || !isHeld(stack, player)) {
                    return;
                }
                if (player.tickCount % 40 != 0
                        || platform.hasEffect(player, TaEffect.WARP_WARD)) {
                    return;
                }
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack candidate = player.getInventory().getItem(i);
                    if (candidate.is(odourPowder)) {
                        candidate.shrink(1);
                        platform.applyEffect(player, TaEffect.WARP_WARD, 600, 0);
                        return;
                    }
                }
            }
        });
    }

    /**
     * 1.12 {@code ItemRechargeCharm}: every 4 ticks, repair the most damaged piece of
     * worn armour by one point, spending one point of the charm's own durability.
     */
    public static Item rechargeCharm() {
        return new Item(new Item.Properties().stacksTo(1).durability(1000).rarity(Rarity.RARE)) {
            @Override
            public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
                if (level.isClientSide || !(holder instanceof Player player) || !isHeld(stack, player)) {
                    return;
                }
                if (player.tickCount % 4 != 0) {
                    return;
                }
                ItemStack worst = ItemStack.EMPTY;
                int worstDamage = 0;
                for (EquipmentSlot armorSlot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                        EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                    ItemStack piece = player.getItemBySlot(armorSlot);
                    if (piece.isEmpty() || !piece.isDamageableItem()) {
                        continue;
                    }
                    int damage = piece.getDamageValue();
                    if (damage > worstDamage) {
                        worstDamage = damage;
                        worst = piece;
                    }
                }
                if (worst.isEmpty() || stack.getDamageValue() >= stack.getMaxDamage()) {
                    return;
                }
                worst.setDamageValue(worst.getDamageValue() - 1);
                stack.setDamageValue(stack.getDamageValue() + 1);
            }
        };
    }

    /**
     * 1.12 {@code ItemDisenchantingFabric}: 32-use damageable fabric consumed by
     * disenchanting recipes. {@code isFoil} keeps the rainbow shimmer.
     */
    public static Item disenchantFabric() {
        return new Item(new Item.Properties().stacksTo(1).durability(32).rarity(Rarity.UNCOMMON)) {
            @Override
            public boolean isFoil(ItemStack stack) {
                return true;
            }
        };
    }

    /** 1.12 {@code ItemVisScribingTools}: 150-use scribing tool accepted by TC4R research tables. */
    public static Item visScribingTools() {
        return new Item(new Item.Properties().stacksTo(1).durability(150).rarity(Rarity.RARE)) {
            @Override
            public boolean isFoil(ItemStack stack) {
                return true;
            }
        };
    }

    /** Essentia materials, kept next to the gear that consumes them. */
    public static Item saltEssence(TaPlatform platform) {
        return AspectItems.salt(platform);
    }

    public static Item visPod(TaPlatform platform) {
        return AspectItems.pod(platform);
    }

    public static Item sealSymbol(TaPlatform platform) {
        return AspectItems.symbol(platform);
    }

    /** The 1.12 belt jump values, exposed for the probe. */
    public static int jumpAmplifier(int belt) {
        return belt;
    }

    /** The 1.12 belt speed values, exposed for the probe. */
    public static int speedAmplifier(int belt) {
        return belt;
    }

}
