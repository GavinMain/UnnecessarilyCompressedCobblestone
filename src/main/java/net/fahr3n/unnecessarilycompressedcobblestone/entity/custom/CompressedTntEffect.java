package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CompressedTntBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
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
import net.minecraft.world.entity.projectile.ShulkerBullet;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
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

            for (int draw = 0; draw < RANDOM_TNT_DRAWS; draw++) {
                Optional<Holder<Block>> drawn = BuiltInRegistries.BLOCK.getTag(ModTags.Blocks.TNT)
                        .flatMap((HolderSet.Named<Block> set) -> set.getRandomElement(level.random));
                if (drawn.isEmpty()) {
                    break;
                }

                Block block = drawn.get().value();
                // Drawing itself would only ever draw again, so it is skipped here rather than left
                // out of the tag - anything else reading that tag should still see the Random TNT.
                if (block instanceof CompressedTntBlock compressed && compressed.getEffect() == RANDOM) {
                    continue;
                }

                lightInPlace(level, pos, block, tnt.getOwner());
                return;
            }

            // Nothing in the tag but itself, or no tag at all.
            BLAST_5X.detonate(level, tnt);
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

    /** Roughly how many apples go up; the actual throw varies by ten either way. */
    private static final int APPLE_COUNT = 100;

    /**
     * How many times {@link #RANDOM} will draw from the tag before giving up and exploding plainly.
     * A draw only fails when it lands on the Random TNT itself, so a handful is far more than the
     * odds ever need.
     */
    private static final int RANDOM_TNT_DRAWS = 16;

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

    /** Which song the Lightning TNT plays. */
    private static final ResourceLocation SONG = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID, "lightning_song");

    /**
     * How wide it plays it. Tighter than it looks like it should be, and for the music rather than
     * for the light: a bolt quiet enough to be a quiet note carries a flat sixteen blocks and fades
     * over that distance, so bolts scattered any further apart than this would lose more of the
     * song's dynamics to where the listener stands than the dynamics themselves are worth.
     */
    public static final double SONG_RADIUS = 4.0;

    private static final double FLASH_RADIUS = 24.0;
    private static final int FLASH_BLINDNESS = 200;
    private static final int FLASH_NAUSEA = 300;

    /** Ten times a wind charge's 1.22, which is what makes the second flock a launch rather than a shove. */
    private static final ExplosionDamageCalculator WIND_BURST_10X =
            new SimpleExplosionDamageCalculator(false, false, Optional.of(1.22F * 10.0F), Optional.empty());

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
    private static void lightInPlace(ServerLevel level, BlockPos pos, Block block, @Nullable LivingEntity owner) {
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
