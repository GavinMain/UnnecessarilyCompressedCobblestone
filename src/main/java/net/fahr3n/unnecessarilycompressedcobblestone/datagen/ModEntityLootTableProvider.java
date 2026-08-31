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

    @Override
    public void generate() {
        add(ModEntities.COMPRESSED_GOLEM.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModBlocks.byLevel(13))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F)))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries,
                                        UniformGenerator.between(0.0F, 1.0F))))
                        .add(LootItem.lootTableItem(ModItems.TIER_1_COMPRESSED_HEART.get()))));

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
