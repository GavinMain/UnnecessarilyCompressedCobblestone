package net.fahr3n.unnecessarilycompressedcobblestone.compat.jei;

import java.util.ArrayList;
import java.util.List;

import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;
import mezz.jei.api.recipe.vanilla.IVanillaRecipeFactory;
import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.enchantment.ModEnchantments;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engraving;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

/**
 * The two things this mod's anvil handler does that JEI's own anvil category cannot find.
 * <p>
 * JEI builds its book recipes by running a real anvil, but only for levels 1 to
 * {@link Enchantment#getMaxLevel()} - and every crafted level sits above that on purpose, since the
 * registered maximum is the table cap that keeps the table and the vanilla anvil from climbing to
 * one. So Compression IV, every Super, Hyper and Giga level, and the staff rungs past the table are
 * each shown here instead. Harvest Festival is registered at its true maximum and JEI already has it.
 * <p>
 * The Potion Engraving is the other: an anvil that copies a potion onto an item is nothing JEI could
 * guess at.
 */
final class AnvilRecipes {
    private AnvilRecipes() {
    }

    static List<IJeiAnvilRecipe> all(IVanillaRecipeFactory factory) {
        List<IJeiAnvilRecipe> recipes = new ArrayList<>();
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return recipes;
        }

        Registry<Enchantment> enchantments = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        for (ModEnchantments.Family family : ModEnchantments.FAMILIES) {
            addCraftedLevels(factory, recipes, enchantments, family.key(), family.maxLevel());
        }
        for (ModEnchantments.StaffEnchantment staff : ModEnchantments.STAFF_ENCHANTMENTS) {
            addCraftedLevels(factory, recipes, enchantments, staff.key(), staff.maxLevel());
        }

        addPotionEngraving(factory, recipes);
        return recipes;
    }

    /**
     * One recipe per piece of gear that takes the enchantment, each carrying every crafted book of
     * it. The gear is found the way the anvil handler decides it - {@code supportsEnchantment} on
     * every item in the registry - so another mod's axe is listed under Super Compression for free.
     */
    private static void addCraftedLevels(IVanillaRecipeFactory factory, List<IJeiAnvilRecipe> recipes,
                                         Registry<Enchantment> enchantments, ResourceKey<Enchantment> key,
                                         int maxLevel) {
        Holder<Enchantment> holder = enchantments.getHolder(key).orElse(null);
        if (holder == null) {
            return;
        }

        List<Integer> levels = new ArrayList<>();
        for (int level = 1; level <= maxLevel; level++) {
            if (ModEnchantments.isCraftedLevel(holder, level)) {
                levels.add(level);
            }
        }
        if (levels.isEmpty()) {
            return;
        }

        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack gear = new ItemStack(item);
            if (gear.isEmpty() || gear.is(Items.BOOK) || gear.is(Items.ENCHANTED_BOOK)
                    || !gear.supportsEnchantment(holder)) {
                continue;
            }

            List<ItemStack> books = new ArrayList<>(levels.size());
            List<ItemStack> outputs = new ArrayList<>(levels.size());
            for (int level : levels) {
                books.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(holder, level)));
                ItemStack enchanted = gear.copy();
                enchanted.enchant(holder, level);
                outputs.add(enchanted);
            }

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            recipes.add(factory.createAnvilRecipe(gear, books, outputs, id("crafted_enchantment."
                    + key.location().getPath() + "." + itemId.getNamespace() + "." + itemId.getPath())));
        }
    }

    /**
     * The blank engraving against every potion there is in every form the handler accepts, since it
     * takes anything carrying potion contents with an effect in them - a tipped arrow included.
     */
    private static void addPotionEngraving(IVanillaRecipeFactory factory, List<IJeiAnvilRecipe> recipes) {
        ItemStack engraving = new ItemStack(Engraving.POTION.item());
        List<ItemStack> potions = new ArrayList<>();
        List<ItemStack> outputs = new ArrayList<>();

        for (Holder<Potion> potion : BuiltInRegistries.POTION.holders().toList()) {
            if (potion.value().getEffects().isEmpty()) {
                continue;
            }

            for (Item bottle : List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION, Items.TIPPED_ARROW)) {
                ItemStack stack = PotionContents.createItemStack(bottle, potion);
                potions.add(stack);

                ItemStack engraved = engraving.copy();
                engraved.set(DataComponents.POTION_CONTENTS, stack.get(DataComponents.POTION_CONTENTS));
                outputs.add(engraved);
            }
        }

        if (!potions.isEmpty()) {
            recipes.add(factory.createAnvilRecipe(engraving, potions, outputs, id("potion_engraving")));
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "anvil." + path);
    }
}
