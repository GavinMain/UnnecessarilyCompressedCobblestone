package net.fahr3n.unnecessarilycompressedcobblestone.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.enchantment.ModEnchantments;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    /** Set before {@link #buildRecipes} runs; the Compression book recipe needs to look the enchantment up. */
    private HolderLookup.Provider registries;

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected CompletableFuture<?> run(CachedOutput output, HolderLookup.Provider registries) {
        this.registries = registries;
        return super.run(output, registries);
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

        // The sword picks up the ladder where the armor left off, at level 5.
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.COMPRESSED_COBBLESTONE_SWORD.get())
                .pattern("C")
                .pattern("C")
                .pattern("S")
                .define('C', ModBlocks.byLevel(5).get())
                .define('S', Items.STICK)
                .unlockedBy(getHasName(ModBlocks.byLevel(5).get()), has(ModBlocks.byLevel(5).get()))
                .save(recipeOutput);

        // The vanilla bow and arrow recipes with the wooden parts swapped for compressed stone:
        // sticks for the bow, the flint arrowhead for the arrow.
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.COMPRESSED_COBBLESTONE_BOW.get())
                .pattern(" CX")
                .pattern("C X")
                .pattern(" CX")
                .define('C', ModBlocks.byLevel(7).get())
                .define('X', Items.STRING)
                .unlockedBy(getHasName(ModBlocks.byLevel(7).get()), has(ModBlocks.byLevel(7).get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.COMPRESSED_COBBLESTONE_ARROW.get(), 4)
                .pattern("C")
                .pattern("S")
                .pattern("F")
                .define('C', ModBlocks.byLevel(8).get())
                .define('S', Items.STICK)
                .define('F', Items.FEATHER)
                .unlockedBy(getHasName(ModBlocks.byLevel(8).get()), has(ModBlocks.byLevel(8).get()))
                .save(recipeOutput);

        // An apple encased the way a golden apple is, one compression level deeper than the arrow.
        ShapedRecipeBuilder.shaped(RecipeCategory.FOOD, ModItems.COMPRESSED_COBBLESTONE_APPLE.get())
                .pattern("CCC")
                .pattern("CAC")
                .pattern("CCC")
                .define('C', ModBlocks.byLevel(9).get())
                .define('A', Items.APPLE)
                .unlockedBy(getHasName(ModBlocks.byLevel(9).get()), has(ModBlocks.byLevel(9).get()))
                .save(recipeOutput);

        // Level 13 stone around a heart cut out of a Compressed Golem, so the table cannot be built
        // until the golem has been.
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.COMPRESSION_INSCRIBER.get())
                .pattern("CCC")
                .pattern("CHC")
                .pattern("CCC")
                .define('C', ModBlocks.byLevel(13).get())
                .define('H', ModItems.TIER_1_COMPRESSED_HEART.get())
                .unlockedBy(getHasName(ModItems.TIER_1_COMPRESSED_HEART.get()), has(ModItems.TIER_1_COMPRESSED_HEART.get()))
                .save(recipeOutput);

        // Every crafted level of Compression: the same frame of leather, filled with a deeper
        // block for the deeper level.
        compressionBook(recipeOutput, ModEnchantments.FIRST_CRAFTED_LEVEL, 6);
        compressionBook(recipeOutput, ModEnchantments.LAST_CRAFTED_LEVEL, 10);
    }

    /**
     * The only source of Compression above level 3: leather wrapped around a layer of compressed
     * cobblestone. The result carries a stored enchantment, so it is built as a stack rather than
     * an item, and it needs an explicit id because the result is a vanilla enchanted book.
     */
    private void compressionBook(RecipeOutput recipeOutput, int level, int blockLevel) {
        // The stored enchantment is set component-first rather than through EnchantmentInstance:
        // during datagen the enchantment holder is a lazy one, and resolving it would make it clone
        // the enchantment through its codec, which needs item tags that datagen has not bound.
        ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        stored.set(this.registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ModEnchantments.COMPRESSION), level);

        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());

        new ShapedRecipeBuilder(RecipeCategory.COMBAT, book)
                .pattern("LLL")
                .pattern("CCC")
                .pattern("LLL")
                .define('L', Items.LEATHER)
                .define('C', ModBlocks.byLevel(blockLevel).get())
                .unlockedBy(getHasName(ModBlocks.byLevel(blockLevel).get()), has(ModBlocks.byLevel(blockLevel).get()))
                .save(recipeOutput, UnnecessarilyCompressedCobblestone.MOD_ID + ":compression_book_" + level);
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
