package net.fahr3n.unnecessarilycompressedcobblestone.datagen;

import java.util.Set;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.TeleportationGateBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.registries.DeferredBlock;

public class ModBlockLootTableProvider extends BlockLootSubProvider {
    protected ModBlockLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        // Every block returned by getKnownBlocks() must get a table here, or datagen fails.
        for (DeferredBlock<?> block : ModBlocks.COMPRESSED_COBBLESTONE_LEVELS) {
            dropSelf(block.get());
        }

        dropSelf(ModBlocks.CARVED_COBBLESTONE_TIER_1.get());
        dropSelf(ModBlocks.CARVED_COBBLESTONE_TIER_2.get());
        dropSelf(ModBlocks.CARVED_COBBLESTONE_TIER_3.get());
        dropSelf(ModBlocks.COMPRESSION_INSCRIBER.get());
        dropSelf(ModBlocks.ENGRAVING_TABLE.get());
        dropSelf(ModBlocks.COMPOSITION_TABLE.get());
        dropSelf(ModBlocks.LASER_AUGMENTATION_TABLE.get());

        // One gate is two blocks, so a plain dropSelf would hand back two items - the same reason
        // vanilla's doors and tall flowers condition their table on the lower half.
        add(ModBlocks.TELEPORTATION_GATE.get(), createSinglePropConditionTable(
                ModBlocks.TELEPORTATION_GATE.get(), TeleportationGateBlock.HALF, DoubleBlockHalf.LOWER));
        dropSelf(ModBlocks.LIGHTNING_CORE.get());
        dropSelf(ModBlocks.MATERIAL_COMPRESSOR_TIER_1.get());
        dropSelf(ModBlocks.MATERIAL_COMPRESSOR_TIER_2.get());
        dropSelf(ModBlocks.MATERIAL_COMPRESSOR_TIER_3.get());
        dropSelf(ModBlocks.MATERIAL_COMPRESSOR_TIER_4.get());
        dropSelf(ModBlocks.MATERIAL_COMPRESSOR_TIER_5.get());
        ModBlocks.TNTS.forEach(tnt -> dropSelf(tnt.get()));

        dropSelf(ModBlocks.COMPRESSED_COBBLESTONE_SAPLING.get());

        // Unlike vanilla's cobweb, which needs shears or a sword, this one comes back to anything -
        // see ModBlocks.webProperties for why that is a property rather than a loot condition.
        dropSelf(ModBlocks.REGEN_WEB.get());
        dropSelf(ModBlocks.DAMAGE_WEB.get());

        // Vanilla's ice needs Silk Touch or it melts away; this drops itself to anything, since the
        // Debris TNT lays a field of it and picking that field back up is the point of it.
        dropSelf(ModBlocks.SLIPPERY_ICE.get());
        dropSelf(ModBlocks.DEV_COMPRESSED_COBBLESTONE.get());
        add(ModBlocks.COMPRESSED_COBBLESTONE_LEAVES.get(),
                createCompressedLeavesDrops(ModBlocks.COMPRESSED_COBBLESTONE_LEAVES.get(),
                        ModBlocks.COMPRESSED_COBBLESTONE_SAPLING.get()));
    }

    /**
     * Oak's leaf drops, with this mod's own two items in place of oak's: the sapling at the same
     * one in twenty vanilla gives, and a tier 2 apple at the same one in two hundred vanilla gives
     * an ordinary one. Both climb with Fortune on the same tables vanilla uses. Shears and Silk
     * Touch take the leaves themselves instead, and there are no sticks: this tree is made of stone.
     */
    protected LootTable.Builder createCompressedLeavesDrops(Block leaves, Block sapling) {
        HolderLookup.RegistryLookup<Enchantment> enchantments = this.registries.lookupOrThrow(Registries.ENCHANTMENT);

        return this.createSilkTouchOrShearsDispatchTable(leaves,
                        this.applyExplosionCondition(leaves, LootItem.lootTableItem(sapling))
                                .when(BonusLevelTableCondition.bonusLevelFlatChance(
                                        enchantments.getOrThrow(Enchantments.FORTUNE), NORMAL_LEAVES_SAPLING_CHANCES)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        // Vanilla's own "broken by hand" condition; its copy of this is private.
                        .when(HAS_SHEARS.or(this.hasSilkTouch()).invert())
                        .add(this.applyExplosionCondition(leaves,
                                        LootItem.lootTableItem(ModItems.TIER_2_COMPRESSED_COBBLESTONE_APPLE.get()))
                                .when(BonusLevelTableCondition.bonusLevelFlatChance(
                                        enchantments.getOrThrow(Enchantments.FORTUNE),
                                        0.005F, 0.0055555557F, 0.00625F, 0.008333334F, 0.025F))));
    }

    /** Ore-style drop with a random count, respecting Silk Touch and Fortune. */
    protected LootTable.Builder createMultipleOreDrops(Block block, Item item, float minDrops, float maxDrops) {
        HolderLookup.RegistryLookup<Enchantment> registryLookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createSilkTouchDispatchTable(block,
                this.applyExplosionDecay(block, LootItem.lootTableItem(item)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(minDrops, maxDrops)))
                        .apply(ApplyBonusCount.addOreBonusCount(registryLookup.getOrThrow(Enchantments.FORTUNE)))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
