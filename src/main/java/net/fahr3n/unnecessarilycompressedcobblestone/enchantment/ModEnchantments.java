package net.fahr3n.unnecessarilycompressedcobblestone.enchantment;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.enchantment.custom.CompressionEnchantmentEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentTarget;

public class ModEnchantments {
    public static final ResourceKey<Enchantment> COMPRESSION = ResourceKey.create(Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "compression"));

    /**
     * The highest level the enchanting table and the anvil hand out on their own. It is also the
     * enchantment's registered max level, which is what makes that cap absolute: the table never
     * offers a level above it and the anvil clamps every combination to it.
     */
    public static final int MAX_TABLE_LEVEL = 3;

    /**
     * The levels only a crafted Compression book can carry, above what the table and anvil reach on
     * their own. Vanilla has no way to produce or even carry over a level this high, so the anvil
     * side of it lives in {@code ModEvents}.
     */
    public static final int FIRST_CRAFTED_LEVEL = MAX_TABLE_LEVEL + 1;

    /** The deepest Compression there is. */
    public static final int LAST_CRAFTED_LEVEL = 5;

    public static void bootstrap(BootstrapContext<Enchantment> context) {
        var items = context.lookup(Registries.ITEM);

        // #minecraft:enchantable/weapon is vanilla's melee weapon set - swords, axes and the mace -
        // so this goes on vanilla gear and on the mod's own sword (which is in #minecraft:swords)
        // alike, and on nothing that is not swung at something.
        register(context, COMPRESSION, Enchantment.enchantment(Enchantment.definition(
                items.getOrThrow(ItemTags.WEAPON_ENCHANTABLE),
                items.getOrThrow(ItemTags.WEAPON_ENCHANTABLE),
                5,
                MAX_TABLE_LEVEL,
                Enchantment.dynamicCost(10, 10),
                Enchantment.dynamicCost(40, 10),
                2,
                EquipmentSlotGroup.MAINHAND))
                .withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER,
                        EnchantmentTarget.VICTIM, new CompressionEnchantmentEffect()));
    }

    private static void register(BootstrapContext<Enchantment> registry, ResourceKey<Enchantment> key,
                                 Enchantment.Builder builder) {
        registry.register(key, builder.build(key.location()));
    }
}
