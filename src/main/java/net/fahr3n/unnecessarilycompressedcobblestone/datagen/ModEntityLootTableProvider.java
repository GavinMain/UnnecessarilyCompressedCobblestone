package net.fahr3n.unnecessarilycompressedcobblestone.datagen;

import java.util.stream.Stream;

import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SmeltItemFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/** Death loot for this mod's entities, the same way {@link ModBlockLootTableProvider} handles blocks. */
public class ModEntityLootTableProvider extends EntityLootSubProvider {
    protected ModEntityLootTableProvider(HolderLookup.Provider registries) {
        super(FeatureFlags.REGISTRY.allFlags(), registries);
    }

    /** The stone a Compressed Silverfish is made of, and the tier its own TNT is crafted at. */
    private static final int SILVERFISH_LEVEL = 150;

    /** A tier above the stone the Compressed Chicken Boss's egg is called up at. */
    private static final int CHICKEN_BOSS_DROP_LEVEL = 157;

    /** And a tier above the stone the second creeper's egg is called up at. */
    private static final int CREEPER_TIER_2_DROP_LEVEL = 164;

    /** And a tier above the stone the Composer's egg is called up at. */
    private static final int COMPOSER_DROP_LEVEL = 172;

    /** A tier above the stone the Compressed Guardian's egg is called up at. */
    private static final int GUARDIAN_DROP_LEVEL = 179;

    /** A tier above the stone the husk's egg is called up at. */
    private static final int HUSK_DROP_LEVEL = 197;

    /** A tier above the stone the snow golem's egg is called up at, and the stone its dome is. */
    private static final int SNOW_GOLEM_DROP_LEVEL = 206;

    /** What the Compressed Ghast gives up: one tier past the stone its own egg is cut from. */
    private static final int GHAST_DROP_LEVEL = 236;

    /**
     * And what the Compressed Dragon gives up, which is the deepest stone in the mod - there is no
     * tier above {@code MAX_COMPRESSION_LEVEL} for a later fight to claim, and this is the last
     * fight.
     */
    private static final int DRAGON_DROP_LEVEL = ModBlocks.MAX_COMPRESSION_LEVEL;

