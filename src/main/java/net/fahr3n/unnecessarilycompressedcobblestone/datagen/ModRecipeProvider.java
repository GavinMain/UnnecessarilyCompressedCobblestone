package net.fahr3n.unnecessarilycompressedcobblestone.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        // Level 1 is nine vanilla cobblestone; every level after that is nine of the level below it.
        compress(recipeOutput, Items.COBBLESTONE, ModBlocks.byLevel(1).get());

        for (int level = 2; level <= ModBlocks.MAX_COMPRESSION_LEVEL; level++) {
            compress(recipeOutput, ModBlocks.byLevel(level - 1).get(), ModBlocks.byLevel(level).get());
        }

        // The armor set: one compression level deeper for every piece further down the body.
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.COMPRESSED_COBBLESTONE_HELMET.get())
                .pattern("CCC")
                .pattern("C C")
                .define('C', ModBlocks.byLevel(1).get())
                .unlockedBy(getHasName(ModBlocks.byLevel(1).get()), has(ModBlocks.byLevel(1).get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.COMPRESSED_COBBLESTONE_CHESTPLATE.get())
                .pattern("C C")
                .pattern("CCC")
                .pattern("CCC")
                .define('C', ModBlocks.byLevel(2).get())
                .unlockedBy(getHasName(ModBlocks.byLevel(2).get()), has(ModBlocks.byLevel(2).get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.COMPRESSED_COBBLESTONE_LEGGINGS.get())
                .pattern("CCC")
                .pattern("C C")
                .pattern("C C")
                .define('C', ModBlocks.byLevel(3).get())
                .unlockedBy(getHasName(ModBlocks.byLevel(3).get()), has(ModBlocks.byLevel(3).get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.COMPRESSED_COBBLESTONE_BOOTS.get())
                .pattern("C C")
                .pattern("C C")
                .define('C', ModBlocks.byLevel(4).get())
                .unlockedBy(getHasName(ModBlocks.byLevel(4).get()), has(ModBlocks.byLevel(4).get()))
                .save(recipeOutput);
    }

    /** Nine of {@code ingredient} in a crafting grid makes one {@code result}. */
    private void compress(RecipeOutput recipeOutput, ItemLike ingredient, ItemLike result) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, result)
                .pattern("CCC")
                .pattern("CCC")
                .pattern("CCC")
                .define('C', ingredient)
                .unlockedBy(getHasName(ingredient), has(ingredient))
                .save(recipeOutput);
    }

    protected static void oreSmelting(RecipeOutput recipeOutput, List<ItemLike> ingredients, RecipeCategory category,
                                      ItemLike result, float experience, int cookingTime, String group) {
        oreCooking(recipeOutput, RecipeSerializer.SMELTING_RECIPE, SmeltingRecipe::new, ingredients, category, result,
                experience, cookingTime, group, "_from_smelting");
    }

    protected static void oreBlasting(RecipeOutput recipeOutput, List<ItemLike> ingredients, RecipeCategory category,
                                      ItemLike result, float experience, int cookingTime, String group) {
        oreCooking(recipeOutput, RecipeSerializer.BLASTING_RECIPE, BlastingRecipe::new, ingredients, category, result,
                experience, cookingTime, group, "_from_blasting");
    }

    protected static <T extends AbstractCookingRecipe> void oreCooking(RecipeOutput recipeOutput, RecipeSerializer<T> cookingSerializer,
                                                                      AbstractCookingRecipe.Factory<T> factory, List<ItemLike> ingredients,
                                                                      RecipeCategory category, ItemLike result, float experience,
                                                                      int cookingTime, String group, String recipeName) {
        for (ItemLike itemlike : ingredients) {
            SimpleCookingRecipeBuilder.generic(Ingredient.of(itemlike), category, result, experience, cookingTime, cookingSerializer, factory)
                    .group(group).unlockedBy(getHasName(itemlike), has(itemlike))
                    .save(recipeOutput, UnnecessarilyCompressedCobblestone.MOD_ID + ":" + getItemName(result) + recipeName + "_" + getItemName(itemlike));
        }
    }
}
