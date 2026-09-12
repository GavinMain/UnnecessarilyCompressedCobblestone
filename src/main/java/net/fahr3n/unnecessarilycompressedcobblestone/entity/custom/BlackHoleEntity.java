package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.ArrayList;
import java.util.List;

import net.fahr3n.unnecessarilycompressedcobblestone.damage.ModDamageTypes;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.fahr3n.unnecessarilycompressedcobblestone.util.DeferredFill;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

/**
 * The black hole a Blackhole TNT leaves behind: two hours of a thing that eats the world.
 * <p>
 * It does three things, and all three grow with its age:
 * <ul>
 * <li><b>It eats blocks.</b> Everything within {@link #eatRadius} is deleted outright - no drops, no
 *     falling - out to {@value #FINAL_EAT_RADIUS} blocks by the end, which is a hundred times a
 *     TNT's power of four. The radius grows <em>exponentially</em>, in two legs: from
 *     {@value #MIN_EAT_RADIUS} to {@value #MID_EAT_RADIUS} over the first half of its life, and from
 *     there to the full figure over the second, so most of the ground it takes goes in the last
 *     quarter. The sphere is eaten one integer shell at a
 *     time from the middle outwards, and the walk carries a position budget and a cursor so no tick
 *     does more than {@value #POSITIONS_PER_TICK} block reads however large the shell has become -
 *     which means that at the very end, when the radius runs fastest, the eating can fall a little
 *     behind it. Shells are clipped to the build height, which is what makes the figure affordable
 *     at all: the part of a 400 block sphere inside the world is a disc a few hundred blocks tall.
 * <li><b>It pulls entities.</b> Everything within {@link #suctionRadius} - which is the eating
 *     radius, so nothing outside the hole is touched - is drawn towards the middle. A mob is pulled
 *     harder the closer it is and the older the hole, and hard enough from the first tick to beat
 *     walking. A player is pulled at just under what they can sprint against in whatever state they
 *     are in, so running straight out always works, slowly. See {@link #pull}. A few of the blocks it eats are thrown in as falling blocks on the way, so the
 *     eating reads as a pull rather than as blocks winking out.
 * <li><b>It swallows what reaches it.</b> Inside the core - the black sphere, {@link #coreRadius} -
 *     loose things (items, orbs, falling blocks, projectiles, primed TNT) are deleted and everything
 *     else takes {@link ModDamageTypes#BLACK_HOLE} damage, which is true damage.
 * </ul>
 * Nothing hurts it but the {@link ModItems#BLACK_HOLE_STOPPER}: hit it or use it on the core with
 * one in the main hand and it collapses. {@code /kill} still works, since that is not an attack.
 * <p>
 * It is an entity rather than a {@code DeferredFill} job so that it saves - a two hour event that a
 * restart quietly cancelled would not be one - and so that it has a body something can be aimed at.
 * It pauses whenever its own chunk is not ticking. Ground that was unloaded when the eating passed
 * over it is not revisited.
 * <p>
 * <b>The Singularity</b> is the same hole with two numbers changed: a lifetime of a minute rather
 * than two hours, and a finale. Every figure above is read off progress through the lifetime, so a
 * short one walks the same growth to the same final radius sixty times as fast - except the eating,
 * which is held to the same per-tick budget and so reaches far less of that radius in a minute. With
 * a finale the hole does not simply vanish when its time is up: it becomes a {@value #FLASH_TICKS}
 * tick flash, a white sphere that swells and holds before it fades, and then
 * {@link DeferredFill#detonateSingularity} - the one explosion and the crater it leaves. Once the
 * flash has started the stopper no longer works.
 */
public class BlackHoleEntity extends Entity {
    /** Two hours: the Blackhole TNT's lifetime. */
    public static final int LIFETIME = 120 * 60 * 20;

    /** How long the flash before a finale lasts, and how wide it swells. */
    public static final int FLASH_TICKS = 100;
    public static final float FLASH_RADIUS = 48.0F;

