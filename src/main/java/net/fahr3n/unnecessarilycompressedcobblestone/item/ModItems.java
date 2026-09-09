package net.fahr3n.unnecessarilycompressedcobblestone.item;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestoneArmorItem;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.ArrowVeilItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.BoltLauncherItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.TntLauncherItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.BossSpawnEggItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.PetSpawnEggItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.UltimateCompressedArmorItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.BrokenCompressedSwordItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedFireworkItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedFishingRodItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompositionBoltItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.RestBoltItem;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Composition;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedArrowTier;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedArrowItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestoneBowItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestoneMaceItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestonePickaxeItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestonePickaxeTier2Item;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestoneSwordItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedArrowStaffItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedArrowTntStaffItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedKatanaItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedHealingStaffItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedLightningStaffItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedSummoningStaffItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedVanillaBoltItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressionJumpArmorItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressionArrowArmorItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressionLightningArmorItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressionMagicArmorItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressionRainArmorItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.AugmentItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.EngravingItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedBoneItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedSaddleItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedScytheItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedShieldItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedSpearItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedTotemItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.DevSwordItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressionBombItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.RayOfLaserItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.NoteBoltItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.SongBoltItem;
import net.fahr3n.unnecessarilycompressedcobblestone.util.PianoNote;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Augment;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engraving;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.PickaxeItem;
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
     * The same sword, carrying every enchantment in the game at 255 - the ones that mean nothing on
     * a sword and the curses included. Which enchantments those are is worked out from the registry
     * when it is held rather than written into its recipe, so another mod's come along too: see
     * {@link BrokenCompressedSwordItem}.
     */
    public static final DeferredItem<BrokenCompressedSwordItem> BROKEN_COMPRESSED_SWORD =
            ITEMS.register("broken_compressed_sword", () -> new BrokenCompressedSwordItem(
                    ModToolTiers.HARDENED_COMPRESSED_COBBLESTONE, new Item.Properties()
                            .attributes(SwordItem.createAttributes(ModToolTiers.HARDENED_COMPRESSED_COBBLESTONE,
                                    3, -2.4F))));

    /**
     * The Compressed Katana. A sword's shape at a netherite-grade tier, three blocks further of
     * reach and a swing a good deal quicker than a sword's - see {@link CompressedKatanaItem}, which
     * states all three as plain attributes so nothing downstream has to know this item exists.
     * <p>
     * The tier's own attack damage bonus of 4 plus the item's 3 is 7, which the tooltip shows as 8
     * once the player's own base damage is counted, the same arithmetic every other weapon here
     * uses. Its durability comes from the tier and is never spent.
     */
    public static final DeferredItem<CompressedKatanaItem> COMPRESSED_KATANA =
            ITEMS.register("compressed_katana", () -> new CompressedKatanaItem(
                    ModToolTiers.HARDENED_COMPRESSED_COBBLESTONE, new Item.Properties()
                            .attributes(CompressedKatanaItem.createAttributes(
                                    ModToolTiers.HARDENED_COMPRESSED_COBBLESTONE))));

    /**
     * The Compressed Fishing Rod: ten catches a cast instead of one, and a rod that never wears out.
     * The durability is there for the same reason every other tool's here is - an item that is not
     * damageable cannot be enchanted at a table - and is never actually spent.
     */
    public static final DeferredItem<CompressedFishingRodItem> COMPRESSED_FISHING_ROD =
            ITEMS.register("compressed_fishing_rod", () -> new CompressedFishingRodItem(
                    new Item.Properties().durability(512).stacksTo(1)));

    /**
     * A firework that is never spent. Everything else about it is vanilla's - the same rocket
     * entity, the same flight and the same burst - so it boosts an elytra and lights the sky exactly
     * as an ordinary one does, and simply stays in the hand afterwards.
     */
    public static final DeferredItem<CompressedFireworkItem> COMPRESSED_FIREWORK =
            ITEMS.register("compressed_firework", () -> new CompressedFireworkItem(
                    new Item.Properties().stacksTo(1)));

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
     * The Compression Magic set. Diamond-grade plate on its own; wearing all four is what buys the
     * half off everything a potion deals, which {@code ModEvents} applies.
     */
    public static final DeferredItem<CompressionMagicArmorItem> COMPRESSION_MAGIC_HELMET =
            ITEMS.register("compression_magic_helmet", () -> new CompressionMagicArmorItem(
                    ModArmorMaterials.COMPRESSION_MAGIC_ARMOR_MATERIAL, ArmorItem.Type.HELMET,
                    armorProperties(ArmorItem.Type.HELMET, 33)));

    public static final DeferredItem<CompressionMagicArmorItem> COMPRESSION_MAGIC_CHESTPLATE =
            ITEMS.register("compression_magic_chestplate", () -> new CompressionMagicArmorItem(
                    ModArmorMaterials.COMPRESSION_MAGIC_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE,
                    armorProperties(ArmorItem.Type.CHESTPLATE, 33)));

    public static final DeferredItem<CompressionMagicArmorItem> COMPRESSION_MAGIC_LEGGINGS =
            ITEMS.register("compression_magic_leggings", () -> new CompressionMagicArmorItem(
                    ModArmorMaterials.COMPRESSION_MAGIC_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS,
                    armorProperties(ArmorItem.Type.LEGGINGS, 33)));

    public static final DeferredItem<CompressionMagicArmorItem> COMPRESSION_MAGIC_BOOTS =
            ITEMS.register("compression_magic_boots", () -> new CompressionMagicArmorItem(
                    ModArmorMaterials.COMPRESSION_MAGIC_ARMOR_MATERIAL, ArmorItem.Type.BOOTS,
                    armorProperties(ArmorItem.Type.BOOTS, 33)));

    /**
     * The Compression Rain set. A step past netherite on its own - see the material for the exact
     * numbers - and wearing all four is what buys the immunity to harmful effects while it is
     * raining, which {@code ModEvents} applies.
     */
    public static final DeferredItem<CompressionRainArmorItem> COMPRESSION_RAIN_HELMET =
            ITEMS.register("compression_rain_helmet", () -> new CompressionRainArmorItem(
                    ModArmorMaterials.COMPRESSION_RAIN_ARMOR_MATERIAL, ArmorItem.Type.HELMET,
                    armorProperties(ArmorItem.Type.HELMET, 37)));

    public static final DeferredItem<CompressionRainArmorItem> COMPRESSION_RAIN_CHESTPLATE =
            ITEMS.register("compression_rain_chestplate", () -> new CompressionRainArmorItem(
                    ModArmorMaterials.COMPRESSION_RAIN_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE,
                    armorProperties(ArmorItem.Type.CHESTPLATE, 37)));

    public static final DeferredItem<CompressionRainArmorItem> COMPRESSION_RAIN_LEGGINGS =
            ITEMS.register("compression_rain_leggings", () -> new CompressionRainArmorItem(
                    ModArmorMaterials.COMPRESSION_RAIN_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS,
                    armorProperties(ArmorItem.Type.LEGGINGS, 37)));

    public static final DeferredItem<CompressionRainArmorItem> COMPRESSION_RAIN_BOOTS =
            ITEMS.register("compression_rain_boots", () -> new CompressionRainArmorItem(
                    ModArmorMaterials.COMPRESSION_RAIN_ARMOR_MATERIAL, ArmorItem.Type.BOOTS,
                    armorProperties(ArmorItem.Type.BOOTS, 37)));

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
     * The pickaxe that gets into the second floor, at {@code ModBlocks.HARDENED_LEVEL_TIER_2} and
     * above - stone the pickaxe before it cannot touch. It reaches four blocks further than an
     * ordinary tool and carries every enchantment in the game at 255, curses excepted, worked out
     * from the registry when it is held so that another mod's enchantments come along too. See
     * {@link CompressedCobblestonePickaxeTier2Item}.
     */
    public static final DeferredItem<CompressedCobblestonePickaxeTier2Item> COMPRESSED_COBBLESTONE_PICKAXE_TIER_2 =
            ITEMS.register("compressed_cobblestone_pickaxe_tier_2", () -> new CompressedCobblestonePickaxeTier2Item(
                    ModToolTiers.HARDENED_COMPRESSED_COBBLESTONE_TIER_2, new Item.Properties()
                            .attributes(CompressedCobblestonePickaxeTier2Item.createAttributes(
                                    ModToolTiers.HARDENED_COMPRESSED_COBBLESTONE_TIER_2))));

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
     * The Ultimate Compressed set, one ability to a slot rather than one to the set: night vision on
     * the helmet, creative flight on the chestplate, two blocks of step height on the leggings and
     * Speed III on the boots. All four live in {@code ModEvents} and are read off the armour
     * material, so this class is only a name - see {@link UltimateCompressedArmorItem}.
     */
    public static final DeferredItem<UltimateCompressedArmorItem> ULTIMATE_COMPRESSED_HELMET =
            ITEMS.register("ultimate_compressed_helmet", () -> new UltimateCompressedArmorItem(
                    ModArmorMaterials.ULTIMATE_COMPRESSED_ARMOR_MATERIAL, ArmorItem.Type.HELMET,
                    armorProperties(ArmorItem.Type.HELMET, 37)));

    public static final DeferredItem<UltimateCompressedArmorItem> ULTIMATE_COMPRESSED_CHESTPLATE =
            ITEMS.register("ultimate_compressed_chestplate", () -> new UltimateCompressedArmorItem(
                    ModArmorMaterials.ULTIMATE_COMPRESSED_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE,
                    armorProperties(ArmorItem.Type.CHESTPLATE, 37)));

    public static final DeferredItem<UltimateCompressedArmorItem> ULTIMATE_COMPRESSED_LEGGINGS =
            ITEMS.register("ultimate_compressed_leggings", () -> new UltimateCompressedArmorItem(
                    ModArmorMaterials.ULTIMATE_COMPRESSED_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS,
                    armorProperties(ArmorItem.Type.LEGGINGS, 37)));

    public static final DeferredItem<UltimateCompressedArmorItem> ULTIMATE_COMPRESSED_BOOTS =
            ITEMS.register("ultimate_compressed_boots", () -> new UltimateCompressedArmorItem(
                    ModArmorMaterials.ULTIMATE_COMPRESSED_ARMOR_MATERIAL, ArmorItem.Type.BOOTS,
                    armorProperties(ArmorItem.Type.BOOTS, 37)));

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

    /** And for the Compressed Witch. Witch purple over brewing green. */
    public static final DeferredItem<BossSpawnEggItem> COMPRESSED_WITCH_SPAWN_EGG =
            ITEMS.register("compressed_witch_spawn_egg", () -> new BossSpawnEggItem(
                    ModEntities.COMPRESSED_WITCH, 0x3A2A4A, 0x67B84B, new Item.Properties()));

    /** The Compressed Composer's egg. Illager grey over the blue of its boss bar. */
    public static final DeferredItem<BossSpawnEggItem> COMPRESSED_COMPOSER_SPAWN_EGG =
            ITEMS.register("compressed_composer_spawn_egg", () -> new BossSpawnEggItem(
                    ModEntities.COMPRESSED_COMPOSER, 0x484B58, 0x3C5DE0, new Item.Properties()));

    /**
     * The same, for the guardian. Prismarine teal over the deep blue of the water it wants to be
     * fought in - though the egg is drawn as a dragon egg like every other boss egg here, so the
     * colours are only for whatever reads them.
     */
    /**
     * The same, for the husk. Desert sand over the dry brown of the thing itself - though the egg is
     * drawn as a dragon egg like every other boss egg here, so the colours are only for whatever
     * reads them.
     */
    /** The same, for the snow golem. Snow over the orange of the pumpkin on its head. */
    public static final DeferredItem<BossSpawnEggItem> COMPRESSED_SNOW_GOLEM_SPAWN_EGG =
            ITEMS.register("compressed_snow_golem_spawn_egg", () -> new BossSpawnEggItem(
                    ModEntities.COMPRESSED_SNOW_GOLEM, 0xEDF2F5, 0xE0900E, new Item.Properties()));

    public static final DeferredItem<BossSpawnEggItem> COMPRESSED_HUSK_SPAWN_EGG =
            ITEMS.register("compressed_husk_spawn_egg", () -> new BossSpawnEggItem(
                    ModEntities.COMPRESSED_HUSK, 0xD9C8A0, 0x7A6A50, new Item.Properties()));

    /** The Compressed Ghast's egg. Ghast white over the red of what comes out of it. */
    public static final DeferredItem<BossSpawnEggItem> COMPRESSED_GHAST_SPAWN_EGG =
            ITEMS.register("compressed_ghast_spawn_egg", () -> new BossSpawnEggItem(
                    ModEntities.COMPRESSED_GHAST, 0xE8E8E8, 0xD44A20, new Item.Properties()));

    /**
     * The Compressed Dragon's egg: the last but one in the mod. A dragon's black over the purple of
     * the eye it is called up out of.
     */
    public static final DeferredItem<BossSpawnEggItem> COMPRESSED_DRAGON_SPAWN_EGG =
            ITEMS.register("compressed_dragon_spawn_egg", () -> new BossSpawnEggItem(
                    ModEntities.COMPRESSED_DRAGON, 0x1C1C24, 0xA05FE0, new Item.Properties()));

    /**
     * The second phase's own egg, cut from the deepest stone there is - the only recipe in the mod
     * built out of tier 255, which is what the first phase pays out. So the fight can be started at
     * either end: kill the first and the second arrives out of it, or spend what the first dropped
     * and meet the second on its own.
     */
    public static final DeferredItem<BossSpawnEggItem> COMPRESSED_DRAGON_TIER_2_SPAWN_EGG =
            ITEMS.register("compressed_dragon_tier_2_spawn_egg", () -> new BossSpawnEggItem(
                    ModEntities.COMPRESSED_DRAGON_TIER_2, 0x0F0F14, 0xE04FD0, new Item.Properties()));

    /**
     * What the fight leaves behind, and the only thing it drops: one egg, which hatches the dragon
     * as something to fly.
     * <p>
     * A plain {@link DeferredSpawnEggItem} rather than a {@link BossSpawnEggItem}, and the
     * difference is the whole point of it - a boss egg leaves a dragon egg on the ground and a boss
     * arrives out of it five seconds later, while this one puts the pet down where it was clicked.
     * It is not a {@code PetSpawnEggItem} either: the pet has no owner to be given, because it has
     * no goals to use one.
     */
    public static final DeferredItem<DeferredSpawnEggItem> COMPRESSED_DRAGON_EGG =
            ITEMS.register("compressed_dragon_egg", () -> new DeferredSpawnEggItem(
                    ModEntities.COMPRESSED_DRAGON_PET, 0x1C1C24, 0xA05FE0, new Item.Properties()));

    /**
     * The Ghast Pet's egg, and the first spawn egg in this mod that is an ordinary spawn egg: it puts
     * the thing down where it was clicked rather than starting a boss summon, so it is drawn as the
     * vanilla template tinted by the two colours below rather than as a dragon egg. Ghast white over
     * the pale blue of something that is on your side.
     * <p>
     * It is a {@link PetSpawnEggItem} rather than a plain one because a pet with no owner hunts
     * players - see that class.
     */
    public static final DeferredItem<PetSpawnEggItem> GHAST_PET_SPAWN_EGG =
            ITEMS.register("ghast_pet_spawn_egg", () -> new PetSpawnEggItem(
                    ModEntities.GHAST_PET, 0xE8E8E8, 0x6BC7E8, new Item.Properties()));

    /** The Ghast Mount's egg. The same white over a saddle's brown. It has no owner to be given. */
    public static final DeferredItem<DeferredSpawnEggItem> GHAST_MOUNT_SPAWN_EGG =
            ITEMS.register("ghast_mount_spawn_egg", () -> new DeferredSpawnEggItem(
                    ModEntities.GHAST_MOUNT, 0xE8E8E8, 0x8A5A2B, new Item.Properties()));

    public static final DeferredItem<BossSpawnEggItem> COMPRESSED_GUARDIAN_SPAWN_EGG =
            ITEMS.register("compressed_guardian_spawn_egg", () -> new BossSpawnEggItem(
                    ModEntities.COMPRESSED_GUARDIAN, 0x5C8B84, 0xF17D31, new Item.Properties()));

    /** The second creeper's egg. The first one's green, darkened a tier. */
    public static final DeferredItem<BossSpawnEggItem> COMPRESSED_CREEPER_TIER_2_SPAWN_EGG =
            ITEMS.register("compressed_creeper_tier_2_spawn_egg", () -> new BossSpawnEggItem(
                    ModEntities.COMPRESSED_CREEPER_TIER_2, 0x1D5A2A, 0x62C34F, new Item.Properties()));

    /** The Compressed Chicken Boss's egg. Cobblestone grey over a beak's yellow. */
    public static final DeferredItem<BossSpawnEggItem> COMPRESSED_CHICKEN_BOSS_SPAWN_EGG =
            ITEMS.register("compressed_chicken_boss_spawn_egg", () -> new BossSpawnEggItem(
                    ModEntities.COMPRESSED_CHICKEN_BOSS, 0x8A8A8A, 0xFFB43D, new Item.Properties()));

    /** And for the Compressed Spirit. Soul-fire blue over the deep blue of a vex. */
    public static final DeferredItem<BossSpawnEggItem> COMPRESSED_SPIRIT_SPAWN_EGG =
            ITEMS.register("compressed_spirit_spawn_egg", () -> new BossSpawnEggItem(
                    ModEntities.COMPRESSED_SPIRIT, 0x1B2F4B, 0x6BE3E8, new Item.Properties()));

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

    /**
     * The eighty-eight keys of a grand piano, one bolt each, registered in one loop rather than as a
     * field each. Everything that tells two of them apart - the id, the name, the sample, the dye
     * its recipe is keyed on, the tier that recipe is built from - comes off {@link PianoNote}, so
     * there is nothing here to keep in step with them.
     */
    public static final List<DeferredItem<NoteBoltItem>> NOTE_BOLTS = registerNoteBolts();

    private static List<DeferredItem<NoteBoltItem>> registerNoteBolts() {
        List<DeferredItem<NoteBoltItem>> bolts = new ArrayList<>(PianoNote.KEYS);
        for (int key = 0; key < PianoNote.KEYS; key++) {
            int note = key;
            bolts.add(ITEMS.register(PianoNote.itemName(key),
                    () -> new NoteBoltItem(note, new Item.Properties())));
        }

        return List.copyOf(bolts);
    }

    /**
     * A whole piece of music in one bolt: the opening of Beethoven's Op. 27 No. 2, played on the
     * note bolts around whatever core it is set on.
     */
    public static final DeferredItem<SongBoltItem> MOONLIGHT_BOLT =
            ITEMS.register("moonlight_bolt", () -> new SongBoltItem(
                    ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "moonlight"),
                    new Item.Properties()));

    /**
     * A bow that shoots bolts. What the shot does is the bolt's business and not the launcher's -
     * see {@link BoltLauncherItem} - so one weapon covers the whole family, the eighty-eight keys
     * of the piano included.
     */
    public static final DeferredItem<BoltLauncherItem> BOLT_LAUNCHER =
            ITEMS.register("bolt_launcher", () -> new BoltLauncherItem(new Item.Properties().durability(384)));

    /**
     * A bow that throws TNT. What the shell does where it lands is the block's business and not the
     * launcher's - see {@link TntLauncherItem} - so one weapon fires every TNT in the {@code #ucc:tnt}
     * tag, this mod's two dozen and anyone else's alike.
     */
    public static final DeferredItem<TntLauncherItem> TNT_LAUNCHER =
            ITEMS.register("tnt_launcher", () -> new TntLauncherItem(new Item.Properties().durability(384)));

    /** A blank sheet of music, until a Composition Table writes one onto it. */
    public static final DeferredItem<CompositionBoltItem> COMPOSITION_BOLT =
            ITEMS.register("composition_bolt", () -> new CompositionBoltItem(new Item.Properties().stacksTo(1)));

    /** A beat of silence on a sheet, and a bolt that is seen and not heard off it. */
    public static final DeferredItem<RestBoltItem> REST_BOLT =
            ITEMS.register("rest_bolt", () -> new RestBoltItem(Composition.SHORT_REST, new Item.Properties()));

    /** Four of them, so a real gap does not cost four squares of the sheet. */
    public static final DeferredItem<RestBoltItem> LONG_REST_BOLT =
            ITEMS.register("long_rest_bolt", () -> new RestBoltItem(Composition.LONG_REST, new Item.Properties()));

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
     * The tier 4 apple, which is where the ramp stops being a ramp. See
     * {@link ModFoodProperties#TIER_4_COMPRESSED_COBBLESTONE_APPLE}.
     */
    public static final DeferredItem<Item> TIER_4_COMPRESSED_COBBLESTONE_APPLE =
            ITEMS.register("tier_4_compressed_cobblestone_apple", () -> new Item(
                    new Item.Properties().food(ModFoodProperties.TIER_4_COMPRESSED_COBBLESTONE_APPLE)));

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

    /**
     * The Compressed Healing Staff: the one staff with no cast at all, priced by half a minute of
     * cooldown instead. See {@link CompressedHealingStaffItem}. The durability is kept and never
     * spent, for the reason every other unbreakable thing here keeps one.
     */
    public static final DeferredItem<CompressedHealingStaffItem> COMPRESSED_HEALING_STAFF =
            ITEMS.register("compressed_healing_staff", () -> new CompressedHealingStaffItem(
                    new Item.Properties().durability(384).stacksTo(1)));

    /** Vanilla bow durability; the draw time and velocity are what set it apart. */
    public static final DeferredItem<CompressedCobblestoneBowItem> COMPRESSED_COBBLESTONE_BOW =
            ITEMS.register("compressed_cobblestone_bow", () -> new CompressedCobblestoneBowItem(
                    new Item.Properties().durability(384)));

    /** The Compressed Witch's, which nothing is built out of yet. */
    public static final DeferredItem<Item> TIER_10_COMPRESSED_HEART =
            ITEMS.register("tier_10_compressed_heart", () -> new Item(new Item.Properties()));

    /** The second golem's, which the fourth Material Compressor is built around. */
    public static final DeferredItem<Item> TIER_9_COMPRESSED_HEART =
            ITEMS.register("tier_9_compressed_heart", () -> new Item(new Item.Properties()));

    /** And the Compressed Spirit's, which nothing is built out of yet. */
    public static final DeferredItem<Item> TIER_8_COMPRESSED_HEART =
            ITEMS.register("tier_8_compressed_heart", () -> new Item(new Item.Properties()));

    /** The Compressed Chicken Boss's, which nothing is built out of yet. */
    public static final DeferredItem<Item> TIER_11_COMPRESSED_HEART =
            ITEMS.register("tier_11_compressed_heart", () -> new Item(new Item.Properties()));

    /** The Compressed Creeper Tier 2's, which nothing is built out of yet. */
    public static final DeferredItem<Item> TIER_12_COMPRESSED_HEART =
            ITEMS.register("tier_12_compressed_heart", () -> new Item(new Item.Properties()));

    /** The Compressed Composer's, which nothing is built out of yet. */
    public static final DeferredItem<Item> TIER_13_COMPRESSED_HEART =
            ITEMS.register("tier_13_compressed_heart", () -> new Item(new Item.Properties()));

    /** The Compressed Guardian's, which nothing is built out of yet. */
    public static final DeferredItem<Item> TIER_14_COMPRESSED_HEART =
            ITEMS.register("tier_14_compressed_heart", () -> new Item(new Item.Properties()));

    /** The Compressed Husk's, which nothing is built out of yet. */
    public static final DeferredItem<Item> TIER_15_COMPRESSED_HEART =
            ITEMS.register("tier_15_compressed_heart", () -> new Item(new Item.Properties()));

    /** The Compressed Snow Golem's, which nothing is built out of yet. */
    public static final DeferredItem<Item> TIER_16_COMPRESSED_HEART =
            ITEMS.register("tier_16_compressed_heart", () -> new Item(new Item.Properties()));

    /** The Compressed Ore Golem's, and the hilt of the Compressed Scythe. */
    public static final DeferredItem<Item> TIER_17_COMPRESSED_HEART =
            ITEMS.register("tier_17_compressed_heart", () -> new Item(new Item.Properties()));

    /** The Compressed Ghast's, which nothing is built out of yet. */
    public static final DeferredItem<Item> TIER_18_COMPRESSED_HEART =
            ITEMS.register("tier_18_compressed_heart", () -> new Item(new Item.Properties()));

    /**
     * The Compressed Scythe: an axe in every list an axe is in, cut from the stone the Compressed
     * Ore Golem drops and hilted on its heart. What separates it from an axe is what it leaves
     * behind - see {@link CompressedScytheItem}.
     * <p>
     * The tier's own attack damage bonus of 4 plus the item's 8 is 12, which the tooltip shows as 13
     * once the player's base damage is counted. Its durability comes from the tier and is never
     * spent.
     */
    public static final DeferredItem<CompressedScytheItem> COMPRESSED_SCYTHE =
            ITEMS.register("compressed_scythe", () -> new CompressedScytheItem(
                    ModToolTiers.HARDENED_COMPRESSED_COBBLESTONE, new Item.Properties()
                            .attributes(CompressedScytheItem.createAttributes(
                                    ModToolTiers.HARDENED_COMPRESSED_COBBLESTONE))));

    public static final DeferredItem<CompressedArrowItem> COMPRESSED_COBBLESTONE_ARROW =
            ITEMS.register("compressed_cobblestone_arrow",
                    () -> new CompressedArrowItem(CompressedArrowTier.COBBLESTONE, new Item.Properties()));

    /** Three times the compressed arrow, which is six times a vanilla one. */
    public static final DeferredItem<CompressedArrowItem> SUPER_COMPRESSED_ARROW =
            ITEMS.register("super_compressed_arrow",
                    () -> new CompressedArrowItem(CompressedArrowTier.SUPER, new Item.Properties()));

    /**
     * Five times the Super, and the heaviest thing in the game to leave a bowstring: on its own it
     * drops at the archer's feet, and it is only worth anything out of a bow carrying the Sniper
     * engraving. See {@link CompressedArrowTier#HYPER}.
     */
    public static final DeferredItem<CompressedArrowItem> HYPER_COMPRESSED_ARROW =
            ITEMS.register("hyper_compressed_arrow",
                    () -> new CompressedArrowItem(CompressedArrowTier.HYPER, new Item.Properties()));

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
     * The Ray of Laser: a guardian's beam in a player's hands. The durability is never spent, for
     * the reason every other unbreakable thing here keeps one - an item with the {@code unbreakable}
     * component cannot be enchanted at a table, and the laser takes enchantments like any weapon.
     */
    /**
     * The Compressed Shield: a shield that never breaks and stops two things no other shield can.
     * The durability is real and never spent, the way every other tool here keeps one.
     */
    /**
     * The Compressed Bone: one bone, one wolf, no rolling. See {@link CompressedBoneItem}.
     */
    public static final DeferredItem<CompressedBoneItem> COMPRESSED_BONE =
            ITEMS.register("compressed_bone", () -> new CompressedBoneItem(new Item.Properties()));

    /**
     * The Compressed Saddle: one saddle, one horse, kept. See {@link CompressedSaddleItem}.
     */
    public static final DeferredItem<CompressedSaddleItem> COMPRESSED_SADDLE =
            ITEMS.register("compressed_saddle", () -> new CompressedSaddleItem(new Item.Properties()));

    public static final DeferredItem<CompressedShieldItem> COMPRESSED_SHIELD =
            ITEMS.register("compressed_shield", () -> new CompressedShieldItem(
                    new Item.Properties().durability(336)));

    /**
     * The Compression Bomb: a thrown grenade that turns a target's damage over time effects into one
     * hit. See {@link CompressionBombItem} for the throw and {@code CompressionBombEntity} for what
     * it does when it lands.
     * <p>
     * Eight to a stack, which is the whole of how many are in hand: it is spent when it is thrown.
     */
    public static final DeferredItem<CompressionBombItem> COMPRESSION_BOMB =
            ITEMS.register("compression_bomb", () -> new CompressionBombItem(
                    new Item.Properties().stacksTo(8)));

    /**
     * The Compressed Spear: the 1.21.11 spear at this mod's depth. A jab with a long arm and a
     * minimum range, and a charge paid for in closing speed rather than in attack damage - see
     * {@link CompressedSpearItem}.
     * <p>
     * The tier's own bonus of 4 plus the item's 3 is 7, which the tooltip shows as 8 once the
     * player's base damage is counted. Its durability comes from the tier and is never spent, the
     * way every other tool here keeps one.
     */
    public static final DeferredItem<CompressedSpearItem> COMPRESSED_SPEAR =
            ITEMS.register("compressed_spear", () -> new CompressedSpearItem(
                    ModToolTiers.HARDENED_COMPRESSED_COBBLESTONE, new Item.Properties()
                            .attributes(CompressedSpearItem.createAttributes(
                                    ModToolTiers.HARDENED_COMPRESSED_COBBLESTONE))));

    /**
     * The Compressed Totem of Undying: a totem no death gets past, including the ones vanilla's own
     * totem refuses outright. See {@link CompressedTotemItem}.
     * <p>
     * One to a stack, exactly like vanilla's: a totem is something held rather than something
     * carried, and stacking them would make the slot it costs meaningless.
     */
    public static final DeferredItem<CompressedTotemItem> COMPRESSED_TOTEM_OF_UNDYING =
            ITEMS.register("compressed_totem_of_undying", () -> new CompressedTotemItem(
                    new Item.Properties().stacksTo(1)));

    public static final DeferredItem<RayOfLaserItem> RAY_OF_LASER =
            ITEMS.register("ray_of_laser", () -> new RayOfLaserItem(
                    new Item.Properties().durability(384).stacksTo(1)));

    /**
     * Every augment, as the item it is crafted and carried as. One per {@link Augment} constant and
     * registered in one loop the way the engravings are, so a new augment is a constant, a recipe
     * and a texture - the item, the model, the creative tab and the table all come free.
     */
    public static final Map<Augment, DeferredItem<AugmentItem>> AUGMENTS = registerAugments();

    private static Map<Augment, DeferredItem<AugmentItem>> registerAugments() {
        Map<Augment, DeferredItem<AugmentItem>> augments = new EnumMap<>(Augment.class);
        for (Augment augment : Augment.values()) {
            augments.put(augment, ITEMS.register(augment.itemName(),
                    () -> new AugmentItem(augment, new Item.Properties().stacksTo(16))));
        }

        return augments;
    }

    /**
     * The dev sword, which is not part of the game: a creative-only blade whose tooltip claims
     * 1 x 10^255 damage and which really deals the largest blow a float can carry. It has no recipe
     * and lives in its own creative tab - see {@link DevSwordItem} for the three ceilings that make
     * the claim a claim rather than a figure.
     */
    public static final DeferredItem<DevSwordItem> DEV_SWORD =
            ITEMS.register("dev_sword", () -> new DevSwordItem(
                    ModToolTiers.HARDENED_COMPRESSED_COBBLESTONE_TIER_2,
                    new Item.Properties().attributes(DevSwordItem.createAttributes())));

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
