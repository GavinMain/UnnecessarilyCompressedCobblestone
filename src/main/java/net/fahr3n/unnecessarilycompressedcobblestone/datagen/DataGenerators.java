package net.fahr3n.unnecessarilycompressedcobblestone.datagen;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = UnnecessarilyCompressedCobblestone.MOD_ID)
public class DataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // Datapack registries first: everything downstream that needs to look up an entry the mod
        // itself defines (the recipe provider needs the Compression enchantment) has to be handed
        // the patched provider rather than the vanilla one.
        ModDatapackProvider datapackProvider = new ModDatapackProvider(packOutput, lookupProvider);
        generator.addProvider(event.includeServer(), datapackProvider);
        CompletableFuture<HolderLookup.Provider> registries = datapackProvider.getRegistryProvider();

        // Server-side data: loot tables, recipes, tags
        generator.addProvider(event.includeServer(), new LootTableProvider(packOutput, Collections.emptySet(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLootTableProvider::new, LootContextParamSets.BLOCK),
                        new LootTableProvider.SubProviderEntry(ModEntityLootTableProvider::new, LootContextParamSets.ENTITY)),
                lookupProvider));
        generator.addProvider(event.includeServer(), new ModRecipeProvider(packOutput, registries));

        BlockTagsProvider blockTagsProvider = new ModBlockTagProvider(packOutput, lookupProvider, existingFileHelper);
        generator.addProvider(event.includeServer(), blockTagsProvider);
        generator.addProvider(event.includeServer(),
                new ModItemTagProvider(packOutput, lookupProvider, blockTagsProvider.contentsGetter(), existingFileHelper));
        // `registries` rather than `lookupProvider`: this one now tags a damage type the mod itself
        // defines, which only exists in the patched provider.
        generator.addProvider(event.includeServer(), new ModDamageTypeTagProvider(packOutput, registries));
        // Mob effects are a code registry, so the plain lookup provider is enough here: nothing in
        // this one is defined by a datapack.
        generator.addProvider(event.includeServer(), new ModMobEffectTagProvider(packOutput, lookupProvider));

        // Client-side data: models and blockstates
        generator.addProvider(event.includeClient(), new ModItemModelProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(), new ModBlockStateProvider(packOutput, existingFileHelper));
    }
}
