package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.Optional;

import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.util.DeferredFill;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.Tags;

/**
 * What a Compressed TNT does when its fuse runs out. Every variant is one constant here, so the
 * block, the primed entity and the save format all stay the same however many are added - the block
 * hands its effect to the entity when it is lit, and the entity writes the constant's name into its
 * NBT.
 */
public enum CompressedTntEffect {
    /** Five times vanilla TNT's blast. */
    BLAST_5X {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            blast(level, tnt, 5 * CompressedPrimedTntEntity.VANILLA_POWER);
        }
    },

    /**
     * Twenty times vanilla TNT's blast, opened from the middle outwards over about a second and a
     * half. At this size the crater is hundreds of thousands of blocks, and removing them all on one
     * tick is what a twenty-times blast would otherwise cost; {@link DeferredFill} spreads it.
     */
    BLAST_20X {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            DeferredFill.queueStagedBlast(level, tnt, tnt.position(),
                    20 * CompressedPrimedTntEntity.VANILLA_POWER);
        }
    },

    /**
     * Breaks nothing at all. It throws a hundred-odd flowers and dyes into the air instead, which
     * rain back down around wherever it went off.
     */
    FLOWERS {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            RandomSource random = level.random;
            int count = FLOWER_COUNT - 10 + random.nextInt(21);

            for (int i = 0; i < count; i++) {
                ItemEntity item = new ItemEntity(level, tnt.getX(), tnt.getY() + 0.5, tnt.getZ(), randomBloom(level));
                // Thrown up and out, so they arc rather than piling up under the blast.
                item.setDeltaMovement(random.nextGaussian() * 0.35, 0.35 + random.nextDouble() * 0.45,
                        random.nextGaussian() * 0.35);
                level.addFreshEntity(item);
            }

            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, tnt.getX(), tnt.getY() + 0.5, tnt.getZ(),
                    120, 1.5, 1.0, 1.5, 0.2);
            level.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST,
                    SoundSource.BLOCKS, 2.0F, 1.4F);
        }
    },

    /** Turns the weather to rain and drops a shoal of guardians on dry land to flop about in it. */
    GUARDIANS {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            level.setWeatherParameters(0, RAIN_TICKS, true, false);

            BlockPos origin = tnt.blockPosition();
            spawnScattered(level, EntityType.GUARDIAN, origin, GUARDIAN_COUNT, GUARDIAN_SPREAD);
            spawnScattered(level, EntityType.ELDER_GUARDIAN, origin, ELDER_GUARDIAN_COUNT, GUARDIAN_SPREAD);

            level.playSound(null, origin, SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.BLOCKS, 2.0F, 1.0F);
        }
    },

    /**
     * Fills the chunk it went off in with level 29 stone, from the bottom of the world to the top,
     * air included. Nothing in that chunk survives.
     * <p>
     * The chunk is handed to {@link DeferredFill} rather than written here: ninety-eight thousand
     * blocks in one tick reads as a crash, so it climbs a few layers a tick over the next few
     * seconds instead.
     */
    CHUNK_FILL {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            DeferredFill.queueChunk(level, new ChunkPos(tnt.blockPosition()),
                    ModBlocks.byLevel(CHUNK_FILL_LEVEL).get().defaultBlockState());

            level.playSound(null, tnt.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 4.0F, 0.5F);
        }
    },

    /**
     * Turns a wide, shallow disc of the world to glass, nearly solid under the blast and fading out
     * with distance. Air is turned to glass along with everything else.
     * <p>
     * The shape of the field and the numbers behind it live in {@link DeferredFill}, which lays it
     * down one horizontal layer per tick: it is some fourteen thousand blocks, and writing them all
     * on the tick the fuse ends would stall the server.
     */
    GLASS_SCATTER {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            BlockPos origin = tnt.blockPosition();
            DeferredFill.queueGlass(level, origin);
            level.playSound(null, origin, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 3.0F, 0.6F);
        }
    },

    /**
     * A flock of Compressed Cobblestone Chickens, dropped in a knot high overhead and then blown
     * apart by a wind charge burst through the middle of them.
     * <p>
     * The knot is spread over {@link #CHICKEN_CLUSTER} blocks rather than stacked on one spot: the
     * {@code maxEntityCramming} rule starts suffocating entities once more than twenty-four of them
     * are inside one another, and a tight ball of forty would kill most of itself on the way down.
     * The wind charge scatters them further still, which is the other half of the same problem.
     */
    CHICKENS {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            RandomSource random = level.random;
            double x = tnt.getX();
            double y = tnt.getY() + CHICKEN_DROP_HEIGHT;
            double z = tnt.getZ();

            for (int i = 0; i < CHICKEN_COUNT; i++) {
                CompressedCobblestoneChickenEntity chicken =
                        ModEntities.COMPRESSED_COBBLESTONE_CHICKEN.get().create(level);
                if (chicken == null) {
                    continue;
                }

                chicken.moveTo(x + (random.nextDouble() - 0.5) * CHICKEN_CLUSTER,
                        y + random.nextDouble() * CHICKEN_CLUSTER,
                        z + (random.nextDouble() - 0.5) * CHICKEN_CLUSTER,
                        random.nextFloat() * 360.0F, 0.0F);
                level.addFreshEntity(chicken);
            }

            // The burst a wind charge makes, let off in the middle of the flock directly rather than
            // by throwing one and hoping. A thrown charge only goes off when it hits something, and
            // among entities it does not reliably collide with, so it sailed through and burst on
            // the ground long after the flock had scattered on its own.
            level.explode(null, null, WIND_BURST, x, y + CHICKEN_CLUSTER / 2.0, z, (float) CHICKEN_CLUSTER,
                    false, Level.ExplosionInteraction.NONE,
                    ParticleTypes.GUST_EMITTER_SMALL, ParticleTypes.GUST_EMITTER_LARGE,
                    SoundEvents.WIND_CHARGE_BURST);
            level.playSound(null, tnt.blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS, 3.0F, 0.8F);
        }
    },

    /**
     * Empties a barracks over the landscape: iron golems and Compressed Golems, five to one. Each
     * one is put down on the surface at its column rather than wherever the maths landed, so none
     * of them end up buried.
     */
    GOLEMS {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            BlockPos origin = tnt.blockPosition();
            spawnOnSurface(level, EntityType.IRON_GOLEM, origin, IRON_GOLEM_COUNT, GOLEM_SPREAD);
            spawnOnSurface(level, ModEntities.COMPRESSED_GOLEM.get(), origin, COMPRESSED_GOLEM_COUNT, GOLEM_SPREAD);

            level.playSound(null, origin, SoundEvents.IRON_GOLEM_REPAIR, SoundSource.BLOCKS, 3.0F, 0.7F);
        }
    };

    /** Roughly how many flowers and dyes go up; the actual throw varies by ten either way. */
    private static final int FLOWER_COUNT = 100;

    /** Ten minutes of rain. */
    private static final int RAIN_TICKS = 12000;

    private static final int GUARDIAN_COUNT = 24;
    private static final int ELDER_GUARDIAN_COUNT = 5;
    private static final int GUARDIAN_SPREAD = 8;

    /** What {@link #CHUNK_FILL} packs the chunk with. */
    private static final int CHUNK_FILL_LEVEL = 29;

    /**
     * A wind charge's own blast: it breaks nothing and hurts nothing, and only shoves. The 1.22
     * multiplier is the one vanilla gives a wind charge.
     */
    private static final ExplosionDamageCalculator WIND_BURST =
            new SimpleExplosionDamageCalculator(false, false, Optional.of(1.22F), Optional.empty());

    private static final int CHICKEN_COUNT = 40;

    /** How far overhead the flock appears. */
    private static final double CHICKEN_DROP_HEIGHT = 25.0;

    /** How wide the knot is spread, which is what keeps it under the entity cramming limit. */
    private static final double CHICKEN_CLUSTER = 5.0;

    private static final int IRON_GOLEM_COUNT = 100;
    private static final int COMPRESSED_GOLEM_COUNT = 20;
    private static final int GOLEM_SPREAD = 24;

    /** Runs this variant's detonation. Only ever called on the server. */
    public abstract void detonate(ServerLevel level, CompressedPrimedTntEntity tnt);

    /** The variant saved under {@code name}, or {@link #BLAST_5X} if the name means nothing. */
    public static CompressedTntEffect byName(String name) {
        for (CompressedTntEffect effect : values()) {
            if (effect.name().equals(name)) {
                return effect;
            }
        }

        return BLAST_5X;
    }

    /** The plain blast, with this entity's power in place of vanilla's hardcoded 4. */
    private static void blast(ServerLevel level, CompressedPrimedTntEntity tnt, float power) {
        level.explode(tnt, Explosion.getDefaultDamageSource(level, tnt), null,
                tnt.getX(), tnt.getY(0.0625), tnt.getZ(), power, false, Level.ExplosionInteraction.TNT);
    }

    /** A flower or a dye, drawn from the tags so other mods' additions come along too. */
    private static ItemStack randomBloom(ServerLevel level) {
        TagKey<Item> tag = level.random.nextBoolean() ? ItemTags.FLOWERS : Tags.Items.DYES;
        Optional<Holder<Item>> picked = BuiltInRegistries.ITEM.getTag(tag)
                .flatMap((HolderSet.Named<Item> set) -> set.getRandomElement(level.random));

        return picked.map(holder -> new ItemStack(holder.value())).orElseGet(() -> new ItemStack(Items.POPPY));
    }

    /** Drops {@code count} of something in a loose cluster, wherever they land. */
    private static void spawnScattered(ServerLevel level, EntityType<?> type, BlockPos origin, int count, int spread) {
        for (int i = 0; i < count; i++) {
            BlockPos pos = origin.offset(level.random.nextInt(spread * 2 + 1) - spread, 0,
                    level.random.nextInt(spread * 2 + 1) - spread);
            if (level.isLoaded(pos)) {
                type.spawn(level, pos, MobSpawnType.TRIGGERED);
            }
        }
    }

    /**
     * Puts {@code count} of something down on the surface of its column, so nothing is spawned
     * inside the ground. A column whose surface has no room to stand is skipped rather than forced.
     */
    private static void spawnOnSurface(ServerLevel level, EntityType<?> type, BlockPos origin, int count, int spread) {
        for (int i = 0; i < count; i++) {
            BlockPos column = origin.offset(level.random.nextInt(spread * 2 + 1) - spread, 0,
                    level.random.nextInt(spread * 2 + 1) - spread);
            if (!level.isLoaded(column)) {
                continue;
            }

            // The first free block above whatever is solid at that column.
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            if (level.noCollision(type.getSpawnAABB(surface.getX() + 0.5, surface.getY(), surface.getZ() + 0.5))) {
                type.spawn(level, surface, MobSpawnType.TRIGGERED);
            }
        }
    }
}
