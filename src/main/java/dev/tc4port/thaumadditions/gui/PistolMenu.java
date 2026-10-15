package dev.tc4port.thaumadditions.gui;

import dev.tc4port.thaumadditions.items.ThaumcraftItems;
import dev.tc4port.thaumadditions.registry.Content;
import dev.tc4port.thaumcraft.api.aspect.AspectAmounts;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.hclite.platform.HCLitePlatform;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Loading the essentia pistol.
 *
 * <p>1.12's pistol was loaded by putting a jar inside it, through a helper that read and wrote a jar
 * block item's contents. The modern Thaumcraft's jars are blocks whose contents live in the block
 * entity, so there is no jar item to put in — the same gap that stops the aura charger. The port
 * therefore loads from crystallised essence instead, which is what the rest of the port's machines
 * already consume, and it also fixes a one-way problem the pistol had: nothing could load it at
 * all, so a pistol could only ever be fired once.</p>
 *
 * <p>One slot: put a crystallised essence in and it goes straight into the pistol. There is nothing
 * to take back out, because 1.12 had no way to unload one either.</p>
 */
public class PistolMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176;
    public static final int HEIGHT = 166;
    /** 1.12's pistol held five hundred points at once. */
    private static final int CAPACITY = 500;

    private final InteractionHand hand;
    private final Player owner;

    /** Client side: the hand arrives in the opening data. */
    public PistolMenu(int windowId, Inventory inventory, FriendlyByteBuf data) {
        this(windowId, inventory, data == null ? InteractionHand.MAIN_HAND
                : InteractionHand.values()[data.readVarInt()]);
    }

    private PistolMenu(int windowId, Inventory inventory, InteractionHand hand) {
        super(Content.PISTOL_MENU.get(), windowId);
        this.hand = hand;
        this.owner = inventory.player;

        Container feed = new SimpleContainer(1) {
            /** Guards the empty-out below: emptying is itself a change. */
            private boolean loading;

            @Override
            public void setChanged() {
                if (loading) {
                    super.setChanged();
                    return;
                }
                // Anything dropped in is loaded immediately and the slot empties again.
                ItemStack placed = getItem(0);
                super.setChanged();
                if (placed.isEmpty()) {
                    return;
                }
                loading = true;
                load(placed);
                super.setItem(0, ItemStack.EMPTY);
                loading = false;
            }
        };
        addSlot(new Slot(feed, 0, 80, 30) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ThaumcraftItems.crystalEssence());
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    /** Moves one crystallised essence's aspect and amount into the pistol. */
    private void load(ItemStack essence) {
        if (essence.isEmpty() || owner == null || !(essence.getItem()
                instanceof dev.tc4port.thaumcraft.api.aspect.AspectItemView view)) {
            return;
        }
        AspectAmounts stored = view.storedAspectData(essence);
        AspectId aspect = null;
        int amount = 0;
        for (var entry : stored.amounts().entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0) {
                aspect = entry.getKey();
                amount = entry.getValue();
                break;
            }
        }
        if (aspect == null) {
            return;
        }
        ItemStack pistol = owner.getItemInHand(hand);
        if (!pistol.is(Content.ESSENTIA_PISTOL.get())) {
            return;
        }
        AspectId finalAspect = aspect;
        int finalAmount = Math.min(CAPACITY, amount * essence.getCount());
        HCLitePlatform.Holder.get().updateItemTag(pistol, tag -> {
            dev.tc4port.thaumadditions.aspect.TaAspect.writeAspect(tag, "Aspect", finalAspect);
            tag.putInt("Essentia", finalAmount);
        });
    }

    /** Opens the reload screen on the server. */
    public static void open(ServerPlayer player, InteractionHand hand) {
        dev.tc4port.thaumadditions.platform.TaPlatform.Active.get().openMenu(player,
                new net.minecraft.world.MenuProvider() {
                    @Override
                    public net.minecraft.network.chat.Component getDisplayName() {
                        return net.minecraft.network.chat.Component.translatable(
                                "item.thaumadds.essentia_pistol");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player who) {
                        return new PistolMenu(windowId, inventory, hand);
                    }
                },
                buf -> buf.writeVarInt(hand.ordinal()));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return owner != null && owner.getItemInHand(hand).is(Content.ESSENTIA_PISTOL.get());
    }
}