    /** A hundred times a TNT's power, in blocks. */
    public static final double FINAL_EAT_RADIUS = 100.0 * CompressedPrimedTntEntity.VANILLA_POWER;

    /** How far it eats on its first tick, and half way through its life. See {@link #eatRadius}. */
    private static final double MIN_EAT_RADIUS = 6.0;
    private static final double MID_EAT_RADIUS = 70.0;

    /** The share of the life the radius takes to open up from nothing: five minutes of two hours. */
    private static final double EAT_RAMP = 1.0 / 24.0;

    /** The black sphere, on the first tick and on the last. */
    private static final double MIN_CORE_RADIUS = 1.0;
    private static final double FINAL_CORE_RADIUS = 16.0;

    /**
     * How hard a player is pulled, as a fraction of the acceleration they themselves can put down
     * sprinting in whatever state they are in - on foot, in the air, or flying. Just under one, so a
     * player who sprints straight away from the hole gets out, slowly, and one who does anything else
     * does not. It is matched against the player's own figure rather than a constant because the
     * three states differ by a factor of five: a pull set against running would be inescapable to
     * anyone mid-jump, and one set against a jump would be nothing to someone on foot.
     */
    private static final double PLAYER_PULL_FRACTION = 0.9;

    /** Vanilla's sprint multiplier on movement speed, and the air acceleration of a sprinting player. */
    private static final double SPRINT_MULTIPLIER = 1.3;
    private static final double SPRINT_AIR_ACCELERATION = 0.026;

    /**
     * How hard a mob is pulled at the middle, in blocks per tick of acceleration, at birth and at the
     * end.
     * <p>
     * The floor is the figure that matters. Something standing on the ground keeps only about 55% of
     * its horizontal speed each tick, so a steady pull of {@code a} settles at roughly {@code 2.2a}:
     * at the old 0.02 that was under a twentieth of a block a tick, slower than any mob walks, and a
     * mob simply walked out of it until the last minutes. At 0.08 the settled pull is about 0.18,
     * faster than a mob wanders - and still below a sprinting player's 0.28, so running away works
     * early and stops working late.
     */
    private static final double MIN_PULL = 0.08;
    private static final double FINAL_PULL = 0.2;

    /**
     * How far down the pull's falloff reaches at the edge of its reach, as a fraction of the pull at
     * the middle. A linear falloff to nothing meant that most of the area it covered barely pulled at
     * all; this keeps the outer ring worth something.
     */
    private static final double EDGE_PULL = 0.3;

    /**
     * The hop a mob standing on the ground is given, as a fraction of the pull, so that it is dragged
     * through the air rather than scraped along the floor against ground friction. Players are not
     * given it: on foot they keep their footing, and the answer to a black hole is to run.
     * <p>
     * The floor is what makes it a hop at all - gravity takes 0.08 a tick, so anything much below
     * 0.15 leaves the ground for a tick or less - and at 0.15 a mob spends about four ticks of every
     * bounce in the air, where drag keeps 91% of its speed rather than friction's 55%.
     */
    private static final double MOB_LIFT = 2.0;
    private static final double MIN_MOB_LIFT = 0.15;
    private static final double MAX_MOB_LIFT = 0.3;

    /** Nothing is dragged out of the last half block, or something at the middle jitters. */
    private static final double DEAD_ZONE = 0.5;

    /**
     * Block positions read per tick, a column's lookup counting as one. Crater's figure: well under
     * the ten thousand that reads as a freeze, since most positions inside the world are also writes.
     */
    private static final int POSITIONS_PER_TICK = 8000;

    /** How many eaten blocks may be thrown in as falling blocks each tick, and how near a player. */
    private static final int THROWN_PER_TICK = 6;
    private static final double THROWN_PLAYER_RANGE = 64.0;

    /** Ticks between bites of the core, and what a bite is worth. */
    private static final int SWALLOW_INTERVAL = 10;
    private static final float SWALLOW_MIN_DAMAGE = 4.0F;
    private static final float SWALLOW_HEALTH_FRACTION = 0.1F;