    @Override
    public void generate() {
        // One block of the deepest stone in the mod's progression, and Looting adds up to one more.
        // Only a silverfish that was placed in the world drops it: one that hatched out of an
        // infestation is marked a brood member and drops nothing, or the effect would be a printer -
        // see CompressedSilverfishEntity#setBroodling.
        add(ModEntities.COMPRESSED_SILVERFISH.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModBlocks.byLevel(SILVERFISH_LEVEL))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries,
                                        UniformGenerator.between(0.0F, 1.0F))))));

        add(ModEntities.COMPRESSED_GOLEM.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModBlocks.byLevel(13))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F)))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries,
                                        UniformGenerator.between(0.0F, 1.0F))))
                        .add(LootItem.lootTableItem(ModItems.TIER_1_COMPRESSED_HEART.get()))));

        // A tier above the stone it was built out of, and the heart the fourth Material Compressor
        // is built around.
        add(ModEntities.COMPRESSED_GOLEM_TIER_2.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModBlocks.byLevel(ModBlocks.GOLEM_TIER_2_LEVEL + 1))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F)))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries,
                                        UniformGenerator.between(0.0F, 1.0F)))))
                // Always exactly one, the way every other boss gives up its heart.
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.TIER_9_COMPRESSED_HEART.get()))));

        // A tier above the stone it was built out of, which is the stone the Compressed Scythe is
        // cut from, and the heart that hilts it. Nothing else in the mod drops either.
        add(ModEntities.COMPRESSED_ORE_GOLEM.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModBlocks.byLevel(ModBlocks.ORE_GOLEM_LEVEL + 1))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F)))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries,
                                        UniformGenerator.between(0.0F, 1.0F)))))
                // Always exactly one, the way every other boss gives up its heart.
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.TIER_17_COMPRESSED_HEART.get()))));

        // A tier above the stone its egg is cut from, and the heart. Nothing else drops either.
        add(ModEntities.COMPRESSED_GHAST.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModBlocks.byLevel(GHAST_DROP_LEVEL))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F)))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries,
                                        UniformGenerator.between(0.0F, 1.0F)))))
                // Always exactly one, the way every other boss gives up its heart.
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.TIER_18_COMPRESSED_HEART.get()))));

        // The mini ghasts a Ghasted player carries get no table at all, and must not: they are
        // registered as MobCategory.MISC and vanilla's own provider refuses a loot table for
        // anything in that category. They drop nothing anyway - MiniGhastEntity refuses loot
        // outright, the way every conjured thing in this mod does.

        // Back the gunpowder that went into calling it up, plus the stone it was packed out of.
        // Both pools take Looting, which adds up to one extra of each per level.
        add(ModEntities.COMPRESSED_CREEPER.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(Items.GUNPOWDER)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 8.0F)))))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModBlocks.byLevel(27))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F)))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries,
                                        UniformGenerator.between(0.0F, 1.0F)))))
                // The heart itself is always exactly one, the way the golem gives up its tier 1.
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.TIER_2_COMPRESSED_HEART.get()))));

        // The stone that called it up, and the heart the Compressed Lightning Staff is built around.
        // Not the trident it was drawn holding: that is scenery, not loot.
        add(ModEntities.COMPRESSED_CONJURER.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModBlocks.byLevel(44))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F)))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries,
                                        UniformGenerator.between(0.0F, 1.0F)))))
                // Always exactly one, the way the golem and the creeper give up theirs.
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.TIER_3_COMPRESSED_HEART.get()))));

        // Its own ammunition, and the stone it was packed out of.
        add(ModEntities.COMPRESSED_SKELETON.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.SUPER_COMPRESSED_ARROW.get())
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(8.0F, 16.0F)))))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModBlocks.byLevel(67))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F)))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries,
                                        UniformGenerator.between(0.0F, 1.0F)))))
                // Always exactly one, the way every other boss gives up its heart.
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.TIER_4_COMPRESSED_HEART.get()))));

        // The two deeper rungs: their own stone, and the heart each is built around.
        add(ModEntities.COMPRESSED_SKELETON_TIER_2.get(), arrowBossDrops(76, ModItems.TIER_5_COMPRESSED_HEART.get()));
        add(ModEntities.COMPRESSED_SKELETON_TIER_3.get(), arrowBossDrops(84, ModItems.TIER_6_COMPRESSED_HEART.get()));

        // The Summoner: the stone it is made of, and the heart the Summoning Staff is built around.
        add(ModEntities.COMPRESSED_SUMMONER.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModBlocks.byLevel(100))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F)))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries,
                                        UniformGenerator.between(0.0F, 1.0F)))))
                // Always exactly one, the way every other boss gives up its heart.
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.TIER_7_COMPRESSED_HEART.get()))));

        // A tier above the stone its egg is called up at, and the heart every boss here gives up.
        add(ModEntities.COMPRESSED_SPIRIT.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModBlocks.byLevel(108))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F)))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries,
                                        UniformGenerator.between(0.0F, 1.0F)))))
                // Always exactly one, the way every other boss gives up its heart.
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.TIER_8_COMPRESSED_HEART.get()))));

        // A tier above the stone its egg is called up at, and the heart every boss gives up.
        add(ModEntities.COMPRESSED_WITCH.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModBlocks.byLevel(132))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F)))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries,
                                        UniformGenerator.between(0.0F, 1.0F)))))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.TIER_10_COMPRESSED_HEART.get()))));

        // A tier above the stone its egg is called up at, and the heart every boss gives up.
        add(ModEntities.COMPRESSED_CREEPER_TIER_2.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModBlocks.byLevel(CREEPER_TIER_2_DROP_LEVEL))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F)))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries,
                                        UniformGenerator.between(0.0F, 1.0F)))))
                // Always exactly one, the way every other boss gives up its heart.
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.TIER_12_COMPRESSED_HEART.get()))));

        // A tier above the stone its egg is called up at, and the heart every boss gives up.
        add(ModEntities.COMPRESSED_COMPOSER.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModBlocks.byLevel(COMPOSER_DROP_LEVEL))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F)))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries,
                                        UniformGenerator.between(0.0F, 1.0F)))))
                // Always exactly one, the way every other boss gives up its heart.
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.TIER_13_COMPRESSED_HEART.get()))));

        // A tier above the stone its egg is called up at, and the heart every boss gives up.
        add(ModEntities.COMPRESSED_GUARDIAN.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModBlocks.byLevel(GUARDIAN_DROP_LEVEL))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F)))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries,
                                        UniformGenerator.between(0.0F, 1.0F)))))
                // Always exactly one, the way every other boss gives up its heart.
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.TIER_14_COMPRESSED_HEART.get()))));

        // A tier above the stone its egg is called up at, and the heart every boss gives up.
        add(ModEntities.COMPRESSED_CHICKEN_BOSS.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModBlocks.byLevel(CHICKEN_BOSS_DROP_LEVEL))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F)))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries,
                                        UniformGenerator.between(0.0F, 1.0F)))))
                // Always exactly one, the way every other boss gives up its heart.
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.TIER_11_COMPRESSED_HEART.get()))));

        // A tier above the stone its egg is called up at, and the heart every boss gives up.
        add(ModEntities.COMPRESSED_HUSK.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModBlocks.byLevel(HUSK_DROP_LEVEL))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F)))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries,
                                        UniformGenerator.between(0.0F, 1.0F)))))
                // Always exactly one, the way every other boss gives up its heart.
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.TIER_15_COMPRESSED_HEART.get()))));

        // A tier above the stone its egg is called up at, and the heart every boss gives up.
        add(ModEntities.COMPRESSED_SNOW_GOLEM.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModBlocks.byLevel(SNOW_GOLEM_DROP_LEVEL))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F)))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries,
                                        UniformGenerator.between(0.0F, 1.0F)))))
                // Always exactly one, the way every other boss gives up its heart.
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.TIER_16_COMPRESSED_HEART.get()))));

        // The deepest stone there is, and no heart: this phase does not so much die as hand the
        // fight over, and what the finale is worth belongs to the mob that arrives out of it.
        add(ModEntities.COMPRESSED_DRAGON.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModBlocks.byLevel(DRAGON_DROP_LEVEL))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 8.0F)))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries,
                                        UniformGenerator.between(0.0F, 1.0F))))));

        // One egg and nothing else, which is the whole of what the last fight in the mod pays out:
        // no stone, no heart, and not a stack of anything. What it leaves behind is the dragon.
        add(ModEntities.COMPRESSED_DRAGON_TIER_2.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.COMPRESSED_DRAGON_EGG.get()))));

        // Hatched rather than fought, so it leaves nothing behind - and neither does the storm.
        add(ModEntities.COMPRESSED_DRAGON_PET.get(), LootTable.lootTable());

        // A tamed pet, so it drops nothing at all - the same as the wolf it used to be.
        add(ModEntities.COMPRESSED_WOLF.get(), LootTable.lootTable());
        add(ModEntities.COMPRESSED_HORSE.get(), LootTable.lootTable());
        add(ModEntities.COMPRESSED_BEE.get(), LootTable.lootTable());

        // Both ghasts a player keeps are hatched rather than fought, so neither leaves anything
        // behind - the same rule every summoned thing in the mod follows.
        add(ModEntities.GHAST_PET.get(), LootTable.lootTable());
        add(ModEntities.GHAST_MOUNT.get(), LootTable.lootTable());

        // Its phantoms drop nothing: they arrive ten at a time and are not the fight.
        add(ModEntities.COMPRESSED_PHANTOM.get(), LootTable.lootTable());

        // The lesser conjurer spawns on its own, so its drop has to be worth nothing much: a block
        // of the shallowest compressed stone, and not always that.
        add(ModEntities.LIGHTNING_CONJURER.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModBlocks.byLevel(1))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 1.0F)))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries,
                                        UniformGenerator.between(0.0F, 1.0F))))));

        // A chicken's own drops: feathers, and meat that comes out cooked if it burned to death.
        add(ModEntities.COMPRESSED_COBBLESTONE_CHICKEN.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(Items.FEATHER)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 2.0F)))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries,
                                        UniformGenerator.between(0.0F, 1.0F)))))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(Items.CHICKEN)
                                .apply(SmeltItemFunction.smelted().when(this.shouldSmeltLoot()))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries,
                                        UniformGenerator.between(0.0F, 1.0F))))));
    }

    /** What a rung of the arrow boss leaves behind: its ammunition, its stone and its heart. */
    private LootTable.Builder arrowBossDrops(int blockLevel, Item heart) {
        return LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.SUPER_COMPRESSED_ARROW.get())
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(8.0F, 16.0F)))))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModBlocks.byLevel(blockLevel))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F)))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries,
                                        UniformGenerator.between(0.0F, 1.0F)))))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(heart)));
    }

    /**
     * Only this mod's entities. The default is every entity type in the game, which makes datagen
     * demand a table for each of them.
     */
    @Override
    protected Stream<EntityType<?>> getKnownEntityTypes() {
        return ModEntities.ENTITY_TYPES.getEntries().stream().map(holder -> (EntityType<?>) holder.value());
    }

    /**
     * The golem is {@link MobCategory#MISC}, like the iron golem it is built from, and vanilla only
     * lets a MISC entity have a loot table if it is on a hardcoded list of its own - which the iron
     * golem is on and this cannot join. The rule is a datagen check only; at runtime a MISC entity
     * rolls its loot table on death like anything else, so it is widened here for this mod's own
     * entities and left alone for everything else.
     */
    @Override
    protected boolean canHaveLootTable(EntityType<?> entityType) {
        return entityType == ModEntities.COMPRESSED_GOLEM.get() || super.canHaveLootTable(entityType);
    }

    /** The primed TNT is MISC and drops nothing, which the default already handles. */
}
