package dev.tc4port.thaumadditions.platform;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Behaviour shared by the ThaumicAdditions gear whose 1.12 form repaired
 * itself from the ambient aura.
 *
 * <p>TC4R dropped the ambient aura pool, so its own void tools restore
 * durability on a timer instead of draining vis. The port keeps that rule
 * so the tools stay functional rather than inert.</p>
 */
public final class TaGearBehavior {
    /** Ticks between one-durability restores. */
    public static final int SELF_REPAIR_INTERVAL = 20;

    private TaGearBehavior() {
    }

    /** Restores one durability point every {@link #SELF_REPAIR_INTERVAL} ticks while damaged. */
    public static void selfRepair(ItemStack stack, Level level, Entity carrier) {
        if (!level.isClientSide && stack.isDamaged() && carrier.tickCount % SELF_REPAIR_INTERVAL == 0)
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));
    }
}
