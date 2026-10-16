package dev.tc4port.thaumadditions.items;

import dev.tc4port.thaumadditions.aspect.TaAspect;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The {@code @ASPECT} hole in the language files, filled in.
 *
 * <p>1.12 gave its crystal block and its seal symbol a name that carried their aspect, and it did
 * that by writing {@code @ASPECT} into the language file where the aspect's name belongs and
 * swapping it out when the stack was asked for its name — the aspect is not known until then, and
 * putting it in the language file would mean a line per aspect per language. The port brought the
 * language files across unchanged, so without this the name reads {@code @ASPECT Crystal Block}.</p>
 */
public final class AspectNames {
    /** What the language files write where the aspect goes. */
    private static final String PLACEHOLDER = "@ASPECT";

    private AspectNames() {
    }

    /**
     * The stack's name, with the placeholder replaced by the aspect the stack carries.
     *
     * <p>The name handed in has already been resolved for the reader's language, so the
     * replacement is the one thing that crosses languages: every language leaves the same hole.
     * A stack with no aspect leaves the name alone rather than leaving a bare marker in it.</p>
     */
    public static Component substitute(Component name, ItemStack stack) {
        String text = name.getString();
        if (!text.contains(PLACEHOLDER)) {
            return name;
        }
        AspectId aspect = AspectItems.aspectOf(stack);
        // A stack with no aspect loses the placeholder rather than showing it. That happens when
        // an item is spawned in or given out by a command, which the aspect-on-creation hook
        // cannot catch; a name missing its aspect reads better than a name with a marker in it.
        String filled = aspect == null ? "" : TaAspect.displayName(aspect);
        return Component.literal(text.replace(PLACEHOLDER, filled).trim())
                .withStyle(name.getStyle());
    }
}
