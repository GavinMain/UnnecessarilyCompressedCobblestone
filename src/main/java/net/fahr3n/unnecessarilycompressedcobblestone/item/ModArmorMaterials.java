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

    /**
     * Diamond's protection values - 3, 6, 8, 3 down the body - and diamond's toughness. It is the
     * best plate in the mod, and it needs to be: what the set is actually for is the half it takes
     * off everything a potion can do, and a player wearing it is expected to be standing in the
     * middle of the thing that made the potion necessary.
     */
    public static final Holder<ArmorMaterial> COMPRESSION_MAGIC_ARMOR_MATERIAL = ARMOR_MATERIALS.register(
            "compression_magic", () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.BOOTS, 3);
                        map.put(ArmorItem.Type.LEGGINGS, 6);
                        map.put(ArmorItem.Type.CHESTPLATE, 8);
                        map.put(ArmorItem.Type.HELMET, 3);
                        map.put(ArmorItem.Type.BODY, 11);
                    }),
                    10,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(ModBlocks.byLevel(124).get()),
                    List.of(new ArmorMaterial.Layer(
                            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "compression_magic"))),
                    2.0F, 0F));

    /**
     * The best plate in the mod, and the first set here that is not measured against diamond:
     * netherite's 3, 6, 8, 3 with a point added to every piece, and netherite's three toughness with
     * a point added to that - so 4, 7, 9, 4 and four toughness, one step past the deepest armour
     * vanilla has. Netherite's enchantability and its tenth of knockback resistance come across
     * unchanged.
     * <p>
     * That is deliberately only a step: the armour note is clear that armour plateaus and that
     * nothing here should chase a ratio through it. What the set is actually worth is in
     * {@code ModEvents} - while it is raining, nothing harmful will stick to the wearer at all.
     */
    public static final Holder<ArmorMaterial> COMPRESSION_RAIN_ARMOR_MATERIAL = ARMOR_MATERIALS.register(
            "compression_rain", () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.BOOTS, 4);
                        map.put(ArmorItem.Type.LEGGINGS, 7);
                        map.put(ArmorItem.Type.CHESTPLATE, 9);
                        map.put(ArmorItem.Type.HELMET, 4);
                        map.put(ArmorItem.Type.BODY, 12);
                    }),
                    15,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(ModBlocks.byLevel(173).get()),
                    List.of(new ArmorMaterial.Layer(
                            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "compression_rain"))),
                    4.0F, 0.1F));

    /**
     * The last plate: the armour and the toughness both at the ceiling the game will hold, and not a
     * point below it.
     * <p>
     * Both figures are chosen against that ceiling rather than picked. {@code Attributes.ARMOR} is a
     * {@code RangedAttribute} that stops at 30 and {@code ARMOR_TOUGHNESS} at 20, and a piece's
     * toughness is applied in full by every piece rather than shared out, so 5, 11, 9, 5 down the
     * body is exactly 30 and five toughness a piece is exactly 20. Anything larger is clamped on the
     * way in with nothing anywhere saying so, which is why there is no point stating it.
     * <p>
     * It is worth being plain about what that is and is not worth. {@code CombatRules} caps armour's
     * contribution at four fifths whatever the numbers are, and the Compression Rain set's 24 behind
     * 16 was already at that ceiling against essentially anything a player meets - so the six points
     * this adds buy almost nothing. What the set is actually for is the four abilities in
     * {@code ModEvents}, one to a slot, and those are not armour at all.
     * <p>
     * Netherite's enchantability and its tenth of knockback resistance come across unchanged, as
     * they do on the Rain set.
     */
    public static final Holder<ArmorMaterial> ULTIMATE_COMPRESSED_ARMOR_MATERIAL = ARMOR_MATERIALS.register(
            "ultimate_compressed", () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.BOOTS, 5);
                        map.put(ArmorItem.Type.LEGGINGS, 9);
                        map.put(ArmorItem.Type.CHESTPLATE, 11);
                        map.put(ArmorItem.Type.HELMET, 5);
                        map.put(ArmorItem.Type.BODY, 13);
                    }),
                    15,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(ModBlocks.byLevel(244).get()),
                    List.of(new ArmorMaterial.Layer(
                            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "ultimate_compressed"))),
                    5.0F, 0.1F));

    public static void register(IEventBus eventBus) {
        ARMOR_MATERIALS.register(eventBus);
    }
}
