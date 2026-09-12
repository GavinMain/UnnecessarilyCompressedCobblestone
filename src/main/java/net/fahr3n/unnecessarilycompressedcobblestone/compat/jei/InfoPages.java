package net.fahr3n.unnecessarilycompressedcobblestone.compat.jei;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import mezz.jei.api.registration.IRecipeRegistration;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CarvedCobblestoneBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.BoltItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Information pages for everything that has no recipe, so that looking one up says where it comes
 * from rather than nothing at all. Every name in a page is read off the thing it names - a block's
 * own name, an entity type's description - so renaming a boss renames it here too.
 */
final class InfoPages {
    private InfoPages() {
    }

    /**
     * Which boss gives up which heart. This restates the entity loot tables, which only the server
     * has; the loot provider is the source of truth and a new heart needs a line in both.
     */
    private static final Map<Supplier<? extends Item>, Supplier<? extends EntityType<?>>> HEARTS = Map.ofEntries(
            Map.entry(ModItems.TIER_1_COMPRESSED_HEART, ModEntities.COMPRESSED_GOLEM),
            Map.entry(ModItems.TIER_2_COMPRESSED_HEART, ModEntities.COMPRESSED_CREEPER),
            Map.entry(ModItems.TIER_3_COMPRESSED_HEART, ModEntities.COMPRESSED_CONJURER),
            Map.entry(ModItems.TIER_4_COMPRESSED_HEART, ModEntities.COMPRESSED_SKELETON),
            Map.entry(ModItems.TIER_5_COMPRESSED_HEART, ModEntities.COMPRESSED_SKELETON_TIER_2),
            Map.entry(ModItems.TIER_6_COMPRESSED_HEART, ModEntities.COMPRESSED_SKELETON_TIER_3),
            Map.entry(ModItems.TIER_7_COMPRESSED_HEART, ModEntities.COMPRESSED_SUMMONER),
            Map.entry(ModItems.TIER_8_COMPRESSED_HEART, ModEntities.COMPRESSED_SPIRIT),
            Map.entry(ModItems.TIER_9_COMPRESSED_HEART, ModEntities.COMPRESSED_GOLEM_TIER_2),
            Map.entry(ModItems.TIER_10_COMPRESSED_HEART, ModEntities.COMPRESSED_WITCH),
            Map.entry(ModItems.TIER_11_COMPRESSED_HEART, ModEntities.COMPRESSED_CHICKEN_BOSS),
            Map.entry(ModItems.TIER_12_COMPRESSED_HEART, ModEntities.COMPRESSED_CREEPER_TIER_2),
            Map.entry(ModItems.TIER_13_COMPRESSED_HEART, ModEntities.COMPRESSED_COMPOSER),
            Map.entry(ModItems.TIER_14_COMPRESSED_HEART, ModEntities.COMPRESSED_GUARDIAN),
            Map.entry(ModItems.TIER_15_COMPRESSED_HEART, ModEntities.COMPRESSED_HUSK),
            Map.entry(ModItems.TIER_16_COMPRESSED_HEART, ModEntities.COMPRESSED_SNOW_GOLEM),
            Map.entry(ModItems.TIER_17_COMPRESSED_HEART, ModEntities.COMPRESSED_ORE_GOLEM),
            Map.entry(ModItems.TIER_18_COMPRESSED_HEART, ModEntities.COMPRESSED_GHAST),
            Map.entry(ModItems.COMPRESSED_DRAGON_EGG, ModEntities.COMPRESSED_DRAGON_TIER_2));

    static void register(IRecipeRegistration registration) {
        HEARTS.forEach((item, boss) -> registration.addItemStackInfo(new ItemStack(item.get()),
                key("boss_drop", boss.get().getDescription())));

        // Every bolt, not only the eighty-eight with no recipe: the Bolt TNT draws from every
        // BoltItem in the registry, so this is true of all of them and of another mod's as well.
        List<ItemStack> bolts = BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof BoltItem)
                .map(ItemStack::new)
                .toList();
        registration.addItemStackInfo(bolts,
                key("bolt_drop", ModBlocks.BOLT_TNT.get().getName()));

        for (var head : ModBlocks.CARVED_HEADS) {
            CarvedCobblestoneBlock block = head.get();
            registration.addItemStackInfo(new ItemStack(block), key("carved_head",
                    ModBlocks.byLevel(block.carvedFromLevel()).get().getName(),
                    ModBlocks.byLevel(block.bodyLevel()).get().getName()));
        }

        registration.addItemStackInfo(List.of(new ItemStack(ModBlocks.COMPRESSED_COBBLESTONE_SAPLING.get()),
                new ItemStack(ModBlocks.COMPRESSED_COBBLESTONE_LEAVES.get())), key("tree"));

        registration.addItemStackInfo(new ItemStack(ModBlocks.SLIPPERY_ICE.get()),
                key("slippery_ice", ModBlocks.DEBRIS_TNT.get().getName()));
    }

    private static Component key(String page, Object... args) {
        return Component.translatable("jei.unnecessarilycompressedcobblestone.info." + page, args);
    }
}