    /** How often the synced age is refreshed; the size only changes perceptibly over seconds. */
    private static final int SYNC_INTERVAL = 20;

    /** Deletes without neighbour updates, so the edge of the hole does not cascade water and sand in. */
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private static final EntityDataAccessor<Integer> DATA_AGE =
            SynchedEntityData.defineId(BlackHoleEntity.class, EntityDataSerializers.INT);

    /** Synced because every size the client draws is a fraction of it. */
    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(BlackHoleEntity.class, EntityDataSerializers.INT);

    /** Whether the hole ends in a flash and a blast; synced so the client knows to draw the flash. */
    private static final EntityDataAccessor<Boolean> DATA_FINALE =
            SynchedEntityData.defineId(BlackHoleEntity.class, EntityDataSerializers.BOOLEAN);

    private static final String TAG_AGE = "black_hole_age";
    private static final String TAG_LIFETIME = "black_hole_lifetime";
    private static final String TAG_FINALE_POWER = "black_hole_finale_power";
    private static final String TAG_SHELL = "black_hole_shell";
    private static final String TAG_DX = "black_hole_dx";
    private static final String TAG_DZ = "black_hole_dz";

    /** Counted on both sides, and snapped to the server's figure whenever it is synced. */
    private int age;

    /** The explosion a finale ends on, or zero for a hole that simply stops. Server side only. */
    private float finalePower;

    /** The eating cursor: which shell, and which column of it. */
    private int shell;
    private int dx = Integer.MIN_VALUE;
    private int dz = Integer.MIN_VALUE;

    /** How many eaten blocks have been thrown in on this tick. */
    private int thrownThisTick;

    public BlackHoleEntity(EntityType<? extends BlackHoleEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    /** The Blackhole TNT's hole: two hours, and then nothing. */
    public BlackHoleEntity(Level level, Vec3 pos) {
        this(level, pos, LIFETIME, 0.0F);
    }

    /**
     * @param lifetime    how many ticks the growth is walked over
     * @param finalePower the explosion it ends on after its flash, or zero to simply stop
     */
    public BlackHoleEntity(Level level, Vec3 pos, int lifetime, float finalePower) {
        this(ModEntities.BLACK_HOLE.get(), level);
        this.entityData.set(DATA_LIFETIME, Math.max(1, lifetime));
        this.entityData.set(DATA_FINALE, finalePower > 0.0F);
        this.finalePower = finalePower;
        setPos(pos.x, pos.y, pos.z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_AGE, 0);
        builder.define(DATA_LIFETIME, LIFETIME);
        builder.define(DATA_FINALE, false);
    }

    /* GROWTH */

    public int lifetime() {
        return this.entityData.get(DATA_LIFETIME);
    }

    /** How far through its life it is, 0 to 1. */
    public double progress() {
        return Mth.clamp(this.age / (double) lifetime(), 0.0, 1.0);
    }

    /** How far through the flash it is, 0 to 1, or -1 while it is still a black hole. */
    public float flashProgress(float partialTicks) {
        if (!this.entityData.get(DATA_FINALE) || this.age < lifetime()) {
            return -1.0F;
        }

        return Mth.clamp((this.age - lifetime() + partialTicks) / FLASH_TICKS, 0.0F, 1.0F);
    }

    private boolean flashing() {
        return flashProgress(0.0F) >= 0.0F;
    }

    /**
     * Two exponential legs rather than one: {@value #MIN_EAT_RADIUS} to {@value #MID_EAT_RADIUS} over
     * the first half of the life, then on to {@value #FINAL_EAT_RADIUS} over the second. A single
     * curve from 3 to 400 spent the whole first hour under forty blocks, which read as nothing
     * happening; this is twice that curve through the first half, and the knee in the middle is what
     * lets the end stay where it was.
     * <p>
     * The first {@value #EAT_RAMP} of the life is scaled up from nothing, so the hole opens from a
     * point rather than appearing six blocks wide: about a block after one minute of two hours, and
     * on the curve proper by five.
     */
    public double eatRadius() {
        double progress = progress();
        double radius = progress < 0.5
                ? MIN_EAT_RADIUS * Math.pow(MID_EAT_RADIUS / MIN_EAT_RADIUS, progress * 2.0)
                : MID_EAT_RADIUS * Math.pow(FINAL_EAT_RADIUS / MID_EAT_RADIUS, (progress - 0.5) * 2.0);

        return radius * Math.min(1.0, progress / EAT_RAMP);
    }

    public double coreRadius() {
        return exponential(MIN_CORE_RADIUS, FINAL_CORE_RADIUS);
    }

    /** {@code from} at birth, {@code to} at the end, multiplying by the same factor every tick between. */
    private double exponential(double from, double to) {
        return from * Math.pow(to / from, progress());
    }

    /** The pull reaches exactly as far as the eating does: nothing outside the hole is touched. */
    public double suctionRadius() {
        return eatRadius();
    }

    /** The box is the core, centred on the position rather than standing on it. */
    @Override
    protected AABB makeBoundingBox() {
        double size = coreRadius() * 2.0;
        return AABB.ofSize(position(), size, size, size);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_AGE.equals(key) && level().isClientSide()) {
            this.age = this.entityData.get(DATA_AGE);
        }

        if (DATA_AGE.equals(key) || DATA_LIFETIME.equals(key)) {
            setBoundingBox(makeBoundingBox());
        }
    }

