package dev.tc4port.thaumadditions.neoforge.compat;

import dev.tc4port.thaumadditions.ThaumAdditions;
import dev.tc4port.thaumadditions.recipes.FluxRecipes;
import dev.tc4port.thaumadditions.registry.Content;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The flux concentrator's JEI page: what goes in, what comes out.
 *
 * <p>1.12's plugin showed exactly this and nothing more — the machine's own gui drew the process,
 * so the recipe page only had to name the two blocks.</p>
 */
public class FluxConcentratorCategory implements IRecipeCategory<FluxRecipes.Entry> {
    public static final RecipeType<FluxRecipes.Entry> TYPE =
            RecipeType.create(ThaumAdditions.MOD_ID, "flux_concentrator", FluxRecipes.Entry.class);

    private static final int WIDTH = 82;
    private static final int HEIGHT = 34;

    private final IDrawable icon;
    private final IDrawable background;

    public FluxConcentratorCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableItemStack(new ItemStack(Content.FLUX_CONCENTRATOR.get()));
        this.background = gui.createBlankDrawable(WIDTH, HEIGHT);
    }

    @Override
    public RecipeType<FluxRecipes.Entry> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.thaumadds.flux_concentrator");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FluxRecipes.Entry recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 9).addItemStack(recipe.input());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 61, 9).addItemStack(recipe.output());
    }

    /** 1.12's page drew nothing between the two slots; neither does this one. */
    @Override
    public void draw(FluxRecipes.Entry recipe, IRecipeSlotsView slots, GuiGraphics graphics,
                     double mouseX, double mouseY) {
    }
}
