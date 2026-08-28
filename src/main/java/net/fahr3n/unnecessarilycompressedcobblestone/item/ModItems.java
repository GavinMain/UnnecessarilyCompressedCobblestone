package net.fahr3n.unnecessarilycompressedcobblestone.item;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestoneArmorItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
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
