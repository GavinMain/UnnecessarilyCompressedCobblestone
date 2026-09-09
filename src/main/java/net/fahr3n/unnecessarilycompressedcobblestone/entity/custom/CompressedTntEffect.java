package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CompressedTntBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedSilverfishEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.BoltItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.RayOfLaserItem;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.ModMobEffects;
import net.fahr3n.unnecessarilycompressedcobblestone.util.ArrowEffects;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Augment;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Augments;
import net.fahr3n.unnecessarilycompressedcobblestone.util.DeferredFill;
import net.fahr3n.unnecessarilycompressedcobblestone.util.DeferredStrikes;
import net.fahr3n.unnecessarilycompressedcobblestone.util.LightningSong;
import net.fahr3n.unnecessarilycompressedcobblestone.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.portal.PortalShape;

import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.OceanMonumentPieces;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
     * Twenty times vanilla TNT's blast, opened from the middle outwards over a few seconds. It is
     * not one explosion: a blast this size is mostly holes, because vanilla casts the same number of
     * rays however big it is, so {@link DeferredFill} builds the crater out of a swarm of smaller
     * blasts instead - which also spreads the block removal over ticks rather than stalling one.
     */
    BLAST_20X {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            DeferredFill.queueClusterBlast(level, tnt, tnt.position(),
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
    },

    /**
     * Breaks nothing, like {@link #FLOWERS}. It throws a hundred-odd apples up instead - plain,
     * golden, enchanted golden, both compressed ones and whatever any other mod has put in
     * {@link ModTags.Items#APPLES} - which rain back down around wherever it went off.
     */
    APPLES {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            RandomSource random = level.random;
            int count = APPLE_COUNT - 10 + random.nextInt(21);

            for (int i = 0; i < count; i++) {
                ItemEntity item = new ItemEntity(level, tnt.getX(), tnt.getY() + 0.5, tnt.getZ(),
                        randomFrom(level, ModTags.Items.APPLES, Items.APPLE));
                // Thrown up and out, so they arc rather than piling up under the blast.
                item.setDeltaMovement(random.nextGaussian() * 0.35, 0.35 + random.nextDouble() * 0.45,
                        random.nextGaussian() * 0.35);
                level.addFreshEntity(item);
            }

            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, tnt.getX(), tnt.getY() + 0.5, tnt.getZ(),
                    120, 1.5, 1.0, 1.5, 0.2);
            level.playSound(null, tnt.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.BLOCKS, 3.0F, 0.7F);
        }
    },

    /**
     * Goes off as some other TNT entirely, drawn at random from {@link ModTags.Blocks#TNT} - which
     * is a tag rather than a list in code precisely so that a TNT this mod has not heard of can be
     * added to the draw from a datapack.
     * <p>
     * It does not run the drawn variant's effect directly, because for anything outside this mod
     * there is nothing to run: what another mod's TNT does lives in its own primed entity, which
     * only that block knows how to make. So the block is put down for an instant, lit the way any
     * fire or redstone would light it, and taken away again, and whatever it primed has its fuse cut
     * to the next tick. That is the one path that works for this mod's TNT and another mod's alike.
     */
    RANDOM {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            BlockPos pos = tnt.blockPosition();
            if (!level.isLoaded(pos)) {
                return;
            }

            if (!lightRandom(level, pos, tnt.getOwner())) {
                // Nothing in the tag but itself, or no tag at all.
                BLAST_5X.detonate(level, tnt);
            }
        }
    },

    /**
     * Breaks nothing. It throws up an armful of enchanted books, every one of them something that
     * will go on a trident: Loyalty, Impaling, Riptide, Channeling and whatever any other mod has
     * added, at a random level each.
     * <p>
     * Which enchantments those are is asked of the registry rather than listed here - anything whose
     * supported items include the trident is in the draw - so a mod that adds a trident enchantment
     * is in this without knowing the mod exists.
     */
    TRIDENT_ENCHANT {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            RandomSource random = level.random;
            List<Holder<Enchantment>> tridentEnchantments = enchantmentsFor(level, Items.TRIDENT);
            if (tridentEnchantments.isEmpty()) {
                return;
            }

            int count = BOOK_COUNT - 4 + random.nextInt(9);
            for (int i = 0; i < count; i++) {
                Holder<Enchantment> enchantment = tridentEnchantments.get(random.nextInt(tridentEnchantments.size()));
                int enchantmentLevel = 1 + random.nextInt(enchantment.value().getMaxLevel());
                ItemStack book = EnchantedBookItem.createForEnchantment(
                        new EnchantmentInstance(enchantment, enchantmentLevel));

                // The same arc the apples are thrown on, so the books rain back down around it.
                ItemEntity item = new ItemEntity(level, tnt.getX(), tnt.getY() + 0.5, tnt.getZ(), book);
                item.setDeltaMovement(random.nextGaussian() * 0.35, 0.35 + random.nextDouble() * 0.45,
                        random.nextGaussian() * 0.35);
                level.addFreshEntity(item);
            }

            level.sendParticles(ParticleTypes.ENCHANT, tnt.getX(), tnt.getY() + 0.5, tnt.getZ(),
                    120, 1.5, 1.0, 1.5, 0.2);
            level.playSound(null, tnt.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 3.0F, 0.8F);
        }
    },

    /**
     * Strips every player nearby out of their armour and locks them into leather instead, every piece
     * of it carrying Curse of Binding, so it cannot be taken off again without dying or grinding it
     * away. What they were wearing is handed back to them - into the inventory if there is room, onto
     * the floor if there is not - so the loss is the protection, not the gear.
     */
    BINDING {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            Optional<Holder.Reference<Enchantment>> binding =
                    level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(Enchantments.BINDING_CURSE);
            if (binding.isEmpty()) {
                return;
            }

            for (Player player : level.getEntitiesOfClass(Player.class,
                    tnt.getBoundingBox().inflate(BINDING_RADIUS), player -> !player.isSpectator() && !player.isCreative())) {
                for (EquipmentSlot slot : ARMOR_SLOTS) {
                    ItemStack worn = player.getItemBySlot(slot);
                    if (!worn.isEmpty()) {
                        player.setItemSlot(slot, ItemStack.EMPTY);
                        // Back into the inventory, or onto the floor if it will not fit.
                        player.getInventory().placeItemBackInInventory(worn);
                    }

                    ItemStack leather = new ItemStack(LEATHER_ARMOR.get(slot));
                    leather.enchant(binding.get(), 1);
                    player.setItemSlot(slot, leather);
                }
            }

            level.playSound(null, tnt.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 3.0F, 0.6F);
        }
    },

    /**
     * The wide glass field: two hundred blocks across and forty blocks tall, in every colour of glass
     * there is. It is far bigger than {@link #GLASS_SCATTER} and far sparser, so what it leaves
     * behind reads as a coloured haze hanging over the landscape rather than as a slab of glass.
     * <p>
     * A volume that size is five million blocks, so {@link DeferredFill} walks it one ring at a time
     * over the next few seconds, and the per block chance is scaled down by how tall it is so the
     * extra height spreads the same amount of glass out rather than multiplying it.
     */
    GLASS_SCATTER_2 {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            BlockPos origin = tnt.blockPosition();
            DeferredFill.queueWideGlass(level, origin);
            level.playSound(null, origin, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 4.0F, 0.4F);
        }
    },

    /**
     * A blanket of anvils out of the sky. They are real falling blocks, so they hurt what they land
     * on and stay where they land, and they come down over half a minute rather than all at once -
     * three hundred falling entities on one tick is its own kind of crash.
     */
    ANVIL_RAIN {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            BlockPos origin = tnt.blockPosition();
            DeferredFill.queueAnvilRain(level, origin, ANVIL_RADIUS, ANVIL_COUNT);
            level.playSound(null, origin, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 4.0F, 0.8F);
        }
    },

    /**
     * A swarm of shulker bullets, which is the only vanilla projectile that carries Levitation. They
     * home the way a shulker's own do, so what they are aimed at matters more than where they start:
     * each one picks something alive nearby and follows it around corners until it lands.
     * <p>
     * A bullet needs a shooter, and TNT lit by redstone has no owner at all, so the nearest player
     * stands in for one. With nobody about and nothing alive in range, nothing is fired.
     */
    LEVITATION {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            LivingEntity shooter = tnt.getOwner() != null
                    ? tnt.getOwner()
                    : level.getNearestPlayer(tnt, BULLET_RADIUS);
            if (shooter == null) {
                return;
            }

            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                    tnt.getBoundingBox().inflate(BULLET_RADIUS),
                    entity -> entity != shooter && entity.isAlive() && entity.canBeSeenAsEnemy());
            if (targets.isEmpty()) {
                return;
            }

            RandomSource random = level.random;
            for (int i = 0; i < BULLET_COUNT; i++) {
                LivingEntity target = targets.get(random.nextInt(targets.size()));
                ShulkerBullet bullet = new ShulkerBullet(level, shooter, target,
                        Direction.Plane.HORIZONTAL.getRandomDirection(random).getAxis());
                bullet.setPos(tnt.getX(), tnt.getY() + 0.5, tnt.getZ());
                level.addFreshEntity(bullet);
            }

            level.playSound(null, tnt.blockPosition(), SoundEvents.SHULKER_SHOOT, SoundSource.BLOCKS, 3.0F, 0.7F);
        }
    },

    /**
     * Lays a disc of soil sixteen blocks across, rolling four blocks either side of where it went
     * off, and grows a grove of compressed cobblestone trees on it - every one of them a different
     * vanilla shape, because what it plants is the same random draw worldgen uses.
     * <p>
     * It is the largest thing any of these do to the ground short of the chunk filler, so it goes
     * through {@link DeferredFill}: the soil is laid a slice of columns at a time and the trees are
     * planted a few a tick once it is down.
     */
    TREENT {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            BlockPos origin = tnt.blockPosition();
            DeferredFill.queueGrove(level, origin, GROVE_RADIUS, GROVE_RELIEF, GROVE_TREES);
            level.playSound(null, origin, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 4.0F, 0.6F);
        }
    },

    /**
     * A burst of Super Compressed Arrows in every direction at once, and a blast that hurts without
     * touching the ground. The arrows are the damage at range; the blast is what makes standing next
     * to it a mistake.
     */
    ARROW_BURST {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            RandomSource random = level.random;

            for (int i = 0; i < ARROW_BURST_COUNT; i++) {
                CompressedArrowEntity arrow = superArrow(level, tnt);
                // A direction drawn off the sphere rather than the box, so the burst is round.
                Vec3 direction = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian())
                        .normalize();
                arrow.shoot(direction.x, direction.y, direction.z, ARROW_BURST_VELOCITY, ARROW_BURST_SPREAD);
                level.addFreshEntity(arrow);
            }

            arrowBlast(level, tnt, ARROW_BURST_BLAST);
            level.playSound(null, tnt.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.BLOCKS, 4.0F, 0.6F);
        }
    },

    /**
     * The same burst thrown as <em>tipped arrows</em>, each carrying a different effect drawn fresh
     * out of the effect registry - any of them, kind or otherwise, because sixty arrows scattered
     * over a clearing is a lottery rather than an attack and half of them will land on nothing.
     * <p>
     * They are vanilla {@link Arrow}s rather than this mod's, and that is the whole point of the
     * tier: a compressed arrow carrying an effect looks exactly like a compressed arrow, so a burst
     * of them was the tier below with nothing to show for itself. A tipped arrow is coloured by its
     * own contents and trails that colour the entire flight, so sixty of them with sixty different
     * effects are sixty different colours in the air, which is what the tier is buying. Each one is
     * set to the Super Compressed arrow's damage so the upgrade is not paid for in damage, and the
     * stack each carries is a real tipped arrow - what is picked up afterwards is what was fired.
     * <p>
     * What may be drawn is never listed: {@code ArrowEffects} walks the registry, so another mod's
     * effects are in the bag the day it adds them, exactly as the Effect TNT's are.
     */
    ARROW_BURST_2 {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            RandomSource random = level.random;

            for (int i = 0; i < ARROW_BURST_COUNT; i++) {
                Arrow arrow = effectArrow(level, tnt);
                arrow.addEffect(ArrowEffects.random(random, false));

                Vec3 direction = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian())
                        .normalize();
                arrow.shoot(direction.x, direction.y, direction.z, ARROW_BURST_VELOCITY, ARROW_BURST_SPREAD);
                level.addFreshEntity(arrow);
            }

            arrowBlast(level, tnt, ARROW_BURST_BLAST);
            level.playSound(null, tnt.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.BLOCKS, 4.0F, 0.4F);
        }
    },

    /**
     * Ten seconds of gravity that belongs to somewhere else. Everything loose within a dozen blocks
     * is dragged towards the point the TNT went off at - players, mobs, arrows and dropped items -
     * and the ground itself is pulled up a handful of blocks at a time and thrown in after them.
     * <p>
     * It is deliberately a weak one. Nothing is destroyed and nothing is killed by the pull itself:
     * the blocks it tears up are real falling blocks, so they land in a heap around the middle and
     * can be picked back up or walked over, and a player who runs hard enough gets out. What it
     * costs whoever is caught is ten seconds of not choosing where they stand.
     * <p>
     * The whole of it is a {@link DeferredFill} job rather than a detonation, because a pull is a
     * thing that happens over time - one tick of it is a shove and nothing more.
     */
    SUCC {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            DeferredFill.queueSingularity(level, tnt.position());
            level.playSound(null, tnt.blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 4.0F, 0.5F);
        }
    },

    /**
     * A dome of tier {@value #DOME_LEVEL} stone, {@value #DOME_RADIUS} blocks across the radius,
     * standing on the layer the charge went off on.
     * <p>
     * It is the shell and nothing else - see {@code DeferredFill.Dome} - so what it costs is a few
     * thousand blocks rather than the sixty thousand a solid hemisphere of that size would be, and
     * what it makes is a room rather than a hill. The stone it is built out of is its own tier,
     * which is the point: from {@code ModBlocks.HARDENED_LEVEL_TIER_2} up nothing in the game moves
     * that block, no explosion included, so what this lays down is permanent to everything but the
     * one pickaxe that opens it.
     * <p>
     * It fills through the deferred job for the ordinary reason, and the ring at a time also happens
     * to be the effect: a dome that assembles itself from the ground up over a couple of seconds
     * reads as a thing being built, where the same blocks written at once read as a stutter.
     */
    DOME {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            DeferredFill.queueDome(level, tnt.blockPosition(), DOME_RADIUS,
                    ModBlocks.byLevel(DOME_LEVEL).get().defaultBlockState());
            level.playSound(null, tnt.blockPosition(), SoundEvents.RESPAWN_ANCHOR_SET_SPAWN,
                    SoundSource.BLOCKS, 4.0F, 0.6F);
        }
    },

    /**
     * The Succ TNT with five minutes and a curve on it - see {@code DeferredFill.BlackHole}.
     * <p>
     * The difference between the two is entirely time. The Succ TNT is ten seconds of a weak,
     * constant pull that a sprint gets out of, and it is over before anybody has had to think about
     * it; this starts weaker than that one does and grows geometrically for five minutes, so it is
     * an event rather than a shove - somewhere to be got away from rather than somewhere to be
     * pulled about in. It ends pulling seventy-five times as hard from six times as far, and the
     * last thirty seconds of it are most of that.
     * <p>
     * There is a ball of black concrete in the middle for the whole five minutes, which is what it
     * reads as from a distance and what everything it drags in piles up against. It is taken away
     * when the job ends.
     */
    BLACKHOLE {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            DeferredFill.queueBlackHole(level, tnt.position());
            level.playSound(null, tnt.blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS,
                    4.0F, 0.1F);
        }
    },

    /**
     * The Blackhole TNT's five minutes in five seconds, and then the bill.
     * <p>
     * The first half is that charge's own job with nothing changed but the clock: the same pull, the
     * same geometric growth, the same digging and the same ball of black concrete in the middle,
     * walked in {@value #SINGULARITY_TICKS} ticks instead of six thousand. Sixty times the speed
     * turns a thing that had to be walked away from into a thing there is no walking away from -
     * every figure in it is bounded by construction, so the compression costs nothing but time.
     * <p>
     * The second half is one explosion, and it is one on purpose rather than the swarm of smaller
     * blasts the Super Compressed TNT uses: a cluster is how you make a <em>crater</em> anybody can
     * see, and this is not about the crater. It is worth
     * {@code min(}{@link #MAX_POWER}{@code , 10000 * vanilla TNT)} - the ceiling, today - and its
     * damage is vanilla's own, which is linear in the radius and so already matches the power
     * exactly. What it costs whatever was left standing is around four hundred and fifty thousand
     * points, out to a reach of sixty-five thousand blocks, which is the whole dimension.
     * <p>
     * The hole it leaves is the size of the black hole that made it rather than the size of the
     * blast, and {@code DeferredFill.BlackHole#finish} is where both halves of that are argued.
     */
    SINGULARITY {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            DeferredFill.queueCollapse(level, tnt.position(), SINGULARITY_TICKS, SINGULARITY_POWER);
            level.playSound(null, tnt.blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS,
                    4.0F, 0.05F);
        }
    },

    /**
     * A field full of livestock, all of them already in love. What can appear is read off the entity
     * registry rather than listed - anything that is an {@code Animal} qualifies, so another mod's
     * creatures come along - with one exception: of this mod's own, only the Compressed Cobblestone
     * Chicken is fit to be bred.
     */
    BREEDING {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            List<EntityType<?>> breedable = breedableTypes(level);
            if (breedable.isEmpty()) {
                return;
            }

            RandomSource random = level.random;
            BlockPos origin = tnt.blockPosition();

            // One species per group, four of them together: a lone animal has nothing to breed with,
            // and a mixed heap of one of everything is the same problem spread thinner.
            for (int group = 0; group < BREEDING_GROUPS; group++) {
                EntityType<?> species = breedable.get(random.nextInt(breedable.size()));
                BlockPos centre = origin.offset(random.nextInt(BREEDING_SPREAD * 2 + 1) - BREEDING_SPREAD, 0,
                        random.nextInt(BREEDING_SPREAD * 2 + 1) - BREEDING_SPREAD);

                for (int i = 0; i < BREEDING_GROUP_SIZE; i++) {
                    // Spread inside the group as well, or four animals arrive inside one another.
                    BlockPos pos = centre.offset(random.nextInt(BREEDING_HUDDLE * 2 + 1) - BREEDING_HUDDLE, 0,
                            random.nextInt(BREEDING_HUDDLE * 2 + 1) - BREEDING_HUDDLE);
                    if (!level.isLoaded(pos)) {
                        continue;
                    }

                    BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
                    Entity spawned = species.spawn(level, surface, MobSpawnType.TRIGGERED);

                    if (spawned instanceof Animal animal) {
                        animal.setInLove(null);
                    }
                }
            }

            level.playSound(null, origin, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 3.0F, 1.4F);
        }
    },

    /**
     * Every effect in the game at once, on everything nearby, for half a minute. The list is the
     * registry itself, so a mod's own effects are in it - and so are the ones nobody wants, because
     * "all of them" is the joke. Instant Damage lands the moment it is applied.
     */
    EFFECTS {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            List<LivingEntity> caught = level.getEntitiesOfClass(LivingEntity.class,
                    tnt.getBoundingBox().inflate(EFFECT_RADIUS), LivingEntity::isAlive);

            for (LivingEntity entity : caught) {
                for (Holder<MobEffect> effect : BuiltInRegistries.MOB_EFFECT.holders().toList()) {
                    entity.addEffect(new MobEffectInstance(effect, EFFECT_DURATION, 0, false, true, true));
                }
            }

            level.sendParticles(ParticleTypes.EFFECT, tnt.getX(), tnt.getY() + 0.5, tnt.getZ(),
                    200, 2.0, 1.5, 2.0, 0.4);
            level.playSound(null, tnt.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 4.0F, 0.7F);
        }
    },

    /**
     * The arrow burst drawn out into a shape: five seconds of arrows turning around the circle,
     * climbing and quickening as they go, which leaves a cone of them standing in the ground. The
     * pattern is time itself, so it is {@link DeferredFill} that draws it.
     */
    ARROW_SPIRAL {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            DeferredFill.queueArrowSpiral(level, tnt.position().add(0.0, 0.5, 0.0), SPIRAL_TICKS,
                    SPIRAL_ARROWS_PER_TICK, SPIRAL_START_VELOCITY, SPIRAL_END_VELOCITY, tnt.getOwner());

            // The same blast the burst makes: it hurts, and it leaves the ground exactly as it was.
            arrowBlast(level, tnt, ARROW_BURST_BLAST);
            level.playSound(null, tnt.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.BLOCKS, 4.0F, 1.4F);
        }
    },

    /**
     * A flashbang. It breaks nothing and does no damage: what it does is take the eyes off everything
     * that could see it go off, which is the one attack in this mod that a wall genuinely stops -
     * line of sight is checked per victim, so anything round a corner is untouched.
     */
    FLASH {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            List<LivingEntity> caught = level.getEntitiesOfClass(LivingEntity.class,
                    tnt.getBoundingBox().inflate(FLASH_RADIUS),
                    entity -> entity.isAlive() && entity.hasLineOfSight(tnt));

            for (LivingEntity entity : caught) {
                entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, FLASH_BLINDNESS, 0, false, false, true));
                entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, FLASH_NAUSEA, 0, false, false, true));
            }

            level.sendParticles(ParticleTypes.FLASH, tnt.getX(), tnt.getY() + 0.5, tnt.getZ(), 6, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(ParticleTypes.END_ROD, tnt.getX(), tnt.getY() + 0.5, tnt.getZ(),
                    200, 0.5, 0.5, 0.5, 0.6);
            level.playSound(null, tnt.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.BLOCKS, 4.0F, 2.0F);
        }
    },

    /**
     * Plays a song in lightning, and in nothing else. One bolt per note, in a tight ring around where
     * the TNT went off, and how loud that bolt is is the note: lightning has no pitch to give, so a
     * tune written for it is a rhythm and a dynamic line played on a single note.
     * <p>
     * Every one of those bolts is a real one, and there are over a thousand of them in four minutes:
     * the ring burns, and anything alive in it dies early in the first bar. It is a performance to be
     * watched from outside.
     * <p>
     * Which song is a datapack file: {@link LightningSong} reads
     * {@code data/<namespace>/songs/<name>.txt}, so a pack can replace {@link #SONG} or add its own
     * without touching the mod. The performance is anchored to the ground rather than to whoever lit
     * it, and carries on whether or not that player is still alive.
     */
    LIGHTNING_SONG {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            MinecraftServer server = level.getServer();
            DeferredStrikes.queueSong(level, tnt.blockPosition(), LightningSong.get(server, SONG), SONG_RADIUS);
        }
    },

    /**
     * The Chicken TNT again, ten times over: four hundred of them, and a burst ten times as hard to
     * throw them with. The flock is dropped in over a second rather than all at once, and the burst
     * waits for the last of them - both of which {@link DeferredFill} handles.
     */
    CHICKENS_2 {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            DeferredFill.queueFlock(level, tnt.position(), CHICKEN_COUNT * 10, CHICKEN_CLUSTER * 3.0,
                    CHICKEN_DROP_HEIGHT, WIND_BURST_10X, (float) (CHICKEN_CLUSTER * 3.0));
            level.playSound(null, tnt.blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS, 4.0F, 0.6F);
        }
    },

    /**
     * Half a minute of wind bursts, a few ticks apart, at a fresh spot and a fresh strength every
     * time. It breaks nothing and damages nothing: what it does is refuse to let anything in it come
     * to rest, so a player caught in the middle is knocked from one burst into the next like a ball
     * still in play. Fall damage is the only thing in it that can kill.
     * <p>
     * The bursts are laid down over time by {@link DeferredStrikes} rather than let off together,
     * and that is the effect rather than a budget: a hundred at once is one shove and then silence.
     */
    PINBALL {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            DeferredStrikes.queuePinball(level, tnt.blockPosition(), PINBALL_TICKS, PINBALL_RADIUS);
            level.playSound(null, tnt.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(),
                    SoundSource.BLOCKS, 3.0F, 0.6F);
        }
    },

    /**
     * The Flower TNT again, in bolts: it breaks nothing and throws an armful of lightning into the
     * air instead, drawn at random from every bolt the game has - the eighty-eight keys of the piano,
     * the plain one, and whatever else has been registered as a {@link BoltItem} by then.
     */
    BOLT_DROP {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            RandomSource random = level.random;
            List<Item> bolts = bolts();
            if (bolts.isEmpty()) {
                return;
            }

            int count = BOLT_COUNT - 4 + random.nextInt(9);
            for (int i = 0; i < count; i++) {
                ItemEntity item = new ItemEntity(level, tnt.getX(), tnt.getY() + 0.5, tnt.getZ(),
                        new ItemStack(bolts.get(random.nextInt(bolts.size()))));
                // Thrown up and out, so they arc rather than piling up under the blast.
                item.setDeltaMovement(random.nextGaussian() * 0.35, 0.35 + random.nextDouble() * 0.45,
                        random.nextGaussian() * 0.35);
                level.addFreshEntity(item);
            }

            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, tnt.getX(), tnt.getY() + 0.5, tnt.getZ(),
                    160, 1.5, 1.0, 1.5, 0.4);
            level.playSound(null, tnt.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS,
                    2.0F, 1.6F);
        }
    },

    /**
     * The Lightning TNT again, now that lightning has a pitch: the same piece, played on the
     * eighty-eight note bolts rather than reduced to one note and a dynamic line.
     * <p>
     * It is the same performance in every other way, and everything the first one's note says about
     * it still holds - every bolt is real, there are thousands of them, and the ring is a place to
     * have been rather than a place to listen from.
     */
    LIGHTNING_SONG_2 {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            MinecraftServer server = level.getServer();
            DeferredStrikes.queueSong(level, tnt.blockPosition(), LightningSong.get(server, SONG_2), SONG_RADIUS);
        }
    },

    /**
     * The opening of Beethoven's Op. 27 No. 2, in the same lightning. It is a quarter as many bolts
     * as the other two and a good deal slower, so it is the one of the three that can be watched
     * from a little way off rather than only survived - but every bolt is still real, and the ring
     * still burns.
     */
    MOONLIGHT {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            MinecraftServer server = level.getServer();
            DeferredStrikes.queueSong(level, tnt.blockPosition(), LightningSong.get(server, MOONLIGHT_SONG),
                    SONG_RADIUS);
        }
    },

    /**
     * Lays a circle of water three blocks across, flat on the ground the fuse ran out on. It is a
     * disc and not a ball: one layer of water, with the layer above it cleared so the pool can be
     * seen and stepped into.
     * <p>
     * Sixty-odd blocks in one tick, which is nothing - two orders of magnitude below where
     * {@link DeferredFill} starts to earn its keep. It still refuses to touch the hardened levels,
     * which nothing in the game moves.
     */
    POOL {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            BlockPos origin = tnt.blockPosition();
            BlockState water = Blocks.WATER.defaultBlockState();
            BlockState air = Blocks.AIR.defaultBlockState();

            for (int x = -POOL_RADIUS; x <= POOL_RADIUS; x++) {
                for (int z = -POOL_RADIUS; z <= POOL_RADIUS; z++) {
                    if (x * x + z * z > POOL_RADIUS * POOL_RADIUS) {
                        continue;
                    }

                    fill(level, origin.offset(x, 0, z), water);
                    fill(level, origin.offset(x, 1, z), air);
                }
            }

            level.playSound(null, origin, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 3.0F, 0.7F);
        }

        /** One block of the pool, if it is loaded and is not one of the levels nothing can move. */
        private void fill(ServerLevel level, BlockPos pos, BlockState state) {
            if (level.isLoaded(pos) && level.getBlockState(pos).getDestroySpeed(level, pos) >= 0.0F) {
                level.setBlock(pos, state, Block.UPDATE_ALL);
            }
        }
    },

    /**
     * Breaks nothing and hurts nothing: everything alive nearby simply gets very fast and stops
     * caring about a block in the way. Speed X is the vanilla effect at amplifier 9; the extra block
     * of step height is {@link ModMobEffects#ZOOM}, which exists as an effect rather than as a bare
     * attribute modifier precisely so that it comes back off on its own - a modifier put straight
     * onto a passing mob would have nothing watching to remove it.
     */
    ZOOM {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            List<LivingEntity> caught = level.getEntitiesOfClass(LivingEntity.class,
                    tnt.getBoundingBox().inflate(ZOOM_RADIUS), LivingEntity::isAlive);

            for (LivingEntity entity : caught) {
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ZOOM_DURATION,
                        ZOOM_SPEED_AMPLIFIER, false, true, true));
                entity.addEffect(new MobEffectInstance(ModMobEffects.ZOOM, ZOOM_DURATION, 0, false, true, true));
            }

            level.sendParticles(ParticleTypes.CLOUD, tnt.getX(), tnt.getY() + 0.5, tnt.getZ(),
                    150, 2.5, 1.0, 2.5, 0.35);
            level.playSound(null, tnt.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                    SoundSource.BLOCKS, 4.0F, 1.6F);
        }
    },

    /**
     * A cobweb every so often, out to the edge of what a player can see - six hundred and forty
     * blocks, which is a render distance of forty chunks - and twenty blocks deep around the blast.
     * <p>
     * That volume is twenty-seven million blocks, so it is not walked: {@link DeferredFill} throws
     * darts at it instead, forty a tick for twenty-five seconds, which is the only way to reach this
     * far without either stalling the server or taking twenty minutes. What lands in air becomes a
     * web and everything else is a miss, so the result is under a tenth of a percent - one web per
     * thirteen hundred blocks of open air, sparse enough that nothing is ever walled in.
     */
    WEBS {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            DeferredFill.queueWebs(level, tnt.blockPosition());
            level.playSound(null, tnt.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 4.0F, 0.5F);
        }
    },

    /**
     * The third rung of the chicken family, and the family's own step is ten: forty birds, then four
     * hundred, then four thousand. That is a great many entities, so the flock is laid down by
     * {@link DeferredFill} at its usual twenty a tick - ten solid seconds of chickens raining out of
     * the sky - over a spread three times the second rung's, which is what keeps them under the
     * entity cramming limit as they land.
     * <p>
     * What actually separates it from the second rung is the wind. The second lets off one enormous
     * burst once the last bird is down; this one runs the Pinball TNT's half-minute of staggered
     * bursts <em>while</em> the flock is still falling, so the birds never settle - which is the same
     * lesson the Pinball TNT itself is built on: a hundred bursts together are one shove and then
     * stillness, and a hundred bursts spread out are something still in play.
     */
    CHICKENS_3 {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            DeferredFill.queueFlock(level, tnt.position(), CHICKEN_COUNT * 100, CHICKEN_CLUSTER * 9.0,
                    CHICKEN_DROP_HEIGHT, WIND_BURST_10X, (float) (CHICKEN_CLUSTER * 9.0));
            DeferredStrikes.queuePinball(level, tnt.blockPosition(), CHICKEN_3_WIND_TICKS,
                    CHICKEN_CLUSTER * 9.0);
            level.playSound(null, tnt.blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS, 4.0F, 0.4F);
        }
    },

    /**
     * Raises a lit nether portal where the fuse ran out and pours wither skulls out of it.
     * <p>
     * Every skull starts inside the doorway and flies away from it, half out of one face and half
     * out of the other, so the portal reads as the thing they arrived through rather than as a
     * target something else is shooting at.
     * <p>
     * The frame is built by hand and then lit through {@link PortalShape#findEmptyPortalShape},
     * which is the same door flint and steel comes through: it re-measures the frame that was just
     * laid and refuses if anything about it is wrong, so a portal raised on broken ground either
     * lights properly or does not appear at all - there is no case where portal blocks are left
     * hanging in a frame that is not one.
     * <p>
     * The skulls are the point of it. They are deliberately <em>not</em> {@code setDangerous}, which
     * is the blue skull that breaks blocks - a barrage of those would take the portal apart on the
     * first volley. A skull that starts inside a lit portal is not carried through it: the portal
     * block only moves an entity that has stood in it for its own cooldown, and one launched out at
     * a wither skull's speed is clear of it long before that.
     */
    PORTAL {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            BlockPos origin = tnt.blockPosition();
            Direction.Axis axis = level.random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
            if (!raisePortal(level, origin, axis)) {
                return;
            }

            // Which way the doorway faces. The portal stands in the plane of `axis`, so the two
            // ways out of it are along the other horizontal axis.
            Vec3 outwards = axis == Direction.Axis.X ? new Vec3(0.0, 0.0, 1.0) : new Vec3(1.0, 0.0, 0.0);

            // Every skull starts *in* the doorway and flies out of it, half through one face and
            // half through the other, rather than being fired at the portal from outside. The
            // portal is the thing they are coming out of, which is what a portal is for.
            RandomSource random = level.random;
            for (int i = 0; i < PORTAL_SKULLS; i++) {
                // A point drawn anywhere across the two-by-three opening, so the stream fills the
                // doorway rather than leaving it from one spot.
                double across = random.nextDouble() * PORTAL_INNER_WIDTH;
                double up = random.nextDouble() * PORTAL_INNER_HEIGHT;
                Vec3 from = Vec3.atBottomCenterOf(origin).add(
                        axis == Direction.Axis.X ? across : 0.0,
                        up,
                        axis == Direction.Axis.X ? 0.0 : across);

                // Alternating rather than drawn, so the two streams are always evenly matched.
                double side = (i % 2 == 0) ? 1.0 : -1.0;
                Vec3 heading = outwards.scale(side)
                        .add((random.nextDouble() - 0.5) * PORTAL_SKULL_SPREAD,
                                (random.nextDouble() - 0.5) * PORTAL_SKULL_SPREAD,
                                (random.nextDouble() - 0.5) * PORTAL_SKULL_SPREAD)
                        .normalize();

                WitherSkull skull = new WitherSkull(EntityType.WITHER_SKULL, level);
                skull.moveTo(from.x, from.y, from.z, 0.0F, 0.0F);
                skull.setOwner(tnt.getOwner());
                // What the LivingEntity constructor does with the movement it is handed; that
                // constructor cannot be used here because TNT lit by redstone has no owner at all.
                skull.setDeltaMovement(heading.scale(skull.accelerationPower));
                level.addFreshEntity(skull);
            }

            level.playSound(null, origin, SoundEvents.WITHER_SHOOT, SoundSource.BLOCKS, 4.0F, 0.8F);
        }

        /**
         * Lays the obsidian and lights it. The doorway is cleared as part of the same pass, since a
         * frame built around a block of stone is not an empty portal shape and would not light.
         *
         * @return whether a portal now stands there
         */
        private boolean raisePortal(ServerLevel level, BlockPos bottomLeft, Direction.Axis axis) {
            Direction across = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
            BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
            BlockState air = Blocks.AIR.defaultBlockState();

            for (int out = -1; out <= PORTAL_INNER_WIDTH; out++) {
                for (int up = -1; up <= PORTAL_INNER_HEIGHT; up++) {
                    BlockPos pos = bottomLeft.relative(across, out).above(up);
                    if (!level.isLoaded(pos)) {
                        return false;
                    }

                    boolean frame = out == -1 || out == PORTAL_INNER_WIDTH
                            || up == -1 || up == PORTAL_INNER_HEIGHT;
                    level.setBlock(pos, frame ? obsidian : air, Block.UPDATE_ALL);
                }
            }

            return PortalShape.findEmptyPortalShape(level, bottomLeft, axis)
                    .map(shape -> {
                        shape.createPortalBlocks();
                        return true;
                    })
                    .orElse(false);
        }
    },

    /**
     * A spawner and a handful of spawn eggs, thrown up the way the Flower TNT throws its blooms.
     * <p>
     * The eggs are drawn from {@code SpawnEggItem.eggs()}, which is vanilla's own list of every
     * registered egg, narrowed to the {@code minecraft} namespace. That narrowing is the one
     * deliberate exception to this mod's usual rule of building on registries and tags rather than
     * on namespaces: a modded egg is a modded mob, and a TNT that hands out four of those at random
     * would be handing out whatever the heaviest boss in the pack happens to be.
     */
    SPAWNER {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            RandomSource random = level.random;
            List<Item> eggs = vanillaEggs();

            throwUp(level, tnt, new ItemStack(Items.SPAWNER));
            for (int i = 0; i < SPAWNER_EGGS && !eggs.isEmpty(); i++) {
                throwUp(level, tnt, new ItemStack(eggs.get(random.nextInt(eggs.size()))));
            }

            level.sendParticles(ParticleTypes.FLAME, tnt.getX(), tnt.getY() + 0.5, tnt.getZ(),
                    80, 1.0, 0.8, 1.0, 0.05);
            level.playSound(null, tnt.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 3.0F, 0.6F);
        }

        /** One item, thrown up and out so it arcs rather than piling up under the blast. */
        private void throwUp(ServerLevel level, CompressedPrimedTntEntity tnt, ItemStack stack) {
            RandomSource random = level.random;
            ItemEntity item = new ItemEntity(level, tnt.getX(), tnt.getY() + 0.5, tnt.getZ(), stack);
            item.setDeltaMovement(random.nextGaussian() * 0.2, 0.4 + random.nextDouble() * 0.3,
                    random.nextGaussian() * 0.2);
            level.addFreshEntity(item);
        }
    },

    /**
     * Winter, laid over the ground and put on everything standing in it. The snow is deep in the
     * middle and thins to a dusting at the rim, and everything alive nearby catches
     * {@link ModMobEffects#FREEZING}, which is the brew that does powder snow's damage rather than
     * vanilla's cosmetic shiver.
     */
    SNOW {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            DeferredFill.queueSnowfield(level, tnt.blockPosition());

            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                    tnt.getBoundingBox().inflate(SNOW_RADIUS), LivingEntity::isAlive)) {
                entity.addEffect(new MobEffectInstance(ModMobEffects.FREEZING, SNOW_FREEZE_TICKS, 0,
                        false, true, true));
            }

            level.playSound(null, tnt.blockPosition(), SoundEvents.SNOW_PLACE, SoundSource.BLOCKS, 4.0F, 0.6F);
        }
    },

    /**
     * A Menger sponge of ancient debris standing in the air, over a floor turned to Slippery Ice.
     * <p>
     * The sponge is the reason it is the size it is. Every level cuts the cube into twenty-seven and
     * throws away the middle of the cube and the middle of each of its six faces, so the smallest
     * hole is the width over three to the depth - and a hole smaller than one block is not a hole at
     * all. Twenty-seven blocks on a side is exactly three levels of detail with the last one landing
     * on single blocks; going deeper would need a wider sponge, not a finer one.
     * <p>
     * It is a solid rather than the flat carpet it used to be because a carpet is only a pattern
     * from directly underneath it: a sponge can be walked around, looked through along all three
     * axes and climbed into, and its tunnels go all the way through in every direction.
     */
    DEBRIS {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            DeferredFill.queueFractal(level, tnt.blockPosition());
            level.playSound(null, tnt.blockPosition(), SoundEvents.ANCIENT_DEBRIS_PLACE,
                    SoundSource.BLOCKS, 4.0F, 0.5F);
        }
    },

    /** A nest of ordinary silverfish, put down on the ground the way the guardians are. */
    SILVERFISH {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            spawnOnSurface(level, EntityType.SILVERFISH, tnt.blockPosition(), SILVERFISH_COUNT,
                    SILVERFISH_SPREAD);
            level.playSound(null, tnt.blockPosition(), SoundEvents.SILVERFISH_AMBIENT,
                    SoundSource.BLOCKS, 4.0F, 0.7F);
        }
    },

    /**
     * The same nest again, of the ones that fly. They are scattered in the air rather than laid on
     * the ground, since that is where they live - {@code spawnOnSurface} would drop the whole swarm
     * on the floor and they would spend their first second climbing back off it.
     */
    SILVERFISH_2 {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            RandomSource random = level.random;
            for (int i = 0; i < COMPRESSED_SILVERFISH_COUNT; i++) {
                CompressedSilverfishEntity silverfish = ModEntities.COMPRESSED_SILVERFISH.get().create(level);
                if (silverfish == null) {
                    continue;
                }

                silverfish.moveTo(tnt.getX() + (random.nextDouble() - 0.5) * COMPRESSED_SILVERFISH_SPREAD,
                        tnt.getY() + random.nextDouble() * COMPRESSED_SILVERFISH_SPREAD,
                        tnt.getZ() + (random.nextDouble() - 0.5) * COMPRESSED_SILVERFISH_SPREAD,
                        random.nextFloat() * 360.0F, 0.0F);
                level.addFreshEntity(silverfish);
            }

            level.sendParticles(ParticleTypes.INFESTED, tnt.getX(), tnt.getY() + 0.5, tnt.getZ(),
                    120, 2.0, 1.5, 2.0, 0.1);
            level.playSound(null, tnt.blockPosition(), SoundEvents.SILVERFISH_AMBIENT,
                    SoundSource.BLOCKS, 4.0F, 0.4F);
        }
    },

    /**
     * A spout of water one block wide and forty tall, climbing out of the ground over two seconds.
     * <p>
     * It is the Pool TNT's opposite in every way that matters: that one lays a flat disc that stays
     * where it is put, and this one raises a column that vanilla's fluid ticking immediately starts
     * pulling apart. Nothing holds the spout up, and it is not meant to be held - what a player sees
     * is a jet that climbs and a sheet that falls back off it, and none of that second half is
     * modelled here. See {@link DeferredFill#queueGeyser}.
     */
    GEYSER {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            DeferredFill.queueGeyser(level, tnt.blockPosition(), Blocks.WATER.defaultBlockState(),
                    GEYSER_HEIGHT);
            level.playSound(null, tnt.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE,
                    SoundSource.BLOCKS, 4.0F, 0.6F);
        }
    },

    /**
     * The same spout in lava. Shorter than the water one, and not out of caution about the height:
     * lava falls at a fifth of water's speed and spreads three blocks rather than seven, so a column
     * of it stands far longer and far narrower than the same column of water - forty blocks of it
     * would be a wall standing for a minute rather than an eruption.
     */
    GEYSER_2 {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            DeferredFill.queueGeyser(level, tnt.blockPosition(), Blocks.LAVA.defaultBlockState(),
                    LAVA_GEYSER_HEIGHT);
            level.playSound(null, tnt.blockPosition(), SoundEvents.LAVA_POP,
                    SoundSource.BLOCKS, 4.0F, 0.5F);
        }
    },

    /**
     * Digs a basin, fills it with water and grows a coral reef in it - the one TNT here that leaves
     * somewhere worth being rather than somewhere to look at. See {@code DeferredFill.Reef} for the
     * shape and for why the coral it plants is waterlogged.
     */
    CORAL {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            DeferredFill.queueReef(level, tnt.blockPosition());
            level.playSound(null, tnt.blockPosition(), SoundEvents.CORAL_BLOCK_PLACE,
                    SoundSource.BLOCKS, 4.0F, 0.8F);
        }
    },

    /**
     * Builds a whole vanilla village where it goes off, villagers, farms, lamp posts and all.
     * <p>
     * Which village is read off the biome rather than fixed, through the same
     * {@code #minecraft:has_structure/village_*} tags worldgen itself picks by - so a desert gets a
     * desert village and another mod's biome gets whichever kind it declared itself as, with nothing
     * here naming a biome. A biome that names no village at all still gets one: the plains village
     * is the fallback, because a TNT that sometimes does nothing is a TNT nobody trusts.
     * <p>
     * The structure is generated with a biome predicate that accepts everything, exactly as
     * {@code /place structure} does, so where it lands is wherever the fuse ran out - it is not
     * looking for somewhere suitable. See {@code DeferredFill.StructureBuild} for why the building
     * of it is a chunk a tick.
     */
    VILLAGE {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            structure(level, tnt, villageFor(level, tnt.blockPosition()), SoundEvents.VILLAGER_YES);
        }
    },

    /**
     * A woodland mansion, dropped whole. It comes with everything a mansion comes with, which
     * includes the evokers and the vindicators inside it - this is a building and an ambush in one
     * package, and the loot is the reason to open it anyway.
     * <p>
     * A mansion will not build below y 60, and that rule is vanilla's own, written inside
     * {@code WoodlandMansionStructure.findGenerationPoint} rather than in the biome list - so it
     * survives the accept-everything biome predicate the way the monument's ocean rule does. Set off
     * in a valley floor, a cave or an ocean it declines and says so with the failure sound; there is
     * no sensible fallback, because a mansion is a building that sits on the ground and there is
     * nowhere down there to sit it on.
     */
    MANSION {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            structure(level, tnt, BuiltinStructures.WOODLAND_MANSION, SoundEvents.WOOD_PLACE);
        }
    },

    /**
     * An ocean monument - the sea temple, the one with the elder guardians and the eight blocks of
     * gold buried under the middle of it.
     * <p>
     * In an ocean it is built the way vanilla builds one, sitting on the sea floor. Everywhere else
     * vanilla refuses: {@code OceanMonumentStructure.findGenerationPoint} insists every biome within
     * twenty-nine blocks is in {@code #minecraft:required_ocean_monument_surrounding}, and that check
     * is inside the structure rather than in the biome list, so the accept-everything predicate used
     * for the other two does not reach it.
     * <p>
     * Rather than do nothing, a refusal falls back on {@link #sunkenMonument}, which builds the same
     * temple from its own piece and skips the check. What that costs is honest and worth knowing: a
     * monument carries no terrain height of its own - {@code MonumentBuilding} is built at a fixed
     * y 39 and stands twenty-three blocks - so inland it arrives *buried*, a sunken temple to be dug
     * down to rather than swum into. That is a result. Nothing is not.
     */
    MONUMENT {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            BlockPos origin = tnt.blockPosition();

            StructureStart start = DeferredFill.generateStructure(level, origin, BuiltinStructures.OCEAN_MONUMENT);
            if (!start.isValid()) {
                start = sunkenMonument(level, origin);
            }

            place(level, origin, start, SoundEvents.ELDER_GUARDIAN_AMBIENT);
        }
    },

    /**
     * Raises pillars of {@link #PILLAR_LEVEL} stone through the chunks around it - one block wide,
     * the whole height of the world, and unbreakable to everything but the tier 2 pickaxe.
     * <p>
     * The density is the design. One column in {@link #PILLAR_CHANCE} of a disc of
     * {@link #PILLAR_RADIUS} is about thirty pillars over three chunks, which is thin enough to walk
     * through and thick enough that the sky is different afterwards - a forest rather than a wall.
     * Every one of them is a permanent change to the terrain, since the stone they are made of is
     * the stone a player has to have beaten the second hardened floor to move.
     */
    PILLARS_2 {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            DeferredFill.queuePillars(level, tnt.blockPosition(), PILLAR_RADIUS, PILLAR_CHANCE,
                    ModBlocks.byLevel(PILLAR_LEVEL).get().defaultBlockState());

            level.playSound(null, tnt.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 4.0F, 0.4F);
        }
    },

    /**
     * The Bee-nt: a swarm of Compressed Bees, thrown into the air where the fuse ran out.
     * <p>
     * They are scattered through the air rather than laid on the ground, for the reason the second
     * Silverfish TNT gives about its own swarm - a bee put on the floor spends its first second
     * climbing off it. Each one is a bee in every respect but two: it hunts on sight, and its sting
     * leaves corrosion rather than poison. See {@link CompressedBeeEntity}.
     * <p>
     * The count is what makes it a weapon rather than a nuisance, and it is a count of *clocks*.
     * Every bee that connects starts an independent ten second countdown on whatever it stung, so
     * {@value #BEE_COUNT} bees are up to {@value #BEE_COUNT} hundred points of corrosion arriving
     * ten seconds later - and every one of them dies of the sting that started it, so what is left
     * after the swarm is a field of dying bees and a target with a great deal to think about.
     */
    BEES {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            RandomSource random = level.random;
            for (int i = 0; i < BEE_COUNT; i++) {
                CompressedBeeEntity bee = ModEntities.COMPRESSED_BEE.get().create(level);
                if (bee == null) {
                    continue;
                }

                bee.moveTo(tnt.getX() + (random.nextDouble() - 0.5) * BEE_SPREAD,
                        tnt.getY() + random.nextDouble() * BEE_SPREAD,
                        tnt.getZ() + (random.nextDouble() - 0.5) * BEE_SPREAD,
                        random.nextFloat() * 360.0F, 0.0F);
                // Nothing paid for these, so they persist rather than despawning the moment the
                // fight moves twenty blocks away - the same rule every summoned thing here follows.
                bee.setPersistenceRequired();
                level.addFreshEntity(bee);
            }

            level.sendParticles(ParticleTypes.FALLING_HONEY, tnt.getX(), tnt.getY() + 0.5, tnt.getZ(),
                    120, 2.0, 1.5, 2.0, 0.1);
            level.playSound(null, tnt.blockPosition(), SoundEvents.BEEHIVE_ENTER,
                    SoundSource.BLOCKS, 4.0F, 0.6F);
        }
    },

    /**
     * Landscaping. A disc of {@value #FLATTEN_RADIUS} blocks is given a floor of tier
     * {@value #FLATTEN_LEVEL} cobblestone and everything standing on that floor - up to the world's
     * ceiling, hill, tree, building and all - is taken away.
     * <p>
     * It breaks nothing itself and is not an explosion at all, which is the only way it could work:
     * the floor it lays is a hardened compression level with an explosion resistance of three and a
     * half million, so a blast strong enough to clear the hill would be a blast that could not put
     * the floor down afterwards.
     * <p>
     * It is by a wide margin the largest single edit in the mod - about eight hundred thousand
     * positions against a chunk fill's ninety-eight thousand - so all of it goes through
     * {@link DeferredFill}, a few layers a tick. See {@code DeferredFill.Flatten} for why the floor
     * is laid before the clear starts and why the clear runs downwards.
     */
    FLATTEN {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            DeferredFill.queueFlatten(level, tnt.blockPosition(), FLATTEN_RADIUS,
                    ModBlocks.byLevel(FLATTEN_LEVEL).get().defaultBlockState());

            level.playSound(null, tnt.blockPosition(), SoundEvents.ANVIL_LAND,
                    SoundSource.BLOCKS, 4.0F, 0.3F);
        }
    },

    /**
     * A small, dense knot of Damage Webs: every free position within {@value #DAMAGE_WEB_RADIUS}
     * blocks of where the fuse ran out, filled.
     * <p>
     * Three blocks is deliberately tiny next to the Web TNT's six hundred and forty, and the two are
     * not the same idea at a different size. That one is a hazard laid across a landscape and is
     * sampled because the volume cannot be walked; this is a trap laid on a doorway, so it is
     * <em>solid</em> - every position, walked outright, because a trap with holes in it is a trap
     * something walks through. A sphere of radius three is about 120 positions, which is nothing.
     * <p>
     * A web is only worth anything where something can walk into it, so only air is replaced -
     * nothing is broken to make room, and a charge set off inside a wall simply webs the gaps.
     */
    DAMAGE_WEBS {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            BlockState web = ModBlocks.DAMAGE_WEB.get().defaultBlockState();
            BlockPos centre = tnt.blockPosition();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            for (int x = -DAMAGE_WEB_RADIUS; x <= DAMAGE_WEB_RADIUS; x++) {
                for (int y = -DAMAGE_WEB_RADIUS; y <= DAMAGE_WEB_RADIUS; y++) {
                    for (int z = -DAMAGE_WEB_RADIUS; z <= DAMAGE_WEB_RADIUS; z++) {
                        if (x * x + y * y + z * z > DAMAGE_WEB_RADIUS * DAMAGE_WEB_RADIUS) {
                            continue;
                        }

                        pos.set(centre.getX() + x, centre.getY() + y, centre.getZ() + z);
                        if (level.isLoaded(pos) && level.getBlockState(pos).isAir()) {
                            level.setBlockAndUpdate(pos.immutable(), web);
                        }
                    }
                }
            }

            level.playSound(null, centre, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 4.0F, 0.4F);
        }
    },

    /**
     * One beam out of a fully augmented Ray of Laser, at whatever living thing is nearest.
     * <p>
     * The laser it fires is not a weapon anybody owns: the stack is built here, given one augment
     * out of every family at the deepest rung that family has, fired once and thrown away. That is
     * the whole design of the charge - it is the only way in the mod to be on the receiving end of
     * an *everything* laser, and it costs a block of tier {@value #LASER_LEVEL} stone rather than
     * the fifteen augments it is imitating.
     * <p>
     * Nothing about which augments exist is written here. {@link Augment#values()} is walked and the
     * deepest of each {@link Augment.Family} is kept, so an augment added tomorrow is on this beam
     * tomorrow - which is the same rule the Random TNT follows about the TNT tag.
     * <p>
     * The beam is drawn as a line of particles rather than by {@code LaserBeamRenderer}: that
     * renderer draws from a hand, off the fact that somebody is holding an item, and there is no
     * hand here. The hit is landed through {@code RayOfLaserItem.land}, which is the same call a
     * player's beam makes.
     */
    LASER {
        @Override
        public void detonate(ServerLevel level, CompressedPrimedTntEntity tnt) {
            Vec3 from = tnt.position().add(0.0, 0.5, 0.0);
            LivingEntity target = nearestLiving(level, from, RayOfLaserItem.RANGE, tnt.getOwner());
            if (target == null) {
                level.playSound(null, tnt.blockPosition(), SoundEvents.GUARDIAN_ATTACK,
                        SoundSource.BLOCKS, 4.0F, 0.6F);
                return;
            }

            Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
            drawBeam(level, from, to);

            // Credited to whoever placed the charge where a player did, which is what makes the kill
            // theirs; a charge lit by redstone has no owner and the beam is nobody's.
            RayOfLaserItem.land(level, fullyAugmentedLaser(), target,
                    tnt.getOwner() instanceof LivingEntity owner ? owner : null);

            level.playSound(null, tnt.blockPosition(), SoundEvents.GUARDIAN_ATTACK,
                    SoundSource.BLOCKS, 4.0F, 0.6F);
        }
    };


    /**
     * How far the Flat TNT reaches. Thirty blocks is a disc of about two thousand eight hundred
     * columns, which is the largest single edit in the mod once it is multiplied by the world's
     * height - and is a build site rather than a decoration, which is what the charge is for.
     */
    private static final int FLATTEN_RADIUS = 30;

    /** What it lays the floor out of: the tier its own block is crafted from. */
    private static final int FLATTEN_LEVEL = 229;

    /**
     * How far the Damage Web TNT throws its webs. Three, and it is meant to be small - see the
     * constant's own note for why this one is solid where the Web TNT is sampled.
     */
    private static final int DAMAGE_WEB_RADIUS = 3;

    /** The tier the Laser TNT is cut from, quoted in its note. */
    private static final int LASER_LEVEL = 233;

    /** How many points the beam's particle line is drawn with, per block of its length. */
    private static final double LASER_BEAM_DENSITY = 2.0;

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

    /** Roughly how many apples go up; the actual throw varies by ten either way. */
    private static final int APPLE_COUNT = 100;

    /**
     * How many times {@link #RANDOM} will draw from the tag before giving up and exploding plainly.
     * A draw only fails when it lands on the Random TNT itself, so a handful is far more than the
     * odds ever need.
     */
    private static final int RANDOM_TNT_DRAWS = 16;

    /**
     * The three {@link #RANDOM} will never draw. Itself, because drawing it would only draw again,
     * and the two collapses, because either one deletes the sphere the drawn TNT would have gone
     * off in - a random draw that half the time is "everything nearby is gone" is not a draw.
     */
    private static final java.util.Set<CompressedTntEffect> NEVER_DRAWN =
            java.util.EnumSet.of(RANDOM, BLACKHOLE, SINGULARITY);

    /** The fuse {@link #RANDOM} leaves on the TNT it lit: one tick, so it goes off effectively now. */
    private static final int COPIED_FUSE = 1;

    /** Roughly how many trident books go up; the actual throw varies by four either way. */
    private static final int BOOK_COUNT = 24;

    /** How far the Binding TNT reaches for someone to re-dress. */
    private static final double BINDING_RADIUS = 8.0;

    /** The four slots the Binding TNT empties, and what it fills them with. */
    private static final List<EquipmentSlot> ARMOR_SLOTS =
            List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private static final Map<EquipmentSlot, Item> LEATHER_ARMOR = Map.of(
            EquipmentSlot.HEAD, Items.LEATHER_HELMET,
            EquipmentSlot.CHEST, Items.LEATHER_CHESTPLATE,
            EquipmentSlot.LEGS, Items.LEATHER_LEGGINGS,
            EquipmentSlot.FEET, Items.LEATHER_BOOTS);

    private static final int ANVIL_COUNT = 300;
    private static final int ANVIL_RADIUS = 20;

    private static final int BULLET_COUNT = 30;

    /** How far the bullets look for something to chase, and for a shooter to be fired by. */
    private static final double BULLET_RADIUS = 24.0;

    /** How far the grove reaches. */
    private static final int GROVE_RADIUS = 16;

    /** How far the soil rolls above and below the blast. */
    private static final int GROVE_RELIEF = 4;

    /** How many trees are planted on it, which the ground will not always have room for. */
    private static final int GROVE_TREES = 20;

    private static final int ARROW_BURST_COUNT = 60;

    /** Fast enough to carry, and scattered enough that the burst is a cloud rather than a star. */
    private static final float ARROW_BURST_VELOCITY = 1.6F;
    private static final float ARROW_BURST_SPREAD = 6.0F;

    /** The blast both arrow TNTs make: it hurts, and it leaves the ground exactly as it found it. */
    private static final float ARROW_BURST_BLAST = 4.0F;

    /** Eight groups of four of the same species: thirty-two animals, in eight breeding pairs of pairs. */
    /** How wide the Dome TNT's dome is, and which tier of stone it is built out of. */
    private static final int DOME_RADIUS = 24;
    private static final int DOME_LEVEL = 241;

    /** How long the Singularity's black hole takes to run its whole curve: five seconds. */
    private static final int SINGULARITY_TICKS = 100;

    /**
     * The largest power this mod will fire as one explosion, and the reason there is a ceiling at
     * all.
     * <p>
     * It is not a taste decision and it is not vanilla's: vanilla has no cap, and a radius is a
     * float that would happily hold a million. Two costs are what set it. The block phase is
     * sixteen cubed rays each stepping a fifth of a block, which is unbounded work in the radius -
     * that one is answered outright by the calculator in {@code DeferredFill}, which kills every ray
     * on its first step, so it does not bind here. What does bind is the entity phase: vanilla
     * builds an axis-aligned box of {@code radius * 4} on a side and asks the level for everything
     * in it, and the scan walks one section column per sixteen blocks of that box's width. At this
     * figure that is eight thousand columns - the same order of work as one tick of any
     * {@code DeferredFill} job, which is the budget this mod has already decided a tick can carry.
     * <p>
     * Being honest about what that buys: {@code 32768} is a reach of 65,536 blocks, which is every
     * loaded entity in the dimension whatever the render distance, hurt for
     * {@code 7 * 65536 + 1} at the middle and falling off linearly from there. The cap is not a
     * softening of this charge. It is the point past which a larger number stops meaning anything
     * a world can express.
     */
    public static final float MAX_POWER = 32768.0F;

    /**
     * What the Singularity ends on: ten thousand sticks of TNT, or {@link #MAX_POWER}, whichever is
     * smaller. Today that is the ceiling, and by some margin.
     */
    private static final float SINGULARITY_POWER =
            Math.min(MAX_POWER, 10000 * CompressedPrimedTntEntity.VANILLA_POWER);

    private static final int BREEDING_GROUPS = 8;
    private static final int BREEDING_GROUP_SIZE = 4;

    /** How far apart the groups are dropped, and how far apart the four within a group are. */
    private static final int BREEDING_SPREAD = 8;
    private static final int BREEDING_HUDDLE = 2;

    /** Which entity types the Breeding TNT may spawn, worked out once and kept. */
    @Nullable
    private static List<EntityType<?>> breedableTypes;

    private static final double EFFECT_RADIUS = 12.0;

    /** Thirty seconds of everything. */
    private static final int EFFECT_DURATION = 600;

    /** Five seconds of spiral, a dozen arrows a tick, climbing and quickening throughout. */
    private static final int SPIRAL_TICKS = 100;
    private static final int SPIRAL_ARROWS_PER_TICK = 12;
    private static final float SPIRAL_START_VELOCITY = 0.8F;
    private static final float SPIRAL_END_VELOCITY = 2.4F;

    /** Which song the Lightning TNT plays: a one-note reduction, since plain lightning has no pitch. */
    private static final ResourceLocation SONG = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID, "lightning_song");

    /** What the second one plays: the same piece with its notes, on the note bolts. */
    private static final ResourceLocation SONG_2 = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID, "railgun_notes");

    /** What the Moonlight TNT plays. The Moonlight Bolt plays the same file from a core. */
    private static final ResourceLocation MOONLIGHT_SONG = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID, "moonlight");

    /** Half a minute of being knocked about, which is the whole of the Pinball TNT. */
    private static final int PINBALL_TICKS = 600;

    /** How far out its bursts are let off, and how far above and below. */
    private static final double PINBALL_RADIUS = 10.0;

    /** How far the pool reaches: three blocks out from where the fuse ran out, and flat. */
    private static final int POOL_RADIUS = 3;

    /**
     * How long the third chicken rung's wind runs: the Pinball TNT's own half minute. The flock is
     * down inside the first ten seconds of it, so the wind is what happens to the birds after they
     * land rather than something that only catches the last of them.
     */
    private static final int CHICKEN_3_WIND_TICKS = PINBALL_TICKS;

    /**
     * The doorway the Portal TNT raises: vanilla's own smallest portal, two blocks wide and three
     * tall on the inside. The frame is one block out from that on every side.
     */
    private static final int PORTAL_INNER_WIDTH = 2;
    private static final int PORTAL_INNER_HEIGHT = 3;

    /** How many skulls come out of it, and how far off straight ahead each one may wander. */
    private static final int PORTAL_SKULLS = 24;
    private static final double PORTAL_SKULL_SPREAD = 0.5;

    /** How many spawn eggs go up with the spawner. */
    private static final int SPAWNER_EGGS = 6;

    /** Every vanilla spawn egg, worked out once and kept. */
    @Nullable
    private static List<Item> vanillaEggs;

    /** How tall the two spouts stand, in blocks: see {@link #GEYSER_2} for why they differ. */
    private static final int GEYSER_HEIGHT = 40;
    private static final int LAVA_GEYSER_HEIGHT = 24;

    /** How many ordinary silverfish the first nest holds, and how far they are scattered. */
    private static final int SILVERFISH_COUNT = 60;
    private static final int SILVERFISH_SPREAD = 8;

    /**
     * And the second. Fewer, because each one flies, crosses ground three times as fast and bites
     * through armour - and because every one that lands a bite turns whatever it bit into a source
     * of more of them.
     */
    /** How many bees come out of a Bee-nt, and how wide the cloud of them is thrown. */
    private static final int BEE_COUNT = 40;
    private static final double BEE_SPREAD = 8.0;

    private static final int COMPRESSED_SILVERFISH_COUNT = 24;
    private static final double COMPRESSED_SILVERFISH_SPREAD = 8.0;

    /** How far the Snow TNT's cold reaches, and how long what it catches keeps it. */
    private static final double SNOW_RADIUS = 20.0;
    private static final int SNOW_FREEZE_TICKS = 300;

    /** How far the Zoom TNT reaches, and how long what it catches keeps it. */
    private static final double ZOOM_RADIUS = 16.0;
    private static final int ZOOM_DURATION = 1200;

    /** Speed X: vanilla's effect is one-based in its name and zero-based in its amplifier. */
    private static final int ZOOM_SPEED_AMPLIFIER = 9;

    /** Roughly how many bolts the Bolt TNT throws; the actual throw varies by four either way. */
    private static final int BOLT_COUNT = 24;

    /** Every bolt item in the game, worked out once and kept. */
    @Nullable
    private static List<Item> bolts;

    /**
     * How wide it plays it. Tighter than it looks like it should be, and for the music rather than
     * for the light: a bolt quiet enough to be a quiet note carries a flat sixteen blocks and fades
     * over that distance, so bolts scattered any further apart than this would lose more of the
     * song's dynamics to where the listener stands than the dynamics themselves are worth. Where
     * that limit actually bites is on the listener's distance from the ring, not on the ring's own
     * width, so this is as wide as it can be while a note struck on the far side of it still reaches
     * someone standing at the near side at most a quarter down.
     */
    public static final double SONG_RADIUS = 7.0;

    private static final double FLASH_RADIUS = 24.0;
    private static final int FLASH_BLINDNESS = 200;
    private static final int FLASH_NAUSEA = 300;

    /** Ten times a wind charge's 1.22, which is what makes the second flock a launch rather than a shove. */
    private static final ExplosionDamageCalculator WIND_BURST_10X =
            new SimpleExplosionDamageCalculator(false, false, Optional.of(1.22F * 10.0F), Optional.empty());

    /**
     * How far the Pillar TNT reaches and how thickly it plants, and what it plants.
     * <p>
     * Fifty blocks is a little over three chunks each way, and one column in two hundred and fifty
     * of a disc that size is about thirty pillars. The level is the same one the TNT is crafted out
     * of, which is the joke and also the price: nothing below the tier 2 pickaxe will take a single
     * one of them back down.
     */
    private static final int PILLAR_RADIUS = 50;
    private static final int PILLAR_CHANCE = 250;
    private static final int PILLAR_LEVEL = 213;

    /**
     * Which village belongs in the biome at {@code pos}, by the same tags worldgen decides with.
     * <p>
     * Reading the tags rather than switching on biome ids is what makes another mod's savanna get a
     * savanna village for free. Plains is the fallback rather than a failure: a biome that carries
     * none of the five - a mushroom island, the Nether, another mod's anything - still gets a
     * village, because "the fuse ran out and nothing happened" is the one outcome worth ruling out.
     */
    private static ResourceKey<Structure> villageFor(ServerLevel level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);

        if (biome.is(BiomeTags.HAS_VILLAGE_DESERT)) {
            return BuiltinStructures.VILLAGE_DESERT;
        }
        if (biome.is(BiomeTags.HAS_VILLAGE_SAVANNA)) {
            return BuiltinStructures.VILLAGE_SAVANNA;
        }
        if (biome.is(BiomeTags.HAS_VILLAGE_SNOWY)) {
            return BuiltinStructures.VILLAGE_SNOWY;
        }
        if (biome.is(BiomeTags.HAS_VILLAGE_TAIGA)) {
            return BuiltinStructures.VILLAGE_TAIGA;
        }

        return BuiltinStructures.VILLAGE_PLAINS;
    }

    /**
     * The shared half of the structure TNTs: generate it here, build it over the ticks that follow.
     */
    private static void structure(ServerLevel level, CompressedPrimedTntEntity tnt,
                                  ResourceKey<Structure> key, SoundEvent sound) {
        BlockPos origin = tnt.blockPosition();
        place(level, origin, DeferredFill.generateStructure(level, origin, key), sound);
    }

    /**
     * Hands a start to {@link DeferredFill} and says which way it went.
     * <p>
     * A structure declining to generate is a real outcome rather than a bug - see the mansion's y 60
     * rule - so it gets a sound of its own instead of silence, which is the only thing standing
     * between a player and "the TNT is broken".
     */
    private static void place(ServerLevel level, BlockPos origin, StructureStart start, SoundEvent sound) {
        if (start.isValid()) {
            DeferredFill.queueStructure(level, start);
            level.playSound(null, origin, sound, SoundSource.BLOCKS, 4.0F, 1.0F);
        } else {
            level.playSound(null, origin, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 3.0F, 0.6F);
        }
    }

    /**
     * An ocean monument built without vanilla's ocean check: its one piece, made by hand.
     * <p>
     * This is {@code OceanMonumentStructure.createTopPiece} plus the scaffolding
     * {@code regeneratePiecesAfterLoad} puts around it, and nothing else - the monument is a single
     * {@code MonumentBuilding} piece that lays out its own rooms, so there is no generation to
     * reimplement and no chance of the pieces differing from vanilla's. Only the biome test is left
     * out, because that is the whole reason for being here.
     */
    private static StructureStart sunkenMonument(ServerLevel level, BlockPos origin) {
        Optional<Holder.Reference<Structure>> holder = level.registryAccess()
                .lookupOrThrow(Registries.STRUCTURE).get(BuiltinStructures.OCEAN_MONUMENT);
        if (holder.isEmpty()) {
            return StructureStart.INVALID_START;
        }

        ChunkPos chunk = new ChunkPos(origin);
        // The starting seed is thrown away by setLargeFeatureSeed on the next line - which is what
        // makes the temple's layout deterministic per chunk, the way a naturally generated one is.
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
        random.setLargeFeatureSeed(level.getSeed(), chunk.x, chunk.z);

        // The piece is anchored twenty-nine blocks back from the chunk corner, which is what puts
        // the fifty-eight block temple centred on the chunk the fuse ran out in.
        StructurePiecesBuilder pieces = new StructurePiecesBuilder();
        pieces.addPiece(new OceanMonumentPieces.MonumentBuilding(random,
                chunk.getMinBlockX() - OceanMonumentPieces.MonumentBuilding.BIOME_RANGE_CHECK,
                chunk.getMinBlockZ() - OceanMonumentPieces.MonumentBuilding.BIOME_RANGE_CHECK,
                Direction.Plane.HORIZONTAL.getRandomDirection(random)));

        return new StructureStart(holder.get().value(), chunk, 0, pieces.build());
    }

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

    /**
     * Every bolt there is, found by walking the item registry rather than by listing them: there are
     * eighty-nine of the mod's own and anything another mod registers as a {@link BoltItem} belongs
     * in the draw too. Worked out on the first blast rather than at class-init, when the registry is
     * not yet built, and kept from then on.
     */
    private static List<Item> bolts() {
        if (bolts == null) {
            bolts = BuiltInRegistries.ITEM.stream().filter(item -> item instanceof BoltItem).toList();
        }

        return bolts;
    }

    /** A flower or a dye, drawn from the tags so other mods' additions come along too. */
    private static ItemStack randomBloom(ServerLevel level) {
        TagKey<Item> tag = level.random.nextBoolean() ? ItemTags.FLOWERS : Tags.Items.DYES;
        return randomFrom(level, tag, Items.POPPY);
    }

    /**
     * Every enchantment in the game that will go on {@code item}. Read off the registry rather than
     * listed, so another mod's trident enchantments are in the draw without anything here changing.
     */
    private static List<Holder<Enchantment>> enchantmentsFor(ServerLevel level, Item item) {
        Holder<Item> holder = item.builtInRegistryHolder();

        return level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).holders()
                .filter(enchantment -> enchantment.value().definition().supportedItems().contains(holder))
                .map(enchantment -> (Holder<Enchantment>) enchantment)
                .toList();
    }

    /**
     * The blast the arrow TNTs make: entities only, never blocks, and scaled by whatever the primed
     * entity is carrying - which is one for a TNT a player lit and more for one an inscribed Arrow
     * TNT Staff threw.
     * <p>
     * {@code ExplosionInteraction.NONE} is what saves the ground; the calculator only decides how
     * hard the thing hits, since its {@code shouldDamageEntity} is true by default.
     */
    private static void arrowBlast(ServerLevel level, CompressedPrimedTntEntity tnt, float radius) {
        float scale = tnt.getDamageScale();
        ExplosionDamageCalculator calculator = new ExplosionDamageCalculator() {
            @Override
            public float getEntityDamageAmount(Explosion explosion, Entity entity) {
                return super.getEntityDamageAmount(explosion, entity) * scale;
            }
        };

        level.explode(tnt, Explosion.getDefaultDamageSource(level, tnt), calculator,
                tnt.getX(), tnt.getY(), tnt.getZ(), radius, false, Level.ExplosionInteraction.NONE);
    }

    /** One Super Compressed Arrow at the TNT, credited to whoever lit it. */
    /**
     * One vanilla tipped arrow at the point the TNT went off, at the Super Compressed arrow's
     * damage. It carries a real {@code minecraft:tipped_arrow} as its pickup stack, so whatever
     * effects are added to it are on the item that is picked up as well as on the arrow in flight -
     * {@code Arrow#addEffect} writes them straight onto that stack, and the colour it works out for
     * the flight and the particles is read off the same place.
     */
    private static Arrow effectArrow(ServerLevel level, CompressedPrimedTntEntity tnt) {
        ItemStack ammo = new ItemStack(Items.TIPPED_ARROW);
        LivingEntity owner = tnt.getOwner();
        Arrow arrow = owner != null
                ? new Arrow(level, owner, ammo, null)
                : new Arrow(level, tnt.getX(), tnt.getY() + 0.5, tnt.getZ(), ammo, null);

        arrow.setPos(tnt.getX(), tnt.getY() + 0.5, tnt.getZ());
        arrow.setBaseDamage(CompressedArrowTier.SUPER.baseDamage());
        return arrow;
    }

    private static CompressedArrowEntity superArrow(ServerLevel level, CompressedPrimedTntEntity tnt) {
        ItemStack ammo = new ItemStack(CompressedArrowTier.SUPER.item().get());
        LivingEntity owner = tnt.getOwner();
        CompressedArrowEntity arrow = owner != null
                ? new CompressedArrowEntity(CompressedArrowTier.SUPER, level, owner, ammo, null)
                : new CompressedArrowEntity(CompressedArrowTier.SUPER, level, tnt.getX(), tnt.getY() + 0.5,
                        tnt.getZ(), ammo, null);

        arrow.setPos(tnt.getX(), tnt.getY() + 0.5, tnt.getZ());
        return arrow;
    }

    /**
     * Everything in the game that can be bred, worked out by asking the entity registry rather than
     * by listing anything: a type qualifies if one of it is an {@link Animal}. Another mod's
     * creatures are therefore in the draw automatically, and this mod's own are kept out of it -
     * golems, creepers and conjurers are not livestock - with the one exception of its chicken.
     * <p>
     * The list is worked out once and kept, because the only way to ask whether a type is an animal
     * is to build one and look, and doing that for every type in the game is not a per-detonation
     * cost.
     */
    private static List<EntityType<?>> breedableTypes(ServerLevel level) {
        List<EntityType<?>> cached = breedableTypes;
        if (cached != null) {
            return cached;
        }

        List<EntityType<?>> found = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            boolean ours = id.getNamespace().equals(UnnecessarilyCompressedCobblestone.MOD_ID);
            if (ours && type != ModEntities.COMPRESSED_COBBLESTONE_CHICKEN.get()) {
                continue;
            }

            Entity sample = type.create(level);
            if (sample instanceof Animal) {
                found.add(type);
            }

            if (sample != null) {
                sample.discard();
            }
        }

        breedableTypes = List.copyOf(found);
        return breedableTypes;
    }

    /**
     * Every spawn egg vanilla ships, worked out on first use and kept. {@code SpawnEggItem.eggs()}
     * is the registry's own list, so it holds modded eggs too - the namespace test is what leaves
     * them out, and it is the one place in this file that reads a namespace rather than a tag.
     */
    private static List<Item> vanillaEggs() {
        List<Item> cached = vanillaEggs;
        if (cached != null) {
            return cached;
        }

        List<Item> found = new ArrayList<>();
        for (SpawnEggItem egg : SpawnEggItem.eggs()) {
            if (BuiltInRegistries.ITEM.getKey(egg).getNamespace().equals("minecraft")) {
                found.add(egg);
            }
        }

        vanillaEggs = List.copyOf(found);
        return vanillaEggs;
    }

    /** One item out of a tag, or {@code fallback} if the tag is empty or missing. */
    private static ItemStack randomFrom(ServerLevel level, TagKey<Item> tag, Item fallback) {
        Optional<Holder<Item>> picked = BuiltInRegistries.ITEM.getTag(tag)
                .flatMap((HolderSet.Named<Item> set) -> set.getRandomElement(level.random));

        return picked.map(holder -> new ItemStack(holder.value())).orElseGet(() -> new ItemStack(fallback));
    }

    /**
     * Puts {@code block} down for an instant, lights it, and puts back whatever was there. Lighting
     * goes through {@code onCaughtFire}, which is the same door fire, redstone and a flaming arrow
     * come through, so any TNT-like block primes exactly as it would in a player's hands.
     * <p>
     * Whatever that primed is then cut down to {@link #COPIED_FUSE}. Only entities that have not
     * ticked yet are touched, so TNT someone else lit a moment ago and left lying next to this one
     * keeps the fuse it was given.
     */
    /**
     * Draws one TNT out of {@code #ucc:tnt} and lights it at {@code pos}.
     * <p>
     * Package private rather than private to this enum: the Compressed Creeper Tier 2's summon is
     * the same draw from the same tag, and a second copy of it would be a second place a datapack's
     * addition could fail to reach. Returns false when the tag holds nothing but the Random TNT
     * itself, or does not exist, which is the caller's cue to do something else.
     */
    static boolean lightRandom(ServerLevel level, BlockPos pos, @Nullable LivingEntity owner) {
        for (int draw = 0; draw < RANDOM_TNT_DRAWS; draw++) {
            Optional<Holder<Block>> drawn = BuiltInRegistries.BLOCK.getTag(ModTags.Blocks.TNT)
                    .flatMap((HolderSet.Named<Block> set) -> set.getRandomElement(level.random));
            if (drawn.isEmpty()) {
                return false;
            }

            Block block = drawn.get().value();
            // Drawing the Random TNT would only ever draw again, and the two collapses take the
            // ground out from under whatever else is going on for a hundred ticks, so all three are
            // skipped here rather than left out of the tag - anything else reading that tag should
            // still see them.
            if (block instanceof CompressedTntBlock compressed && NEVER_DRAWN.contains(compressed.getEffect())) {
                continue;
            }

            lightInPlace(level, pos, block, owner);
            return true;
        }

        return false;
    }

    /**
     * Public rather than private to this enum for the same reason {@link #lightRandom} is package
     * private: the TNT Launcher's shell lands on it too, and a second copy of the trick would be a
     * second place another mod's TNT could fail to light.
     */
    public static void lightInPlace(ServerLevel level, BlockPos pos, Block block, @Nullable LivingEntity owner) {
        BlockState previous = level.getBlockState(pos);
        BlockState lit = block.defaultBlockState();

        // UPDATE_INVISIBLE, and put back the same way: the block exists for part of one tick purely
        // so that it can be lit, and no client should ever see it appear.
        level.setBlock(pos, lit, Block.UPDATE_INVISIBLE);
        lit.onCaughtFire(level, pos, null, owner);
        if (level.getBlockState(pos) == lit) {
            level.setBlock(pos, previous, Block.UPDATE_INVISIBLE);
        }

        for (PrimedTnt primed : level.getEntitiesOfClass(PrimedTnt.class, new AABB(pos).inflate(1.0))) {
            if (primed.tickCount == 0 && primed.getFuse() > COPIED_FUSE) {
                primed.setFuse(COPIED_FUSE);
            }
        }
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
     * A Ray of Laser carrying one augment out of every family, at that family's deepest rung.
     * <p>
     * Built by walking {@link Augment#values()} rather than by listing anything, so a new augment
     * joins this beam the day it is added. "Deepest" is by {@link Augment#level()}, which is the
     * rung number for the two families that have rungs and zero for the thirteen that are single
     * constants - so the effect families each contribute their one member and the comparison never
     * has to know which sort it is looking at.
     */
    private static ItemStack fullyAugmentedLaser() {
        ItemStack laser = new ItemStack(ModItems.RAY_OF_LASER.get());

        Map<Augment.Family, Augment> deepest = new EnumMap<>(Augment.Family.class);
        for (Augment augment : Augment.values()) {
            deepest.merge(augment.family(), augment,
                    (held, candidate) -> candidate.level() > held.level() ? candidate : held);
        }

        for (Augment augment : deepest.values()) {
            Augments.add(laser, augment);
        }

        return laser;
    }

    /**
     * Whatever living thing is nearest {@code from} within {@code range}, excluding {@code except}
     * and anything that is not there to be hit.
     * <p>
     * Armour stands are left out for the reason every targeting rule in this mod leaves them out -
     * they are furniture - and so is anything a beam could not see the point of hitting, by the same
     * test the laser's own aim uses.
     */
    @Nullable
    private static LivingEntity nearestLiving(ServerLevel level, Vec3 from, double range,
                                              @Nullable Entity except) {
        LivingEntity nearest = null;
        double best = range * range;

        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(from, from).inflate(range),
                entity -> entity.isAlive() && !entity.isSpectator() && entity.isPickable())) {
            if (candidate == except) {
                continue;
            }

            double distance = candidate.distanceToSqr(from);
            if (distance < best) {
                best = distance;
                nearest = candidate;
            }
        }

        return nearest;
    }

    /** The beam, as a line of bubbles: there is no hand for the laser's own renderer to draw from. */
    private static void drawBeam(ServerLevel level, Vec3 from, Vec3 to) {
        int points = Math.max(1, (int) (from.distanceTo(to) * LASER_BEAM_DENSITY));
        for (int i = 0; i <= points; i++) {
            Vec3 at = from.lerp(to, (double) i / points);
            level.sendParticles(ParticleTypes.BUBBLE_POP, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
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
