package net.fahr3n.unnecessarilycompressedcobblestone.item;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestoneArmorItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestoneArrowItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestoneBowItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestoneSwordItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
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

    /** What a Compressed Golem leaves behind. Just an item for now. */
    public static final DeferredItem<Item> TIER_1_COMPRESSED_HEART =
            ITEMS.register("tier_1_compressed_heart", () -> new Item(new Item.Properties()));

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
