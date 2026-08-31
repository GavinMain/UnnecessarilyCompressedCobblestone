package net.fahr3n.unnecessarilycompressedcobblestone.item;

import java.util.EnumMap;
import java.util.List;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, UnnecessarilyCompressedCobblestone.MOD_ID);

    /** One point of defense per piece, no toughness, and the pieces never take damage. */
    public static final Holder<ArmorMaterial> COMPRESSED_COBBLESTONE_ARMOR_MATERIAL = ARMOR_MATERIALS.register(
            "compressed_cobblestone", () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.BOOTS, 1);
                        map.put(ArmorItem.Type.LEGGINGS, 1);
                        map.put(ArmorItem.Type.CHESTPLATE, 1);
                        map.put(ArmorItem.Type.HELMET, 1);
                        map.put(ArmorItem.Type.BODY, 1);
                    }),
                    9,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(ModBlocks.byLevel(1).get()),
                    List.of(new ArmorMaterial.Layer(
                            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "compressed_cobblestone"))),
                    0F, 0F));

    /**
     * Iron's protection values, iron's enchantability and toughness. The set is not meant to
     * out-armour the plain compressed set - what it is for is the step height and the jump, which
     * live in {@code ModEvents} and only apply when all four pieces are worn.
     */
    public static final Holder<ArmorMaterial> COMPRESSION_JUMP_ARMOR_MATERIAL = ARMOR_MATERIALS.register(
            "compression_jump", () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.BOOTS, 2);
                        map.put(ArmorItem.Type.LEGGINGS, 5);
                        map.put(ArmorItem.Type.CHESTPLATE, 6);
                        map.put(ArmorItem.Type.HELMET, 2);
                        map.put(ArmorItem.Type.BODY, 5);
                    }),
                    9,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(ModBlocks.byLevel(20).get()),
                    List.of(new ArmorMaterial.Layer(
                            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "compression_jump"))),
                    0F, 0F));

    /**
     * Leather's protection values exactly - 1, 3, 2, 1 down the body - and no toughness. The set is
     * deliberately the weakest armour in the mod against ordinary damage; what it is worth wearing
     * for is the lightning it shrugs off and the speed it carries, both of which live in
     * {@code ModEvents} and only apply when all four pieces are worn.
     */
    public static final Holder<ArmorMaterial> COMPRESSION_LIGHTNING_ARMOR_MATERIAL = ARMOR_MATERIALS.register(
            "compression_lightning", () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.BOOTS, 1);
                        map.put(ArmorItem.Type.LEGGINGS, 2);
                        map.put(ArmorItem.Type.CHESTPLATE, 3);
                        map.put(ArmorItem.Type.HELMET, 1);
                        map.put(ArmorItem.Type.BODY, 3);
                    }),
                    9,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(ModBlocks.byLevel(39).get()),
                    List.of(new ArmorMaterial.Layer(
                            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "compression_lightning"))),
                    0F, 0F));

    /**
     * Diamond's numbers exactly - 3, 8, 6, 3 down the body, two toughness, enchantability 10 - so
     * the set is worth wearing against anything, not only against the thing it is named for. What
     * it is really for is the projectile reduction each piece carries and the glowing it paints on
     * whatever the wearer's arrows hit, both of which live in {@code ModEvents}; only the set bonus
     * needs all four pieces, the reduction counts pieces one at a time.
     */
    public static final Holder<ArmorMaterial> COMPRESSION_ARROW_ARMOR_MATERIAL = ARMOR_MATERIALS.register(
            "compression_arrow", () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.BOOTS, 3);
                        map.put(ArmorItem.Type.LEGGINGS, 6);
                        map.put(ArmorItem.Type.CHESTPLATE, 8);
                        map.put(ArmorItem.Type.HELMET, 3);
                        map.put(ArmorItem.Type.BODY, 11);
                    }),
                    10,
                    SoundEvents.ARMOR_EQUIP_DIAMOND,
                    () -> Ingredient.of(ModBlocks.byLevel(69).get()),
                    List.of(new ArmorMaterial.Layer(
                            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "compression_arrow"))),
                    2F, 0F));

    public static void register(IEventBus eventBus) {
        ARMOR_MATERIALS.register(eventBus);
    }
}
