package dev.tc4port.thaumadditions.forge.compat;

import dev.tc4port.thaumadditions.ThaumAdditions;
import dev.tc4port.thaumadditions.aspect.TaAspect;
import dev.tc4port.thaumadditions.items.AspectItems;
import dev.tc4port.thaumadditions.recipes.FluxRecipes;
import dev.tc4port.thaumadditions.registry.Content;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The 1.20.1 node's JEI plugin, written against JEI 15.
 *
 * <p>1.12's plugin did two things, and this does the same two:</p>
 *
 * <ul>
 *   <li>It taught JEI to tell the aspect-carrying items apart, so that a salt essence of ignis and
 *       one of aqua are different entries rather than one. The interpreter is the same idea as
 *       upstream's: read the stack's aspect and use it as the subtype.</li>
 *   <li>It showed the flux concentrator's table as a recipe category, with the machine as the
 *       catalyst that makes it.</li>
 * </ul>
 *
 * <p>The plugin lives in the node, not in shared source, because JEI's own API moved between the
 * two versions the nodes need. The recipe data it shows does not move, and lives in
 * {@link FluxRecipes}.</p>
 */
@JeiPlugin
public class ThaumAddsJeiPlugin implements IModPlugin {
    /** The id JEI files this plugin's recipes under. */
    public static final ResourceLocation UID =
            ResourceLocation.tryParse(ThaumAdditions.MOD_ID + ":jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new FluxConcentratorCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(FluxConcentratorCategory.TYPE, FluxRecipes.entries());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Content.FLUX_CONCENTRATOR.get()),
                FluxConcentratorCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(Content.MITHRILLIUM_SMELTER.get()),
                mezz.jei.api.constants.RecipeTypes.SMELTING);
    }

    /** The aspect of a stack, as a string JEI can sort by. */
    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        IIngredientSubtypeInterpreter<ItemStack> interpreter = (stack, context) -> {
            AspectId aspect = AspectItems.aspectOf(stack);
            return aspect == null ? IIngredientSubtypeInterpreter.NONE : TaAspect.displayName(aspect);
        };
        registration.registerSubtypeInterpreter(Content.SALT_ESSENCE.get(), interpreter);
        registration.registerSubtypeInterpreter(Content.VIS_POD.get(), interpreter);
        registration.registerSubtypeInterpreter(Content.SEAL_SYMBOL.get(), interpreter);
    }
}
