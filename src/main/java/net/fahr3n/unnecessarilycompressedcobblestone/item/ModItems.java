package net.fahr3n.unnecessarilycompressedcobblestone.item;

import java.util.EnumMap;
import java.util.Map;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestoneArmorItem;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.ArrowVeilItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.BossSpawnEggItem;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedArrowTier;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedArrowItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestoneBowItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestoneMaceItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestonePickaxeItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestoneSwordItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedArrowStaffItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedArrowTntStaffItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedLightningStaffItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedSummoningStaffItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedVanillaBoltItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressionJumpArmorItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressionArrowArmorItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressionLightningArmorItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.EngravingItem;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engraving;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(UnnecessarilyCompressedCobblestone.MOD_ID);

    // Standalone items go here, e.g.:
    // public static final DeferredItem<Item> SOME_ITEM = ITEMS.register("some_item",
    //         () -> new Item(new Item.Properties()));
    //
    // BlockItems are registered automatically by ModBlocks.registerBlock, into this same register.

    public static final DeferredItem<CompressedCobblestoneArmorItem> COMPRESSED_COBBLESTONE_HELMET =
            ITEMS.register("compressed_cobblestone_helmet", () -> new CompressedCobblestoneArmorItem(
                    ModArmorMaterials.COMPRESSED_COBBLESTONE_ARMOR_MATERIAL, ArmorItem.Type.HELMET,
                    armorProperties(ArmorItem.Type.HELMET)));

    public static final DeferredItem<CompressedCobblestoneArmorItem> COMPRESSED_COBBLESTONE_CHESTPLATE =
            ITEMS.register("compressed_cobblestone_chestplate", () -> new CompressedCobblestoneArmorItem(
                    ModArmorMaterials.COMPRESSED_COBBLESTONE_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE,
                    armorProperties(ArmorItem.Type.CHESTPLATE)));

    public static final DeferredItem<CompressedCobblestoneArmorItem> COMPRESSED_COBBLESTONE_LEGGINGS =
            ITEMS.register("compressed_cobblestone_leggings", () -> new CompressedCobblestoneArmorItem(
                    ModArmorMaterials.COMPRESSED_COBBLESTONE_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS,
                    armorProperties(ArmorItem.Type.LEGGINGS)));

    public static final DeferredItem<CompressedCobblestoneArmorItem> COMPRESSED_COBBLESTONE_BOOTS =
            ITEMS.register("compressed_cobblestone_boots", () -> new CompressedCobblestoneArmorItem(
                    ModArmorMaterials.COMPRESSED_COBBLESTONE_ARMOR_MATERIAL, ArmorItem.Type.BOOTS,
                    armorProperties(ArmorItem.Type.BOOTS)));

    /**
     * Six attack damage, iron attack speed. The attribute holds 5: the number shown in the
     * tooltip is that plus the player's own base attack damage of 1, exactly how an iron sword
     * gets to its 6. The sword never actually takes damage (see
     * {@link CompressedCobblestoneSwordItem}), but the tier still carries a real durability so
     * it stays enchantable and repairable like any other sword.
     */
    public static final DeferredItem<CompressedCobblestoneSwordItem> COMPRESSED_COBBLESTONE_SWORD =
            ITEMS.register("compressed_cobblestone_sword", () -> new CompressedCobblestoneSwordItem(
                    ModToolTiers.COMPRESSED_COBBLESTONE, new Item.Properties()
                            .attributes(SwordItem.createAttributes(ModToolTiers.COMPRESSED_COBBLESTONE, 3, -2.4F))));

    /**
     * The Compression Jump set. The pieces are ordinary iron-strength armour on their own; wearing
     * all four is what buys the step height and the jump, which {@code ModEvents} applies.
     */
    public static final DeferredItem<CompressionJumpArmorItem> COMPRESSION_JUMP_HELMET =
            ITEMS.register("compression_jump_helmet", () -> new CompressionJumpArmorItem(
                    ModArmorMaterials.COMPRESSION_JUMP_ARMOR_MATERIAL, ArmorItem.Type.HELMET,
                    armorProperties(ArmorItem.Type.HELMET)));

    public static final DeferredItem<CompressionJumpArmorItem> COMPRESSION_JUMP_CHESTPLATE =
            ITEMS.register("compression_jump_chestplate", () -> new CompressionJumpArmorItem(
                    ModArmorMaterials.COMPRESSION_JUMP_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE,
                    armorProperties(ArmorItem.Type.CHESTPLATE)));

    public static final DeferredItem<CompressionJumpArmorItem> COMPRESSION_JUMP_LEGGINGS =
            ITEMS.register("compression_jump_leggings", () -> new CompressionJumpArmorItem(
                    ModArmorMaterials.COMPRESSION_JUMP_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS,
                    armorProperties(ArmorItem.Type.LEGGINGS)));

    public static final DeferredItem<CompressionJumpArmorItem> COMPRESSION_JUMP_BOOTS =
            ITEMS.register("compression_jump_boots", () -> new CompressionJumpArmorItem(
                    ModArmorMaterials.COMPRESSION_JUMP_ARMOR_MATERIAL, ArmorItem.Type.BOOTS,
                    armorProperties(ArmorItem.Type.BOOTS)));

    /**
     * The pickaxe that gets into the hardened levels. Netherite-grade to begin with, and one point
     * of mining speed per digit of Compression Energy on top of that; like every other tool here it
     * keeps a real durability it never spends, so it stays enchantable and repairable.
     */
    public static final DeferredItem<CompressedCobblestonePickaxeItem> COMPRESSED_COBBLESTONE_PICKAXE =
            ITEMS.register("compressed_cobblestone_pickaxe", () -> new CompressedCobblestonePickaxeItem(
                    ModToolTiers.HARDENED_COMPRESSED_COBBLESTONE, new Item.Properties()
                            .attributes(PickaxeItem.createAttributes(ModToolTiers.HARDENED_COMPRESSED_COBBLESTONE,
                                    1.0F, -2.8F))));

    /**
     * Vanilla mace numbers - 5 attack damage, -3.4 attack speed, the 500 durability it never
     * spends - so the smash attack scales off fall distance exactly as it does on the real thing.
     * It is not {@code Rarity.EPIC}, since this one is crafted rather than pulled out of a vault.
     */
    public static final DeferredItem<CompressedCobblestoneMaceItem> COMPRESSED_COBBLESTONE_MACE =
            ITEMS.register("compressed_cobblestone_mace", () -> new CompressedCobblestoneMaceItem(
                    new Item.Properties().durability(500)
                            .component(DataComponents.TOOL, MaceItem.createToolProperties())
                            .attributes(MaceItem.createAttributes())));

    /**
     * The Compression Lightning set. The pieces are leather-grade armour on their own; wearing all
     * four is what buys the speed and the near-immunity to lightning, which {@code ModEvents}
     * applies.
     */
    public static final DeferredItem<CompressionLightningArmorItem> COMPRESSION_LIGHTNING_HELMET =
            ITEMS.register("compression_lightning_helmet", () -> new CompressionLightningArmorItem(
                    ModArmorMaterials.COMPRESSION_LIGHTNING_ARMOR_MATERIAL, ArmorItem.Type.HELMET,
                    armorProperties(ArmorItem.Type.HELMET)));

    public static final DeferredItem<CompressionLightningArmorItem> COMPRESSION_LIGHTNING_CHESTPLATE =
            ITEMS.register("compression_lightning_chestplate", () -> new CompressionLightningArmorItem(
                    ModArmorMaterials.COMPRESSION_LIGHTNING_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE,
                    armorProperties(ArmorItem.Type.CHESTPLATE)));

    public static final DeferredItem<CompressionLightningArmorItem> COMPRESSION_LIGHTNING_LEGGINGS =
            ITEMS.register("compression_lightning_leggings", () -> new CompressionLightningArmorItem(
                    ModArmorMaterials.COMPRESSION_LIGHTNING_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS,
                    armorProperties(ArmorItem.Type.LEGGINGS)));

    public static final DeferredItem<CompressionLightningArmorItem> COMPRESSION_LIGHTNING_BOOTS =
            ITEMS.register("compression_lightning_boots", () -> new CompressionLightningArmorItem(
                    ModArmorMaterials.COMPRESSION_LIGHTNING_ARMOR_MATERIAL, ArmorItem.Type.BOOTS,
                    armorProperties(ArmorItem.Type.BOOTS)));

    /**
     * The Compression Arrow set. Diamond-grade on its own, and each piece worn takes a fifth off
     * every projectile that lands; all four together make the wearer's own arrows paint whatever
     * they hit. {@code ModEvents} owns both.
     */
    public static final DeferredItem<CompressionArrowArmorItem> COMPRESSION_ARROW_HELMET =
            ITEMS.register("compression_arrow_helmet", () -> new CompressionArrowArmorItem(
                    ModArmorMaterials.COMPRESSION_ARROW_ARMOR_MATERIAL, ArmorItem.Type.HELMET,
                    armorProperties(ArmorItem.Type.HELMET, 33)));

    public static final DeferredItem<CompressionArrowArmorItem> COMPRESSION_ARROW_CHESTPLATE =
            ITEMS.register("compression_arrow_chestplate", () -> new CompressionArrowArmorItem(
                    ModArmorMaterials.COMPRESSION_ARROW_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE,
                    armorProperties(ArmorItem.Type.CHESTPLATE, 33)));

    public static final DeferredItem<CompressionArrowArmorItem> COMPRESSION_ARROW_LEGGINGS =
            ITEMS.register("compression_arrow_leggings", () -> new CompressionArrowArmorItem(
                    ModArmorMaterials.COMPRESSION_ARROW_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS,
                    armorProperties(ArmorItem.Type.LEGGINGS, 33)));

    public static final DeferredItem<CompressionArrowArmorItem> COMPRESSION_ARROW_BOOTS =
            ITEMS.register("compression_arrow_boots", () -> new CompressionArrowArmorItem(
                    ModArmorMaterials.COMPRESSION_ARROW_ARMOR_MATERIAL, ArmorItem.Type.BOOTS,
                    armorProperties(ArmorItem.Type.BOOTS, 33)));

    /**
     * The only way to call the boss up. The colours are kept for anything that reads them, but they
     * are not what is drawn: every boss egg in this mod is a dragon egg, and using one starts the
     * summon animation rather than putting a creeper down.
     */
    public static final DeferredItem<BossSpawnEggItem> COMPRESSED_CREEPER_SPAWN_EGG =
            ITEMS.register("compressed_creeper_spawn_egg", () -> new BossSpawnEggItem(
                    ModEntities.COMPRESSED_CREEPER, 0x0DA70B, 0x5B5B5B, new Item.Properties()));

    /** The same, for the second rung of the arrow boss. */
    public static final DeferredItem<BossSpawnEggItem> COMPRESSED_SKELETON_TIER_2_SPAWN_EGG =
            ITEMS.register("compressed_skeleton_tier_2_spawn_egg", () -> new BossSpawnEggItem(
                    ModEntities.COMPRESSED_SKELETON_TIER_2, 0xC1C1C1, 0x8A2020, new Item.Properties()));

    /** And for the last. */
    public static final DeferredItem<BossSpawnEggItem> COMPRESSED_SKELETON_TIER_3_SPAWN_EGG =
            ITEMS.register("compressed_skeleton_tier_3_spawn_egg", () -> new BossSpawnEggItem(
                    ModEntities.COMPRESSED_SKELETON_TIER_3, 0xC1C1C1, 0x2A2A6A, new Item.Properties()));

    /** And for the Summoner, which is called up the same way every other boss here is. */
    public static final DeferredItem<BossSpawnEggItem> COMPRESSED_SUMMONER_SPAWN_EGG =
            ITEMS.register("compressed_summoner_spawn_egg", () -> new BossSpawnEggItem(
                    ModEntities.COMPRESSED_SUMMONER, 0x340000, 0x51A03E, new Item.Properties()));

    /** The same, for the skeleton. Bone white over compressed grey. */
    public static final DeferredItem<BossSpawnEggItem> COMPRESSED_SKELETON_SPAWN_EGG =
            ITEMS.register("compressed_skeleton_spawn_egg", () -> new BossSpawnEggItem(
                    ModEntities.COMPRESSED_SKELETON, 0xC1C1C1, 0x494949, new Item.Properties()));

    /** The same, for the conjurer. */
    public static final DeferredItem<BossSpawnEggItem> COMPRESSED_CONJURER_SPAWN_EGG =
            ITEMS.register("compressed_conjurer_spawn_egg", () -> new BossSpawnEggItem(
                    ModEntities.COMPRESSED_CONJURER, 0x2C6D6B, 0xE8F6FF, new Item.Properties()));

    /**
     * Thirty seconds of cover from everything that was not fired by the person holding it. Not
     * consumed and never worn out; what limits it is the minute it spends on cooldown.
     */
    public static final DeferredItem<ArrowVeilItem> ARROW_VEIL =
            ITEMS.register("arrow_veil", () -> new ArrowVeilItem(new Item.Properties().stacksTo(1)));

    /**
     * The first bolt, and the plainest: a core loaded with this throws exactly what a thunderstorm
     * throws. Bolts are only ever used by being put on a Lightning Core.
     */
    public static final DeferredItem<CompressedVanillaBoltItem> COMPRESSED_VANILLA_BOLT =
            ITEMS.register("compressed_vanilla_bolt", () -> new CompressedVanillaBoltItem(new Item.Properties()));

    /** What a Compressed Golem leaves behind. Just an item for now. */
    public static final DeferredItem<Item> TIER_1_COMPRESSED_HEART =
            ITEMS.register("tier_1_compressed_heart", () -> new Item(new Item.Properties()));

    /** The same, out of a Compressed Creeper. Also just an item for now. */
    public static final DeferredItem<Item> TIER_2_COMPRESSED_HEART =
            ITEMS.register("tier_2_compressed_heart", () -> new Item(new Item.Properties()));

    /** The same again, out of a Compressed Conjurer. Also just an item for now. */
    public static final DeferredItem<Item> TIER_3_COMPRESSED_HEART =
            ITEMS.register("tier_3_compressed_heart", () -> new Item(new Item.Properties()));

    /** And again, out of a Compressed Skeleton. The Arrow Staff is built around one. */
    public static final DeferredItem<Item> TIER_4_COMPRESSED_HEART =
            ITEMS.register("tier_4_compressed_heart", () -> new Item(new Item.Properties()));

    /** Out of the second rung of the arrow boss. The Arrow TNT Staff is built around one. */
    public static final DeferredItem<Item> TIER_5_COMPRESSED_HEART =
            ITEMS.register("tier_5_compressed_heart", () -> new Item(new Item.Properties()));

    /** Out of the last rung of the arrow boss. The Engraving Table is built around one. */
    public static final DeferredItem<Item> TIER_6_COMPRESSED_HEART =
            ITEMS.register("tier_6_compressed_heart", () -> new Item(new Item.Properties()));

    /** Out of the Summoner. The Summoning Staff is built around one. */
    public static final DeferredItem<Item> TIER_7_COMPRESSED_HEART =
            ITEMS.register("tier_7_compressed_heart", () -> new Item(new Item.Properties()));

    /** Slowness II bought with Resistance and Regeneration III. */
    public static final DeferredItem<Item> TIER_1_COMPRESSED_COBBLESTONE_APPLE =
            ITEMS.register("tier_1_compressed_cobblestone_apple", () -> new Item(
                    new Item.Properties().food(ModFoodProperties.TIER_1_COMPRESSED_COBBLESTONE_APPLE)));

    /** The tier 1 apple pressed again: only Slowness I, and both boons a level deeper. */
    public static final DeferredItem<Item> TIER_2_COMPRESSED_COBBLESTONE_APPLE =
            ITEMS.register("tier_2_compressed_cobblestone_apple", () -> new Item(
                    new Item.Properties().food(ModFoodProperties.TIER_2_COMPRESSED_COBBLESTONE_APPLE)));

    /** The tier 2 apple pressed once more: no weight left at all, and both boons at V. */
    public static final DeferredItem<Item> TIER_3_COMPRESSED_COBBLESTONE_APPLE =
            ITEMS.register("tier_3_compressed_cobblestone_apple", () -> new Item(
                    new Item.Properties().food(ModFoodProperties.TIER_3_COMPRESSED_COBBLESTONE_APPLE)));

    /**
     * The Compressed Lightning Staff. The durability is never spent (see
     * {@link CompressedLightningStaffItem}) but has to be there, since an item that is not
     * damageable cannot be enchanted at a table - and the staff's three enchantments are half of
     * what it is.
     */
    public static final DeferredItem<CompressedLightningStaffItem> COMPRESSED_LIGHTNING_STAFF =
            ITEMS.register("compressed_lightning_staff", () -> new CompressedLightningStaffItem(
                    new Item.Properties().durability(384).stacksTo(1)));

    /**
     * The Compressed Arrow Staff. Same shape of weapon as the lightning one and the same three
     * enchantments; what it does with them is mark something and bury it.
     */
    public static final DeferredItem<CompressedArrowStaffItem> COMPRESSED_ARROW_STAFF =
            ITEMS.register("compressed_arrow_staff", () -> new CompressedArrowStaffItem(
                    new Item.Properties().durability(384).stacksTo(1)));

    /**
     * The Compressed Summoning Staff. Ten seconds of casting for a phantom that fights on your side,
     * which is the only thing in the mod that does.
     */
    public static final DeferredItem<CompressedSummoningStaffItem> COMPRESSED_SUMMONING_STAFF =
            ITEMS.register("compressed_summoning_staff", () -> new CompressedSummoningStaffItem(
                    new Item.Properties().durability(384).stacksTo(1)));

    /** The same staff again, throwing lit Arrow TNT instead of arrows. */
    public static final DeferredItem<CompressedArrowTntStaffItem> COMPRESSED_ARROW_TNT_STAFF =
            ITEMS.register("compressed_arrow_tnt_staff", () -> new CompressedArrowTntStaffItem(
                    new Item.Properties().durability(384).stacksTo(1)));

    /** Vanilla bow durability; the draw time and velocity are what set it apart. */
    public static final DeferredItem<CompressedCobblestoneBowItem> COMPRESSED_COBBLESTONE_BOW =
            ITEMS.register("compressed_cobblestone_bow", () -> new CompressedCobblestoneBowItem(
                    new Item.Properties().durability(384)));

    public static final DeferredItem<CompressedArrowItem> COMPRESSED_COBBLESTONE_ARROW =
            ITEMS.register("compressed_cobblestone_arrow",
                    () -> new CompressedArrowItem(CompressedArrowTier.COBBLESTONE, new Item.Properties()));

    /** Three times the compressed arrow, which is six times a vanilla one. */
    public static final DeferredItem<CompressedArrowItem> SUPER_COMPRESSED_ARROW =
            ITEMS.register("super_compressed_arrow",
                    () -> new CompressedArrowItem(CompressedArrowTier.SUPER, new Item.Properties()));

    /**
     * Every engraving, as the item it is crafted and carried as. One per {@link Engraving} constant
     * and registered in one loop rather than as a field each, so a new engraving is a constant, a
     * recipe and a texture - the item, the model, the creative tab and the table all come free.
     */
    public static final Map<Engraving, DeferredItem<EngravingItem>> ENGRAVINGS = registerEngravings();

    private static Map<Engraving, DeferredItem<EngravingItem>> registerEngravings() {
        Map<Engraving, DeferredItem<EngravingItem>> engravings = new EnumMap<>(Engraving.class);
        for (Engraving engraving : Engraving.values()) {
            engravings.put(engraving, ITEMS.register(engraving.itemName(),
                    () -> new EngravingItem(engraving, new Item.Properties().stacksTo(16))));
        }

        return engravings;
    }

    /**
     * Iron-sized durability. The pieces never actually take damage
     * (see {@link CompressedCobblestoneArmorItem}), but having a real max durability is what
     * makes them enchantable and repairable like any other armor.
     */
    private static Item.Properties armorProperties(ArmorItem.Type type) {
        return armorProperties(type, 15);
    }

    /**
     * The same, at a chosen durability multiplier - vanilla's own per-material number, 15 for iron
     * and 33 for diamond. None of this armour ever spends a point of it, but the bar is still what
     * a player reads the grade of a piece off, so a diamond-statted set should show a diamond bar.
     */
    private static Item.Properties armorProperties(ArmorItem.Type type, int durabilityMultiplier) {
        return new Item.Properties().durability(type.getDurability(durabilityMultiplier));
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