    /* TICK */

    @Override
    public void tick() {
        super.tick();
        this.age++;

        if (!(level() instanceof ServerLevel level)) {
            return;
        }

        if (this.age >= lifetime()) {
            if (this.finalePower <= 0.0F) {
                collapse(level);
            } else {
                flash(level);
            }

            return;
        }

        if (this.age % SYNC_INTERVAL == 1) {
            this.entityData.set(DATA_AGE, this.age);
            setBoundingBox(makeBoundingBox());
        }

        double core = coreRadius();
        pull(level, core);
        eat(level);

        level.sendParticles(ParticleTypes.PORTAL, getX(), getY(), getZ(), 30,
                core * 1.5, core * 1.5, core * 1.5, 0.8);
        level.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 20,
                core * 0.8, core * 0.8, core * 0.8, 0.02);

        if (this.age % 80 == 0) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS,
                    4.0F, 0.3F);
        }
    }

    /**
     * The finale: {@value #FLASH_TICKS} ticks of light, and then the blast. The pull and the eating
     * have stopped; what is left is a white sphere the renderer swells over the core, and a storm of
     * flash particles to carry it past the edge of the entity's own drawing. The age is synced every
     * tick while this runs, since the flash is short enough that a second's drift would be seen.
     */
    private void flash(ServerLevel level) {
        int into = this.age - lifetime();
        this.entityData.set(DATA_AGE, this.age);

        if (into == 0) {
            setBoundingBox(makeBoundingBox());
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS,
                    8.0F, 0.5F);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS,
                    8.0F, 1.6F);
        }

        if (into % 2 == 0) {
            double spread = FLASH_RADIUS * Math.min(1.0, 0.25 + into / (double) FLASH_TICKS);
            level.sendParticles(ParticleTypes.FLASH, getX(), getY(), getZ(), 12, spread * 0.5, spread * 0.5,
                    spread * 0.5, 0.0);
            level.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 40, spread * 0.4, spread * 0.4,
                    spread * 0.4, 0.3);
        }

        if (into >= FLASH_TICKS) {
            DeferredFill.detonateSingularity(level, position(), this.finalePower);
            discard();
        }
    }

    /**
     * Every loaded entity in reach, drawn in; everything at the core, swallowed. The whole entity
     * list is walked rather than queried by box, because by the end the reach is five hundred blocks
     * and a box that size is tens of thousands of chunk sections, where the list is only as long as
     * the number of entities.
     * <p>
     * Two things make it actually drag mobs in rather than nudge them: a floor under the strength
     * ({@link #MIN_PULL}) that beats walking, and a falloff that keeps {@link #EDGE_PULL} of it at
     * the edge rather than dropping to nothing. A mob on the ground is also given a small hop, so
     * the pull carries it through the air instead of losing most of itself to friction every tick.
     */
    private void pull(ServerLevel level, double core) {
        Vec3 centre = position();
        double reach = suctionRadius();
        double strength = MIN_PULL + (FINAL_PULL - MIN_PULL) * progress();
        boolean bite = this.age % SWALLOW_INTERVAL == 0;

        // Copied first: swallowing kills things, and a death spawns drops into the list being read.
        List<Entity> entities = new ArrayList<>();
        level.getAllEntities().forEach(entities::add);

        for (Entity entity : entities) {
            if (entity == this || entity instanceof BlackHoleEntity || entity instanceof PartEntity<?>
                    || entity.isSpectator() || !entity.isAlive()) {
                continue;
            }

            Vec3 toCentre = centre.subtract(entity.getBoundingBox().getCenter());
            double distanceSqr = toCentre.lengthSqr();
            if (distanceSqr > reach * reach) {
                continue;
            }

            double distance = Math.sqrt(distanceSqr);
            if (distance <= core) {
                swallow(level, entity, bite);
                if (!entity.isAlive()) {
                    continue;
                }
            }

            if (distance < DEAD_ZONE) {
                continue;
            }

            double pull = entity instanceof Player player
                    ? playerPull(player)
                    : strength * (EDGE_PULL + (1.0 - EDGE_PULL) * (1.0 - distance / reach));
            Vec3 motion = entity.getDeltaMovement().add(toCentre.scale(pull / distance));
            if (entity instanceof LivingEntity && !(entity instanceof Player) && entity.onGround()) {
                motion = new Vec3(motion.x, Math.max(motion.y, Mth.clamp(pull * MOB_LIFT, MIN_MOB_LIFT, MAX_MOB_LIFT)), motion.z);
            }

            entity.setDeltaMovement(motion);
            entity.resetFallDistance();

            // A player's own client is what actually moves them, so the server has to say so.
            entity.hurtMarked = true;
        }
    }

    /**
     * A player's pull: {@link #PLAYER_PULL_FRACTION} of the acceleration they would get sprinting in
     * their current state, the same everywhere inside the hole and at every age.
     * <p>
     * On foot that is the movement speed attribute - with the sprint multiplier put on if they are not
     * already sprinting, so Speed and Slowness move the line with them - over the friction of the
     * block underfoot, which is vanilla's own {@code getFrictionInfluencedSpeed}. Flying is the
     * abilities' flying speed, doubled for a sprint; falling or jumping is a sprinting player's air
     * acceleration.
     */
    private static double playerPull(Player player) {
        double escape;
        if (player.getAbilities().flying) {
            escape = player.getAbilities().getFlyingSpeed() * 2.0;
        } else if (player.onGround()) {
            double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
            if (!player.isSprinting()) {
                speed *= SPRINT_MULTIPLIER;
            }

            float friction = player.level().getBlockState(player.getOnPos())
                    .getFriction(player.level(), player.getOnPos(), player);
            escape = speed * (0.21600002F / (friction * friction * friction));
        } else {
            escape = SPRINT_AIR_ACCELERATION;
        }

        return escape * PLAYER_PULL_FRACTION;
    }

    private void swallow(ServerLevel level, Entity entity, boolean bite) {
        if (entity instanceof ItemEntity || entity instanceof ExperienceOrb || entity instanceof FallingBlockEntity
                || entity instanceof Projectile || entity instanceof PrimedTnt) {
            entity.discard();
            return;
        }

        if (!bite) {
            return;
        }

        float damage = SWALLOW_MIN_DAMAGE;
        if (entity instanceof LivingEntity living) {
            damage = Math.max(damage, living.getMaxHealth() * SWALLOW_HEALTH_FRACTION);
        }

        entity.hurt(level.damageSources().source(ModDamageTypes.BLACK_HOLE, this), damage);
    }

    /* EATING */

    /**
     * Eats outwards from wherever the cursor stopped, until the shell reaches the radius this age
     * allows or the tick's budget runs out.
     * <p>
     * A shell is every position whose distance from the middle is at least {@code shell} and less
     * than {@code shell + 1}. Only the columns that can hold such a position inside the build height
     * are walked: a column nearer the middle than {@code sqrt(shellﾂｲ - reachﾂｲ)} has its whole band
     * above the sky or below the floor, which from a radius of a few hundred is nearly all of them.
     */
    private void eat(ServerLevel level) {
        int limit = Mth.floor(eatRadius());
        int budget = POSITIONS_PER_TICK;
        this.thrownThisTick = 0;

        BlockPos centre = blockPosition();
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        int reachSqr = Math.max(Mth.square(centre.getY() - minY), Mth.square(maxY - 1 - centre.getY()));
        List<ServerPlayer> players = level.players();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        while (this.shell <= limit && budget > 0) {
            int inner = this.shell * this.shell;
            int outer = (this.shell + 1) * (this.shell + 1);
            int holeSqr = inner - reachSqr;

            if (this.dx == Integer.MIN_VALUE) {
                this.dx = -this.shell;
                this.dz = Integer.MIN_VALUE;
            }

            if (this.dx > this.shell) {
                this.shell++;
                this.dx = Integer.MIN_VALUE;
                continue;
            }

            int rowSqr = outer - 1 - this.dx * this.dx;
            if (rowSqr < 0) {
                this.dx++;
                this.dz = Integer.MIN_VALUE;
                continue;
            }

            int zOuter = Mth.floor(Math.sqrt(rowSqr));
            int zInner = holeSqr - this.dx * this.dx > 0 ? Mth.ceil(Math.sqrt(holeSqr - this.dx * this.dx)) : 0;

            if (this.dz == Integer.MIN_VALUE) {
                this.dz = -zOuter;
            }

            if (this.dz > zOuter) {
                this.dx++;
                this.dz = Integer.MIN_VALUE;
                continue;
            }

            // Jump the part of the row whose columns are entirely outside the world.
            if (zInner > 0 && this.dz > -zInner && this.dz < zInner) {
                this.dz = zInner;
                continue;
            }

            int x = centre.getX() + this.dx;
            int z = centre.getZ() + this.dz;
            int flatSqr = this.dx * this.dx + this.dz * this.dz;
            this.dz++;
            budget--;

            if (flatSqr >= outer || !level.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) {
                continue;
            }

            int dy = inner > flatSqr ? Mth.ceil(Math.sqrt(inner - flatSqr)) : 0;
            while (dy > 0 && (dy - 1) * (dy - 1) + flatSqr >= inner) {
                dy--;
            }
            while (dy * dy + flatSqr < inner) {
                dy++;
            }

            for (; dy * dy + flatSqr < outer; dy++) {
                budget -= eatAt(level, pos.set(x, centre.getY() + dy, z), minY, maxY, players);
                if (dy > 0) {
                    budget -= eatAt(level, pos.set(x, centre.getY() - dy, z), minY, maxY, players);
                }
            }
        }
    }

    /** One position of a shell: deleted, or now and then thrown in. Returns what it cost the budget. */
    private int eatAt(ServerLevel level, BlockPos.MutableBlockPos pos, int minY, int maxY, List<ServerPlayer> players) {
        if (pos.getY() < minY || pos.getY() >= maxY) {
            return 0;
        }

        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F) {
            return 1;
        }

        if (this.thrownThisTick < THROWN_PER_TICK && state.getFluidState().isEmpty() && !state.hasBlockEntity()
                && level.random.nextInt(8) == 0 && nearPlayer(players, pos.getX(), pos.getY(), pos.getZ())) {
            throwIn(level, pos.immutable(), state);
            this.thrownThisTick++;
        } else {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
        }

        return 1;
    }

    private static boolean nearPlayer(List<ServerPlayer> players, int x, int y, int z) {
        for (ServerPlayer player : players) {
            if (player.distanceToSqr(x + 0.5, y + 0.5, z + 0.5) < THROWN_PLAYER_RANGE * THROWN_PLAYER_RANGE) {
                return true;
            }
        }

        return false;
    }

    /**
     * An eaten block sent in as a falling block, for the look of it. It never lands - a cancelled drop
     * places nothing - and floats rather than falls, so the pull alone steers it to the core, where it
     * is swallowed with everything else.
     */
    private void throwIn(ServerLevel level, BlockPos pos, BlockState state) {
        FallingBlockEntity block = FallingBlockEntity.fall(level, pos, state);
        block.disableDrop();
        block.setNoGravity(true);
        block.dropItem = false;

        Vec3 toCentre = position().subtract(block.position());
        block.setDeltaMovement(toCentre.normalize().scale(0.3));
        // fall() leaves the block's fluid behind; the hole takes that too.
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
    }

    /* STOPPING IT */

    /** Gone, with a noise and a flash. Nothing it ate comes back. */
    public void collapse(ServerLevel level) {
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(), getZ(), 400,
                coreRadius(), coreRadius(), coreRadius(), 0.5);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS,
                6.0F, 0.6F);
        discard();
    }

    /** Whether this player is holding the one thing that can end it. */
    private static boolean holdsStopper(Player player) {
        return player.getMainHandItem().is(ModItems.BLACK_HOLE_STOPPER.get());
    }

    private void stoppedBy(Player player) {
        // Past the point of no return: once the flash has begun, the blast is coming.
        if (level() instanceof ServerLevel level && isAlive() && !flashing()) {
            if (!player.getAbilities().instabuild) {
                player.getMainHandItem().shrink(1);
            }

            collapse(level);
        }
    }

    /**
     * A swing. With the stopper it collapses the hole; with anything else it does nothing at all -
     * returning true is what stops {@code Player.attack} in its tracks, so no weapon is worn and no
     * sweep goes off against something that cannot be hurt.
     */
    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (attacker instanceof Player player && holdsStopper(player)) {
            stoppedBy(player);
        }

        return true;
    }

    /** Using the stopper on it does the same as hitting it with one. */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !holdsStopper(player)) {
            return InteractionResult.PASS;
        }

        stoppedBy(player);
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    /** Invincible: every other way of damaging it is refused. The stopper is not damage. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    /** Room for the halo the renderer draws around the core, and for the flash when there is one. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(this.entityData.get(DATA_FINALE) ? FLASH_RADIUS : coreRadius());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        this.age = compound.getInt(TAG_AGE);
        this.entityData.set(DATA_LIFETIME, compound.contains(TAG_LIFETIME) ? Math.max(1, compound.getInt(TAG_LIFETIME)) : LIFETIME);
        this.finalePower = compound.getFloat(TAG_FINALE_POWER);
        this.entityData.set(DATA_FINALE, this.finalePower > 0.0F);
        this.shell = compound.getInt(TAG_SHELL);
        this.dx = compound.contains(TAG_DX) ? compound.getInt(TAG_DX) : Integer.MIN_VALUE;
        this.dz = compound.contains(TAG_DZ) ? compound.getInt(TAG_DZ) : Integer.MIN_VALUE;
        this.entityData.set(DATA_AGE, this.age);
        setBoundingBox(makeBoundingBox());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt(TAG_AGE, this.age);
        compound.putInt(TAG_LIFETIME, lifetime());
        compound.putFloat(TAG_FINALE_POWER, this.finalePower);
        compound.putInt(TAG_SHELL, this.shell);
        compound.putInt(TAG_DX, this.dx);
        compound.putInt(TAG_DZ, this.dz);
    }
}
