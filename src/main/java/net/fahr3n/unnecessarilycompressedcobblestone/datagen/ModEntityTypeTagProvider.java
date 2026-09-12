package net.fahr3n.unnecessarilycompressedcobblestone.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

/**
 * The mod's entity type tags: which of its mobs are bosses and which are pets, and the other mods'
 * tags those two are handed to.
 */
public class ModEntityTypeTagProvider extends EntityTypeTagsProvider {
    /**
     * ProjectE's two repel blacklists. The Interdiction Torch and the Swiftwolf's Rending Gale push
     * away every {@code Mob} near them, which against a boss is not a tool but the end of the fight -
     * a torch in the arena and the boss can never reach anybody - and against a pet shoves it out of
     * the base it lives in. Written as tag files under ProjectE's namespace, so they cost nothing
     * when ProjectE is absent: nothing reads them.
     */
    private static final TagKey<EntityType<?>> PROJECTE_INTERDICTION_BLACKLIST = projectE("blacklist/interdiction");
    private static final TagKey<EntityType<?>> PROJECTE_SWRG_BLACKLIST = projectE("blacklist/swrg");

    public ModEntityTypeTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                    @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, UnnecessarilyCompressedCobblestone.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Every mob whose loot table gives up a heart, plus both phases of the dragon. The phantoms,
        // vexes and mini ghasts a boss conjures are not the fight and are left out.
        List<Supplier<? extends EntityType<?>>> bosses = List.of(
                ModEntities.COMPRESSED_GOLEM,
                ModEntities.COMPRESSED_GOLEM_TIER_2,
                ModEntities.COMPRESSED_ORE_GOLEM,
                ModEntities.COMPRESSED_CREEPER,
                ModEntities.COMPRESSED_CREEPER_TIER_2,
                ModEntities.COMPRESSED_CONJURER,
                ModEntities.COMPRESSED_SKELETON,
                ModEntities.COMPRESSED_SKELETON_TIER_2,
                ModEntities.COMPRESSED_SKELETON_TIER_3,
                ModEntities.COMPRESSED_SUMMONER,
                ModEntities.COMPRESSED_SPIRIT,
                ModEntities.COMPRESSED_WITCH,
                ModEntities.COMPRESSED_CHICKEN_BOSS,
                ModEntities.COMPRESSED_COMPOSER,
                ModEntities.COMPRESSED_GUARDIAN,
                ModEntities.COMPRESSED_HUSK,
                ModEntities.COMPRESSED_SNOW_GOLEM,
                ModEntities.COMPRESSED_GHAST,
                ModEntities.COMPRESSED_DRAGON,
                ModEntities.COMPRESSED_DRAGON_TIER_2);
        var bossTag = tag(ModTags.EntityTypes.BOSSES);
        bosses.forEach(boss -> bossTag.add(boss.get()));

        tag(ModTags.EntityTypes.PETS).add(
                ModEntities.COMPRESSED_WOLF.get(),
                ModEntities.COMPRESSED_HORSE.get(),
                ModEntities.COMPRESSED_BEE.get(),
                ModEntities.COMPRESSED_DRAGON_PET.get(),
                ModEntities.GHAST_PET.get(),
                ModEntities.GHAST_MOUNT.get());

        tag(Tags.EntityTypes.BOSSES).addTag(ModTags.EntityTypes.BOSSES);

        for (TagKey<EntityType<?>> blacklist : List.of(PROJECTE_INTERDICTION_BLACKLIST, PROJECTE_SWRG_BLACKLIST)) {
            tag(blacklist).addTag(ModTags.EntityTypes.BOSSES).addTag(ModTags.EntityTypes.PETS);
        }
    }

    private static TagKey<EntityType<?>> projectE(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("projecte", path));
    }
}
