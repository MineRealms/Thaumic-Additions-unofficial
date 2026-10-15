package dev.tc4port.thaumadditions.armor;

import dev.tc4port.thaumadditions.platform.TaArmorSetMember;
import dev.tc4port.thaumadditions.platform.TaArmorTick;
import dev.tc4port.thaumadditions.platform.TaEffect;
import dev.tc4port.thaumadditions.platform.TaPlatform;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The 1.12 {@code ItemMithminiteDress.onArmorTick} behaviour, ported to the
 * subset that has a portable modern equivalent.
 *
 * <p>Ported: the full-set harmful-effect cleanse, the head's water breathing and
 * dark-vision night vision, and the chest's fire resistance + extinguish.</p>
 *
 * <p>Not ported yet, because the systems they hook do not exist in the port:
 * the sanity-checker potion (TC4R has the HUD item but no matching effect),
 * the odour-powder warp ward (odour powder is still an inert item), the
 * {@code TAR_Flight} entity flag and {@code TAR_LockFOV} (no flight or FOV-lock
 * system), the chest's +20 max-health attribute, and the legs/feet movement
 * bonuses (step height, speed, jump).</p>
 */
public final class MithminiteArmorBehavior implements TaArmorTick {
    /** Set id shared by the four mithminite pieces (see {@link TaArmorSetMember}). */
    public static final String SET_ID = "mithminite";

    private static final int CLEANSE_INTERVAL = 20;
    private static final int HEAD_INTERVAL = 10;
    private static final int WATER_BREATHING_TICKS = 31;
    private static final int NIGHT_VISION_TICKS = 299;
    private static final int FIRE_RESISTANCE_TICKS = 119;
    /** 1.12 tested {@code lightBrightness * 16 < 7}. */
    private static final int DARK_LIGHT_LEVEL = 7;

    private static final EquipmentSlot[] SET_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final TaPlatform platform;

    public MithminiteArmorBehavior(TaPlatform platform) {
        this.platform = platform;
    }

    @Override
    public void onWornTick(ItemStack stack, Level level, Player wearer, ArmorItem.Type type) {
        if (level.isClientSide)
            return;

        if (wearer.tickCount % CLEANSE_INTERVAL == 0 && wearingFullSet(wearer))
            platform.clearHarmfulEffects(wearer);

        switch (type) {
            case HELMET -> head(level, wearer);
            case CHESTPLATE -> chest(wearer);
            default -> {
            }
        }
    }

    private void head(Level level, Player wearer) {
        if (wearer.tickCount % HEAD_INTERVAL != 0)
            return;

        if (wearer.isInWater())
            platform.applyEffect(wearer, TaEffect.WATER_BREATHING, WATER_BREATHING_TICKS, 0);

        if (level.getMaxLocalRawBrightness(wearer.blockPosition()) < DARK_LIGHT_LEVEL) {
            platform.applyEffect(wearer, TaEffect.NIGHT_VISION, NIGHT_VISION_TICKS, 0);
        } else if (platform.hasEffect(wearer, TaEffect.NIGHT_VISION)) {
            platform.removeEffect(wearer, TaEffect.NIGHT_VISION);
        }
    }

    private void chest(Player wearer) {
        if (wearer.isOnFire()) {
            platform.applyEffect(wearer, TaEffect.FIRE_RESISTANCE, FIRE_RESISTANCE_TICKS, 0);
            wearer.clearFire();
        }
    }

    private static boolean wearingFullSet(Player player) {
        for (EquipmentSlot slot : SET_SLOTS) {
            ItemStack worn = player.getItemBySlot(slot);
            if (!(worn.getItem() instanceof TaArmorSetMember member) || !SET_ID.equals(member.armorSetId()))
                return false;
        }
        return true;
    }
}
