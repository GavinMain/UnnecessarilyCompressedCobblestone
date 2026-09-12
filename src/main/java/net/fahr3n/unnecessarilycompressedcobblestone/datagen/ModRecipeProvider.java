package net.fahr3n.unnecessarilycompressedcobblestone.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CompressorTier;
import net.fahr3n.unnecessarilycompressedcobblestone.enchantment.ModEnchantments;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Augment;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engraving;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.item.enchantment.Enchantment;
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
    /** The tier the Compressed Spirit's egg is called up at. */
    private static final int SPIRIT_EGG_TIER = 107;

    /** The tier the Compressed Witch's egg is called up at. */
    private static final int WITCH_EGG_TIER = 131;

    /** And the tier the Compressed Chicken Boss's egg is called up at. */
    private static final int CHICKEN_BOSS_EGG_TIER = 156;

    /** The stone the Compressed Katana's blade is folded out of. */
    private static final int KATANA_TIER = 157;

    /** And the tier the Compressed Creeper Tier 2's egg is called up at. */
    private static final int CREEPER_TIER_2_EGG_TIER = 163;

    /** And the stone the Composer's egg is called up at. */
    private static final int COMPOSER_EGG_TIER = 171;

    /** The Compressed Guardian's egg, one clear of every other boss egg's ring. */
    private static final int GUARDIAN_EGG_TIER = 178;

    /** The Compressed Husk's egg, which drops the tier above it. */
    private static final int HUSK_EGG_TIER = 196;

    /** The Compressed Shield's stone. */
    private static final int SHIELD_TIER = 200;

    /** The Compressed Bone's stone. */
    private static final int BONE_TIER = 201;

    /** The stone the Compressed Saddle is cut from. */
    private static final int SADDLE_TIER = 208;

    /** The Compressed Snow Golem's egg, which drops the tier above it. */
    private static final int SNOW_GOLEM_EGG_TIER = 205;

    /** The stone the Ray of Laser's barrel is drawn from, one clear of the Guardian's own egg. */
    private static final int RAY_OF_LASER_TIER = 179;

    /** And the tier the table that fits augments to it is built at. */
    private static final int AUGMENTATION_TABLE_TIER = 180;

    /** The stone the broken sword is cut from. */
    private static final int BROKEN_SWORD_TIER = 135;

    /** And the stone the reusable firework is packed in. */
    private static final int FIREWORK_TIER = 136;

    /** The stone the healing web is spun out of. */
    /** The tier the Compressed Ghast's egg is cut from; the boss drops the one above it. */
    private static final int GHAST_EGG_TIER = 235;

    /**
     * The tier the Compressed Dragon's egg is cut from, and the last egg tier in the mod: the one
     * above it is {@code MAX_COMPRESSION_LEVEL}, which is what the fight pays out. Nothing deeper
     * can be asked for, which is the right shape for a final boss - the ring costs eight blocks of
     * the second deepest stone there is.
     */
    private static final int DRAGON_EGG_TIER = 254;

    /**
     * And the second phase's, which is the one recipe in the mod built out of
     * {@code MAX_COMPRESSION_LEVEL}: the deepest stone there is buys the last fight there is, and
     * the first phase is where that stone comes from.
     */
    private static final int DRAGON_TIER_2_EGG_TIER = ModBlocks.MAX_COMPRESSION_LEVEL;

    private static final int REGEN_WEB_TIER = 139;

    /** The Damage Web's own tier. Ninety-two levels past the web it is the other half of. */
    private static final int DAMAGE_WEB_TIER = 231;

    /** The stone the fishing rod is cut from, and the block it fishes up. */
    private static final int FISHING_ROD_TIER = 142;

    /** The stone the spirit was standing on, which is what its launcher is drawn in. */
    private static final int LAUNCHER_TIER = 108;

    /** What the TNT Launcher is cut from. Nothing else in the mod uses this tier. */
    private static final int TNT_LAUNCHER_TIER = 215;

    /** What the Compressed Scythe is cut from: the stone the Compressed Ore Golem drops. */
    private static final int SCYTHE_TIER = 223;

    /** What the Compression Bomb is cast from. Nothing else in the mod uses this tier. */
    private static final int BOMB_TIER = 248;

    /** What the Compressed Spear is forged from. */
    private static final int SPEAR_TIER = 249;

    /** What the Compressed Totem of Undying is cut from. */
    private static final int TOTEM_TIER = 253;

    /** The Composition Table and the blank sheet that goes with it. */
    private static final int COMPOSITION_TIER = 109;

    /** Both rests. */
    private static final int REST_TIER = 110;

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

        // The broken sword, in the sword's own shape at a far deeper tier. It carries no
        // enchantments out of the grid: what "every enchantment" means depends on what is loaded,
        // so the sword fills itself in when it is first held - see BrokenCompressedSwordItem.
        Block brokenSwordStone = ModBlocks.byLevel(BROKEN_SWORD_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.BROKEN_COMPRESSED_SWORD.get())
                .pattern("C")
                .pattern("C")
                .pattern("S")
                .define('C', brokenSwordStone)
                .define('S', Items.STICK)
                .unlockedBy(getHasName(brokenSwordStone), has(brokenSwordStone))
                .save(recipeOutput);

        // The firework, drawn as vanilla's is - paper wrapped round gunpowder - with the stone in
        // place of the paper, which is what makes it the one that never runs out.
        Block fireworkStone = ModBlocks.byLevel(FIREWORK_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COMPRESSED_FIREWORK.get())
                .pattern("CXC")
                .pattern("CXC")
                .pattern("CXC")
                .define('C', fireworkStone)
                .define('X', Items.GUNPOWDER)
                .unlockedBy(getHasName(fireworkStone), has(fireworkStone))
                .save(recipeOutput);

        // The healing web: string for the web itself, a glistering melon slice for what it does,
        // and the stone at the corners. String rather than cobweb on purpose - vanilla has no way
        // to craft a cobweb, so building on one would gate a tier 139 block behind a mineshaft.
        Block webStone = ModBlocks.byLevel(REGEN_WEB_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.REGEN_WEB.get())
                .pattern("CXC")
                .pattern("XGX")
                .pattern("CXC")
                .define('C', webStone)
                .define('X', Items.STRING)
                .define('G', Items.GLISTERING_MELON_SLICE)
                .unlockedBy(getHasName(webStone), has(webStone))
                .save(recipeOutput);

        // The harming web: the healing one's grid with a fermented spider eye where the melon
        // slice was, which is vanilla's own way of writing "the same potion, inverted".
        Block damageWebStone = ModBlocks.byLevel(DAMAGE_WEB_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.DAMAGE_WEB.get())
                .pattern("CXC")
                .pattern("XGX")
                .pattern("CXC")
                .define('C', damageWebStone)
                .define('X', Items.STRING)
                .define('G', Items.FERMENTED_SPIDER_EYE)
                .unlockedBy(getHasName(damageWebStone), has(damageWebStone))
                .save(recipeOutput);

        // The healing staff, in the diagonal every staff here is built on, around a glistering
        // melon slice rather than a boss heart - it is the one staff that is not a trophy.
        Block healingStaffStone = ModBlocks.byLevel(234).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.COMPRESSED_HEALING_STAFF.get())
                .pattern("  G")
                .pattern(" C ")
                .pattern("C  ")
                .define('G', Items.GLISTERING_MELON_SLICE)
                .define('C', healingStaffStone)
                .unlockedBy(getHasName(healingStaffStone), has(healingStaffStone))
                .save(recipeOutput);

        // Vanilla's fishing rod shape - a diagonal shaft with a line hanging off it - in the stone
        // it fishes up.
        Block rodStone = ModBlocks.byLevel(FISHING_ROD_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.COMPRESSED_FISHING_ROD.get())
                .pattern("  C")
                .pattern(" CX")
                .pattern("C X")
                .define('C', rodStone)
                .define('X', Items.STRING)
                .unlockedBy(getHasName(rodStone), has(rodStone))
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

        // And the same again five times over at 168, four at a time like both of the others. It is
        // useless without the Sniper engraving, which is deliberate: see CompressedArrowTier.
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.HYPER_COMPRESSED_ARROW.get(), 4)
                .pattern("C")
                .pattern("S")
                .pattern("F")
                .define('C', ModBlocks.byLevel(168).get())
                .define('S', Items.STICK)
                .define('F', Items.FEATHER)
                .unlockedBy(getHasName(ModBlocks.byLevel(168).get()), has(ModBlocks.byLevel(168).get()))
                .save(recipeOutput);

        // The same arrow at a far deeper tier, four at a time like the first.
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.SUPER_COMPRESSED_ARROW.get(), 4)
                .pattern("C")
                .pattern("S")
                .pattern("F")
                .define('C', ModBlocks.byLevel(65).get())
                .define('S', Items.STICK)
                .define('F', Items.FEATHER)
                .unlockedBy(getHasName(ModBlocks.byLevel(65).get()), has(ModBlocks.byLevel(65).get()))
                .save(recipeOutput);

        // An apple encased the way a golden apple is, one compression level deeper than the arrow.
        ShapedRecipeBuilder.shaped(RecipeCategory.FOOD, ModItems.TIER_1_COMPRESSED_COBBLESTONE_APPLE.get())
                .pattern("CCC")
                .pattern("CAC")
                .pattern("CCC")
                .define('C', ModBlocks.byLevel(9).get())
                .define('A', Items.APPLE)
                .unlockedBy(getHasName(ModBlocks.byLevel(9).get()), has(ModBlocks.byLevel(9).get()))
                .save(recipeOutput);

        // The tier 1 apple encased a second time, at level 35 - the same step up an enchanted
        // golden apple takes over a golden one, so tier 2 costs a tier 1 as well as the stone.
        ShapedRecipeBuilder.shaped(RecipeCategory.FOOD, ModItems.TIER_2_COMPRESSED_COBBLESTONE_APPLE.get())
                .pattern("CCC")
                .pattern("CAC")
                .pattern("CCC")
                .define('C', ModBlocks.byLevel(35).get())
                .define('A', ModItems.TIER_1_COMPRESSED_COBBLESTONE_APPLE.get())
                .unlockedBy(getHasName(ModItems.TIER_1_COMPRESSED_COBBLESTONE_APPLE.get()),
                        has(ModItems.TIER_1_COMPRESSED_COBBLESTONE_APPLE.get()))
                .save(recipeOutput);

        // The tier 2 apple encased a third time, at level 98. There is no third heart in it: the
        // stone alone is the price by this depth.
        ShapedRecipeBuilder.shaped(RecipeCategory.FOOD, ModItems.TIER_3_COMPRESSED_COBBLESTONE_APPLE.get())
                .pattern("CCC")
                .pattern("CAC")
                .pattern("CCC")
                .define('C', ModBlocks.byLevel(98).get())
                .define('A', ModItems.TIER_2_COMPRESSED_COBBLESTONE_APPLE.get())
                .unlockedBy(getHasName(ModItems.TIER_2_COMPRESSED_COBBLESTONE_APPLE.get()),
                        has(ModItems.TIER_2_COMPRESSED_COBBLESTONE_APPLE.get()))
                .save(recipeOutput);

        // The tier 3 apple encased a fourth time, at level 240. Nothing new in the grid: what
        // makes tier 4 a different apple is what is inside it, not what it is wrapped in.
        ShapedRecipeBuilder.shaped(RecipeCategory.FOOD, ModItems.TIER_4_COMPRESSED_COBBLESTONE_APPLE.get())
                .pattern("CCC")
                .pattern("CAC")
                .pattern("CCC")
                .define('C', ModBlocks.byLevel(240).get())
                .define('A', ModItems.TIER_3_COMPRESSED_COBBLESTONE_APPLE.get())
                .unlockedBy(getHasName(ModItems.TIER_3_COMPRESSED_COBBLESTONE_APPLE.get()),
                        has(ModItems.TIER_3_COMPRESSED_COBBLESTONE_APPLE.get()))
                .save(recipeOutput);

        // The two ghasts a player keeps, each around the heart of the one that had to be beaten
        // first. The pet is the ring the other summoning eggs use; the mount is that ring with a
        // saddle in the middle of it, which is both the obvious grid for a thing to be ridden and
        // what keeps the two recipes apart at tiers a block would not.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GHAST_PET_SPAWN_EGG.get())
                .pattern("CCC")
                .pattern("CHC")
                .pattern("CCC")
                .define('C', ModBlocks.byLevel(238).get())
                .define('H', ModItems.TIER_18_COMPRESSED_HEART.get())
                .unlockedBy(getHasName(ModItems.TIER_18_COMPRESSED_HEART.get()),
                        has(ModItems.TIER_18_COMPRESSED_HEART.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GHAST_MOUNT_SPAWN_EGG.get())
                .pattern("CHC")
                .pattern("CSC")
                .pattern("CCC")
                .define('C', ModBlocks.byLevel(239).get())
                .define('H', ModItems.TIER_18_COMPRESSED_HEART.get())
                .define('S', Items.SADDLE)
                .unlockedBy(getHasName(ModItems.TIER_18_COMPRESSED_HEART.get()),
                        has(ModItems.TIER_18_COMPRESSED_HEART.get()))
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

        // The Engraving Table: a ring of level 84 stone around the heart of the deepest arrow boss,
        // so it cannot be built until that boss has been beaten.
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.ENGRAVING_TABLE.get())
                .pattern("CCC")
                .pattern("CHC")
                .pattern("CCC")
                .define('C', ModBlocks.byLevel(84).get())
                .define('H', ModItems.TIER_6_COMPRESSED_HEART.get())
                .unlockedBy(getHasName(ModItems.TIER_6_COMPRESSED_HEART.get()), has(ModItems.TIER_6_COMPRESSED_HEART.get()))
                .save(recipeOutput);

        // The gate: a doorway of level 96 stone around an ender pearl, which is the one thing in
        // the game that already means "somewhere else". Two are wanted before either does anything,
        // so the recipe makes two.
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.TELEPORTATION_GATE.get(), 2)
                .pattern("CPC")
                .pattern("C C")
                .pattern("CPC")
                .define('C', ModBlocks.byLevel(96).get())
                .define('P', Items.ENDER_PEARL)
                .unlockedBy(getHasName(Items.ENDER_PEARL), has(Items.ENDER_PEARL))
                .save(recipeOutput);

        // Every engraving, each at its own tier. The grid is the same for all of them - a diamond
        // chisel wrapped in stone - so the tier is what tells two engravings apart, exactly as it is
        // for the enchanted books.
        for (Engraving engraving : Engraving.values()) {
            engraving(recipeOutput, engraving);
        }

        // The Compressed Shield, in vanilla's own shield grid: a plate of stone over a stick,
        // which is the shape a player already knows and is not a shape anything else here uses.
        Block shieldStone = ModBlocks.byLevel(SHIELD_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.COMPRESSED_SHIELD.get())
                .pattern("CSC")
                .pattern("CCC")
                .pattern(" C ")
                .define('C', shieldStone)
                .define('S', Items.STICK)
                .unlockedBy(getHasName(shieldStone), has(shieldStone))
                .save(recipeOutput);

        // The Compressed Bone: a bone laid through a column of stone. The shape is vertical and
        // three tall, which nothing else cut from this stone uses.
        Block boneStone = ModBlocks.byLevel(BONE_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COMPRESSED_BONE.get())
                .pattern(" C ")
                .pattern(" B ")
                .pattern(" C ")
                .define('C', boneStone)
                .define('B', Items.BONE)
                .unlockedBy(getHasName(boneStone), has(boneStone))
                .save(recipeOutput);

        // The Compressed Saddle: a saddle laid across a bar of stone. Horizontal and three wide,
        // where the Compressed Bone's is vertical - and on a tier nothing else here is cut from, so
        // the shape only has to be readable rather than unique.
        Block saddleStone = ModBlocks.byLevel(SADDLE_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COMPRESSED_SADDLE.get())
                .pattern("CSC")
                .define('C', saddleStone)
                .define('S', Items.SADDLE)
                .unlockedBy(getHasName(saddleStone), has(saddleStone))
                .save(recipeOutput);

        // The Ray of Laser: the Compressed Guardian's own heart set at the end of a barrel of the
        // stone that boss was standing on, so the weapon cannot be built until the beam it fires has
        // been survived. The grid is a diagonal rather than the tables' ring, which is what keeps it
        // clear of every other recipe cut from this stone.
        Block laserStone = ModBlocks.byLevel(RAY_OF_LASER_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.RAY_OF_LASER.get())
                .pattern("  H")
                .pattern(" C ")
                .pattern("C  ")
                .define('C', laserStone)
                .define('H', ModItems.TIER_14_COMPRESSED_HEART.get())
                .unlockedBy(getHasName(ModItems.TIER_14_COMPRESSED_HEART.get()),
                        has(ModItems.TIER_14_COMPRESSED_HEART.get()))
                .save(recipeOutput);

        // The Laser Augmentation Table, in the same empty frame the Composition Table is built in -
        // it is the fourth table of that family, and like that one it holds no heart.
        Block augmentationStone = ModBlocks.byLevel(AUGMENTATION_TABLE_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.LASER_AUGMENTATION_TABLE.get())
                .pattern("CCC")
                .pattern("C C")
                .pattern("CCC")
                .define('C', augmentationStone)
                .unlockedBy(getHasName(augmentationStone), has(augmentationStone))
                .save(recipeOutput);

        // Every augment, each at its own tier. One grid for all of them, around a prismarine shard
        // rather than the engravings' diamond, so the two families' tiers need never agree.
        for (Augment augment : Augment.values()) {
            augment(recipeOutput, augment);
        }

        // The Compression Jump set, one level deeper per piece down the body, picking up where the
        // TNT left off.
        armorSet(recipeOutput, ModItems.COMPRESSION_JUMP_HELMET.get(), ModItems.COMPRESSION_JUMP_CHESTPLATE.get(),
                ModItems.COMPRESSION_JUMP_LEGGINGS.get(), ModItems.COMPRESSION_JUMP_BOOTS.get(), 20);

        // The Compression Lightning set, the same one level deeper per piece, at 39 through 42.
        armorSet(recipeOutput, ModItems.COMPRESSION_LIGHTNING_HELMET.get(), ModItems.COMPRESSION_LIGHTNING_CHESTPLATE.get(),
                ModItems.COMPRESSION_LIGHTNING_LEGGINGS.get(), ModItems.COMPRESSION_LIGHTNING_BOOTS.get(), 39);

        // The Compression Arrow set, the same one level deeper per piece, at 69 through 72.
        armorSet(recipeOutput, ModItems.COMPRESSION_ARROW_HELMET.get(), ModItems.COMPRESSION_ARROW_CHESTPLATE.get(),
                ModItems.COMPRESSION_ARROW_LEGGINGS.get(), ModItems.COMPRESSION_ARROW_BOOTS.get(), 69);

        // The Compression Magic set, the same again at 124 through 127.
        armorSet(recipeOutput, ModItems.COMPRESSION_MAGIC_HELMET.get(), ModItems.COMPRESSION_MAGIC_CHESTPLATE.get(),
                ModItems.COMPRESSION_MAGIC_LEGGINGS.get(), ModItems.COMPRESSION_MAGIC_BOOTS.get(), 124);

        // The Compression Rain set, the same one level deeper per piece, at 173 through 176.
        armorSet(recipeOutput, ModItems.COMPRESSION_RAIN_HELMET.get(), ModItems.COMPRESSION_RAIN_CHESTPLATE.get(),
                ModItems.COMPRESSION_RAIN_LEGGINGS.get(), ModItems.COMPRESSION_RAIN_BOOTS.get(), 173);

        // The Ultimate Compressed set, the last of the six, at 244 through 247.
        armorSet(recipeOutput, ModItems.ULTIMATE_COMPRESSED_HELMET.get(), ModItems.ULTIMATE_COMPRESSED_CHESTPLATE.get(),
                ModItems.ULTIMATE_COMPRESSED_LEGGINGS.get(), ModItems.ULTIMATE_COMPRESSED_BOOTS.get(), 244);

        // The vanilla pickaxe shape in the stone it is meant to break: the first hardened level.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.COMPRESSED_COBBLESTONE_PICKAXE.get())
                .pattern("CCC")
                .pattern(" S ")
                .pattern(" S ")
                .define('C', ModBlocks.byLevel(ModBlocks.HARDENED_LEVEL).get())
                .define('S', Items.STICK)
                .unlockedBy(getHasName(ModBlocks.byLevel(ModBlocks.HARDENED_LEVEL).get()),
                        has(ModBlocks.byLevel(ModBlocks.HARDENED_LEVEL).get()))
                .save(recipeOutput);

        // The same shape again in the stone the second floor starts at, which is stone the pickaxe
        // above can already break - so the tier 2 pickaxe is the last thing craftable before the
        // levels that need it, and nothing else in the game can reach past it.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.COMPRESSED_COBBLESTONE_PICKAXE_TIER_2.get())
                .pattern("CCC")
                .pattern(" S ")
                .pattern(" S ")
                .define('C', ModBlocks.byLevel(ModBlocks.HARDENED_LEVEL_TIER_2 - 1).get())
                .define('S', Items.STICK)
                .unlockedBy(getHasName(ModBlocks.byLevel(ModBlocks.HARDENED_LEVEL_TIER_2 - 1).get()),
                        has(ModBlocks.byLevel(ModBlocks.HARDENED_LEVEL_TIER_2 - 1).get()))
                .save(recipeOutput);

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

        // The same staff again, around the skeleton's heart and its own stone.
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.COMPRESSED_ARROW_STAFF.get())
                .pattern("  H")
                .pattern(" C ")
                .pattern("C  ")
                .define('H', ModItems.TIER_4_COMPRESSED_HEART.get())
                .define('C', ModBlocks.byLevel(67).get())
                .unlockedBy(getHasName(ModItems.TIER_4_COMPRESSED_HEART.get()),
                        has(ModItems.TIER_4_COMPRESSED_HEART.get()))
                .save(recipeOutput);

        // A staff: the heart of the conjurer that dropped it, set on a shaft of level 44 stone.
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.COMPRESSED_LIGHTNING_STAFF.get())
                .pattern("  H")
                .pattern(" C ")
                .pattern("C  ")
                .define('H', ModItems.TIER_3_COMPRESSED_HEART.get())
                .define('C', ModBlocks.byLevel(44).get())
                .unlockedBy(getHasName(ModItems.TIER_3_COMPRESSED_HEART.get()),
                        has(ModItems.TIER_3_COMPRESSED_HEART.get()))
                .save(recipeOutput);

        // The two deeper rungs of the arrow boss, each in its own stone.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COMPRESSED_SKELETON_TIER_2_SPAWN_EGG.get())
                .pattern("CCC")
                .pattern("C C")
                .pattern("CCC")
                .define('C', ModBlocks.byLevel(75).get())
                .unlockedBy(getHasName(ModBlocks.byLevel(75).get()), has(ModBlocks.byLevel(75).get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COMPRESSED_SKELETON_TIER_3_SPAWN_EGG.get())
                .pattern("CCC")
                .pattern("C C")
                .pattern("CCC")
                .define('C', ModBlocks.byLevel(83).get())
                .unlockedBy(getHasName(ModBlocks.byLevel(83).get()), has(ModBlocks.byLevel(83).get()))
                .save(recipeOutput);

        // The staff that throws its TNT, around the heart of the boss that rained it.
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.COMPRESSED_ARROW_TNT_STAFF.get())
                .pattern("  H")
                .pattern(" C ")
                .pattern("C  ")
                .define('H', ModItems.TIER_5_COMPRESSED_HEART.get())
                .define('C', ModBlocks.byLevel(76).get())
                .unlockedBy(getHasName(ModItems.TIER_5_COMPRESSED_HEART.get()),
                        has(ModItems.TIER_5_COMPRESSED_HEART.get()))
                .save(recipeOutput);

        // The same ring again, deeper, for the skeleton.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COMPRESSED_SKELETON_SPAWN_EGG.get())
                .pattern("CCC")
                .pattern("C C")
                .pattern("CCC")
                .define('C', ModBlocks.byLevel(66).get())
                .unlockedBy(getHasName(ModBlocks.byLevel(66).get()), has(ModBlocks.byLevel(66).get()))
                .save(recipeOutput);

        // A ring of level 43 stone around nothing at all. It has to be a ring rather than a full
        // grid: nine of one block in a grid is already the recipe that compresses it a level.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COMPRESSED_CONJURER_SPAWN_EGG.get())
                .pattern("CCC")
                .pattern("C C")
                .pattern("CCC")
                .define('C', ModBlocks.byLevel(43).get())
                .unlockedBy(getHasName(ModBlocks.byLevel(43).get()), has(ModBlocks.byLevel(43).get()))
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
        tnt(recipeOutput, ModBlocks.APPLE_TNT.get(), 36);
        tnt(recipeOutput, ModBlocks.RANDOM_TNT.get(), 37);
        tnt(recipeOutput, ModBlocks.TRIDENT_ENCHANT_TNT.get(), 45);
        tnt(recipeOutput, ModBlocks.BINDING_TNT.get(), 47);
        tnt(recipeOutput, ModBlocks.GLASS_TNT_TIER_2.get(), 48);
        tnt(recipeOutput, ModBlocks.ANVIL_TNT.get(), 49);
        tnt(recipeOutput, ModBlocks.LEVITATION_TNT.get(), 56);
        tnt(recipeOutput, ModBlocks.CHICKEN_TNT_TIER_2.get(), 51);
        tnt(recipeOutput, ModBlocks.TREENT_TNT.get(), 59);
        tnt(recipeOutput, ModBlocks.ARROW_TNT.get(), 68);
        tnt(recipeOutput, ModBlocks.BREEDING_TNT.get(), 81);
        tnt(recipeOutput, ModBlocks.EFFECT_TNT.get(), 93);
        tnt(recipeOutput, ModBlocks.ARROW_SPIRAL_TNT.get(), 73);
        tnt(recipeOutput, ModBlocks.FLASH_TNT.get(), 80);
        tnt(recipeOutput, ModBlocks.LIGHTNING_SONG_TNT.get(), 79);
        tnt(recipeOutput, ModBlocks.PINBALL_TNT.get(), 102);
        tnt(recipeOutput, ModBlocks.BOLT_TNT.get(), 103);
        tnt(recipeOutput, ModBlocks.MOONLIGHT_TNT.get(), 104);
        tnt(recipeOutput, ModBlocks.LIGHTNING_SONG_TNT_2.get(), 105);
        tnt(recipeOutput, ModBlocks.POOL_TNT.get(), 113);
        tnt(recipeOutput, ModBlocks.ZOOM_TNT.get(), 133);
        tnt(recipeOutput, ModBlocks.WEB_TNT.get(), 138);
        tnt(recipeOutput, ModBlocks.CHICKEN_TNT_TIER_3.get(), 141);
        tnt(recipeOutput, ModBlocks.DEBRIS_TNT.get(), 140);
        tnt(recipeOutput, ModBlocks.PORTAL_TNT.get(), 143);
        tnt(recipeOutput, ModBlocks.SPAWNER_TNT.get(), 144);
        tnt(recipeOutput, ModBlocks.SNOW_TNT.get(), 147);
        tnt(recipeOutput, ModBlocks.SILVERFISH_TNT.get(), 149);
        tnt(recipeOutput, ModBlocks.SILVERFISH_TNT_TIER_2.get(), 150);
        tnt(recipeOutput, ModBlocks.GEYSER_TNT.get(), 154);
        tnt(recipeOutput, ModBlocks.GEYSER_TNT_TIER_2.get(), 155);
        tnt(recipeOutput, ModBlocks.CORAL_TNT.get(), 160);
        tnt(recipeOutput, ModBlocks.ARROW_TNT_TIER_2.get(), 165);
        tnt(recipeOutput, ModBlocks.SUCC_TNT.get(), 169);
        tnt(recipeOutput, ModBlocks.VILLAGE_TNT.get(), 210);
        tnt(recipeOutput, ModBlocks.MANSION_TNT.get(), 211);
        tnt(recipeOutput, ModBlocks.PYRAMID_TNT.get(), 212);
        tnt(recipeOutput, ModBlocks.PILLAR_TNT_TIER_2.get(), 213);
        tnt(recipeOutput, ModBlocks.BEE_NT.get(), 217);
        tnt(recipeOutput, ModBlocks.FLAT_TNT.get(), 229);
        tnt(recipeOutput, ModBlocks.DAMAGE_WEB_TNT.get(), 232);
        tnt(recipeOutput, ModBlocks.LASER_TNT.get(), 233);
        tnt(recipeOutput, ModBlocks.DOME_TNT.get(), 241);
        tnt(recipeOutput, ModBlocks.BLACKHOLE_TNT.get(), 242);
        tnt(recipeOutput, ModBlocks.SINGULARITY_TNT.get(), 254);


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

        // The second tier of the machine, built the same way out of the stone it makes: a ring of
        // level 50 around the heart of the creeper, so it waits on that fight the way the first
        // waits on the golem.
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.MATERIAL_COMPRESSOR_TIER_2.get())
                .pattern("CCC")
                .pattern("CHC")
                .pattern("CCC")
                .define('C', ModBlocks.byLevel(50).get())
                .define('H', ModItems.TIER_2_COMPRESSED_HEART.get())
                .unlockedBy(getHasName(ModItems.TIER_2_COMPRESSED_HEART.get()), has(ModItems.TIER_2_COMPRESSED_HEART.get()))
                .save(recipeOutput);

        // The third tier, built out of the stone it makes like the two below it. There is no heart
        // at its centre - nothing this deep is gated behind a boss - so it is a bare ring of level
        // 90, which is a shape no other recipe uses at that tier.
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.MATERIAL_COMPRESSOR_TIER_3.get())
                .pattern("CCC")
                .pattern("C C")
                .pattern("CCC")
                .define('C', ModBlocks.byLevel(90).get())
                .unlockedBy(getHasName(ModBlocks.byLevel(90).get()), has(ModBlocks.byLevel(90).get()))
                .save(recipeOutput);

        // The fourth tier, built out of the stone it makes like the three below it: a ring of level
        // 123 around the second golem's heart. It is the only one that eats a plain item rather than
        // a block of them, which is what makes a diamond worth spending on it.
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.MATERIAL_COMPRESSOR_TIER_4.get())
                .pattern("CCC")
                .pattern("CHC")
                .pattern("CCC")
                .define('C', ModBlocks.byLevel(CompressorTier.TIER_4.outputLevel()).get())
                .define('H', ModItems.TIER_9_COMPRESSED_HEART.get())
                .unlockedBy(getHasName(ModItems.TIER_9_COMPRESSED_HEART.get()),
                        has(ModItems.TIER_9_COMPRESSED_HEART.get()))
                .save(recipeOutput);

        // The fifth tier, built out of the stone it makes like the four below it: a ring of level
        // 187 around the Compressed Guardian's heart, so the machine that turns netherite into the
        // deepest stone any machine makes waits on the deepest fight there is.
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.MATERIAL_COMPRESSOR_TIER_5.get())
                .pattern("CCC")
                .pattern("CHC")
                .pattern("CCC")
                .define('C', ModBlocks.byLevel(CompressorTier.TIER_5.outputLevel()).get())
                .define('H', ModItems.TIER_14_COMPRESSED_HEART.get())
                .unlockedBy(getHasName(ModItems.TIER_14_COMPRESSED_HEART.get()),
                        has(ModItems.TIER_14_COMPRESSED_HEART.get()))
                .save(recipeOutput);

        // The Summoner's egg, in the ring every boss egg uses, at the tier below its own stone.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COMPRESSED_SUMMONER_SPAWN_EGG.get())
                .pattern("CCC")
                .pattern("C C")
                .pattern("CCC")
                .define('C', ModBlocks.byLevel(99).get())
                .unlockedBy(getHasName(ModBlocks.byLevel(99).get()), has(ModBlocks.byLevel(99).get()))
                .save(recipeOutput);

        // The Summoning Staff, in the same diagonal every other staff is drawn as: the deepest
        // stone in the mod's progression under the heart of the boss that drops it.
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.COMPRESSED_SUMMONING_STAFF.get())
                .pattern("  H")
                .pattern(" C ")
                .pattern("C  ")
                .define('H', ModItems.TIER_7_COMPRESSED_HEART.get())
                .define('C', ModBlocks.byLevel(100).get())
                .unlockedBy(getHasName(ModItems.TIER_7_COMPRESSED_HEART.get()),
                        has(ModItems.TIER_7_COMPRESSED_HEART.get()))
                .save(recipeOutput);

        // Drawn as the dome it raises: an arch of level 64 stone standing on two legs.
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.ARROW_VEIL.get())
                .pattern(" C ")
                .pattern("C C")
                .pattern("C C")
                .define('C', ModBlocks.byLevel(64).get())
                .unlockedBy(getHasName(ModBlocks.byLevel(64).get()), has(ModBlocks.byLevel(64).get()))
                .save(recipeOutput);

        // The black hole's answer: the same level 242 stone the Blackhole TNT is made of, so anyone
        // who can make one can stop one, held between eyes of ender and crying obsidian.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.BLACK_HOLE_STOPPER.get())
                .pattern("OEO")
                .pattern("ECE")
                .pattern("OEO")
                .define('O', Items.CRYING_OBSIDIAN)
                .define('E', Items.ENDER_EYE)
                .define('C', ModBlocks.byLevel(242).get())
                .unlockedBy(getHasName(ModBlocks.BLACKHOLE_TNT.get()), has(ModBlocks.BLACKHOLE_TNT.get()))
                .save(recipeOutput);

        // The core is the inscriber's shape again, in level 60 stone around nothing: it holds a
        // bolt rather than containing anything of its own.
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.LIGHTNING_CORE.get())
                .pattern("CCC")
                .pattern("C C")
                .pattern("CCC")
                .define('C', ModBlocks.byLevel(60).get())
                .unlockedBy(getHasName(ModBlocks.byLevel(60).get()), has(ModBlocks.byLevel(60).get()))
                .save(recipeOutput);

        // The bolt itself, drawn as one: a zigzag of level 61 stone.
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.COMPRESSED_VANILLA_BOLT.get())
                .pattern("  C")
                .pattern(" CC")
                .pattern("  C")
                .define('C', ModBlocks.byLevel(61).get())
                .unlockedBy(getHasName(ModBlocks.byLevel(61).get()), has(ModBlocks.byLevel(61).get()))
                .save(recipeOutput);

        // The Bolt Launcher: a bow drawn in the stone the Compressed Spirit was standing on, with
        // its heart for a grip. Nothing else in the mod is built out of either.
        Block launcherStone = ModBlocks.byLevel(LAUNCHER_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.BOLT_LAUNCHER.get())
                .pattern(" CS")
                .pattern("H S")
                .pattern(" CS")
                .define('C', launcherStone)
                .define('S', Items.STRING)
                .define('H', ModItems.TIER_8_COMPRESSED_HEART.get())
                .unlockedBy(getHasName(ModItems.TIER_8_COMPRESSED_HEART.get()),
                        has(ModItems.TIER_8_COMPRESSED_HEART.get()))
                .save(recipeOutput);

        // The TNT Launcher, in the Bolt Launcher's own frame with a block of TNT where that one
        // holds a heart: the two are siblings and the grid says so. It collides with nothing - a
        // shaped recipe is told apart by its ingredients as well as its shape, and this one differs
        // in two of the three.
        Block tntLauncherStone = ModBlocks.byLevel(TNT_LAUNCHER_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.TNT_LAUNCHER.get())
                .pattern(" CS")
                .pattern("T S")
                .pattern(" CS")
                .define('C', tntLauncherStone)
                .define('S', Items.STRING)
                .define('T', Items.TNT)
                .unlockedBy(getHasName(tntLauncherStone), has(tntLauncherStone))
                .save(recipeOutput);

        // The Composition Table, in the same frame the Engraving Table and the inscriber are built
        // in - it is the third table of that family - around nothing, because it holds no heart.
        Block compositionStone = ModBlocks.byLevel(COMPOSITION_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.COMPOSITION_TABLE.get())
                .pattern("CCC")
                .pattern("C C")
                .pattern("CCC")
                .define('C', compositionStone)
                .unlockedBy(getHasName(compositionStone), has(compositionStone))
                .save(recipeOutput);

        // The blank sheet, in the bolt's own zigzag at the table's tier.
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.COMPOSITION_BOLT.get())
                .pattern("  C")
                .pattern(" CC")
                .pattern("  C")
                .define('C', compositionStone)
                .unlockedBy(getHasName(compositionStone), has(compositionStone))
                .save(recipeOutput);

        // The two rests, at one tier: a bar of stone for a beat of silence and two for four of them,
        // which is the only thing telling their grids apart and is the same thing they mean.
        Block restStone = ModBlocks.byLevel(REST_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.REST_BOLT.get())
                .pattern("CCC")
                .define('C', restStone)
                .unlockedBy(getHasName(restStone), has(restStone))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.LONG_REST_BOLT.get())
                .pattern("CCC")
                .pattern("CCC")
                .define('C', restStone)
                .unlockedBy(getHasName(restStone), has(restStone))
                .save(recipeOutput);

        // The Compressed Witch's egg, in the ring every boss egg uses.
        Block witchStone = ModBlocks.byLevel(WITCH_EGG_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COMPRESSED_WITCH_SPAWN_EGG.get())
                .pattern("CCC")
                .pattern("C C")
                .pattern("CCC")
                .define('C', witchStone)
                .unlockedBy(getHasName(witchStone), has(witchStone))
                .save(recipeOutput);

        // The second creeper's egg, in the ring every boss egg uses.
        Block creeperTier2Stone = ModBlocks.byLevel(CREEPER_TIER_2_EGG_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COMPRESSED_CREEPER_TIER_2_SPAWN_EGG.get())
                .pattern("CCC")
                .pattern("C C")
                .pattern("CCC")
                .define('C', creeperTier2Stone)
                .unlockedBy(getHasName(creeperTier2Stone), has(creeperTier2Stone))
                .save(recipeOutput);

        // The Compressed Katana: a blade laid diagonally across the grid, hilted on the heart of
        // the boss that drops the stone it is folded from. The heart is what keeps this shape clear
        // of every other diagonal in the mod, and what makes the katana the one weapon here that
        // cannot be crafted until its boss is dead.
        Block katanaStone = ModBlocks.byLevel(KATANA_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.COMPRESSED_KATANA.get())
                .pattern("  C")
                .pattern(" C ")
                .pattern("H  ")
                .define('C', katanaStone)
                .define('H', ModItems.TIER_11_COMPRESSED_HEART.get())
                .unlockedBy(getHasName(katanaStone), has(katanaStone))
                .save(recipeOutput);

        // The Compressed Spear: a point on a shaft, laid up the middle of the grid. Three of its own
        // stone in a column is a shape nothing else here uses, and it wants no heart - unlike the
        // katana and the scythe, this is not a weapon a boss has to be beaten for.
        Block spearStone = ModBlocks.byLevel(SPEAR_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.COMPRESSED_SPEAR.get())
                .pattern("  C")
                .pattern(" C ")
                .pattern("C  ")
                .define('C', spearStone)
                .unlockedBy(getHasName(spearStone), has(spearStone))
                .save(recipeOutput);

        // The Compression Bomb: a shell of its own stone around gunpowder, in the ring every boss
        // egg uses. The ring is only shared with those, and they are all cut from tiers of their
        // own with a heart in the middle, so the gunpowder is what tells this apart from all of
        // them - and is also the only thing about the bomb that says what it is.
        Block bombStone = ModBlocks.byLevel(BOMB_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.COMPRESSION_BOMB.get())
                .pattern("CCC")
                .pattern("CGC")
                .pattern("CCC")
                .define('C', bombStone)
                .define('G', Items.GUNPOWDER)
                .unlockedBy(getHasName(bombStone), has(bombStone))
                .save(recipeOutput);

        // The Compressed Totem of Undying: vanilla's totem set in its own stone. The totem in the
        // middle is what earns it - an elytra-grade drop from a raid - and is what keeps this grid
        // clear of everything else cut from this stone.
        Block totemStone = ModBlocks.byLevel(TOTEM_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.COMPRESSED_TOTEM_OF_UNDYING.get())
                .pattern("CCC")
                .pattern("CTC")
                .pattern("CCC")
                .define('C', totemStone)
                .define('T', Items.TOTEM_OF_UNDYING)
                .unlockedBy(getHasName(Items.TOTEM_OF_UNDYING), has(Items.TOTEM_OF_UNDYING))
                .save(recipeOutput);

        // The Compressed Scythe: a curved blade over a haft, hilted on the heart of the boss that
        // drops the stone it is cut from. Like the katana it is the shape plus the heart that keeps
        // it clear of everything else, and it is the second weapon here that cannot be crafted
        // until its boss is dead.
        Block scytheStone = ModBlocks.byLevel(SCYTHE_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.COMPRESSED_SCYTHE.get())
                .pattern("CCC")
                .pattern("C H")
                .pattern("  C")
                .define('C', scytheStone)
                .define('H', ModItems.TIER_17_COMPRESSED_HEART.get())
                .unlockedBy(getHasName(ModItems.TIER_17_COMPRESSED_HEART.get()),
                        has(ModItems.TIER_17_COMPRESSED_HEART.get()))
                .save(recipeOutput);

        // The Compressed Composer's egg, in the ring every boss egg uses.
        Block composerStone = ModBlocks.byLevel(COMPOSER_EGG_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COMPRESSED_COMPOSER_SPAWN_EGG.get())
                .pattern("CCC")
                .pattern("C C")
                .pattern("CCC")
                .define('C', composerStone)
                .unlockedBy(getHasName(composerStone), has(composerStone))
                .save(recipeOutput);

        // The Compressed Guardian's egg, in the ring every boss egg uses.
        Block guardianStone = ModBlocks.byLevel(GUARDIAN_EGG_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COMPRESSED_GUARDIAN_SPAWN_EGG.get())
                .pattern("CCC")
                .pattern("C C")
                .pattern("CCC")
                .define('C', guardianStone)
                .unlockedBy(getHasName(guardianStone), has(guardianStone))
                .save(recipeOutput);

        // The Compressed Husk's egg, in the ring every boss egg uses.
        Block huskStone = ModBlocks.byLevel(HUSK_EGG_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COMPRESSED_HUSK_SPAWN_EGG.get())
                .pattern("CCC")
                .pattern("C C")
                .pattern("CCC")
                .define('C', huskStone)
                .unlockedBy(getHasName(huskStone), has(huskStone))
                .save(recipeOutput);

        // The Compressed Snow Golem's egg, in the ring every boss egg uses.
        Block snowGolemStone = ModBlocks.byLevel(SNOW_GOLEM_EGG_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COMPRESSED_SNOW_GOLEM_SPAWN_EGG.get())
                .pattern("CCC")
                .pattern("C C")
                .pattern("CCC")
                .define('C', snowGolemStone)
                .unlockedBy(getHasName(snowGolemStone), has(snowGolemStone))
                .save(recipeOutput);

        // The Compressed Ghast's egg, in the ring every boss egg uses.
        Block ghastStone = ModBlocks.byLevel(GHAST_EGG_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COMPRESSED_GHAST_SPAWN_EGG.get())
                .pattern("CCC")
                .pattern("C C")
                .pattern("CCC")
                .define('C', ghastStone)
                .unlockedBy(getHasName(ghastStone), has(ghastStone))
                .save(recipeOutput);

        // The Compressed Chicken Boss's egg, in the ring every boss egg uses.
        Block chickenBossStone = ModBlocks.byLevel(CHICKEN_BOSS_EGG_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COMPRESSED_CHICKEN_BOSS_SPAWN_EGG.get())
                .pattern("CCC")
                .pattern("C C")
                .pattern("CCC")
                .define('C', chickenBossStone)
                .unlockedBy(getHasName(chickenBossStone), has(chickenBossStone))
                .save(recipeOutput);

        // The Compressed Dragon's egg, in the ring every boss egg uses.
        Block dragonStone = ModBlocks.byLevel(DRAGON_EGG_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COMPRESSED_DRAGON_SPAWN_EGG.get())
                .pattern("CCC")
                .pattern("C C")
                .pattern("CCC")
                .define('C', dragonStone)
                .unlockedBy(getHasName(dragonStone), has(dragonStone))
                .save(recipeOutput);

        // The second phase's egg, in the ring every boss egg uses.
        Block dragonTier2Stone = ModBlocks.byLevel(DRAGON_TIER_2_EGG_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COMPRESSED_DRAGON_TIER_2_SPAWN_EGG.get())
                .pattern("CCC")
                .pattern("C C")
                .pattern("CCC")
                .define('C', dragonTier2Stone)
                .unlockedBy(getHasName(dragonTier2Stone), has(dragonTier2Stone))
                .save(recipeOutput);

        // The Compressed Spirit's egg, in the ring every boss egg uses.
        Block spiritStone = ModBlocks.byLevel(SPIRIT_EGG_TIER).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COMPRESSED_SPIRIT_SPAWN_EGG.get())
                .pattern("CCC")
                .pattern("C C")
                .pattern("CCC")
                .define('C', spiritStone)
                .unlockedBy(getHasName(spiritStone), has(spiritStone))
                .save(recipeOutput);

        // Every crafted level of every rung of the Compression family: the same frame of leather,
        // filled with a deeper block for the deeper level.
        for (ModEnchantments.Family family : ModEnchantments.FAMILIES) {
            for (int level = family.firstCraftedLevel(); level <= family.maxLevel(); level++) {
                enchantedBook(recipeOutput, family.key(), level, family.bookTier(level), family.maxLevel() > 1);
            }
        }

        // The craft-only singles. One level, one book, and so no numeral in the id.
        for (ModEnchantments.CraftedEnchantment crafted : ModEnchantments.CRAFTED_ENCHANTMENTS) {
            for (int level = 1; level <= crafted.maxLevel(); level++) {
                enchantedBook(recipeOutput, crafted.key(), level, crafted.bookTier(),
                        crafted.maxLevel() > 1);
            }
        }

        // Every rung of the staff's three that sits above what the enchanting table hands out.
        for (ModEnchantments.StaffEnchantment staff : ModEnchantments.STAFF_ENCHANTMENTS) {
            for (int level = staff.firstCraftedLevel(); level <= staff.maxLevel(); level++) {
                enchantedBook(recipeOutput, staff.key(), level, staff.bookTier(level), true);
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
    private void enchantedBook(RecipeOutput recipeOutput, ResourceKey<Enchantment> key, int level, int bookTier,
                               boolean numbered) {
        // The stored enchantment is set component-first rather than through EnchantmentInstance:
        // during datagen the enchantment holder is a lazy one, and resolving it would make it clone
        // the enchantment through its codec, which needs item tags that datagen has not bound.
        ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        stored.set(this.registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key), level);

        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());

        Block cobblestone = ModBlocks.byLevel(bookTier).get();
        ShapedRecipeBuilder builder = new ShapedRecipeBuilder(RecipeCategory.COMBAT, book);
        ModEnchantments.BOOK_PATTERN.forEach(builder::pattern);

        String name = key.location().getPath() + "_book";
        builder.define('L', Items.LEATHER)
                .define('C', cobblestone)
                .unlockedBy(getHasName(cobblestone), has(cobblestone))
                .save(recipeOutput, UnnecessarilyCompressedCobblestone.MOD_ID + ":" + name
                        + (numbered ? "_" + level : ""));
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

    /**
     * One engraving or sigil: its family's core wrapped in the tier of stone it is cut from. The
     * core is what tells the two families' grids apart - a diamond for an engraving, an amethyst
     * shard for a sigil - so the two only have to keep their tiers clear of their own kind.
     */
    private void engraving(RecipeOutput recipeOutput, Engraving engraving) {
        Block cobblestone = ModBlocks.byLevel(engraving.blockTier()).get();

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ENGRAVINGS.get(engraving).get())
                .pattern("CCC")
                .pattern("CDC")
                .pattern("CCC")
                .define('C', cobblestone)
                .define('D', engraving.core())
                .unlockedBy(getHasName(cobblestone), has(cobblestone))
                .save(recipeOutput);
    }

    /**
     * One augment: its own tier of stone wrapped around a prismarine shard. See {@link Augment#CORE}
     * for why the core is a shard and not the engravings' diamond.
     */
    private void augment(RecipeOutput recipeOutput, Augment augment) {
        Block cobblestone = ModBlocks.byLevel(augment.blockTier()).get();

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.AUGMENTS.get(augment).get())
                .pattern("CCC")
                .pattern("CPC")
                .pattern("CCC")
                .define('C', cobblestone)
                .define('P', Augment.CORE)
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
