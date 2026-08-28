package net.fahr3n.unnecessarilycompressedcobblestone.datagen;

import java.util.stream.Stream;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

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
                        .add(LootItem.lootTableItem(ModItems.TIER_1_COMPRESSED_HEART.get()))));
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
}
