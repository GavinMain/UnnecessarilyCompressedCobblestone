package net.fahr3n.unnecessarilycompressedcobblestone.item;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestoneArmorItem;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestoneArrowItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestoneBowItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestoneMaceItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestoneSwordItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressionJumpArmorItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.SwordItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
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
     * Vanilla mace numbers - 5 attack damage, -3.4 attack speed, the 500 durability it never
     * spends - so the smash attack scales off fall distance exactly as it does on the real thing.
     * It is not {@code Rarity.EPIC}, since this one is crafted rather than pulled out of a vault.
     */
    public static final DeferredItem<CompressedCobblestoneMaceItem> COMPRESSED_COBBLESTONE_MACE =
            ITEMS.register("compressed_cobblestone_mace", () -> new CompressedCobblestoneMaceItem(
                    new Item.Properties().durability(500)
                            .component(DataComponents.TOOL, MaceItem.createToolProperties())
                            .attributes(MaceItem.createAttributes())));

    /** Creeper green over compressed grey. The only way to call the boss up. */
    public static final DeferredItem<DeferredSpawnEggItem> COMPRESSED_CREEPER_SPAWN_EGG =
            ITEMS.register("compressed_creeper_spawn_egg", () -> new DeferredSpawnEggItem(
                    ModEntities.COMPRESSED_CREEPER, 0x0DA70B, 0x5B5B5B, new Item.Properties()));

    /** What a Compressed Golem leaves behind. Just an item for now. */
    public static final DeferredItem<Item> TIER_1_COMPRESSED_HEART =
            ITEMS.register("tier_1_compressed_heart", () -> new Item(new Item.Properties()));

    /** The same, out of a Compressed Creeper. Also just an item for now. */
    public static final DeferredItem<Item> TIER_2_COMPRESSED_HEART =
            ITEMS.register("tier_2_compressed_heart", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> COMPRESSED_COBBLESTONE_APPLE =
            ITEMS.register("compressed_cobblestone_apple", () -> new Item(
                    new Item.Properties().food(ModFoodProperties.COMPRESSED_COBBLESTONE_APPLE)));

    /** Vanilla bow durability; the draw time and velocity are what set it apart. */
    public static final DeferredItem<CompressedCobblestoneBowItem> COMPRESSED_COBBLESTONE_BOW =
            ITEMS.register("compressed_cobblestone_bow", () -> new CompressedCobblestoneBowItem(
                    new Item.Properties().durability(384)));

    public static final DeferredItem<CompressedCobblestoneArrowItem> COMPRESSED_COBBLESTONE_ARROW =
            ITEMS.register("compressed_cobblestone_arrow", () -> new CompressedCobblestoneArrowItem(
                    new Item.Properties()));

    /**
     * Iron-sized durability. The pieces never actually take damage
     * (see {@link CompressedCobblestoneArmorItem}), but having a real max durability is what
     * makes them enchantable and repairable like any other armor.
     */
    private static Item.Properties armorProperties(ArmorItem.Type type) {
        return new Item.Properties().durability(type.getDurability(15));
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
