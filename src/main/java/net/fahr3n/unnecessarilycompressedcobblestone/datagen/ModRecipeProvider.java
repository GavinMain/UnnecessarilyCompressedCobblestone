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
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
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
import net.minecraft.world.level.block.Block;
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
        // Each rung goes back down again too, so the whole ladder is reversible and nothing is
        // stranded at a level that is too deep to spend.
        compress(recipeOutput, Items.COBBLESTONE, ModBlocks.byLevel(1).get());
        decompress(recipeOutput, ModBlocks.byLevel(1).get(), Items.COBBLESTONE);

        for (int level = 2; level <= ModBlocks.MAX_COMPRESSION_LEVEL; level++) {
            compress(recipeOutput, ModBlocks.byLevel(level - 1).get(), ModBlocks.byLevel(level).get());
            decompress(recipeOutput, ModBlocks.byLevel(level).get(), ModBlocks.byLevel(level - 1).get());
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

        // The Compression Jump set, one level deeper per piece down the body, picking up where the
        // TNT left off.
        armorSet(recipeOutput, ModItems.COMPRESSION_JUMP_HELMET.get(), ModItems.COMPRESSION_JUMP_CHESTPLATE.get(),
                ModItems.COMPRESSION_JUMP_LEGGINGS.get(), ModItems.COMPRESSION_JUMP_BOOTS.get(), 20);

        // The mace is laid out like a sign: a slab of stone on a stick.
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.COMPRESSED_COBBLESTONE_MACE.get())
                .pattern("CCC")
                .pattern("CCC")
                .pattern(" S ")
                .define('C', ModBlocks.byLevel(25).get())
                .define('S', Items.STICK)
                .unlockedBy(getHasName(ModBlocks.byLevel(25).get()), has(ModBlocks.byLevel(25).get()))
                .save(recipeOutput);

        // A cross of gunpowder with the corners packed out in level 26 stone.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COMPRESSED_CREEPER_SPAWN_EGG.get())
                .pattern("CXC")
                .pattern("XXX")
                .pattern("CXC")
                .define('X', Items.GUNPOWDER)
                .define('C', ModBlocks.byLevel(26).get())
                .unlockedBy(getHasName(ModBlocks.byLevel(26).get()), has(ModBlocks.byLevel(26).get()))
                .save(recipeOutput);

        // Vanilla's TNT grid with compressed cobblestone in place of the sand; the tier is the
        // only thing that tells one TNT's recipe from another's.
        tnt(recipeOutput, ModBlocks.COMPRESSED_TNT.get(), 17);
        tnt(recipeOutput, ModBlocks.SUPER_COMPRESSED_TNT.get(), 18);
        tnt(recipeOutput, ModBlocks.FLOWER_TNT.get(), 27);
        tnt(recipeOutput, ModBlocks.GUARDIAN_TNT.get(), 28);
        tnt(recipeOutput, ModBlocks.CHUNK_TNT.get(), 30);
        tnt(recipeOutput, ModBlocks.GLASS_TNT.get(), 31);
        tnt(recipeOutput, ModBlocks.CHICKEN_TNT.get(), 34);
        tnt(recipeOutput, ModBlocks.GOLEM_TNT.get(), 32);

        // The machine is the inscriber's recipe three levels deeper: a ring of level 16 stone around
        // a second golem heart, so it too waits on the golem being beaten.
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.MATERIAL_COMPRESSOR_TIER_1.get())
                .pattern("CCC")
                .pattern("CHC")
                .pattern("CCC")
                .define('C', ModBlocks.byLevel(16).get())
                .define('H', ModItems.TIER_1_COMPRESSED_HEART.get())
                .unlockedBy(getHasName(ModItems.TIER_1_COMPRESSED_HEART.get()), has(ModItems.TIER_1_COMPRESSED_HEART.get()))
                .save(recipeOutput);

        // Every crafted level of every rung of the Compression family: the same frame of leather,
        // filled with a deeper block for the deeper level.
        for (ModEnchantments.Family family : ModEnchantments.FAMILIES) {
            for (int level = family.firstCraftedLevel(); level <= family.maxLevel(); level++) {
                compressionBook(recipeOutput, family, level);
            }
        }
    }

    /**
     * The only source of a Compression level above what the enchanting table hands out: leather
     * wrapped around compressed cobblestone. The result carries a stored enchantment, so it is
     * built as a stack rather than an item, and it needs an explicit id because the result is a
     * vanilla enchanted book.
     * <p>
     * Giga Compression has only the one level, so its book drops the level from its id the way an
     * enchantment with a single level drops the numeral from its name.
     */
    private void compressionBook(RecipeOutput recipeOutput, ModEnchantments.Family family, int level) {
        // The stored enchantment is set component-first rather than through EnchantmentInstance:
        // during datagen the enchantment holder is a lazy one, and resolving it would make it clone
        // the enchantment through its codec, which needs item tags that datagen has not bound.
        ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        stored.set(this.registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(family.key()), level);

        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());

        Block cobblestone = ModBlocks.byLevel(family.bookTier(level)).get();
        ShapedRecipeBuilder builder = new ShapedRecipeBuilder(RecipeCategory.COMBAT, book);
        ModEnchantments.BOOK_PATTERN.forEach(builder::pattern);

        String name = family.key().location().getPath() + "_book";
        builder.define('L', Items.LEATHER)
                .define('C', cobblestone)
                .unlockedBy(getHasName(cobblestone), has(cobblestone))
                .save(recipeOutput, UnnecessarilyCompressedCobblestone.MOD_ID + ":" + name
                        + (family.maxLevel() > 1 ? "_" + level : ""));
    }

    /**
     * One {@code ingredient} on its own unpacks back into nine {@code result}, the way a block of
     * iron gives back its ingots.
     * <p>
     * The id has to be spelled out. A recipe is named after what it produces by default, and
     * unpacking level 3 produces the same block that packing level 2 does, so the two would collide
     * on {@code compressed_cobblestone_2} and only one of them would load.
     */
    private void decompress(RecipeOutput recipeOutput, ItemLike ingredient, ItemLike result) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, result, 9)
                .requires(ingredient)
                .unlockedBy(getHasName(ingredient), has(ingredient))
                .save(recipeOutput, UnnecessarilyCompressedCobblestone.MOD_ID + ":"
                        + getItemName(result) + "_from_" + getItemName(ingredient));
    }

    /**
     * A full armour set in the vanilla shapes, each piece one compression level deeper than the one
     * above it, starting at {@code firstLevel} for the helmet.
     */
    private void armorSet(RecipeOutput recipeOutput, ItemLike helmet, ItemLike chestplate, ItemLike leggings,
                          ItemLike boots, int firstLevel) {
        armorPiece(recipeOutput, helmet, firstLevel, "CCC", "C C");
        armorPiece(recipeOutput, chestplate, firstLevel + 1, "C C", "CCC", "CCC");
        armorPiece(recipeOutput, leggings, firstLevel + 2, "CCC", "C C", "C C");
        armorPiece(recipeOutput, boots, firstLevel + 3, "C C", "C C");
    }

    private void armorPiece(RecipeOutput recipeOutput, ItemLike result, int level, String... pattern) {
        Block cobblestone = ModBlocks.byLevel(level).get();
        ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result);
        for (String row : pattern) {
            builder.pattern(row);
        }

        builder.define('C', cobblestone)
                .unlockedBy(getHasName(cobblestone), has(cobblestone))
                .save(recipeOutput);
    }

    /** Vanilla's checkerboard of gunpowder and sand, with a compressed block for the sand. */
    private void tnt(RecipeOutput recipeOutput, ItemLike result, int blockLevel) {
        Block cobblestone = ModBlocks.byLevel(blockLevel).get();

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, result)
                .pattern("XCX")
                .pattern("CXC")
                .pattern("XCX")
                .define('X', Items.GUNPOWDER)
                .define('C', cobblestone)
                .unlockedBy(getHasName(cobblestone), has(cobblestone))
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
